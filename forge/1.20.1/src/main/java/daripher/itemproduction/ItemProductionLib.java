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

        LOGGER.warn("[ItemProductionLib] API SUCCESSFULLY INITIALIZED WITH HIGH-PERFORMANCE FULL-STACK ARCHITECTURE!");
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
     * NEUE ZENTRALE STACK-SICHERUNG (Automod- und Performance-sicher):
     * Schickt den gesamten Stack als Ganzes durch das Event.
     * Schreibt das Ergebnis DIREKT physisch in das Original-Objekt zurück,
     * damit Minecraft alle Ofen- und Inventar-Slots fehlerfrei aktualisiert!
     */
    public static ItemStack itemProduced(ItemStack stack, Player player, String productionType) {
        if (stack.isEmpty() || player == null) {
            return stack;
        }

        String crafterName = player.getName().getString();

        // Ein einziger Log-Aufruf für den gesamten Stack (spart IO-Leistung im Server-Thread)
        daripher.itemproduction.util.DebugLogger.logItemProduction(stack, player, crafterName, productionType);

        // Wir feuern das Event GENAU EINMAL ab – egal ob 1 oder 10.000 Items verarbeitet werden!
        ItemProducedEvent event = new ItemProducedEvent(stack, player);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);

        // Wir holen uns das modifizierte Ergebnis aus dem Skill-Tree
        ItemStack resultStack = event.getStack();

        // KORREKTUR: Wir weisen dem ECHTEN, übergebenen Stack die neue Gesamtanzahl zu!
        // Nur so merken Minecraft und Automods live, dass Bonus-Items generiert wurden.
        stack.setCount(resultStack.getCount());

        return stack;
    }

    /**
     * Hilfsmethode für Mixins und BlockEntities.
     * Ermittelt dynamisch den korrekten Produktionstyp, damit der Passive Skill Tree 
     * die Events nicht wegen des Typs "unknown" blockiert!
     */
    public static ItemStack itemProduced(ItemStack stack, Player player) {
        String detectedType = "crafting"; // Standard-Fallback

        if (player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.containerMenu instanceof net.minecraft.world.inventory.SmithingMenu) {
                detectedType = "smithing";
            } else if (serverPlayer.containerMenu instanceof net.minecraft.world.inventory.FurnaceMenu) {
                detectedType = "furnace";
            } else if (serverPlayer.containerMenu instanceof net.minecraft.world.inventory.BlastFurnaceMenu) {
                detectedType = "blast_furnace";
            } else if (serverPlayer.containerMenu instanceof net.minecraft.world.inventory.SmokerMenu) {
                detectedType = "smoker";
            } else if (serverPlayer.containerMenu != null) {
                // Farmer's Delight Menüs über den Klassennamen abfangen
                String menuName = serverPlayer.containerMenu.getClass().getSimpleName().toLowerCase();
                if (menuName.contains("cookingpot")) {
                    detectedType = "cookingpot";
                } else if (menuName.contains("skillet")) {
                    detectedType = "skillet";
                }
            }
        }
        
        return itemProduced(stack, player, detectedType);
    }

    /**
     * Fallback-Methode für BlockEntities, die direkt über die Instanz aufgerufen werden.
     */
    public static ItemStack itemProduced(ItemStack stack, BlockEntity blockEntity) {
        if (stack.isEmpty() || !(blockEntity instanceof Interactive interactive)) {
            return stack;
        }
        Player user = interactive.getUser();

        // Erkennt Kochtopf und Bratpfanne anhand des BlockEntity-Klassennamens für die Configs
        String blockTypeName = blockEntity.getClass().getSimpleName().toLowerCase();
        String productionType = blockTypeName.contains("cookingpot") ? "cookingpot" : (blockTypeName.contains("skillet") ? "skillet" : "block_entity");

        return user == null ? stack : itemProduced(stack, user, productionType);
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
