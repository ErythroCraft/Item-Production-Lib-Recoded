package daripher.itemproduction.mixin.minecraft;

import daripher.itemproduction.ItemProductionLib;
import daripher.itemproduction.block.entity.Interactive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlockEntity.class)
public class CampfireBlockEntityMixin {

    /**
     * Injiziert sich ans ENDE von cookTick.
     * Triggert EXAKT in der Sekunde, in der ein Item auf dem Lagerfeuer fertig gebraten wurde.
     */
    @Inject(method = "cookTick", at = @At("TAIL"))
    private static void onCampfireCookingFinished(Level level, BlockPos pos, BlockState state,
                                                  CampfireBlockEntity blockEntity, CallbackInfo ci) {
        if (level == null || level.isClientSide() || blockEntity == null) {
            return;
        }

        if (blockEntity instanceof Interactive interactive) {
            // Sichere Spieler-Ermittlung via UUID (Ohne schwere Umkreissuche im Dauertakt!)
            Player foundPlayer = interactive.resolveUser(level);

            // Fallback: Nur wenn KEINE UUID existiert (z. B. Automatisierung), suchen wir den nächsten Spieler
            if (foundPlayer == null) {
                foundPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 8.0, p -> p instanceof ServerPlayer);
            }

            if (foundPlayer instanceof ServerPlayer serverPlayer) {
                processFinishedCampfireItems(blockEntity, serverPlayer);
            }
        }
    }

    /**
     * Hilfsmethode, um das fertige Item an deine Lib zu übergeben.
     * Nutzt den neuen Accessor, um völlig barrierefrei auf die privaten Arrays zuzugreifen.
     */
    @Unique
    private static void processFinishedCampfireItems(CampfireBlockEntity blockEntity, ServerPlayer player) {
        // Holt die privaten Arrays sauber über das Accessor-Interface
        CampfireBlockEntityAccessor accessor = (CampfireBlockEntityAccessor) blockEntity;
        int[] cookingProgress = accessor.itemproductionGetCookingProgress();
        int[] cookingTime = accessor.itemproductionGetCookingTime();
        NonNullList<ItemStack> items = blockEntity.getItems();

        for (int i = 0; i < items.size(); i++) {
            // Wenn der Fortschritt exakt die maximale Kochzeit erreicht hat
            if (cookingProgress[i] >= cookingTime[i] && cookingTime[i] > 0) {
                ItemStack finishedItem = items.get(i);

                if (!finishedItem.isEmpty()) {
                    int count = finishedItem.getCount();
                    String itemName = finishedItem.getItem().toString();
                    String playerName = player.getName().getString();

                    // Logger füttern
                    daripher.itemproduction.util.DebugLogger.logCookingPotStack(playerName, itemName, count, "SERVER_CAMPFIRE_FINISHED");

                    // Saubere, tag-freie Kopie an deine Lib übergeben
                    ItemProductionLib.itemProduced(finishedItem.copy(), player);

                    daripher.itemproduction.util.DebugLogger.logStackLoopEnd();
                }
            }
        }
    }
}
