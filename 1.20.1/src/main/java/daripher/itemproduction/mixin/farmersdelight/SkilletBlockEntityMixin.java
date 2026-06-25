package daripher.itemproduction.mixin.farmersdelight;

import daripher.itemproduction.ItemProductionLib;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity", remap = false)
public class SkilletBlockEntityMixin {

    @ModifyVariable(method = "cookingTick", at = @At("STORE"), ordinal = 0)
    private ItemStack modifySkilletOutput(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            BlockEntity blockEntity = (BlockEntity) (Object) this;
            return ItemProductionLib.itemProduced(stack.copy(), blockEntity);
        }
        return stack;
    }
}
