package daripher.itemproduction.mixin.minecraft;

import daripher.itemproduction.ItemProductionLib;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {

  // NEU: Ein Schutz-Flag, das verhindert, dass sich das Menü beim Aktualisieren
  // im Kreis aufruft
  @Unique
  private boolean itemProductionIsProcessing = false;

  /**
   * Wird aufgerufen, wenn der Schmiedetisch das Ergebnis eines Upgrades
   * berechnet.
   * Injiziert sich ganz am Ende (TAIL) der createResult-Methode.
   */
  @Inject(method = "createResult", at = @At("TAIL"))
  private void itemProduced(CallbackInfo callbackInfo) {
    // Falls wir das Item gerade selbst modifizieren, ignorieren wir diesen
    // Folge-Aufruf
    if (this.itemProductionIsProcessing) {
      return;
    }

    // Wir casten uns die aktuelle Instanz sicher auf die Oberklasse
    // ItemCombinerMenu um
    // Da SmithingMenu im Spiel immer davon erbt, ist das absolut crashsicher!
    @SuppressWarnings("ConstantConditions")
    ItemCombinerMenu combinerMenu = (ItemCombinerMenu) (Object) this;

    // Wir nutzen den Forge-Helper, um an die geschützten Felder der Oberklasse
    // heranzukommen.
    // Das umgeht alle Konstruktor-Änderungen und macht das Mixin unzerstörbar.
    try {
      ResultContainer resultSlots = net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(
          ItemCombinerMenu.class, combinerMenu, "f_39767_"); // "resultSlots" Searge-Feld
      Player player = net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(
          ItemCombinerMenu.class, combinerMenu, "f_39768_"); // "player" Searge-Feld

      if (resultSlots != null && player != null) {
        ItemStack outputStack = resultSlots.getItem(0);

        if (!outputStack.isEmpty()) {
          try {
            // Endlosschleifen-Schutz aktivieren
            this.itemProductionIsProcessing = true;

            // Boni aus dem Skilltree berechnen
            ItemStack modifiedStack = ItemProductionLib.itemProduced(outputStack.copy(), player);

            // Das verbesserte Item sicher zurück in den Ausgabe-Slot schreiben
            resultSlots.setItem(0, modifiedStack);
          } finally {
            this.itemProductionIsProcessing = false;
          }
        }
      }
    } catch (Exception ignored) {
      // NOSONAR: Verhindert Abstürze bei abweichenden Forge-Mapping-Builds
    }
  }
}
