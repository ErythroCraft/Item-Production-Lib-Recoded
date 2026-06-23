package daripher.itemproduction.block.entity;

import javax.annotation.Nullable;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public interface Interactive {

  @Nullable
  Player getUser();

  void setUser(@Nullable Player player);

  @Nullable
  UUID getUserUUID();

  void setUserUUID(@Nullable UUID uuid);

  /**
   * Hilfsmethode: Speichert die Spieler-ID dauerhaft in den NBT-Daten des Blocks
   * (beim Weltspeichern).
   */
  default void saveUserNbt(CompoundTag tag) {
    if (getUserUUID() != null) {
      tag.putUUID("ItemProductionUser", getUserUUID());
    }
  }

  /**
   * Hilfsmethode: Lädt die Spieler-ID wieder aus den NBT-Daten des Blocks (beim
   * Weltladen).
   */
  default void loadUserNbt(CompoundTag tag) {
    if (tag.hasUUID("ItemProductionUser")) {
      setUserUUID(tag.getUUID("ItemProductionUser"));
    }
  }
}
