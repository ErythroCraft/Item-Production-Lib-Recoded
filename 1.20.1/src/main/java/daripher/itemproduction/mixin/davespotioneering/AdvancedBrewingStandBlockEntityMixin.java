package daripher.itemproduction.mixin.davespotioneering;

import daripher.itemproduction.ItemProductionLib;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemStackHandler; // NEUER IMPORT: Löst den cSpell-Fehler
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfar.davespotioneering.blockentity.AdvancedBrewingStandBlockEntity;

@Mixin(value = AdvancedBrewingStandBlockEntity.class, remap = false)
public abstract class AdvancedBrewingStandBlockEntityMixin {

  /**
   * Injiziert sich direkt ans Ende des Brauvorgangs (TAIL) der Mod.
   */
  @Inject(method = "brewPotions", at = @At("TAIL"))
  private void enhanceBrewedPotions(CallbackInfo callbackInfo) {
    @SuppressWarnings("DataFlowIssue")
    AdvancedBrewingStandBlockEntity blockEntity = (AdvancedBrewingStandBlockEntity) (Object) this;

    blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
      for (int slot = 0; slot < 3; slot++) {
        ItemStack stack = handler.getStackInSlot(slot);

        if (!stack.isEmpty()) {
          ItemStack modified = ItemProductionLib.itemProduced(stack, blockEntity);

          // KORREKTUR: Modernes Pattern Matching (Java 17) löst die SonarQube-Warnung
          // S6201
          if (handler instanceof ItemStackHandler itemStackHandler) {
            itemStackHandler.setStackInSlot(slot, modified);
          }
        }
      }
    });
  }
}
