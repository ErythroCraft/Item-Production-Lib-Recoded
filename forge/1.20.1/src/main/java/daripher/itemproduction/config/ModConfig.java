package daripher.itemproduction.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ModConfig {
    public static final ForgeConfigSpec SERVER_SPEC;

    public static final ForgeConfigSpec.BooleanValue SHOW_STACK_TEST_LOGS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CRAFTING_LOGS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FURNACE_LOGS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SMOKER_LOGS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BLAST_FURNACE_LOGS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BREWING_LOGS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_UNKNOWN_LOGS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment(" ==========================================================================",
                " ITEM PRODUCTION LIB - ADVANCED DEBUG CONFIG",
                " Hier kannst du die Warn-Logs fuer jeden Block einzeln aktivieren/deaktivieren.",
                " Das hilft dir, beim Erstellen oder Bearbeiten von Skills die Konsole sauber zu",
                " halten, indem nur die Ausgabe des Blocks geloggt wird, den du gerade testest.",
                " ==========================================================================").push("logging");

        SHOW_STACK_TEST_LOGS = builder
                .comment(" Schaltet die grosse [SKILL-TREE-KOCHTOPF-TEST]-Box und die Schleifenschritte fuer den Kochtopf ein/aus.")
                .define("showCookingPotLogs", true);

        ENABLE_CRAFTING_LOGS = builder
                .comment(" Schaltet die Test-Meldungen fuer die Werkbank (Crafting Table) ein/aus.")
                .define("enableCraftingTableLogs", true);

        ENABLE_FURNACE_LOGS = builder
                .comment(" Schaltet die Test-Meldungen fuer den Standard-Ofen (Furnace) ein/aus.")
                .define("enableStandardFurnaceLogs", true);

        ENABLE_SMOKER_LOGS = builder
                .comment(" Schaltet die Test-Meldungen fuer den Raeucherofen (Smoker) ein/aus.")
                .define("enableSmokerLogs", true);

        ENABLE_BLAST_FURNACE_LOGS = builder
                .comment(" Schaltet die Test-Meldungen fuer den Schmelzofen (Blast Furnace) ein/aus.")
                .define("enableBlastFurnaceLogs", true);

        ENABLE_BREWING_LOGS = builder
                .comment(" Schaltet die Test-Meldungen fuer den Alchemie-Braustand (Brewing Stand) ein/aus.")
                .define("enableBrewingStandLogs", true);

        ENABLE_UNKNOWN_LOGS = builder
                .comment(" Schaltet die Test-Meldungen fuer alle extra installierten Mod- oder Zusatz-Menues (z.B. Steinschneider, Webstuhl etc.) ein/aus.")
                .define("enableAdditionalModMenuLogs", true);

        builder.pop();
        SERVER_SPEC = builder.build();
    }

    private ModConfig() {
        // SonarQube-konform
    }
}
