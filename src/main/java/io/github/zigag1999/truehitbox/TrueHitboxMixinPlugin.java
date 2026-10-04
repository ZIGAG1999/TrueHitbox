package io.github.zigag1999.truehitbox;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * The mixins use {@code require = 0}, so a changed or missing target can't crash the game. This plugin notices when
 * the injection point is missing and logs it once, at the time the target class is transformed (not per frame).
 */
public final class TrueHitboxMixinPlugin implements IMixinConfigPlugin
{
  private static final Logger LOGGER = LoggerFactory.getLogger("TrueHitbox");
  private static final String HITBOX_MIXIN = "EntityHitboxDebugRendererMixin";
  // Shared with the @WrapOperation in EntityHitboxDebugRendererMixin so the check can't drift from the injection.
  public static final String TARGET_METHOD = "emitGizmos";
  public static final String WRAPPED_NAME = "showHitboxes";
  public static final String WRAPPED_DESC = "(Lnet/minecraft/world/entity/Entity;FZ)V";
  public static final String WRAPPED_CALL =
      "Lnet/minecraft/client/renderer/debug/EntityHitboxDebugRenderer;" + WRAPPED_NAME + WRAPPED_DESC;

  @Override
  public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
  {
    if (!mixinClassName.endsWith(HITBOX_MIXIN))
      return;
    try
    {
      // MixinExtras applies @WrapOperation after this callback, so the original call is still in place here if the
      // injection point exists.
      for (MethodNode method : targetClass.methods)
        if (method.name.equals(TARGET_METHOD))
          for (AbstractInsnNode insn : method.instructions)
            if (insn instanceof MethodInsnNode call && call.owner.equals(targetClass.name)
                && call.name.equals(WRAPPED_NAME) && call.desc.equals(WRAPPED_DESC))
              return;
      LOGGER.warn("TrueHitbox did not find {}.{} calling {} (changed by a game update or another mod); the yellow and "
                  + "green boxes will probably not appear. Vanilla hitboxes still work.",
                  targetClassName, TARGET_METHOD, WRAPPED_NAME);
    }
    catch (RuntimeException e)
    {
      LOGGER.warn("TrueHitbox could not check its hitbox hook in {}", targetClassName, e);
    }
  }

  @Override
  public void onLoad(String mixinPackage)
  {
  }

  @Override
  public String getRefMapperConfig()
  {
    return null;
  }

  @Override
  public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
  {
    return true;
  }

  @Override
  public void acceptTargets(Set<String> myTargets, Set<String> otherTargets)
  {
  }

  @Override
  public List<String> getMixins()
  {
    return null;
  }

  @Override
  public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
  {
  }
}
