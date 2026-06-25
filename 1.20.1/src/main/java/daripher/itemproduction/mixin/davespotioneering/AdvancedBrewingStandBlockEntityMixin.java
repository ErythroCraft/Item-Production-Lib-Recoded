package daripher.itemproduction.mixin.davespotioneering;

import daripher.itemproduction.ItemProductionLib;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfar.davespotioneering.blockentity.AdvancedBrewingStandBlockEntity;

@Mixin(value = AdvancedBrewingStandBlockEntity.class, remap = false)
public abstract class AdvancedBrewingStandBlockEntityMixin {

  @Inject(method = "brewPotions", at = @At("TAIL"))
  private void enhanceBrewedPotions(CallbackInfo callbackInfo) {
    @SuppressWarnings("DataFlowIssue")
    AdvancedBrewingStandBlockEntity blockEntity = (AdvancedBrewingStandBlockEntity) (Object) this;

    // KORREKTUR: Modifikation nur auf dem Server durchführen, um Geister-Tränke zu
    // verhindern
    if (blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide()) {
      blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
        for (int slot = 0; slot < 3; slot++) {
          ItemStack stack = handler.getStackInSlot(slot);

          if (!stack.isEmpty()) {
            ItemStack modified = ItemProductionLib.itemProduced(stack, blockEntity);

            if (handler instanceof ItemStackHandler itemStackHandler) {
              itemStackHandler.setStackInSlot(slot, modified);
            }
          }
        }
      });
    }
  }
}