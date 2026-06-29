package daripher.itemproduction.mixin.farmersdelight;

import daripher.itemproduction.ItemProductionLib;
import daripher.itemproduction.block.entity.Interactive;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable; // KORREKTUR: Import geändert

// 1. Klasse direkt importieren
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

// 2. KORREKTUR: "value" statt "targets" verwenden
@Mixin(value = CookingPotBlockEntity.class, remap = false)
public abstract class CookingPotBlockEntityMixin {

    // KORREKTUR: Nutzt CallbackInfoReturnable<Boolean>, da die Originalmethode einen Boolean zurückgibt
    @Inject(method = "processCooking", at = @At("TAIL"))
    private void onCookingFinished(CallbackInfoReturnable<Boolean> cir) {
        net.minecraft.world.level.block.entity.BlockEntity blockEntity = (net.minecraft.world.level.block.entity.BlockEntity) (Object) this;
        Level level = blockEntity.getLevel();

        if (level == null || level.isClientSide()) {
            return;
        }

        // 1. Hol das fertige Essen über die originale Farmer's Delight Methode
        ItemStack finishedFood = this.getMeal();

        if (finishedFood.isEmpty()) {
            return;
        }

        // 2. Greife über das neue Interactive-Interface sicher auf den Spieler zu
        Player foundPlayer = null;
        if ((Object) this instanceof Interactive interactive) {
            foundPlayer = interactive.resolveUser(level);
        }

        ServerPlayer targetPlayer = null;
        if (foundPlayer instanceof ServerPlayer serverPlayer) {
            targetPlayer = serverPlayer;
        }

        // FALLBACK: Falls der Topf automatisiert befüllt wurde (z.B. Pipes/Hopper), nimm den nächsten Spieler
        if (targetPlayer == null) {
            BlockPos pos = blockEntity.getBlockPos();
            Player closestPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 8.0, false);
            if (closestPlayer instanceof ServerPlayer serverPlayer) {
                targetPlayer = serverPlayer;
            }
        }

        // 3. Wenn ein gültiger Spieler gefunden wurde, führe die Produktion aus
        if (targetPlayer != null) {
            int count = finishedFood.getCount();
            String itemName = finishedFood.getItem().toString();
            String playerName = targetPlayer.getName().getString();

            // Logger mit dem neuen Event-Typ füttern
            daripher.itemproduction.util.DebugLogger.logCookingPotStack(playerName, itemName, count, "SERVER_RECIPE_FINISHED");

            // Übergibt eine saubere Kopie des Stacks ohne NBT-Tags an deine Hauptbibliothek
            ItemProductionLib.itemProduced(finishedFood.copy(), targetPlayer);

            daripher.itemproduction.util.DebugLogger.logStackLoopEnd();
        }
    }

    // Shadow-Verweis auf das originale Farmer's Delight Repository, um das Essen auszulesen
    @Shadow
    public abstract ItemStack getMeal();
}
