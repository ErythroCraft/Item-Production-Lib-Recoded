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
        // Utility-Klassen-Konstruktor (SonarQube)
    }

    /**
     * Spezialisierter Test-Block exklusiv fuer den Kochtopf.
     */
    public static void logCookingPotStack(String playerName, String itemName, int count, String clickType) {
        // FIXED java:S5411: Expliziter primitiver Vergleich, um Unboxing-Warnungen zu vermeiden
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
     * Filtert nach Block-Typ und prüft die jeweiligen Config-Schalter!
     */
    /**
     * Intelligenter, zentraler Logger fuer die Hauptklasse.
     * Filtert nach Block-Typ und prueft die jeweiligen Config-Schalter!
     */
    public static void logItemProduction(ItemStack stack, Player player, String crafterName) {
        String menuName = player.containerMenu.getClass().getSimpleName();
        String playerName = player.getName().getString();
        String itemName = stack.getItem().toString();
        int count = stack.getCount();

        // FIXED java:S5411: Alle Abfragen nutzen jetzt explizite primitive Boolean-Vergleiche
        if (menuName.contains("CraftingMenu") || menuName.contains("Workflow")) {
            if (Boolean.FALSE.equals(ModConfig.ENABLE_CRAFTING_LOGS.get())) return;
            LOGGER.warn("============ [WERKBANK-PRODUKTION] ================");
            logDetails(menuName, itemName, count, crafterName, playerName);
            LOGGER.warn(LINE_SEPARATOR);
        } else if (menuName.contains("FurnaceMenu")) {
            if (Boolean.FALSE.equals(ModConfig.ENABLE_FURNACE_LOGS.get())) return;
            LOGGER.warn("============ [OFEN-SCHMELZVORGANG] ================");
            logDetails(menuName, itemName, count, crafterName, playerName);
            LOGGER.warn(LINE_SEPARATOR);
        } else if (menuName.contains("SmokerMenu")) {
            if (Boolean.FALSE.equals(ModConfig.ENABLE_SMOKER_LOGS.get())) return;
            LOGGER.warn("============ [RAEUCHEROFEN-KOCHVORGANG] ===========");
            logDetails(menuName, itemName, count, crafterName, playerName);
            LOGGER.warn(LINE_SEPARATOR);
        } else if (menuName.contains("BlastFurnaceMenu")) {
            if (Boolean.FALSE.equals(ModConfig.ENABLE_BLAST_FURNACE_LOGS.get())) return;
            LOGGER.warn("============ [SCHMELZOFEN-PRODUKTION] =============");
            logDetails(menuName, itemName, count, crafterName, playerName);
            LOGGER.warn(LINE_SEPARATOR);
        } else if (menuName.contains("BrewingStandMenu")) {
            if (Boolean.FALSE.equals(ModConfig.ENABLE_BREWING_LOGS.get())) return;
            LOGGER.warn("============ [ALCHEMIE-BRAUSTAND] ==================");
            logDetails(menuName, itemName, count, crafterName, playerName);
            LOGGER.warn(LINE_SEPARATOR);
        } else {
            // REPARATUR: Sauberer Fallback für alle anderen Mod-Menüs ohne Klammerfehler!
            if (Boolean.FALSE.equals(ModConfig.ENABLE_UNKNOWN_LOGS.get())) return;
            LOGGER.warn("============ [ZUSAETZLICHES MOD-MENUE: {}] ============", menuName);
            logDetails(menuName, itemName, count, crafterName, playerName);
            LOGGER.warn(LINE_SEPARATOR);
        }
    }

    private static void logDetails(String menu, String item, int count, String crafter, String picker) {
        LOGGER.warn("[INFO] Menue-Klasse: {}", menu);
        LOGGER.warn("[INFO] Gegenstand:  {}x {}", count, item);
        LOGGER.warn("[INFO] Gecraftet von: {}", crafter);
        LOGGER.warn("[INFO] Entnommen von: {}", picker);
    }
}
