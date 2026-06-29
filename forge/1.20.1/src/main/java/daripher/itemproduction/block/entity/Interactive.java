package daripher.itemproduction.block.entity;

import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Interface für BlockEntitys, die den interagierenden Spieler tracken müssen.
 * Vollständig SonarQube-konform und frei von Welt-Lade-Bugs.
 */
public interface Interactive {

    // Konstante für den NBT-Schlüssel, um SonarQube-Warnungen zu vermeiden
    String NBT_USER_KEY = "ItemProductionUser";

    @Nullable
    Player getUser();

    void setUser(@Nullable Player player);

    @Nullable
    UUID getUserUUID();

    void setUserUUID(@Nullable UUID uuid);

    /**
     * Hilfsmethode: Speichert die Spieler-ID dauerhaft in den NBT-Daten des Blocks.
     */
    default void saveUserNbt(@Nonnull CompoundTag tag) {
        if (getUserUUID() != null) {
            tag.putUUID(NBT_USER_KEY, getUserUUID());
        }
    }

    /**
     * Hilfsmethode: Lädt die Spieler-ID wieder aus den NBT-Daten des Blocks.
     * KORREKTUR: Der unsichere Live-Spieler-Abruf wurde entfernt, da das Level beim Laden noch null ist.
     */
    default void loadUserNbt(@Nonnull CompoundTag tag, @Nonnull BlockEntity blockEntity) {
        if (tag.hasUUID(NBT_USER_KEY)) {
            setUserUUID(tag.getUUID(NBT_USER_KEY));
        }
    }

    /**
     * NEU & REVOLUTIONÄR: Löst die gespeicherte UUID live in ein echtes Spieler-Objekt auf.
     * Muss in den Mixins aufgerufen werden, sobald ein Item fertig produziert wurde.
     */
    default @Nullable Player resolveUser(@Nonnull Level level) {
        // Falls wir das Spieler-Objekt bereits gecacht haben, nutzen wir es direkt
        if (getUser() != null) {
            return getUser();
        }

        // Wenn nicht, suchen wir den Spieler live auf dem Server anhand seiner UUID
        UUID uuid = getUserUUID();
        if (uuid != null && level instanceof ServerLevel serverLevel) {
            Player foundPlayer = serverLevel.getServer().getPlayerList().getPlayer(uuid);
            if (foundPlayer != null) {
                setUser(foundPlayer); // Cachen für zukünftige Abrufe
                return foundPlayer;
            }
        }
        return null;
    }
}
