package io.github.zigag1999.truehitbox.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.zigag1999.truehitbox.TrueHitboxMixinPlugin;
import io.github.zigag1999.truehitbox.render.ExtraHitboxRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Adds TrueHitbox's boxes next to vanilla's F3+B hitboxes. Wraps the call site in {@code emitGizmos} instead of
 * injecting into {@code showHitboxes} itself, so the extra boxes still appear when another mod (e.g. Combat
 * Hitboxes+) cancels or replaces {@code showHitboxes}. The original call always runs first and is never skipped.
 */
@Mixin(EntityHitboxDebugRenderer.class)
public abstract class EntityHitboxDebugRendererMixin
{
  @Shadow
  @Final
  private Minecraft minecraft;

  @WrapOperation(method = TrueHitboxMixinPlugin.TARGET_METHOD,
                 at = @At(value = "INVOKE", target = TrueHitboxMixinPlugin.WRAPPED_CALL))
  private void truehitbox$emitExtraBoxes(EntityHitboxDebugRenderer renderer, Entity entity, float partialTicks,
                                         boolean isServerEntity, Operation<Void> original)
  {
    original.call(renderer, entity, partialTicks, isServerEntity);
    // Vanilla's own (debug-flag only) server-entity pass also goes through this call; draw ours once per entity.
    if (!isServerEntity)
      ExtraHitboxRenderer.emit(minecraft, entity);
  }
}
