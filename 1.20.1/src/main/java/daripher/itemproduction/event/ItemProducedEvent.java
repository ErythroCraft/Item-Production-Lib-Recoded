package daripher.itemproduction.event;

import javax.annotation.Nonnull;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class ItemProducedEvent extends Event {
  private @Nonnull ItemStack stack;
  private final @Nonnull Player player;

  public ItemProducedEvent(@Nonnull ItemStack stack, @Nonnull Player player) {
    this.stack = stack.copy();
    this.player = player;
  }

  public @Nonnull ItemStack getStack() {
    return this.stack;
  }

  public void setStack(@Nonnull ItemStack newStack) {
    this.stack = newStack;
  }

  public @Nonnull Player getPlayer() {
    return this.player;
  }

  public void mergeAndSetStack(@Nonnull ItemStack modifiedStack) {
    if (modifiedStack.isEmpty()) {
      this.stack = modifiedStack;
      return;
    }

    if (modifiedStack.hasTag()) {
      net.minecraft.nbt.CompoundTag originalTag = this.stack.getOrCreateTag();
      if (modifiedStack.getTag() != null) {
        originalTag.merge(modifiedStack.getTag());
      }
      this.stack.setTag(originalTag);
    }

    this.stack.setCount(modifiedStack.getCount());
  }
}
