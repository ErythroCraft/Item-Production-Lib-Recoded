package daripher.itemproduction.mixin.minecraft;

import daripher.itemproduction.ItemProductionLib;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Klinkt sich in das SmithingMenu ein, um die Produktion beim Schmiedetisch abzufangen.
 * Nutzt die offizielle Slot-API des Containers, um Sichtbarkeitsfehler komplett zu umgehen.
 */
@Mixin(SmithingMenu.class)
public class SmithingResultSlotMixin {

    @Unique
    private boolean itemproductionIsProcessing = false;

    /**
     * 'createResult' wird von Minecraft aufgerufen, sobald eine gültige Kombination 
     * (z.B. Werkzeug + Netherite + Upgrade-Template) im Schmiedetisch liegt.
     */
    @Inject(method = "createResult", at = @At("TAIL"), remap = true)
    private void onSmithingCreateResultTail(CallbackInfo ci) {
        if (this.itemproductionIsProcessing) {
            return;
        }

        // KORREKTUR: Wir nutzen den Accessor, um das geschützte 'player' Feld sicher auszulesen
        ItemCombinerMenu combinerMenu = (ItemCombinerMenu) (Object) this;
        Player player = ((ItemCombinerMenuAccessor) combinerMenu).getPlayer();

        // Nur auf dem Server verarbeiten, wenn ein gültiger Spieler aktiv ist
        if (player != null && !player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {

            // Verwendung des standardisierten Container-Systems
            // Slot 2 ist beim Schmiedetisch IMMER der Ergebnis-Slot (0 = Basis, 1 = Upgrade, 2 = Ergebnis)
            AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

            try {
                Slot resultSlot = menu.getSlot(2);
                ItemStack outputStack = resultSlot.getItem();

                if (!outputStack.isEmpty()) {
                    try {
                        this.itemproductionIsProcessing = true;

                        int count = outputStack.getCount();
                        String itemName = outputStack.getItem().toString();
                        String playerName = serverPlayer.getName().getString();

                        // Logger befüllen
                        daripher.itemproduction.util.DebugLogger.logCookingPotStack(playerName, itemName, count, "SERVER_SMITHING_PREVIEW_GENERATED");

                        // Übergibt eine saubere Kopie des Items an die Lib (Boni werden berechnet)
                        ItemStack modified = ItemProductionLib.itemProduced(outputStack.copy(), serverPlayer);

                        // Setzt die Anzahl basierend auf dem modifizierten Ergebnis-Stack
                        if (modified != null) {
                            outputStack.setCount(modified.getCount());
                        }

                        daripher.itemproduction.util.DebugLogger.logStackLoopEnd();
                    } catch (Exception e) {
                        org.apache.logging.log4j.LogManager.getLogger("ItemProductionLib")
                                .warn("[SCHMIEDE-FEHLER] Fehler bei der Verarbeitung im Schmiede-Menü: {}", e.getMessage());
                    } finally {
                        this.itemproductionIsProcessing = false;
                    }
                }
            } catch (Exception ignored) {
                // Schützt vor ungültigen Slot-Zuweisungen, falls andere Mods das Menü verändern
            }
        }
    }
}
