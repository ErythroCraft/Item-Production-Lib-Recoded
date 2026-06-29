package daripher.itemproduction.mixin.minecraft;

import daripher.itemproduction.ItemProductionLib;
import daripher.itemproduction.block.entity.Interactive; // Das neue Interface importieren
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

import javax.annotation.Nonnull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {

    /**
     * Injiziert sich am Ende des Vanilla-Brauvorgangs (TAIL).
     * Komplett befreit von Item-Tags für perfektes Trank-Stacking.
     */
    @Inject(method = "doBrew", at = @At("TAIL"))
    private static void enhanceBrewedPotions(
            Level level,
            @Nonnull BlockPos blockPos,
            NonNullList<ItemStack> itemStacks,
            CallbackInfo callbackInfo) {

        if (level == null || level.isClientSide()) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity == null) {
            return;
        }

        // 1. Greife über das neue Interactive-Interface sicher auf den Spieler zu
        Player foundPlayer = null;
        if (blockEntity instanceof Interactive interactive) {
            foundPlayer = interactive.resolveUser(level);
        }

        ServerPlayer targetPlayer = null;
        if (foundPlayer instanceof ServerPlayer serverPlayer) {
            targetPlayer = serverPlayer;
        }

        // FALLBACK: Falls automatisiert gebraut wurde (z.B. Hopper befüllt), nimm den nächsten Spieler
        if (targetPlayer == null) {
            Player closestPlayer = level.getNearestPlayer(blockPos.getX(), blockPos.getY(), blockPos.getZ(), 8.0, false);
            if (closestPlayer instanceof ServerPlayer serverPlayer) {
                targetPlayer = serverPlayer;
            }
        }

        // 2. Wenn ein Spieler zugeordnet werden konnte, verarbeite die 3 Trank-Ausgangsslots
        if (targetPlayer != null) {
            // Slots 0, 1 und 2 sind bei Minecraft-Brauständen traditionell die Trank-Ausgänge
            for (int slot = 0; slot < 3; slot++) {
                ItemStack stack = itemStacks.get(slot);

                if (!stack.isEmpty()) {
                    int count = stack.getCount();
                    String itemName = stack.getItem().toString();
                    String playerName = targetPlayer.getName().getString();

                    // Logger füttern
                    daripher.itemproduction.util.DebugLogger.logCookingPotStack(playerName, itemName, count, "SERVER_VANILLA_BREWING_FINISHED");

                    // Übergibt eine saubere Kopie des Tranks an deine Lib. 
                    // Die Trankflasche im Slot bleibt komplett tag-frei und perfekt stapelbar!
                    ItemProductionLib.itemProduced(stack.copy(), targetPlayer);

                    daripher.itemproduction.util.DebugLogger.logStackLoopEnd();
                }
            }
        }

        blockEntity.setChanged();
    }
}
