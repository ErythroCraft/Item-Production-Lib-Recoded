package daripher.itemproduction.event;

import javax.annotation.Nonnull;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

public class ItemProducedEvent extends Event {
  // KORREKTUR: 'final' entfernt, damit das Item vom Skilltree verändert werden
  // kann
  private @Nonnull ItemStack stack;
  private final @Nonnull Player player;

  public ItemProducedEvent(@Nonnull ItemStack stack, @Nonnull Player player) {
    // Nutzen des defensiven Kopierens, um ungewollte Nebeneffekte zu vermeiden
    this.stack = stack.copy();
    this.player = player;
  }

  public @Nonnull ItemStack getStack() {
    return this.stack;
  }

  // NEU: Ermöglicht es dem Passive Skill Tree, das modifizierte Item ins Event
  // zurückzuschreiben
  public void setStack(@Nonnull ItemStack newStack) {
    this.stack = newStack;
  }

  public @Nonnull Player getPlayer() {
    return this.player;
  }

  /**
   * Überträgt die NBT-Modifikationen des Skilltrees sicher auf das Original-Item,
   * ohne Haltbarkeit, Namen oder Verzauberungen zu überschreiben.
   */
  public void mergeAndSetStack(@Nonnull ItemStack modifiedStack) {
    if (modifiedStack.hasTag()) {
      // Wir nehmen das originale Tag und verschmelzen es mit den neuen
      // Skilltree-Daten
      net.minecraft.nbt.CompoundTag originalTag = this.stack.getOrCreateTag();
      if (modifiedStack.getTag() != null) {
        originalTag.merge(modifiedStack.getTag());
      }
      this.stack.setTag(originalTag);
    }
    this.stack.setCount(modifiedStack.getCount());
  }

}
