package daripher.itemproduction.mixin.minecraft;

import daripher.itemproduction.ItemProductionLib;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Klinkt sich in die Entnahme des fertigen Items aus dem Steinschneider ein.
 * Targetet den spezifischen Result-Slot, den das StonecutterMenu für die Ausgabe nutzt.
 * In Minecraft 1.20.1 ist dies die anonyme Slot-Klasse im StonecutterMenu (StonecutterMenu$2).
 */
@Mixin(targets = "net.minecraft.world.inventory.StonecutterMenu$2")
public class StonecutterResultSlotMixin {

    /**
     * 'onTake' wird aufgerufen, wenn der Spieler (oder ein Shift-Klick) das fertige Item 
     * (z. B. Steinstufen oder bearbeiteten Mod-Stein) erfolgreich aus dem Steinschneider herausnimmt.
     */
    // In deiner StonecutterResultSlotMixin.java ebenfalls so abändern:
    @Inject(method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), remap = true)
    private void onStonecutterItemTaken(Player player, ItemStack stack, CallbackInfo ci) {
        // Nur auf dem Server verarbeiten
        if (player == null || player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (stack == null || stack.isEmpty()) {
            return;
        }

        try {
            int count = stack.getCount();
            String itemName = stack.getItem().toString();
            String playerName = serverPlayer.getName().getString();

            // Logger füttern mit dem echten Steinschneider-Event
            daripher.itemproduction.util.DebugLogger.logCookingPotStack(playerName, itemName, count, "PLAYER_STONECUTTING_FINISHED");

            // Übergibt eine saubere Kopie des bearbeiteten Items an deine Lib.
            // Das Item landet komplett tag-frei und perfekt stapelbar im Inventar des Spielers!
            ItemProductionLib.itemProduced(stack.copy(), serverPlayer);

            daripher.itemproduction.util.DebugLogger.logStackLoopEnd();
        } catch (Exception e) {
            org.apache.logging.log4j.LogManager.getLogger("ItemProductionLib")
                    .warn("[STONECUTTER-FEHLER] Fehler bei der Verarbeitung im Steinschneider-Slot: {}", e.getMessage());
        }
    }
}
