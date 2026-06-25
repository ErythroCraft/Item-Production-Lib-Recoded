package daripher.itemproduction.mixin.minecraft;

import daripher.itemproduction.ItemProductionLib;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    /**
     * Fängt jeden Klick in einer GUI auf Netzwerk-Ebene auf dem Server ab.
     * Perfekt, um den echten Pickup-Moment beim Steinschneider zu erwischen!
     */
    @Inject(method = "handleContainerClick", at = @At("HEAD"))
    private void onPlayerPickupItemFromContainer(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (this.player == null || this.player.level().isClientSide()) {
            return;
        }

        AbstractContainerMenu containerMenu = this.player.containerMenu;

        // Wir prüfen, ob der Spieler gerade ein Steinschneider-Menü geöffnet hat
        if (containerMenu instanceof StonecutterMenu && packet.getSlotNum() == 1) {
                // Wir holen uns das Item, das sich im Ausgabe-Slot befindet
                ItemStack outputStack = containerMenu.getSlot(1).getItem();

                if (!outputStack.isEmpty()) {
                    // Erst JETZT beim echten Linksklick/Pickup jagen wir es durch die Library!
                    ItemStack modified = ItemProductionLib.itemProduced(outputStack.copy(), this.player);

                    // Schreibt die gelernten Skilltree-Buffs live auf das entnommene Item
                    containerMenu.getSlot(1).set(modified);
                }
            }
        
    }
}
