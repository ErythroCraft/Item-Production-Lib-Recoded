package daripher.itemproduction.mixin.farmersdelight;

import daripher.itemproduction.ItemProductionLib;
import daripher.itemproduction.block.entity.Interactive;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity", remap = false)
public class CookingPotBlockEntityMixin implements Interactive {

    // SonarQube-konforme Namen ohne das $-Zeichen
    @Unique
    private Player ipLastUser;

    @Unique
    private UUID ipLastUserUuid;

    @Override
    public Player getUser() {
        return this.ipLastUser;
    }

    @Override
    public void setUser(Player player) {
        this.ipLastUser = player;
        if (player != null) {
            this.ipLastUserUuid = player.getUUID();
        }
    }

    @Override
    public UUID getUserUUID() {
        return this.ipLastUserUuid;
    }

    @Override
    public void setUserUUID(UUID uuid) {
        this.ipLastUserUuid = uuid;
    }

    @Inject(method = "cookingTick", at = @At("TAIL"))
    private static void modifyCookingPotOutputViaForge(
            Level level,
            BlockPos pos,
            BlockState state,
            @Coerce BlockEntity blockEntity,
            CallbackInfo ci) {

        if (level == null || level.isClientSide() || blockEntity == null) {
            return;
        }

        blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent((IItemHandler handler) -> {
            ItemStack stack = handler.getStackInSlot(8); // Slot 8 = Result

            if (!stack.isEmpty() && !stack.getOrCreateTag().contains("SkillTreeProcessed")) {
                stack.getOrCreateTag().putBoolean("SkillTreeProcessed", true);

                ItemStack modified = ItemProductionLib.itemProduced(stack.copy(), blockEntity);

                stack.setTag(modified.getTag());
                stack.setCount(modified.getCount());
            }
        });
    }
}
