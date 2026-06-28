package daripher.itemproduction.mixin.minecraft;

import daripher.itemproduction.ItemProductionLib;
import daripher.itemproduction.util.ItemProcessingHelper;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.StonecutterBlock;
import se.mickelus.tetra.items.forged.StonecutterItem;

import org.antlr.v4.misc.EscapeSequenceParsing.Result;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slot.class)
public abstract class SlotMixin {

    @Shadow
    public abstract int getSlotIndex();

    @Unique
    private static long lastProcessedGameTime = -1L;

    @Unique
    private static int lastProcessedSlotId = -1;

    /**
     * Injects into the onTake method of any slot to intercept item creation safely.
     */
    /**
     * Injects into the onTake method of any slot to intercept item creation safely.
     */
    @Inject(method = "onTake", at = @At("HEAD"))
    private void onTakeItemFromOutputSlot(Player player, ItemStack stack, CallbackInfo ci) {
        if (player == null || player.level().isClientSide() || stack.isEmpty()) {
            return;
        }

        Slot instance = (Slot) (Object) this;
        AbstractContainerMenu containerMenu = player.containerMenu;
        if (containerMenu == null) {
            return;
        }

        // 1. Variablen sauber definieren
        int slotId = this.getSlotIndex();
        String menuClassName = containerMenu.getClass().getName();

        // 2. Deine Test-Logzeile (Nutzt jetzt die korrekt deklarierten Variablen)
        // Hinweis: Falls "LOGGER" in dieser Klasse nicht existiert, nutze System.out.println
        System.out.println("[ItemProductionLib-TEST] Klick auf Slot: " + slotId + " im Menü: " + menuClassName);

        // Passive click converter: Immediately stamp old items in chests when clicked
        convertOldItemsOnClick(containerMenu, slotId, player);

        // Debounce filter: Prevents double triggering within the same server tick
        long currentTick = player.level().getGameTime();
        if (currentTick == lastProcessedGameTime && slotId == lastProcessedSlotId) {
            return;
        }

        boolean isOutputSlot = false;

        // FIXED java:S1871: Aufteilung in klare logische Kategorien
        boolean isResultSlotType = instance instanceof ResultSlot
                || instance instanceof FurnaceResultSlot
                || instance instanceof MerchantResultSlot
                || instance.getClass().getName().contains("ResultSlot");

        boolean isVanillaOutputSlot = (containerMenu instanceof SmithingMenu && slotId == 2)
                || (containerMenu instanceof AnvilMenu && slotId == 2)
                || (containerMenu instanceof GrindstoneMenu && slotId == 2)
                || (containerMenu instanceof CartographyTableMenu && slotId == 2)
                || (containerMenu instanceof EnchantmentMenu && slotId == 0)
                || (containerMenu instanceof BrewingStandMenu && (slotId == 0 || slotId == 1 || slotId == 2));

        // FIXED java:S1872: Nutzen von Class.isInstance() statt instof/String-Vergleich
        boolean isModOutputSlot = false;
        try {
            Class<?> cookingPotMenuClass = Class.forName("vectorwing.farmersdelight.common.block.entity.container.CookingPotMenu");
            if (cookingPotMenuClass.isInstance(containerMenu) && slotId == 8) {
                isModOutputSlot = true;
            }
        } catch (ClassNotFoundException e) {
            // Farmer's Delight nicht geladen, Bedingung bleibt false
        }

        // Kategorien kombinieren
        if (isResultSlotType || isVanillaOutputSlot || isModOutputSlot) {
            isOutputSlot = true;
        }

    }

    /**
     * FIXED java:S2696: Thread-safe static helper method to update the debounce variables.
     */
    @Unique
    private static synchronized void updateDebounceTracker(long currentTick, int slotId) {
        lastProcessedGameTime = currentTick;
        lastProcessedSlotId = slotId;
    }


    /**
     * FIXED java:S117: Renamed to match the required camelCase regular expression.
     */
    @Unique
    private void convertOldItemsOnClick(AbstractContainerMenu menu, int clickedSlot, Player player) {
        try {
            if (clickedSlot < 0 || clickedSlot >= menu.slots.size()) {
                return;
            }

            ItemStack clickedItem = menu.getSlot(clickedSlot).getItem();
            if (clickedItem.isEmpty() || ItemProcessingHelper.isProcessed(clickedItem)) {
                return;
            }

            if (isGeneralTargetItem(clickedItem)) {
                // Now correctly burns the player's name into old items upon interaction
                ItemProcessingHelper.markAsProcessed(clickedItem, player);
            }
        } catch (Exception ignored) {
            // Protects against invalid slot indices
        }
    }


    @Unique
    private boolean isGeneralTargetItem(ItemStack stack) {
        String itemPath = stack.getItem().toString();
        return itemPath.contains("soup")
                || itemPath.contains("stew")
                || itemPath.contains("meal")
                || itemPath.contains("food")
                || itemPath.contains("potion")
                || itemPath.contains("elixir")
                || itemPath.contains("bottle");
    }
}
