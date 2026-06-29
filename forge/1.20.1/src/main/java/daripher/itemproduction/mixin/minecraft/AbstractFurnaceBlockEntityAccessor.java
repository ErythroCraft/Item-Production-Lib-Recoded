package daripher.itemproduction.mixin.minecraft;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceBlockEntityAccessor {

    // Erzeugt die saubere Brücke zum geschützten dataAccess-Feld des Ofens
    @Accessor("dataAccess")
    ContainerData itemproductionGetDataAccess();
}
