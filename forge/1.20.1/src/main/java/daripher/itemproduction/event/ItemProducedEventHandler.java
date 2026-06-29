package daripher.itemproduction.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource; // KORREKTUR: Minecrafts eigenen Random-Typen importieren
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "itemproductionlib", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ItemProducedEventHandler {

    @SubscribeEvent
    public static void onItemProduced(ItemProducedEvent event) {
        Player player = event.getPlayer();
        ItemStack originalStack = event.getStack();

        if (player == null || originalStack.isEmpty()) {
            return;
        }

        ItemStack modifiedStack = originalStack.copy();
        int currentCount = modifiedStack.getCount();

        // =========================================================================
        // AUTOMOD-SICHERE FULL-STACK LOGIK (OHNE EVENT-LAG!)
        // =========================================================================
        
        float bonusChance = 0.20f; // Beispiel: 20% Chance auf Verdopplung
        int bonusItems = 0;
        
        // KORREKTUR: Typ auf RandomSource geändert
        RandomSource rand = player.level().getRandom();

        // Bei kleinen Stacks nutzen wir die exakte Schleife
        if (currentCount <= 64) {
            for (int i = 0; i < currentCount; i++) {
                if (rand.nextFloat() < bonusChance) {
                    bonusItems++;
                }
            }
        } else {
            // Die mathematisch faire Durchschnitts-Zuweisung für riesige Stacks
            double mean = currentCount * bonusChance;
            // Standardabweichung für die faire Varianz (Zufalls-Streuung)
            double standardDeviation = Math.sqrt(currentCount * bonusChance * (1.0f - bonusChance));
            
            // RandomSource besitzt zum Glück ebenfalls nextGaussian()!
            bonusItems = (int) Math.round(mean + rand.nextGaussian() * standardDeviation);
            
            if (bonusItems < 0) bonusItems = 0;
        }

        if (bonusItems > 0) {
            modifiedStack.setCount(currentCount + bonusItems);
            
            org.apache.logging.log4j.LogManager.getLogger("ItemProductionLib")
                .info("[PERFORMANCE-PRODUKTION] Riesiger Stack verarbeitet: +{} extra Items für {}!", 
                        bonusItems, player.getName().getString());
        }

        // Ergebnis zurück an deine Library senden
        event.mergeAndSetStack(modifiedStack);
    }
}
