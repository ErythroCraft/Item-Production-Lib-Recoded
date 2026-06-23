package daripher.itemproduction.mixin.minecraft;

import daripher.itemproduction.ItemProductionLib;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

  @Shadow
  @Final
  private ResultContainer resultSlots;
  @Shadow
  @Final
  private Player player;

  // KORREKTUR: Namen an CamelCase angepasst (ohne $) um java:S116 zu lösen
  @Unique
  private boolean itemProductionIsProcessing = false;

  /**
   * Wird aufgerufen, wenn sich die Items im Crafting-Gitter verändern.
   * Aktualisiert das Ergebnis-Item im Ausgabeslot mit den gelernten
   * Skilltree-Boni.
   */
  @Inject(method = "slotsChanged", at = @At(value = "TAIL"))
  private void itemProduced(Container container, CallbackInfo callbackInfo) {
    // Wenn wir das Item gerade selbst modifizieren, ignorieren wir diesen
    // Folge-Aufruf
    if (this.itemProductionIsProcessing) {
      return;
    }

    net.minecraft.world.item.ItemStack outputStack = this.resultSlots.getItem(0);

    if (!outputStack.isEmpty()) {
      try {
        // Sperre aktivieren: Wir fangen an zu modifizieren
        this.itemProductionIsProcessing = true;

        // Boni berechnen
        net.minecraft.world.item.ItemStack modifiedStack = ItemProductionLib.itemProduced(outputStack.copy(),
            this.player);

        // Das verbesserte Item sicher in den Ausgabeslot des Handwerkstischs legen
        this.resultSlots.setItem(0, modifiedStack);
      } finally {
        // Sperre im 'finally'-Block IMMER wieder lösen, damit das nächste Rezept
        // funktioniert
        this.itemProductionIsProcessing = false;
      }
    }
  }
}
