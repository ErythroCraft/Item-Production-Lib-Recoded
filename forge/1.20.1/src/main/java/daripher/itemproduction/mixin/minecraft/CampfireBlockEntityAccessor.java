package daripher.itemproduction.mixin.minecraft;

import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CampfireBlockEntity.class)
public interface CampfireBlockEntityAccessor {

    @Accessor("cookingProgress")
    int[] itemproductionGetCookingProgress();

    @Accessor("cookingTime")
    int[] itemproductionGetCookingTime();
}
