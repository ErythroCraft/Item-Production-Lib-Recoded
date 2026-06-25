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

  @Unique
  private boolean itemProductionIsProcessing = false;

  /**
   * Wird aufgerufen, wenn der Schmiedetisch das Ergebnis eines Upgrades
   * berechnet.
   */
  @Inject(method = "createResult", at = @At("TAIL"))
  private void itemProduced(CallbackInfo callbackInfo) {
    if (this.itemProductionIsProcessing) {
      return;
    }

    @SuppressWarnings("ConstantConditions")
    ItemCombinerMenu combinerMenu = (ItemCombinerMenu) (Object) this;

    try {
      // Sicheres Auslesen der privaten Felder über Java-Reflection mit SRG- und
      // Klartext-Fallback
      java.lang.reflect.Field resultField;
      java.lang.reflect.Field playerField;

      try {
        resultField = ItemCombinerMenu.class.getDeclaredField("f_39767_"); // SRG resultSlots
        playerField = ItemCombinerMenu.class.getDeclaredField("f_39768_"); // SRG player
      } catch (NoSuchFieldException e) {
        resultField = ItemCombinerMenu.class.getDeclaredField("resultSlots");
        playerField = ItemCombinerMenu.class.getDeclaredField("player");
      }

      resultField.setAccessible(true);
      playerField.setAccessible(true);

      ResultContainer resultSlots = (ResultContainer) resultField.get(combinerMenu);
      Player player = (Player) playerField.get(combinerMenu);

      // KORREKTUR: Modifikation NUR auf dem Server erlauben, um Desynchronisationen
      // zu verhindern!
      if (resultSlots != null && player != null && !player.level().isClientSide()) {
        ItemStack outputStack = resultSlots.getItem(0);

        if (!outputStack.isEmpty()) {
          try {
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
