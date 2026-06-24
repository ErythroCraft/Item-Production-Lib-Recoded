package daripher.itemproduction.mixin.farmersdelight;

import org.spongepowered.asm.mixin.Mixin;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

@Mixin(CookingPotBlockEntity.class)
public class CookingPotBlockEntityMixin {
  // LEER: Verhindert jegliche static- und Injektions-Abstürze im Modpack!
  // In "ItemProductionLib.java" aufgenommen.
}
