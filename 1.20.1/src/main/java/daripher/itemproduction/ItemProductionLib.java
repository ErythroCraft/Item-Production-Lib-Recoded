package daripher.itemproduction;

import daripher.itemproduction.block.entity.Interactive;
import daripher.itemproduction.event.ItemProducedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ItemProductionLib.MOD_ID)
public class ItemProductionLib {
  public static final String MOD_ID = "itemproductionlib";
  private static final Logger LOGGER = LogManager.getLogger("ItemProductionLib");

  public ItemProductionLib() {
    IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
    forgeEventBus.register(this);
    LOGGER.warn("[ItemProductionLib] API ERFOLGREICH INITIALISIERT!");
  }

  public static ItemStack itemProduced(ItemStack stack, Player player) {
    if (stack.isEmpty() || player == null)
      return stack;

    LOGGER.warn("[ItemProductionLib-DEBUG] Event gefeuert fuer: " + stack.getItem().toString() + " von "
        + player.getName().getString());

    ItemProducedEvent event = new ItemProducedEvent(stack, player);
    MinecraftForge.EVENT_BUS.post(event);
    return event.getStack();
  }

  public static ItemStack itemProduced(ItemStack stack, BlockEntity blockEntity) {
    if (stack.isEmpty() || !(blockEntity instanceof Interactive interactive))
      return stack;
    Player user = interactive.getUser();
    return user == null ? stack : itemProduced(stack, user);
  }

  @SubscribeEvent
  public void onPlayerCraftOrCook(PlayerEvent.ItemCraftedEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      ItemStack original = event.getCrafting();
      if (!original.isEmpty()) {
        ItemStack modified = itemProduced(original.copy(), serverPlayer);
        original.setTag(modified.getTag());
        original.setCount(modified.getCount());
      }
    }
  }

  /**
   * FORGE OFEN EVENT:
   * Erfasst das fertig geschmolzene Item, sobald es aus dem Ofen-Ausgabeslot
   * entnommen wird.
   */
  @SubscribeEvent
  public void onPlayerSmeltItem(PlayerEvent.ItemSmeltedEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      ItemStack original = getSmeltingItemSecure(event);

      if (original != null && !original.isEmpty()) {
        ItemStack modified = itemProduced(original.copy(), serverPlayer);
        original.setTag(modified.getTag());
        original.setCount(modified.getCount());
      }
    }
  }

  /**
   * Hilfsmethode: Holt das geschützte 'smelting'-Feld sicher per Reflection aus
   * dem Event.
   * Nutzt einen Fallback zwischen Produktions- (SRG) und Entwicklungs-Mappings.
   */
  @Nullable
  private ItemStack getSmeltingItemSecure(PlayerEvent.ItemSmeltedEvent event) {
    try {
      java.lang.reflect.Field smeltingField;
      try {
        // 1. Versuch: Produktionsumgebung (SRG-Name)
        smeltingField = PlayerEvent.ItemSmeltedEvent.class.getDeclaredField("f_40243_");
      } catch (NoSuchFieldException e) {
        // 2. Versuch: Entwicklungsumgebung (Klartext-Name)
        smeltingField = PlayerEvent.ItemSmeltedEvent.class.getDeclaredField("smelting");
      }

      smeltingField.setAccessible(true);
      return (ItemStack) smeltingField.get(event);
    } catch (Exception ignored) {
      // NOSONAR: Schützt vor abweichenden Forge-Builds und gibt im Fehlerfall null
      // zurück
      return null;
    }
  }

  public static float getProductionSpeedMultiplier(Player player, String productionType) {
    if (!(player instanceof ServerPlayer serverPlayer))
      return 1.0f;
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
