package daripher.itemproduction.util;

import daripher.itemproduction.config.ModConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DebugLogger {
    private static final Logger LOGGER = LogManager.getLogger("ItemProductionLib");
    private static final String LINE_SEPARATOR = "------------------------------------------------------------------------";

    private DebugLogger() {
        // Privater Utility-Konstruktor für SonarQube (java:S1118)
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Spezialisierter Test-Block exklusiv fuer Kochtöpfe und Küchengeräte.
     */
    public static void logCookingPotStack(String playerName, String itemName, int count, String clickType) {
        if (Boolean.FALSE.equals(ModConfig.SHOW_STACK_TEST_LOGS.get())) return;

        LOGGER.warn("================ [SKILL-TREE-KOCHTOPF-TEST] ================");
        LOGGER.warn("SPIELER: {}", playerName);
        LOGGER.warn("GERICHT: {}", itemName);
        LOGGER.warn("MENGE IM SLOT: {}", count);
        LOGGER.warn("KLICK-TYP: {}", clickType);
        LOGGER.warn("------------------------------------------------------------");
    }

    public static void logStackLoopStep(int current, int total) {
        if (Boolean.FALSE.equals(ModConfig.SHOW_STACK_TEST_LOGS.get())) return;
        LOGGER.warn("[STACK-SCHRITT] Verarbeite Item {} von {} fuer den Skill Tree...", current, total);
    }

    public static void logStackLoopEnd() {
        if (Boolean.FALSE.equals(ModConfig.SHOW_STACK_TEST_LOGS.get())) return;
        LOGGER.warn("============================================================");
    }

    /**
     * Intelligenter, zentraler Logger fuer die Hauptklasse.
     * FIX: Nutzt jetzt einen übergebenen 'productionType' statt des absturzgefährdeten containerMenu-Objekts!
     */
    public static void logItemProduction(ItemStack stack, Player player, String crafterName, String productionType) {
        if (player == null || stack == null) return;

        String playerName = player.getName().getString();
        String itemName = stack.getItem().toString();
        int count = stack.getCount();
        String headerTitle;

        // FIXED java:S1871 & java:S1192: Zusammenfassung der Filterung und Beseitigung von Redundanzen
        switch (productionType.toLowerCase()) {
            case "crafting":
                if (Boolean.FALSE.equals(ModConfig.ENABLE_CRAFTING_LOGS.get())) return;
                headerTitle = "============ [WERKBANK-PRODUKTION] ================";
                break;
            case "furnace":
                if (Boolean.FALSE.equals(ModConfig.ENABLE_FURNACE_LOGS.get())) return;
                headerTitle = "============ [OFEN-SCHMELZVORGANG] ================";
                break;
            case "smoker":
                if (Boolean.FALSE.equals(ModConfig.ENABLE_SMOKER_LOGS.get())) return;
                headerTitle = "============ [RAEUCHEROFEN-KOCHVORGANG] ===========";
                break;
            case "blast_furnace":
                if (Boolean.FALSE.equals(ModConfig.ENABLE_BLAST_FURNACE_LOGS.get())) return;
                headerTitle = "============ [SCHMELZOFEN-PRODUKTION] =============";
                break;
            case "brewing":
                if (Boolean.FALSE.equals(ModConfig.ENABLE_BREWING_LOGS.get())) return;
                headerTitle = "============ [ALCHEMIE-BRAUSTAND] ==================";
                break;
            default:
                if (Boolean.FALSE.equals(ModConfig.ENABLE_UNKNOWN_LOGS.get())) return;
                headerTitle = "============ [ZUSAETZLICHES MOD-MENUE: " + productionType + "] ============";
                break;
        }

        // Der eigentliche Log-Vorgang wird zentral nur EINMAL ausgeführt
        LOGGER.warn(headerTitle);
        LOGGER.warn("[INFO] Typ/Klasse: {}", productionType);
        LOGGER.warn("[INFO] Gegenstand: {} x {}", count, itemName); // <-- HIER GEFIXT
        LOGGER.warn("[INFO] Gecraftet von: {}", crafterName);
        LOGGER.warn("[INFO] Entnommen von: {}", playerName);
        LOGGER.warn(LINE_SEPARATOR);

    }
}
