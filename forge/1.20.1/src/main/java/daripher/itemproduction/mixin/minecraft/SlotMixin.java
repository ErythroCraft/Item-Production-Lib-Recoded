package daripher.itemproduction.mixin.minecraft;

import daripher.itemproduction.ItemProductionLib;
import daripher.itemproduction.util.ItemProcessingHelper;
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

        int slotId = this.getSlotIndex();

        // Passive click converter: Immediately stamp old items in chests when clicked
        convertOldItemsOnClick(containerMenu, slotId, player);

        // Debounce filter: Prevents double triggering within the same server tick
        long currentTick = player.level().getGameTime();
        if (currentTick == lastProcessedGameTime && slotId == lastProcessedSlotId) {
            return;
        }

        boolean isOutputSlot = false;
        String menuClassName = containerMenu.getClass().getName();

        // FIXED java:S1871: Splitting the conditions into clear logical categories
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

        boolean isModOutputSlot = menuClassName.contains("CookingPot") && slotId == 8;

        // Combine categories to assign the flag without block duplication
        if (isResultSlotType || isVanillaOutputSlot || isModOutputSlot) {
            isOutputSlot = true;
        }

        // Process output logic using your clean helper class
        if (isOutputSlot && !ItemProcessingHelper.isProcessed(stack)) {
            // BEHEBT DEN FEHLER: Aufruf der statischen Methode statt direktem Setzen
            updateDebounceTracker(currentTick, slotId);

            ItemProcessingHelper.markAsProcessed(stack, player);
            ItemProductionLib.itemProduced(stack, player);
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
