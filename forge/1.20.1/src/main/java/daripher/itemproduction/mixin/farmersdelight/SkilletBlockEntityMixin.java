package daripher.itemproduction.mixin.farmersdelight;

import daripher.itemproduction.ItemProductionLib;
import daripher.itemproduction.block.entity.Interactive;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;

@Mixin(value = SkilletBlockEntity.class, remap = false)
public abstract class SkilletBlockEntityMixin {

    @Shadow
    public abstract ItemStack getStoredStack();

    @Unique
    private static int itemproductionLastCount = 0;

    @Unique
    private static ItemStack itemproductionLastItem = ItemStack.EMPTY;

    /**
     * Wir klinken uns am ANFANG des Ticks ein, um den aktuellen Inhalt zu sichern.
     */
    @Inject(method = "cookingTick", at = @At("HEAD"))
    private static void onCookingTickHead(Level level, BlockPos pos, BlockState state, SkilletBlockEntity blockEntity, CallbackInfo ci) {
        if (level == null || level.isClientSide() || blockEntity == null) {
            return;
        }
        ItemStack current = blockEntity.getStoredStack();
        itemproductionLastCount = current.getCount();
        itemproductionLastItem = current.copy();
    }

    /**
     * Wir klinken uns am ENDE des Ticks ein. 
     * Wenn das rohe Item zu einem fertigen Item wurde, schlagen wir zu!
     */
    @Inject(method = "cookingTick", at = @At("TAIL"))
    private static void onCookingTickTail(Level level, BlockPos pos, BlockState state, SkilletBlockEntity blockEntity, CallbackInfo ci) {
        if (level == null || level.isClientSide() || blockEntity == null) {
            return;
        }

        ItemStack currentStack = blockEntity.getStoredStack();
        if (currentStack.isEmpty()) {
            return;
        }

        // Logik: Ein Item wurde fertig gebraten, wenn sich das Item im Slot verändert hat 
        // (z.B. von roh zu gekocht) ODER wenn der Stack an fertigem Essen größer geworden ist!
        boolean itemChanged = !ItemStack.isSameItem(itemproductionLastItem, currentStack) && !itemproductionLastItem.isEmpty();
        boolean countIncreased = currentStack.getCount() > itemproductionLastCount;

        if (itemChanged || countIncreased) {
            // Sicherstellen, dass es kein rohes Item mehr ist
            if (currentStack.getItem().isEdible() && !currentStack.getDescriptionId().contains("raw")) {

                // Spieler über das funktionierende Interface holen
                Player foundPlayer = null;
                if ((Object) blockEntity instanceof Interactive interactive) {
                    foundPlayer = interactive.resolveUser(level);
                }

                ServerPlayer targetPlayer = null;
                if (foundPlayer instanceof ServerPlayer serverPlayer) {
                    targetPlayer = serverPlayer;
                } else {
                    // Fallback Umkreis
                    Player closestPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 8.0, false);
                    if (closestPlayer instanceof ServerPlayer serverPlayer) {
                        targetPlayer = serverPlayer;
                    }
                }

                if (targetPlayer != null) {
                    // Wir jagen eine 1er-Kopie durch deine funktionierende Library
                    ItemStack singleCooked = currentStack.copy();
                    singleCooked.setCount(1);

                    ItemStack bonusResult = ItemProductionLib.itemProduced(singleCooked, targetPlayer, "skillet");

                    // Wenn der Skill-Tree die Anzahl erhöht hat (z.B. Ergebnis ist 2 statt 1)
                    int bonusAmount = bonusResult.getCount() - 1;
                    if (bonusAmount > 0) {
                        ItemStack extraDrop = currentStack.copy();
                        extraDrop.setCount(bonusAmount);

                        // Physisch neben der Pfanne spawnen lassen!
                        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, extraDrop);

                        org.apache.logging.log4j.LogManager.getLogger("ItemProductionLib")
                                .info("[PFANNEN-BONUS] {} x extra {} generiert!", bonusAmount, extraDrop.getItem().toString());
                    }
                }
            }
        }
    }
}
