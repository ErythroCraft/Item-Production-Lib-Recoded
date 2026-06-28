package daripher.itemproduction.mixin.farmersdelight;

import daripher.itemproduction.ItemProductionLib;
import daripher.itemproduction.util.ItemProcessingHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intelligenter Klick-Trigger für das Kochtopf-Menü.
 * Unterscheidet zwischen normalem Essen-Herausnehmen und dem Editor-Modus (Feststelltaste).
 */
@Mixin(AbstractContainerMenu.class)
public class CookingPotMenuMixin {

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void onCookingPotSlotClicked(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        if (player == null || player.level().isClientSide() || !(player instanceof ServerPlayer)) {
            return;
        }

        if (clickType == ClickType.CLONE) {
            return;
        }

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        String menuClassName = menu.getClass().getName();

        if (menuClassName.contains("CookingPotMenu") && slotId == 8) {
            try {
                Slot slot = menu.getSlot(slotId);
                ItemStack stack = slot.getItem();

                if (!stack.isEmpty() && !ItemProcessingHelper.isProcessed(stack)) {
                    int count = stack.getCount();
                    String itemName = stack.getItem().toString();
                    String playerName = player.getName().getString();

                    // FIX: Eindeutiger Aufruf mit vollem Pfad
                    daripher.itemproduction.util.DebugLogger.logCookingPotStack(playerName, itemName, count, clickType.toString());

                    ItemProcessingHelper.markAsProcessed(stack, player);

                    // Reicht den Stack an die Hauptklasse weiter (dort wird er bei Bedarf zerlegt)
                    ItemProductionLib.itemProduced(stack.copy(), player);

                    // FIX: Eindeutiger Aufruf mit vollem Pfad
                    daripher.itemproduction.util.DebugLogger.logStackLoopEnd();
                }
            } catch (Exception e) {
                org.apache.logging.log4j.LogManager.getLogger("ItemProductionLib")
                        .warn("[STACK-TEST-FEHLER] Fehler bei der Stack-Verarbeitung: {}", e.getMessage());
            }
        }
    }
}
