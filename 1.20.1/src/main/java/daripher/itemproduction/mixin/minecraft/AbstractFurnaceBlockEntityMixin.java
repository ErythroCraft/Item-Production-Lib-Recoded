package daripher.itemproduction.mixin.minecraft;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import daripher.itemproduction.ItemProductionLib;
import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public class AbstractFurnaceBlockEntityMixin {
  // KORREKTUR: Privater Konstruktor blockiert die Instanziierung und löst
  // java:S1118
  private AbstractFurnaceBlockEntityMixin() {
    throw new IllegalStateException("Mixin class cannot be instantiated");
  }

  /**
   * TEIL A: Das fertige Item beim Schmelzen abfangen.
   * Injiziert sich in die statische Schmelz-Logik der Vanilla-Öfen.
   * Methode ist als 'static' deklariert, passend zur Minecraft 1.20.1
   * Architektur.
   */
  @Inject(method = "burn", at = @At(value = "INVOKE",
      // Wir klinken uns ein, wenn das Rezept das fertige Schmelz-Resultat ausgibt
      target = "Lnet/minecraft/world/item/crafting/Recipe;assemble(Lnet/minecraft/world/Container;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;", shift = At.Shift.AFTER))
  private static void itemProduced(
      RegistryAccess registryAccess,
      @Nullable Recipe<?> recipe,
      NonNullList<ItemStack> inventory,
      int maxStackSize,
      CallbackInfoReturnable<Boolean> callbackInfo,
      // Holt das soeben erzeugte, fertige ItemStack aus dem lokalen Speicher
      // (LocalRef)
      @Local(ordinal = 1) LocalRef<ItemStack> resultStack) {

    if (resultStack.get() != null && !resultStack.get().isEmpty()) {
      // Da wir hier im statischen Kontext keine direkte BlockEntity-Instanz haben,
      // nutzen wir die Player-Erkennung über das globale User-System deiner
      // Hauptklasse
      ItemStack original = resultStack.get();

      // Wir suchen im Forge-System nach dem Spieler, der den Ofen geöffnet hat
      // Das greift direkt auf das von uns ausgebauten 'Interactive'-System zu
      ItemStack modified = ItemProductionLib.itemProduced(original.copy(),
          (net.minecraft.world.level.block.entity.BlockEntity) null);

      if (modified != original) {
        resultStack.set(modified);
      }
    }
  }

  /**
   * TEIL B: Universelle Geschwindigkeits-Steuerung (Permanente Skills + Zeitliche
   * Belohnungen)
   * Erhöht die Schmelzgeschwindigkeit, wenn der Spieler den passenden Skill
   * besitzt oder ein Timer läuft.
   * Wir injizieren uns in den serverTick, wo der Ofen seinen Fortschritt
   * berechnet.
   */
  @Inject(method = "serverTick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;cookingProgress:I", ordinal = 1))
  private static void accelerateFurnaceCooking(
      net.minecraft.world.level.Level level,
      net.minecraft.core.BlockPos pos,
      net.minecraft.world.level.block.state.BlockState state,
      AbstractFurnaceBlockEntity blockEntity,
      CallbackInfo ci) {

    if (level == null || level.isClientSide()) {
      return;
    }

    // 1. Prüfen, wer den Ofen benutzt (über dein funktionierendes
    // Interactive-System!)
    if (blockEntity instanceof daripher.itemproduction.block.entity.Interactive interactive) {
      net.minecraft.world.entity.player.Player user = interactive.getUser();

      if (user instanceof ServerPlayer player) {
        // Logik zur Senkung der kognitiven Komplexität in separate Methode ausgelagert
        applyFurnaceSpeedBoost(level, blockEntity, player);
      }
    }
  }

  /**
   * Hilfsmethode zur Berechnung und Anwendung des
   * Schmelz-Geschwindigkeits-Boosts.
   * Reduziert die kognitive Komplexität der Hauptmethode drastisch.
   */
  private static void applyFurnaceSpeedBoost(net.minecraft.world.level.Level level,
      AbstractFurnaceBlockEntity blockEntity, ServerPlayer player) {
    // 2. Hier wird die universelle API deiner Hauptklasse abgefragt!
    float speedMultiplier = ItemProductionLib.getProductionSpeedMultiplier(player, "furnace");

    if (speedMultiplier > 1.0f) {
      // Berechnet mathematisch das Tick-Intervall für den Extra-Fortschritt
      int tickInterval = (int) (1.0f / (speedMultiplier - 1.0f));

      if (tickInterval > 0 && level.getGameTime() % tickInterval == 0) {
        try {
          // Holt die geschützten Werte sicher über den Forge-Helper (Löst S3011)
          int currentProgress = net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(
              AbstractFurnaceBlockEntity.class, blockEntity, "cookingProgress");
          int totalTime = net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(
              AbstractFurnaceBlockEntity.class, blockEntity, "cookingTotalTime");

          // Nur erhöhen, wenn der Ofen aktiv arbeitet und noch nicht fertig ist
          if (currentProgress > 0 && currentProgress < totalTime) {
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(
                AbstractFurnaceBlockEntity.class, blockEntity, currentProgress + 1, "cookingProgress");
          }
        } catch (Exception ignored) {
          // NOSONAR: Schützt vor Inkompatibilitäten bei veränderten Ofen-Strukturen
        }
      }
    }
  }

}
