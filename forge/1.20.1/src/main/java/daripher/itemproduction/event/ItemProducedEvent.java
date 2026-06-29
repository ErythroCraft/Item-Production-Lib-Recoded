package daripher.itemproduction.event;

import javax.annotation.Nonnull;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Event, das gefeuert wird, sobald ein Gegenstand erfolgreich produziert wurde.
 * Vollständig bereinigt von permanenten NBT-Modifikationen, um perfektes Item-Stacking zu garantieren.
 */
@Cancelable
public class ItemProducedEvent extends Event {
    
    private @Nonnull ItemStack stack;
    private final @Nonnull Player player;

    public ItemProducedEvent(@Nonnull ItemStack stack, @Nonnull Player player) {
        // Defensives Kopieren verhindert, dass unfertige Zwischenzustände das Original korrumpieren
        this.stack = stack.copy();
        this.player = player;
    }

    public @Nonnull ItemStack getStack() {
        return this.stack;
    }

    /**
     * Ermöglicht es dem Passive Skill Tree, das produzierte Item-Objekt direkt zu ersetzen.
     */
    public void setStack(@Nonnull ItemStack newStack) {
        this.stack = newStack;
    }

    public @Nonnull Player getPlayer() {
        return this.player;
    }

    /**
     * Übernimmt Boni aus dem Skilltree, OHNE NBT-Tags in das Item einzubrennen.
     * Unterstützt weiterhin Mengen-Multiplikatoren (z.B. Ertrags-Verdoppelung).
     * Permanent verändernde NBT-Attribute (wie Skilltree-Werte auf Items) wurden entfernt,
     * um das globale Stacking-Problem zu lösen.
     */
    public void mergeAndSetStack(@Nonnull ItemStack modifiedStack) {
        if (modifiedStack.isEmpty()) {
            this.stack = modifiedStack;
            return;
        }

        // WICHTIG: Wir ignorieren modifiedStack.getTag() vollständig!
        // Dadurch landen keine störenden Skilltree-Tags mehr auf den Items.
        
        // Erlaubt weiterhin Skilltree-Boni, die die Anzahl der hergestellten Items verändern (z.B. Doppelter Ertrag)
        this.stack.setCount(modifiedStack.getCount());
    }
}
