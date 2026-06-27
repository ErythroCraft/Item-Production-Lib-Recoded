package daripher.itemproduction.util;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemProcessingHelper {

    public static final String DEFAULT_TAG = "SkillTreeProcessed";
    public static final String CRAFTER_UUID_TAG = "ItemProductionCrafterUUID";
    public static final String CRAFTER_NAME_TAG = "ItemProductionCrafterName";

    /**
     * Checks if the item has already been processed by the mod.
     */
    public static boolean isProcessed(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.getOrCreateTag().contains(DEFAULT_TAG);
    }

    /**
     * Marks the item as processed and stores the creator's identity (UUID and name).
     */
    public static void markAsProcessed(ItemStack stack, Player player) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(DEFAULT_TAG, true);

        if (player != null) {
            tag.putUUID(CRAFTER_UUID_TAG, player.getUUID());
            tag.putString(CRAFTER_NAME_TAG, player.getName().getString());
        }
    }

    /**
     * Retrieves the UUID of the original creator from the item's NBT data.
     */
    public static UUID getCrafterUUID(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        return (tag != null && tag.contains(CRAFTER_UUID_TAG)) ? tag.getUUID(CRAFTER_UUID_TAG) : null;
    }

    /**
     * Retrieves the name of the original creator from the item's NBT data.
     */
    public static String getCrafterName(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return "Unknown";
        }
        CompoundTag tag = stack.getTag();
        return (tag != null && tag.contains(CRAFTER_NAME_TAG)) ? tag.getString(CRAFTER_NAME_TAG) : "Unknown";
    }
}
