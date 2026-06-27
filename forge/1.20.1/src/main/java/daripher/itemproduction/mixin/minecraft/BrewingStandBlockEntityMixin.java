package daripher.itemproduction.mixin.minecraft;

import daripher.itemproduction.ItemProductionLib;
import daripher.itemproduction.util.ItemProcessingHelper; // Neu: Hilfsklasse importieren
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {

    /**
     * Injects at the end of the vanilla brewing process (TAIL).
     */
    @Inject(method = "doBrew", at = @At("TAIL"))
    private static void enhanceBrewedPotions(
            Level level,
            BlockPos blockPos,
            NonNullList<ItemStack> itemStacks,
            CallbackInfo callbackInfo) {

        if (level == null || level.isClientSide()) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(blockPos);

        for (int slot = 0; slot < 3; slot++) {
            ItemStack stack = itemStacks.get(slot);

            // SAFETY FIX: Only process if the item is not empty and has NOT been processed yet
            // We do NOT call markAsProcessed here, leaving the player stamping to the SlotMixin!
            if (!stack.isEmpty() && !ItemProcessingHelper.isProcessed(stack)) {
                ItemStack modified = ItemProductionLib.itemProduced(stack.copy(), blockEntity);

                stack.setTag(modified.getTag());
                stack.setCount(modified.getCount());
            }
        }

        if (blockEntity != null) {
            blockEntity.setChanged();
        }
    }
}
