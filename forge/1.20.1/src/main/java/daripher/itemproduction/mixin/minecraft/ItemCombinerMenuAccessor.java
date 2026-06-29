package daripher.itemproduction.mixin.minecraft;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {

    // Holt das geschützte 'player' Feld (Mojang-Mapping)
    @Accessor("player")
    Player getPlayer();
}
