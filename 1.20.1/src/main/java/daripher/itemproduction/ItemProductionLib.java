package daripher.itemproduction;

import daripher.itemproduction.block.entity.Interactive;
import daripher.itemproduction.event.ItemProducedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent; // NEUER IMPORT!
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nullable;

@Mod(ItemProductionLib.MOD_ID)
public class ItemProductionLib {
  public static final String MOD_ID = "itemproductionlib";
  private static final Logger LOGGER = LogManager.getLogger("ItemProductionLib");

  public ItemProductionLib() {
    IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;

    forgeEventBus.addListener(this::setBlockEntityUser);
    forgeEventBus.addListener(this::onLeftClickCookingBlock);
    forgeEventBus.addListener(this::onItemSpawnInWorld);
    forgeEventBus.addListener(this::onContainerClick); // Registriert den Container-Klick!

    forgeEventBus.register(this);
    LOGGER.warn("[ItemProductionLib] API ERFOLGREICH INITIALISIERT!");
  }

  /**
   * DER UNZERSTÖRBARE GUI-RETTER (Für Steinschneider & Co.):
   * Fängt jede Item-Entnahme direkt im Container-Menü des Spielers ab.
   */
  @SubscribeEvent
  public void onContainerClick(PlayerContainerEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      // Holt das Item, das der Spieler gerade durch den Klick in der GUI an die Maus
      // genommen hat
      ItemStack outputStack = event.getContainer().getCarried();

      if (!outputStack.isEmpty() && !outputStack.getOrCreateTag().contains("SkillTreeProcessed")) {
        // Globalen Schutzstempel aufsetzen für perfektes Stacking
        outputStack.getOrCreateTag().putBoolean("SkillTreeProcessed", true);

        // Durch deine Library jagen für das Log-Signal
        ItemStack modifiedStack = itemProduced(outputStack.copy(), serverPlayer);
        event.getContainer().setCarried(modifiedStack);
      }
    }
  }

  private void setBlockEntityUser(PlayerInteractEvent.RightClickBlock event) {
    if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND
        || !(event.getEntity() instanceof ServerPlayer serverPlayer)) {
      return;
    }

    BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
    if (blockEntity == null)
      return;

    if (blockEntity instanceof Interactive interactive) {
      interactive.setUser(serverPlayer);
    }

    processCookingPotManual(blockEntity, serverPlayer);
  }

  private void onLeftClickCookingBlock(PlayerInteractEvent.LeftClickBlock event) {
    if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer serverPlayer)) {
      return;
    }
    BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
    if (blockEntity == null)
      return;

    processCookingPotManual(blockEntity, serverPlayer);
  }

  private void processCookingPotManual(BlockEntity blockEntity, ServerPlayer serverPlayer) {
    String entityClassName = blockEntity.getClass().getName();
    if (entityClassName.contains("CookingPotBlockEntity")) {
      blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent((IItemHandler handler) -> {
        ItemStack stack = handler.getStackInSlot(8);
        if (!stack.isEmpty() && !stack.getOrCreateTag().contains("SkillTreeProcessed")) {
          stack.getOrCreateTag().putBoolean("SkillTreeProcessed", true);
          ItemStack modified = itemProduced(stack.copy(), serverPlayer);
          stack.setTag(modified.getTag());
          stack.setCount(modified.getCount());
        }
      });
    }
  }

  private void onItemSpawnInWorld(EntityJoinLevelEvent event) {
    if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity itemEntity)) {
      return;
    }

    ItemStack stack = itemEntity.getItem();
    if (stack.isEmpty() || stack.getOrCreateTag().contains("SkillTreeProcessed") || itemEntity.getAge() > 0) {
      return;
    }

    BlockPos pos = itemEntity.blockPosition();
    BlockEntity blockEntity = event.getLevel().getBlockEntity(pos);

    if (blockEntity == null) {
      blockEntity = event.getLevel().getBlockEntity(pos.below());
    }

    if (blockEntity != null) {
      String registryName = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).toString();
      String className = blockEntity.getClass().getName();

      if (registryName.contains("campfire") || className.contains("SkilletBlockEntity")) {
        stack.getOrCreateTag().putBoolean("SkillTreeProcessed", true);
        ItemStack modified = itemProduced(stack.copy(), blockEntity);
        itemEntity.setItem(modified);
      }
    }
  }

  public static ItemStack itemProduced(ItemStack stack, Player player) {
    if (stack.isEmpty() || player == null)
      return stack;
    stack.getOrCreateTag().putBoolean("SkillTreeProcessed", true);

    LOGGER.warn("[ItemProductionLib-DEBUG] Event SUCCESSFULLY intercepted for: " + stack.getItem().toString() + " by "
        + player.getName().getString());

    ItemProducedEvent event = new ItemProducedEvent(stack, player);
    MinecraftForge.EVENT_BUS.post(event);
    return event.getStack();
  }

  public static ItemStack itemProduced(ItemStack stack, BlockEntity blockEntity) {
    if (stack.isEmpty() || !(blockEntity instanceof Interactive interactive)) {
      return stack;
    }
    Player user = interactive.getUser();
    return user == null ? stack : itemProduced(stack, user);
  }

  /**
   * FÄNGT WERKBANK UND STEINSCHNEIDER AB:
   * Triggert exakt in der Millisekunde, in der der Spieler das Item aus der GUI
   * zieht.
   * VÖLLIG OHNE INSTABILE MIXINS!
   */
  @SubscribeEvent
  public void onPlayerCraftOrCook(PlayerEvent.ItemCraftedEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      ItemStack original = event.getCrafting();

      if (!original.isEmpty() && !original.getOrCreateTag().contains("SkillTreeProcessed")) {
        original.getOrCreateTag().putBoolean("SkillTreeProcessed", true);

        ItemStack modified = itemProduced(original.copy(), serverPlayer);
        original.setTag(modified.getTag());
        original.setCount(modified.getCount());
      }

    }
  }

  @SubscribeEvent
  public void onPlayerSmeltItem(PlayerEvent.ItemSmeltedEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      ItemStack original = getSmeltingItemSecure(event);
      if (original != null && !original.isEmpty()) {
        original.getOrCreateTag().putBoolean("SkillTreeProcessed", true);
        ItemStack modified = itemProduced(original.copy(), serverPlayer);
        original.setTag(modified.getTag());
        original.setCount(modified.getCount());
      }
    }
  }

  @Nullable
  private ItemStack getSmeltingItemSecure(PlayerEvent.ItemSmeltedEvent event) {
    try {
      java.lang.reflect.Field smeltingField;
      try {
        smeltingField = PlayerEvent.ItemSmeltedEvent.class.getDeclaredField("f_40243_");
      } catch (NoSuchFieldException e) {
        smeltingField = PlayerEvent.ItemSmeltedEvent.class.getDeclaredField("smelting");
      }
      smeltingField.setAccessible(true);
      return (ItemStack) smeltingField.get(event);
    } catch (Exception ignored) {
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
