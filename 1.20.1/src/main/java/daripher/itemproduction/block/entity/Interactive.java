package daripher.itemproduction.block.entity;

import javax.annotation.Nullable;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface Interactive {

  @Nullable
  Player getUser();

  void setUser(@Nullable Player player);

  @Nullable
  UUID getUserUUID();

  void setUserUUID(@Nullable UUID uuid);

  default void saveUserNbt(CompoundTag tag) {
    if (getUserUUID() != null) {
      tag.putUUID("ItemProductionUser", getUserUUID());
    }
  }

  default void loadUserNbt(CompoundTag tag, BlockEntity blockEntity) {
    if (tag.hasUUID("ItemProductionUser")) {
      UUID uuid = tag.getUUID("ItemProductionUser");
      setUserUUID(uuid);

      if (getUser() == null && blockEntity.getLevel() instanceof ServerLevel serverLevel) {
        Player player = serverLevel.getServer().getPlayerList().getPlayer(uuid);
        if (player != null) {
          setUser(player);
        }
      }
    }
  }
}
