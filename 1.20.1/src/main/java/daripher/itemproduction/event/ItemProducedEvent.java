package daripher.itemproduction.event;

import javax.annotation.Nonnull;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Event, das gefeuert wird, sobald ein Gegenstand erfolgreich produziert wurde.
 * Markiert als @Cancelable, um ordnungsgemäße Bus-Prioritäten in Forge zu
 * garantieren.
 */
@Cancelable
public class ItemProducedEvent extends Event {
  private @Nonnull ItemStack stack;
  private final @Nonnull Player player;

  public ItemProducedEvent(@Nonnull ItemStack stack, @Nonnull Player player) {
    // Defensives Kopieren verhindert, dass unfertige Zwischenzustände das Original
    // korrumpieren
    this.stack = stack.copy();
    this.player = player;
  }

  public @Nonnull ItemStack getStack() {
    return this.stack;
  }

  /**
   * Ermöglicht es dem Passive Skill Tree, das modifizierte Item direkt zu
   * ersetzen.
   * WICHTIG: Erlaubt vollen Support für Dariphers Standard-Mod-Logik!
   */
  public void setStack(@Nonnull ItemStack newStack) {
    this.stack = newStack;
  }

  public @Nonnull Player getPlayer() {
    return this.player;
  }

  /**
   * Kombiniert die NBT-Modifikationen des Skilltrees sicher mit dem
   * Original-Item.
   * Verhindert das Löschen von Vanilla-Daten (wie Namen, Verzauberungen,
   * Trank-Effekten).
   */
  public void mergeAndSetStack(@Nonnull ItemStack modifiedStack) {
    if (modifiedStack.isEmpty()) {
      this.stack = modifiedStack;
      return;
    }

    if (modifiedStack.hasTag()) {
      // Holt das bestehende Tag oder erstellt ein neues, falls keins existiert
      net.minecraft.nbt.CompoundTag originalTag = this.stack.getOrCreateTag();
      if (modifiedStack.getTag() != null) {
        // Verschmilzt die Skilltree-NBTs sanft mit den bestehenden Attributen
        originalTag.merge(modifiedStack.getTag());
      }
      this.stack.setTag(originalTag);
    }

    // Übernimmt die veränderte Anzahl (falls Skills z.B. die Ausbeute verdoppeln)
    this.stack.setCount(modifiedStack.getCount());
  }
}
