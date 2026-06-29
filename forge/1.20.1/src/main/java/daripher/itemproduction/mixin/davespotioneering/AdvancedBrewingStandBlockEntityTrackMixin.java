package daripher.itemproduction.mixin.davespotioneering;

import daripher.itemproduction.block.entity.Interactive;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 1. Importiere die Klasse direkt aus Dave's Potioneering
import tfar.davespotioneering.blockentity.AdvancedBrewingStandBlockEntity;

// 2. KORREKTUR: Nutze "value" statt "targets" und setze remap auf false
@Mixin(value = AdvancedBrewingStandBlockEntity.class, remap = false)
public class AdvancedBrewingStandBlockEntityTrackMixin implements Interactive {

    @Unique
    private Player itemproductionUser = null;

    @Unique
    private UUID itemproductionUserUuid = null;

    // --- INTERFACE IMPLEMENTIERUNG (Getter & Setter) ---
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

    // FIX: remap = false gesetzt und Descriptor durch Player-Parameter eindeutig definiert
    @Inject(method = "startOpen", at = @At("HEAD"), remap = false)
    private void onStartOpen(Player player, CallbackInfo ci) {
        if (player != null && !player.level().isClientSide()) {
            this.itemproductionUser = player;
            this.itemproductionUserUuid = player.getUUID();
        }
    }

    // FIX: remap = false gesetzt und CallbackInfoReturnable<Boolean> genutzt, da stillValid ein Boolean liefert!
    @Inject(method = "stillValid", at = @At("HEAD"), remap = false)
    private void onStillValid(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player != null && !player.level().isClientSide()) {
            this.itemproductionUser = player;
            this.itemproductionUserUuid = player.getUUID();
        }
    }

    // Schreibt die UUID über die standardisierte Interface-Logik in die Speicherdatei (remap = false)
    @Inject(method = "saveAdditional", at = @At("TAIL"), remap = false)
    private void onSave(CompoundTag tag, CallbackInfo ci) {
        this.saveUserNbt(tag);
    }

    // Lädt die UUID über die standardisierte Interface-Logik ein (remap = false)
    @Inject(method = "load", at = @At("TAIL"), remap = false)
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        this.loadUserNbt(tag, (net.minecraft.world.level.block.entity.BlockEntity) (Object) this);
    }
}
