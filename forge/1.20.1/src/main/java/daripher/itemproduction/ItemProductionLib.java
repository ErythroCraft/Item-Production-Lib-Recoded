package daripher.itemproduction;

import daripher.itemproduction.block.entity.Interactive;
import daripher.itemproduction.event.ItemProducedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ItemProductionLib.MOD_ID)
public class ItemProductionLib {
    public static final String MOD_ID = "itemproductionlib";
    private static final Logger LOGGER = LogManager.getLogger("ItemProductionLib");

    public ItemProductionLib() {
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        forgeEventBus.register(this);

        // Registriert die Forge-Config sauber über den Common-Typen
        ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                daripher.itemproduction.config.ModConfig.SERVER_SPEC,
                "itemproductionlib-common.toml"
        );

        LOGGER.warn("[ItemProductionLib] API SUCCESSFULLY INITIALIZED WITH TAG-FREE ARCHITECTURE!");
    }

    /**
     * Registriert den Spieler im BlockEntity, sobald er einen blockbasierten Ofen/Kochtopf öffnet oder anklickt.
     */
    @SubscribeEvent
    public void setBlockEntityUser(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // KORREKTUR: Defensiver Check beseitigt die Eclipse/SonarQube Null-Safety Warnung komplett!
        net.minecraft.core.BlockPos pos = event.getPos();
        if (pos == null) {
            return;
        }

        BlockEntity blockEntity = event.getLevel().getBlockEntity(pos);
        if (blockEntity instanceof Interactive interactive) {
            interactive.setUser(serverPlayer);
            interactive.setUserUUID(serverPlayer.getUUID()); // Direkt mitspeichern für den Weltlade-Schutz!
        }
    }

    /**
     * ZENTRALE STACK-SICHERUNG (Für direkte Spieler-Aufrufe wie Werkbänke):
     * Zerlegt den Stack für das Event und das Logging in 1er-Teile, um Boni präzise pro Item zu berechnen.
     * Nutzt den neuen, absturzsicheren DebugLogger-Typen!
     */
    public static ItemStack itemProduced(ItemStack stack, Player player, String productionType) {
        if (stack.isEmpty() || player == null) {
            return stack;
        }

        int totalCount = stack.getCount();
        ItemStack finalModifiedStack = stack.copy();

        // Da wir keine Tags mehr nutzen, ist der Urheber beim frischen Craften immer der extrahierende Spieler selbst
        String crafterName = player.getName().getString();

        // Fall 1: Wenn die Anzahl bereits 1 ist
        if (totalCount <= 1) {
            daripher.itemproduction.util.DebugLogger.logItemProduction(stack, player, crafterName, productionType);

            ItemProducedEvent event = new ItemProducedEvent(stack, player);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
            return event.getStack();
        }

        // Fall 2: Wenn es ein großer Stack ist (Werkbank, Ofen, etc.)
        for (int i = 1; i <= totalCount; i++) {
            daripher.itemproduction.util.DebugLogger.logStackLoopStep(i, totalCount);

            ItemStack singleItem = stack.copy();
            singleItem.setCount(1);

            daripher.itemproduction.util.DebugLogger.logItemProduction(singleItem, player, crafterName, productionType);

            ItemProducedEvent event = new ItemProducedEvent(singleItem, player);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);

            if (i == totalCount) {
                // FIX: Wir übernehmen NUR NOCH die Anzahl (falls Skills Erträge verändern). 
                // Das Setzen von Tags (setTag) fällt komplett weg, um das Stacking-Problem dauerhaft zu lösen!
                finalModifiedStack.setCount(event.getStack().getCount());
            }
        }

        return finalModifiedStack;
    }

    /**
     * Hilfsmethode für Mixins, die den Typen mitsenden (z.B. Kochtopf, Bratpfanne).
     */
    public static ItemStack itemProduced(ItemStack stack, Player player) {
        return itemProduced(stack, player, "unknown");
    }

    /**
     * Fallback-Methode für BlockEntities, die direkt über die Instanz aufgerufen werden.
     */
    public static ItemStack itemProduced(ItemStack stack, BlockEntity blockEntity) {
        if (stack.isEmpty() || !(blockEntity instanceof Interactive interactive)) {
            return stack;
        }
        Player user = interactive.getUser();
        return user == null ? stack : itemProduced(stack, user, "block_entity");
    }

    /**
     * Berechnet den Produktions-Geschwindigkeits-Buff für Schmelzöfen.
     */
    public static float getProductionSpeedMultiplier(Player player, String productionType) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return 1.0f;
        }
        float multiplier = 1.0f;
        net.minecraft.nbt.CompoundTag forgeData = serverPlayer.getPersistentData();
        String nbtKey = "ProductionBuff_" + productionType.toUpperCase();

        if (forgeData.contains(nbtKey)) {
            long expiryTime = forgeData.getLong(nbtKey);
            if (serverPlayer.level().getGameTime() < expiryTime) {
                multiplier += 0.3f;
            } else {
                forgeData.remove(nbtKey);
            }
        }
        return multiplier;
    }
}
