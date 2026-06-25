package daripher.itemproduction.mixin.minecraft;

import daripher.itemproduction.ItemProductionLib;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    /**
     * DER UNZERSTÖRBARE NETZWERK-SICHERER:
     * Fängt jeden Links-, Rechts- oder Shift-Klick exakt beim Herausnehmen ab!
     * Gilt für Werkbank, Steinschneider, Öfen, Kochtopf UND den Braustand.
     */
    @Inject(method = "handleContainerClick", at = @At("HEAD"))
    private void onAnyContainerClickPickup(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (this.player == null || this.player.level().isClientSide()) {
            return;
        }

        AbstractContainerMenu containerMenu = this.player.containerMenu;
        if (containerMenu == null) {
            return;
        }

        String menuClassName = containerMenu.getClass().getName();
        int clickedSlot = packet.getSlotNum();
        int targetSlot = -1;

        // FALL A: Werkbank (Crafting Table) -> Slot 0
        if (containerMenu instanceof CraftingMenu && clickedSlot == 0) {
            targetSlot = 0;
        }
        // FALL B: Steinschneider (Stonecutter) -> Slot 1
        else if (containerMenu instanceof StonecutterMenu && clickedSlot == 1) {
            targetSlot = 1;
        }
        // FALL C: ÖFEN (Furnace, Blast Furnace, Smoker) -> Slot 2
        else if ((containerMenu instanceof FurnaceMenu || containerMenu instanceof BlastFurnaceMenu
                || containerMenu instanceof SmokerMenu) && clickedSlot == 2) {
            targetSlot = 2;
        }
        // FALL D: BRAUSTAND (Brewing Stand) -> Trank-Ausgabeslots sind physisch 0, 1
        // und 2! [^1]
        else if (containerMenu instanceof BrewingStandMenu
                && (clickedSlot == 0 || clickedSlot == 1 || clickedSlot == 2)) {
            targetSlot = clickedSlot;
        }
        // FALL E: Kochtopf (Farmer's Delight) -> Dynamischer Slot-Check
        else if (menuClassName.contains("CookingPot")) {
            targetSlot = clickedSlot;
        }

        // Wenn ein gültiger Ausgabe-Slot real angeklickt wurde:
        if (targetSlot != -1) {
            try {
                ItemStack slotItem = containerMenu.getSlot(targetSlot).getItem();

                if (!slotItem.isEmpty() && !slotItem.getOrCreateTag().contains("SkillTreeProcessed")) {
                    String itemPath = slotItem.getItem().toString();

                    // FILTER-SCHRANKE: Beim Braustand lassen wir NUR echte Tränke (potions /
                    // elixirs) durch!
                    boolean isAllowedItem = containerMenu instanceof CraftingMenu
                            || containerMenu instanceof StonecutterMenu ||
                            containerMenu instanceof FurnaceMenu || containerMenu instanceof BlastFurnaceMenu
                            || containerMenu instanceof SmokerMenu ||
                            (containerMenu instanceof BrewingStandMenu && (itemPath.contains("potion")
                                    || itemPath.contains("elixir") || itemPath.contains("bottle")))
                            ||
                            itemPath.contains("soup") || itemPath.contains("stew") || itemPath.contains("meal")
                            || itemPath.contains("food");

                    if (isAllowedItem) {
                        // Durch deine Library jagen -> Jetzt erfährt der passive Skilltree des
                        // ECHTEN SPIELERS (this.player) vom Pickup, vergibt XP und brennt die Buffs
                        // ein!
                        ItemStack modified = ItemProductionLib.itemProduced(slotItem.copy(), this.player);
                        containerMenu.getSlot(targetSlot).set(modified);
                    }
                }
            } catch (Exception ignored) {
                // Schützt vor Klicks außerhalb des GUI-Rasters
            }
        }
    }
}
