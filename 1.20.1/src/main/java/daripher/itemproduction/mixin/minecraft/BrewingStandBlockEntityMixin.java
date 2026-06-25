package daripher.itemproduction.mixin.minecraft;

import daripher.itemproduction.ItemProductionLib;
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

  private BrewingStandBlockEntityMixin() {
    throw new IllegalStateException("Mixin class cannot be instantiated");
  }

  @Inject(method = "doBrew", at = @At("TAIL"))
  private static void enhanceBrewedPotions(
      Level level,
      BlockPos blockPos,
      NonNullList<ItemStack> itemStacks,
      CallbackInfo callbackInfo) {

    if (level != null && !level.isClientSide()) {
      BlockEntity blockEntity = level.getBlockEntity(blockPos);

      for (int slot = 0; slot < 3; slot++) {
        ItemStack stack = itemStacks.get(slot);

        if (!stack.isEmpty()) {
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
}
