package daripher.itemproduction.mixin.minecraft;

import daripher.itemproduction.ItemProductionLib;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slot.class)
public abstract class SlotMixin {

    @Shadow
    public int index;

    /**
     * Klinkt sich in die 'onTake'-Methode JEDES Slots ein.
     * Filtert gezielt die verbleibenden Vanilla-Ausgabelots heraus.
     * Vollständig befreit von Item-Tags für perfektes Gegenstands-Stacking!
     */
    @Inject(method = "onTake", at = @At("HEAD"))
    private void onTakeItemFromOutputSlot(Player player, ItemStack stack, CallbackInfo ci) {
        // Nur auf dem Server verarbeiten, wenn das Item nicht leer ist
        if (player == null || player.level().isClientSide() || stack.isEmpty() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        AbstractContainerMenu containerMenu = player.containerMenu;
        if (containerMenu == null) {
            return;
        }

        int slotId = this.index;

        // Wir prüfen nur noch Menüs, die wir NICHT bereits über spezifische Mixins abgefangen haben.
        // Kochtopf, Pfanne, Braustand, Schmiedetisch und Werkbänke sind bereits autark und hier tabu!
        if (isRemainingVanillaOutputSlot(containerMenu, slotId)) {
            try {
                int count = stack.getCount();
                String itemName = stack.getItem().toString();
                String playerName = serverPlayer.getName().getString();

                // Logger füttern
                daripher.itemproduction.util.DebugLogger.logCookingPotStack(playerName, itemName, count, "PLAYER_UI_PRODUCTION_FINISHED");

                // Übergibt eine saubere Kopie des Items an deine Lib.
                // Das Item im Slot bleibt komplett tag-frei und stapelt sich perfekt im Inventar!
                ItemProductionLib.itemProduced(stack.copy(), serverPlayer);

                daripher.itemproduction.util.DebugLogger.logStackLoopEnd();
            } catch (Exception e) {
                org.apache.logging.log4j.LogManager.getLogger("ItemProductionLib")
                        .warn("[UI-FEHLER] Fehler bei der Verarbeitung im Slot {}: {}", slotId, e.getMessage());
            }
        }
    }

    /**
     * Prüft, ob es sich um den Ausgangsslot der verbleibenden Vanilla-Menüs handelt.
     */
    @Unique
    private boolean isRemainingVanillaOutputSlot(AbstractContainerMenu containerMenu, int slotId) {
        return (containerMenu instanceof AnvilMenu && slotId == 2)
                || (containerMenu instanceof GrindstoneMenu && slotId == 2)
                || (containerMenu instanceof CartographyTableMenu && slotId == 2)
                || (containerMenu instanceof EnchantmentMenu && slotId == 0);
    }
}
