package daripher.itemproduction.mixin.davespotioneering;

import daripher.itemproduction.ItemProductionLib;
import daripher.itemproduction.block.entity.Interactive; // Das neue, universelle Interface importieren
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfar.davespotioneering.blockentity.AdvancedBrewingStandBlockEntity;

@Mixin(value = AdvancedBrewingStandBlockEntity.class, remap = false)
public abstract class AdvancedBrewingStandBlockEntityMixin {

    @Inject(method = "brewPotions", at = @At("TAIL"))
    private void enhanceBrewedPotions(CallbackInfo callbackInfo) {
        AdvancedBrewingStandBlockEntity blockEntity = (AdvancedBrewingStandBlockEntity) (Object) this;
        Level level = blockEntity.getLevel();

        if (level == null || level.isClientSide()) {
            return;
        }

        // 1. Greife über das neue Interactive-Interface sicher auf den Spieler zu
        Player foundPlayer = null;
        if ((Object) this instanceof Interactive interactive) {
            // Versucht den gecachten Spieler zu nehmen oder sucht ihn fehlerfrei live auf dem Server
            foundPlayer = interactive.resolveUser(level);
        }

        ServerPlayer targetPlayer = null;
        if (foundPlayer instanceof ServerPlayer serverPlayer) {
            targetPlayer = serverPlayer;
        }

        // FALLBACK: Falls automatisiert gebraut wurde (z.B. Hopper befüllt), nimm den nächsten Spieler
        if (targetPlayer == null) {
            BlockPos pos = blockEntity.getBlockPos();
            Player closestPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 8.0, false);
            if (closestPlayer instanceof ServerPlayer serverPlayer) {
                targetPlayer = serverPlayer;
            }
        }

        // 2. Wenn ein Spieler zugeordnet werden konnte, verarbeite die 3 Trank-Ausgangsslots
        if (targetPlayer != null) {
            final ServerPlayer finalPlayer = targetPlayer; // Finaler Verweis für das Lambda

            blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent((IItemHandler handler) -> {
                // Slots 0, 1 und 2 sind bei Minecraft-Brauständen traditionell die Trank-Ausgänge
                for (int slot = 0; slot < 3; slot++) {
                    ItemStack stack = handler.getStackInSlot(slot);

                    if (!stack.isEmpty()) {
                        int count = stack.getCount();
                        String itemName = stack.getItem().toString();
                        String playerName = finalPlayer.getName().getString();

                        // Logger füttern
                        daripher.itemproduction.util.DebugLogger.logCookingPotStack(playerName, itemName, count, "SERVER_BREWING_FINISHED");

                        // Übergibt eine saubere Kopie des Tranks an deine Lib. 
                        // Die Trankflasche im Slot bleibt komplett tag-frei!
                        ItemProductionLib.itemProduced(stack.copy(), finalPlayer);

                        daripher.itemproduction.util.DebugLogger.logStackLoopEnd();
                    }
                }
            });
        }
    }
}
