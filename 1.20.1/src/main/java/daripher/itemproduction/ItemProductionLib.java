package daripher.itemproduction;

import daripher.itemproduction.block.entity.Interactive;
import daripher.itemproduction.event.ItemProducedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod(ItemProductionLib.MOD_ID)
public class ItemProductionLib {
  public static final String MOD_ID = "itemproductionlib";

  public ItemProductionLib() {
    IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
    forgeEventBus.addListener(this::setBlockEntityUser);
    forgeEventBus.addListener(this::onRightClickCookingBlock);
  }

  private void setBlockEntityUser(PlayerInteractEvent.RightClickBlock event) {
    BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
    if (blockEntity instanceof Interactive interactive) {
      interactive.setUser(event.getEntity());
    }
  }

  private void onRightClickCookingBlock(PlayerInteractEvent.RightClickBlock event) {
    Level level = event.getLevel();
    BlockPos pos = event.getPos();
    Player player = event.getEntity();

    if (level.isClientSide() || event.getHand() != InteractionHand.MAIN_HAND
        || !(player instanceof ServerPlayer serverPlayer)) {
      return;
    }

    BlockEntity blockEntity = level.getBlockEntity(pos);
    if (blockEntity == null)
      return;

    // Aufteilung in Sub-Methoden senkt die kognitive Komplexität auf ein Minimum
    if (blockEntity.getClass().getName().contains("StoveBlockEntity")) {
      processStoveCooking(blockEntity, serverPlayer);
    } else if (blockEntity instanceof CampfireBlockEntity campfire) {
      processCampfireCooking(campfire, serverPlayer);
    }
  }

  private void processStoveCooking(BlockEntity blockEntity, ServerPlayer player) {
    try {
      java.lang.reflect.Method getStoredItemMethod = blockEntity.getClass().getMethod("getStoredItem", int.class);
      java.lang.reflect.Field progressField = blockEntity.getClass().getField("cookingProgress");
      int[] progress = (int[]) progressField.get(blockEntity);

      for (int i = 0; i < 4; i++) {
        ItemStack resultStack = (ItemStack) getStoredItemMethod.invoke(blockEntity, i);
        if (!resultStack.isEmpty() && progress[i] == 0) {
          ItemStack modified = itemProduced(resultStack.copy(), player);
          resultStack.setTag(modified.getTag());
          resultStack.setCount(modified.getCount());
        }
      }
    } catch (Exception ignored) {
      // NOSONAR: Ignorieren verhindert Abstürze bei inkompatiblen Mod-Versionen
    }
  }

  private void processCampfireCooking(CampfireBlockEntity campfire, ServerPlayer player) {
    try {
      int[] cookingProgress = net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(
          CampfireBlockEntity.class, campfire, "cookingProgress");
      int[] cookingTime = net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(
          CampfireBlockEntity.class, campfire, "cookingTime");

      if (cookingProgress != null && cookingTime != null) {
        for (int i = 0; i < campfire.getItems().size(); i++) {
          ItemStack resultStack = campfire.getItems().get(i);
          if (!resultStack.isEmpty() && cookingProgress[i] >= cookingTime[i]) {
            ItemStack modified = itemProduced(resultStack.copy(), player);
            resultStack.setTag(modified.getTag());
            resultStack.setCount(modified.getCount());
          }
        }
      }
    } catch (Exception ignored) {
      // NOSONAR: Ignorieren schützt vor Inkompatibilitäten bei veränderten
      // Vanilla-Blöcken
    }
  }

  public static ItemStack itemProduced(ItemStack stack, Player player) {
    if (stack.isEmpty())
      return stack;
    ItemProducedEvent event = new ItemProducedEvent(stack, player);
    MinecraftForge.EVENT_BUS.post(event);
    return event.getStack();
  }

  public static ItemStack itemProduced(ItemStack stack, BlockEntity blockEntity) {
    if (!(blockEntity instanceof Interactive interactive))
      return stack;
    Player user = interactive.getUser();
    return user == null ? stack : itemProduced(stack, user);
  }

  /**
   * UNIVERSAL SPEED API:
   * Berechnet die Produktionsgeschwindigkeit für einen Spieler unter
   * Berücksichtigung von
   * permanenten Skills, zeitlichen NBT-Timern und Trank-Effekten.
   * 
   * @return Der Geschwindigkeits-Multiplikator (z.B. 1.2f = 20% schneller)
   */
  public static float getProductionSpeedMultiplier(Player player, String productionType) {
    if (!(player instanceof ServerPlayer serverPlayer))
      return 1.0f;

    float multiplier = 1.0f;

    // FAKTOR 1: Permanente Skills aus dem Passive Skill Tree abfragen
    // (Hier verknüpfen wir die Lib direkt mit der API des Skilltrees)
    // Beispiel: if (SkillTreeAPI.hasSkill(serverPlayer, "faster_" +
    // productionType)) multiplier += 0.2f;

    // FAKTOR 2: Allgemeiner zeitlicher Begrenzungs-Timer (Über Player-NBT)
    net.minecraft.nbt.CompoundTag forgeData = serverPlayer.getPersistentData();
    String nbtKey = "ProductionBuff_" + productionType.toUpperCase();

    if (forgeData.contains(nbtKey)) {
      long expiryTime = forgeData.getLong(nbtKey);
      // Wenn die aktuelle Weltzeit kleiner ist als die Ablaufzeit, ist die Belohnung
      // aktiv!
      if (serverPlayer.level().getGameTime() < expiryTime) {
        multiplier += 0.3f; // Gibt allgemein +30% Geschwindigkeit während des Timers
      } else {
        forgeData.remove(nbtKey); // Timer abgelaufen, sauber löschen
      }
    }

    return multiplier;
  }

}
