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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Importiert die originale Farmer's Delight Klasse
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;

@Mixin(value = SkilletBlockEntity.class, remap = false)
public abstract class SkilletBlockEntityMixin {

    // Shadow-Verweis auf die originale Methode von Farmer's Delight, um an das Item zu kommen
    @Shadow
    public abstract ItemStack getStoredStack();

    /**
     * KORREKTUR: Wir injizieren uns am Ende von 'finishCooking'.
     * Diese Methode wird von Farmer's Delight genau dann aufgerufen, wenn ein Bratvorgang 
     * erfolgreich abgeschlossen wurde – egal ob einzelnes Item oder Teil eines Stacks!
     */
    @Inject(method = "finishCooking", at = @At("TAIL"))
    private void onSkilletCookingFinished(Level level, CallbackInfo ci) {
        if (level == null || level.isClientSide()) {
            return;
        }

        // 1. Hole das frisch fertiggestellte Essen (bzw. den aktuellen Stack) aus der Pfanne
        ItemStack finishedFood = this.getStoredStack();
        if (finishedFood.isEmpty()) {
            return;
        }

        // Das betroffene BlockEntity für Positionsdaten holen
        net.minecraft.world.level.block.entity.BlockEntity blockEntity = (net.minecraft.world.level.block.entity.BlockEntity) (Object) this;
        BlockPos pos = blockEntity.getBlockPos();

        // 2. Greife über das Interactive-Interface sicher auf den Spieler zu
        Player foundPlayer = null;
        if ((Object) this instanceof Interactive interactive) {
            foundPlayer = interactive.resolveUser(level);
        }

        ServerPlayer targetPlayer = null;
        if (foundPlayer instanceof ServerPlayer serverPlayer) {
            targetPlayer = serverPlayer;
        }

        // FALLBACK: Falls die Pfanne automatisiert befüllt wurde (z.B. Pipes/Hopper), nimm den nächsten Spieler
        if (targetPlayer == null) {
            Player closestPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 8.0, false);
            if (closestPlayer instanceof ServerPlayer serverPlayer) {
                targetPlayer = serverPlayer;
            }
        }

        // 3. Übergabe an Ihre Hauptbibliothek
        if (targetPlayer != null) {
            // Da ein Stack verarbeitet wird, übergeben wir eine Kopie mit der korrekten Anzahl des aktuellen Ergebnisses
            int count = finishedFood.getCount();
            String itemName = finishedFood.getItem().toString();
            String playerName = targetPlayer.getName().getString();

            // Logger füttern
            daripher.itemproduction.util.DebugLogger.logCookingPotStack(playerName, itemName, count, "SERVER_SKILLET_RECIPE_FINISHED");

            // Reicht eine saubere Kopie des fertigen Essens ohne störende NBT-Tags weiter
            ItemProductionLib.itemProduced(finishedFood.copy(), targetPlayer);

            daripher.itemproduction.util.DebugLogger.logStackLoopEnd();
        }
    }
}
