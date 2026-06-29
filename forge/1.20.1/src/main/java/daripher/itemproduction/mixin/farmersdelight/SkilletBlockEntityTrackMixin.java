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
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;

@Mixin(value = SkilletBlockEntity.class, remap = false)
public class SkilletBlockEntityTrackMixin implements Interactive {

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

    /**
     * Klinkt sich in die statische cookingTick-Methode von Farmer's Delight ein.
     * Da die Methode statisch ist, müssen wir die blockEntity-Instanz explizit prüfen.
     */
    @Inject(method = "cookingTick", at = @At("HEAD"))
    private static void onSkilletCookingTickHead(Level level, BlockPos pos, BlockState state, SkilletBlockEntity blockEntity, CallbackInfo ci) {
        if (level == null || level.isClientSide() || blockEntity == null) {
            return;
        }

        // KORREKTUR: Sicherer Cast auf das Interface über die übergebene BlockEntity-Instanz
        if ((Object) blockEntity instanceof Interactive interactive && interactive.getUser() == null) {
            interactive.resolveUser(level);
        }
    }

    /**
     * KORREKTUR: remap = true gesetzt, da saveAdditional eine Minecraft-Methode ist.
     */
    @Inject(method = "saveAdditional", at = @At("TAIL"), remap = true)
    private void onSave(CompoundTag tag, CallbackInfo ci) {
        this.saveUserNbt(tag);
    }

    /**
     * KORREKTUR: remap = true gesetzt, da load eine Minecraft-Methode ist.
     */
    @Inject(method = "load", at = @At("TAIL"), remap = true)
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        this.loadUserNbt(tag, (net.minecraft.world.level.block.entity.BlockEntity) (Object) this);
    }
}
