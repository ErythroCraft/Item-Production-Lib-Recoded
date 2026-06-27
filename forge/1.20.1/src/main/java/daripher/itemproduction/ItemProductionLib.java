package daripher.itemproduction;

import daripher.itemproduction.block.entity.Interactive;
import daripher.itemproduction.event.ItemProducedEvent;
import daripher.itemproduction.util.ItemProcessingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ItemProductionLib.MOD_ID)
public class ItemProductionLib {
    public static final String MOD_ID = "itemproductionlib";
    private static final Logger LOGGER = LogManager.getLogger("ItemProductionLib");

    public ItemProductionLib() {
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        forgeEventBus.addListener(this::onItemSpawnInWorld);
        forgeEventBus.register(this);
        LOGGER.warn("[ItemProductionLib] API SUCCESSFULLY INITIALIZED!");
    }

    @SubscribeEvent
    public void setBlockEntityUser(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (blockEntity instanceof Interactive interactive) {
            interactive.setUser(serverPlayer);
        }
    }

    /**
     * Intercepts items popping out into the world (e.g., Campfire cooking or Skillet).
     */
    private void onItemSpawnInWorld(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity itemEntity)) {
            return;
        }

        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty() || ItemProcessingHelper.isProcessed(stack) || itemEntity.getAge() > 0) {
            return;
        }

        BlockPos pos = itemEntity.blockPosition();
        BlockEntity blockEntity = event.getLevel().getBlockEntity(pos);

        if (blockEntity == null) {
            blockEntity = event.getLevel().getBlockEntity(pos.below());
        }

        if (blockEntity != null) {
            // DEINE NEUE ZEILE: Sauber integriert für Forge-Mod-Kompatibilität!
            String registryName = net.minecraftforge.registries.ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(blockEntity.getType()).toString();
            String className = blockEntity.getClass().getName();

            if (registryName.contains("campfire") || className.contains("SkilletBlockEntity")) {
                Player creator = (blockEntity instanceof Interactive interactive) ? interactive.getUser() : null;
                ItemProcessingHelper.markAsProcessed(stack, creator);

                ItemStack modified = itemProduced(stack.copy(), blockEntity);
                itemEntity.setItem(modified);
            }
        }
    }

    /**
     * Core processing method invoked when a player grabs the output.
     */
    public static ItemStack itemProduced(ItemStack stack, Player player) {
        if (stack.isEmpty() || player == null) {
            return stack;
        }

        String pickerName = player.getName().getString();
        String crafterName = ItemProcessingHelper.getCrafterName(stack);

        // Standardized English log output showing both identities
        LOGGER.warn("[ItemProductionLib-DEBUG] Item taken: " + stack.getItem().toString());
        LOGGER.warn("[ItemProductionLib-DEBUG] Crafted by: " + crafterName);
        LOGGER.warn("[ItemProductionLib-DEBUG] Taken out by: " + pickerName);

        ItemProducedEvent event = new ItemProducedEvent(stack, player);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getStack();
    }

    /**
     * Fallback method for block entities that route through to the interactive user.
     */
    public static ItemStack itemProduced(ItemStack stack, BlockEntity blockEntity) {
        if (stack.isEmpty() || !(blockEntity instanceof Interactive interactive)) {
            return stack;
        }
        Player user = interactive.getUser();
        return user == null ? stack : itemProduced(stack, user);
    }

    @SubscribeEvent
    public void onPlayerCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack original = event.getCrafting();
        if (original.isEmpty() || event.getInventory() == null) {
            return;
        }

        if (!ItemProcessingHelper.isProcessed(original)) {
            ItemProcessingHelper.markAsProcessed(original, serverPlayer);

            ItemStack modified = itemProduced(original.copy(), serverPlayer);
            original.setTag(modified.getTag());
            original.setCount(modified.getCount());
        }
    }

    @SubscribeEvent
    public void onPlayerSmelt(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack original = event.getSmelting();
        if (original.isEmpty()) {
            return;
        }

        if (!ItemProcessingHelper.isProcessed(original)) {
            ItemProcessingHelper.markAsProcessed(original, serverPlayer);

            ItemStack modified = itemProduced(original.copy(), serverPlayer);
            original.setTag(modified.getTag());
            original.setCount(modified.getCount());
        }
    }

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
