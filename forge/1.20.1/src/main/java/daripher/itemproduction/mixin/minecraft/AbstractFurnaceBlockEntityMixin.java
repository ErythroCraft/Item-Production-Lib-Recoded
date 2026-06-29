package daripher.itemproduction.mixin.minecraft;

import daripher.itemproduction.ItemProductionLib;
import daripher.itemproduction.block.entity.Interactive;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    /**
     * Injiziert sich in den Server-Tick des Ofens, um die Geschwindigkeit dynamisch zu berechnen.
     */
    @Inject(method = "serverTick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;cookingProgress:I", ordinal = 1))
    private static void accelerateFurnaceCooking(
            Level level,
            BlockPos pos,
            BlockState state,
            AbstractFurnaceBlockEntity blockEntity,
            CallbackInfo ci) {
        if (level == null || level.isClientSide() || blockEntity == null) {
            return;
        }

        if ((Object) blockEntity instanceof Interactive interactive) {
            Player user = interactive.resolveUser(level);
            if (user instanceof ServerPlayer player) {
                applyFurnaceSpeedBoost(level, blockEntity, player);
            }
        }
    }

    /**
     * Erhöht den Ofen-Fortschritt basierend auf den gelernten Skilltree-Multiplikatoren.
     * Nutzt den Mixin-Accessor, um völlig barrierefrei und ohne Reflection auf das Array zuzugreifen!
     */
    @Unique
    private static void applyFurnaceSpeedBoost(Level level, AbstractFurnaceBlockEntity blockEntity, ServerPlayer player) {
        float speedMultiplier = ItemProductionLib.getProductionSpeedMultiplier(player, "furnace");

        if (speedMultiplier > 1.0f) {
            int tickInterval = (int) (1.0f / (speedMultiplier - 1.0f));

            if (tickInterval > 0 && level.getGameTime() % tickInterval == 0) {
                // Holt das ContainerData-Array sauber über das Accessor-Interface
                ContainerData data = ((AbstractFurnaceBlockEntityAccessor) blockEntity).itemproductionGetDataAccess();

                int currentProgress = data.get(2); // cookingProgress
                int totalTime = data.get(3);       // cookingTotalTime

                if (currentProgress > 0 && currentProgress < totalTime) {
                    data.set(2, currentProgress + 1);
                }
            }
        }
    }
}
