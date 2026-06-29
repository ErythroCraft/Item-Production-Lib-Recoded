package daripher.itemproduction.mixin.minecraft;

import daripher.itemproduction.block.entity.Interactive;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingStandBlockEntity.class)
public class BrewingStandBlockEntityTrackMixin implements Interactive {

    @Unique
    private Player itemproductionUser = null;

    @Unique
    private UUID itemproductionUserUuid = null;

    @Override public Player getUser() { return this.itemproductionUser; }
    @Override public void setUser(Player player) { this.itemproductionUser = player; }
    @Override public UUID getUserUUID() { return this.itemproductionUserUuid; }
    @Override public void setUserUUID(UUID uuid) { this.itemproductionUserUuid = uuid; }

    /**
     * FIX: Statt startOpen/stillValid klinken wir uns in den serverTick des Braustands ein.
     * Da deine Hauptklasse 'ItemProductionLib' beim Rechtsklick den Spieler bereits registriert,
     * sorgt dieser Tick nur dafür, dass der Server-Cache aktiv bleibt, falls der Spieler das GUI offen hat.
     */
    @Inject(method = "serverTick", at = @At("HEAD"), remap = true)
    private static void onBrewingTickUpdate(Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity blockEntity, CallbackInfo ci) {
        if (level == null || level.isClientSide() || blockEntity == null) {
            return;
        }

        // Falls die UUID geladen wurde, aber der Live-Spieler-Cache durch einen Reload null ist,
        // reaktivieren wir ihn sicher über die resolveUser-Methode
        if (blockEntity instanceof Interactive interactive && interactive.getUser() == null) {
            interactive.resolveUser(level);
        }
    }

    // Die NBT-Speicherung bleibt exakt so wie sie war und ist absolut sinnvoll!
    @Inject(method = "saveAdditional", at = @At("TAIL"), remap = true)
    private void onSave(CompoundTag tag, CallbackInfo ci) {
        this.saveUserNbt(tag);
    }

    @Inject(method = "load", at = @At("TAIL"), remap = true)
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        this.loadUserNbt(tag, (net.minecraft.world.level.block.entity.BlockEntity) (Object) this);
    }
}
