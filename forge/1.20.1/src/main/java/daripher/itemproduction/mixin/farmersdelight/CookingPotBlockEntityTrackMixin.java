package daripher.itemproduction.mixin.farmersdelight;

import daripher.itemproduction.block.entity.Interactive;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Importiert die originale Farmer's Delight Klasse
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

@Mixin(value = CookingPotBlockEntity.class, remap = false)
public class CookingPotBlockEntityTrackMixin implements Interactive {

    @Unique
    private Player itemproductionUser = null;

    @Unique
    private UUID itemproductionUserUuid = null;

    @Override
    public Player getUser() {
        return this.itemproductionUser;
    }

    @Override
    public void setUser(Player player) {
        this.itemproductionUser = player;
    }

    @Override
    public UUID getUserUUID() {
        return this.itemproductionUserUuid;
    }

    @Override
    public void setUserUUID(UUID uuid) {
        this.itemproductionUserUuid = uuid;
    }

    /**
     * FIX: Statt startOpen/stillValid nutzen wir die genuine Server-Tick-Methode des Kochtopfs (cookingTick).
     * Da die Hauptklasse 'ItemProductionLib' die Daten beim Rechtsklick bereits einspeist,
     * sorgt dieser Tick nur dafür, dass der Spieler-Cache nach Server-Neustarts live reaktiviert wird.
     */
    @Inject(method = "cookingTick", at = @At("HEAD"))
    private static void onCookingTickUpdate(Level level, BlockPos pos, BlockState state, CookingPotBlockEntity blockEntity, CallbackInfo ci) {
        if (level == null || level.isClientSide() || blockEntity == null) {
            return;
        }

        // Falls die UUID geladen wurde, aber das direkte Spieler-Objekt im Cache null ist,
        // reaktivieren wir es sicher über die resolveUser-Methode aus deinem Interface!
        if (blockEntity instanceof Interactive interactive && interactive.getUser() == null) {
            interactive.resolveUser(level);
        }
    }

    // Die NBT-Speicherung bleibt exakt so wie sie war und sichert die UUID in den Weltdaten
    @Inject(method = "saveAdditional", at = @At("TAIL"), remap = true)
    private void onSave(CompoundTag tag, CallbackInfo ci) {
        this.saveUserNbt(tag);
    }

    @Inject(method = "load", at = @At("TAIL"), remap = true)
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        this.loadUserNbt(tag, (net.minecraft.world.level.block.entity.BlockEntity) (Object) this);
    }
}
