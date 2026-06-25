package daripher.itemproduction.mixin.minecraft;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StonecutterMenu.class)
public abstract class StonecutterMenuMixin {

  @Shadow
  @Final
  private ResultContainer resultContainer;

  /**
   * Bereitet das Item bei der Auswahl stumm vor.
   * Schreibt NUR das Tag, erzeugt aber KEINEN Konsolen-Spam!
   */
  @Inject(method = "clickMenuButton", at = @At("TAIL"))
  private void onStonecutterButtonClick(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
    if (player == null || player.level().isClientSide()) {
      return;
    }

    ItemStack outputStack = this.resultContainer.getItem(0);
    if (!outputStack.isEmpty() && !outputStack.getOrCreateTag().contains("SkillTreeProcessed")) {
      // Setzt nur den Stempel für das spätere fehlerfreie Stacking
      outputStack.getOrCreateTag().putBoolean("SkillTreeProcessed", true);
    }
  }

  @Inject(method = "quickMoveStack", at = @At("HEAD"))
  private void onStonecutterQuickMove(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
    if (player == null || player.level().isClientSide() || slotIndex != 1) {
      return;
    }

    ItemStack outputStack = this.resultContainer.getItem(0);
    if (!outputStack.isEmpty() && !outputStack.getOrCreateTag().contains("SkillTreeProcessed")) {
      outputStack.getOrCreateTag().putBoolean("SkillTreeProcessed", true);
    }
  }
}
