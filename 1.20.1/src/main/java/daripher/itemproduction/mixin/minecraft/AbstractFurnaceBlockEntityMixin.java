package daripher.itemproduction.mixin.minecraft;

import daripher.itemproduction.ItemProductionLib;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

  protected AbstractFurnaceBlockEntityMixin() {
  }

  @Inject(method = "serverTick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;cookingProgress:I", ordinal = 1))
  private static void accelerateFurnaceCooking(
      net.minecraft.world.level.Level level,
      net.minecraft.core.BlockPos pos,
      net.minecraft.world.level.block.state.BlockState state,
      AbstractFurnaceBlockEntity blockEntity,
      CallbackInfo ci) {

    if (level == null || level.isClientSide()) {
      return;
    }

    if (blockEntity instanceof daripher.itemproduction.block.entity.Interactive interactive) {
      net.minecraft.world.entity.player.Player user = interactive.getUser();

      if (user instanceof ServerPlayer player) {
        applyFurnaceSpeedBoost(level, blockEntity, player);
      }
    }
  }

  private static void applyFurnaceSpeedBoost(net.minecraft.world.level.Level level,
      AbstractFurnaceBlockEntity blockEntity, ServerPlayer player) {
    float speedMultiplier = ItemProductionLib.getProductionSpeedMultiplier(player, "furnace");

    if (speedMultiplier > 1.0f) {
      int tickInterval = (int) (1.0f / (speedMultiplier - 1.0f));

      if (tickInterval > 0 && level.getGameTime() % tickInterval == 0) {
        try {
          int currentProgress = net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(
              AbstractFurnaceBlockEntity.class, blockEntity, "cookingProgress");
          int totalTime = net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(
              AbstractFurnaceBlockEntity.class, blockEntity, "cookingTotalTime");

          if (currentProgress > 0 && currentProgress < totalTime) {
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(
                AbstractFurnaceBlockEntity.class, blockEntity, currentProgress + 1, "cookingProgress");
          }
        } catch (Exception ignored) {
          // NOSONAR
        }
      }
    }
  }
}
