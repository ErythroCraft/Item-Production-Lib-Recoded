package daripher.itemproduction.mixin.visualworkbench;

import daripher.itemproduction.ItemProductionLib;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Klinkt sich in die Entnahme des fertigen Items aus dem Crafting-Ergebnis-Slot ein.
 * Funktioniert universell für die Visual Workbench, da diese die Standard-Vanilla-Slot-Logik erbt.
 */
@Mixin(net.minecraft.world.inventory.ResultSlot.class)
public class CraftingResultSlotMixin {

    /**
     * 'onTake' wird aufgerufen, wenn der Spieler (oder ein Shift-Klick) das Item erfolgreich 
     * aus dem Ergebnisfeld der Werkbank herausnimmt.
     */
    @Inject(method = "onTake", at = @At("HEAD"), remap = true)
    private void onCraftingItemTaken(Player player, ItemStack stack, CallbackInfo ci) {
        // Nur auf dem Server verarbeiten
        if (player == null || player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Falls der Slot aus irgendeinem Grund leer sein sollte, abbrechen
        if (stack == null || stack.isEmpty()) {
            return;
        }

        try {
            int count = stack.getCount();
            String itemName = stack.getItem().toString();
            String playerName = serverPlayer.getName().getString();

            // Logger füttern mit dem echten Crafting-Event
            daripher.itemproduction.util.DebugLogger.logCookingPotStack(playerName, itemName, count, "PLAYER_CRAFTING_FINISHED");

            // Übergibt eine saubere Kopie des gecrafteten Items an deine Lib.
            // Das Item landet komplett tag-frei und perfekt stapelbar im Inventar des Spielers!
            ItemProductionLib.itemProduced(stack.copy(), serverPlayer);

            daripher.itemproduction.util.DebugLogger.logStackLoopEnd();
        } catch (Exception e) {
            org.apache.logging.log4j.LogManager.getLogger("ItemProductionLib")
                    .warn("[CRAFTING-FEHLER] Fehler bei der Verarbeitung im Crafting-Slot: {}", e.getMessage());
        }
    }
}
