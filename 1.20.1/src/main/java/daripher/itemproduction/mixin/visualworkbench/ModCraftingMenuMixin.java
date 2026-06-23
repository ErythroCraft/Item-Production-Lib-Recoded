package daripher.itemproduction.mixin.visualworkbench;

import daripher.itemproduction.ItemProductionLib;
import fuzs.visualworkbench.world.inventory.ModCraftingMenu;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModCraftingMenu.class, remap = false)
public class ModCraftingMenuMixin {

  // KORREKTUR: @Final-Annotation entfernt und durch das echte Java-Schlüsselwort
  // 'final' ersetzt
  @Shadow
  @Final
  private ResultContainer resultSlots;
  @Shadow
  @Final
  private Player player;

  // NEU: Verhindert die Endlosschleife, wenn das Mixin das Item im Ausgabeslot
  // aktualisiert
  @Unique
  private boolean itemProductionIsProcessing = false;

  /**
   * Wird aufgerufen, wenn sich die Items im Gitter der Visual Workbench ändern.
   * Berechnet und aktualisiert die Skilltree-Boni für das fertige Produkt.
   */
  @Inject(method = "slotsChanged", at = @At(value = "TAIL"))
  private void itemProduced(Container container, CallbackInfo callbackInfo) {
    // Wenn das Mixin das Item gerade selbst aktualisiert, ignorieren wir diesen
    // Folge-Aufruf
    if (this.itemProductionIsProcessing) {
      return;
    }

    ItemStack outputStack = this.resultSlots.getItem(0);

    if (!outputStack.isEmpty()) {
      try {
        // Schutz aktivieren
        this.itemProductionIsProcessing = true;

        // Boni über deine Hauptklasse berechnen
        ItemStack modifiedStack = ItemProductionLib.itemProduced(outputStack.copy(), this.player);

        // Das verbesserte Item zurück in den Ausgabeslot der Visual Workbench legen
        this.resultSlots.setItem(0, modifiedStack);
      } finally {
        // Schutz im 'finally'-Block auf jeden Fall wieder aufheben
        this.itemProductionIsProcessing = false;
      }
    }
  }
}
