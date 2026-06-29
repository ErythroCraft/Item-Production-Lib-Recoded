package daripher.itemproduction.mixin.minecraft;

import daripher.itemproduction.block.entity.Interactive;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public class BlockEntityMixin implements Interactive {

  @Unique
  private @Nullable Player itemProductionPlayer;
  @Unique
  private @Nullable UUID itemProductionPlayerUUID;

  @Override
  public @Nullable Player getUser() {
    return this.itemProductionPlayer;
  }

  @Override
  public void setUser(@Nullable Player player) {
    this.itemProductionPlayer = player;
    if (player != null) {
      this.itemProductionPlayerUUID = player.getUUID();
    }
  }

  @Override
  public @Nullable UUID getUserUUID() {
    return this.itemProductionPlayerUUID;
  }

  @Override
  public void setUserUUID(@Nullable UUID uuid) {
    this.itemProductionPlayerUUID = uuid;
  }

  @Inject(method = "saveAdditional", at = @At("TAIL"))
  private void injectSaveUserNbt(@Nonnull CompoundTag tag, CallbackInfo ci) {
    this.saveUserNbt(tag);
  }

  @Inject(method = "load", at = @At("TAIL"))
  private void injectLoadUserNbt(@Nonnull CompoundTag tag, CallbackInfo ci) {
    // KORREKTUR: Wir übergeben 'this' (als BlockEntity) an die neue
    // loadUserNbt-Methode.
    // Damit kann das Interface nach dem Weltladen den Spieler sicher über die UUID
    // reaktivieren.
    this.loadUserNbt(tag, (BlockEntity) (Object) this);
  }
}
