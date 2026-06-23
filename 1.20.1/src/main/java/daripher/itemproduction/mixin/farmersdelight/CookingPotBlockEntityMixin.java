package daripher.itemproduction.mixin.farmersdelight;

import daripher.itemproduction.ItemProductionLib;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

@Mixin(CookingPotBlockEntity.class)
public class CookingPotBlockEntityMixin {

  /**
   * Injiziert sich direkt in die statische Kochschleife, sobald der Topf
   * das Gericht erfolgreich fertigstellt und abspeichert (setChanged).
   */
  @Inject(method = "processCooking", at = @At(value = "INVOKE", target = "Lvectorwing/farmersdelight/common/block/entity/CookingPotBlockEntity;setChanged()V", ordinal = 0), remap = false)
  private static void enhanceCookedDish(CookingPotBlockEntity blockEntity, CallbackInfo ci) {
    if (blockEntity != null) {
      Level level = blockEntity.getLevel();

      if (level != null && !level.isClientSide()) {
        // Slot 2 ist in Farmer's Delight der standardisierte Ausgabe-Slot im Topf
        ItemStack resultStack = blockEntity.getInventory().getStackInSlot(2);

        if (!resultStack.isEmpty()) {
          // Wir jagen den Inhalt des Slots durch das Interactive-System deiner Library
          ItemStack modified = ItemProductionLib.itemProduced(resultStack.copy(), blockEntity);

          // Das fertige Gericht im Topf mit den gelernten Skilltree-Boni überschreiben
          resultStack.setTag(modified.getTag());
          resultStack.setCount(modified.getCount());
        }
      }
    }
  }
}
