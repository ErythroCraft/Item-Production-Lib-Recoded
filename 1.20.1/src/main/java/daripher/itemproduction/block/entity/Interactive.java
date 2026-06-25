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

  /**
   * Hilfsmethode: Speichert die Spieler-ID dauerhaft in den NBT-Daten des Blocks.
   */
  default void saveUserNbt(CompoundTag tag) {
    if (getUserUUID() != null) {
      tag.putUUID("ItemProductionUser", getUserUUID());
    }
  }

  /**
   * Hilfsmethode: Lädt die Spieler-ID wieder aus den NBT-Daten des Blocks.
   * KORREKTUR: Versucht nach dem Weltladen automatisch, den echten Spieler auf
   * dem Server
   * anhand der gespeicherten UUID wiederzufinden, falls das Player-Objekt null
   * ist.
   */
  default void loadUserNbt(CompoundTag tag, BlockEntity blockEntity) {
    if (tag.hasUUID("ItemProductionUser")) {
      UUID uuid = tag.getUUID("ItemProductionUser");
      setUserUUID(uuid);

      // Falls wir auf dem Server sind und das direkte Spieler-Objekt noch fehlt,
      // holen wir uns den Spieler live aus der Welt zurück!
      if (getUser() == null && blockEntity.getLevel() instanceof ServerLevel serverLevel) {
        Player player = serverLevel.getServer().getPlayerList().getPlayer(uuid);
        if (player != null) {
          setUser(player);
        }
      }
    }
  }
}
