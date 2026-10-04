package io.github.zigag1999.truehitbox.render;

import io.github.zigag1999.truehitbox.TrueHitbox;
import io.github.zigag1999.truehitbox.config.HitboxMode;
import io.github.zigag1999.truehitbox.config.TrueHitboxConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;

/**
 * Emits TrueHitbox's extra boxes for one entity. Called from the F3+B hitbox renderer right after vanilla has
 * emitted its own (white) box for that entity, while vanilla's gizmo collector is active.
 */
public final class ExtraHitboxRenderer
{
  private static GizmoStyle yellowStyle = GizmoStyle.stroke(TrueHitboxConfig.DEFAULT_YELLOW);
  private static GizmoStyle greenStyle = GizmoStyle.stroke(TrueHitboxConfig.DEFAULT_GREEN);
  private static boolean failed;

  private ExtraHitboxRenderer()
  {
  }

  /** Rebuilds the cached styles. Call when the config's colors change, not per frame. */
  public static void applyColors(TrueHitboxConfig config)
  {
    yellowStyle = GizmoStyle.stroke(config.yellowColor());
    greenStyle = GizmoStyle.stroke(config.greenColor());
  }

  public static void emit(Minecraft minecraft, Entity entity)
  {
    if (failed)
      return;
    HitboxMode mode = TrueHitbox.config().mode();
    if (!mode.showYellow())
      return;
    try
    {
      emitYellow(entity);
      if (mode.showGreen())
        emitGreen(minecraft, entity);
    }
    catch (RuntimeException | LinkageError e)
    {
      failed = true;
      TrueHitbox.LOGGER.error("TrueHitbox failed while drawing hitboxes; extra boxes are disabled until restart", e);
    }
  }

  /** The box the client hit-checks: current, non-interpolated bounding box, as used by the crosshair pick. */
  private static void emitYellow(Entity entity)
  {
    if (entity.isPickable())
      Gizmos.cuboid(HitboxBoxes.hitCheckBox(entity.getBoundingBox(), entity.getPickRadius()), yellowStyle);
    // The dragon itself isn't pickable; its parts are, and vanilla draws them inside the dragon's hitboxes.
    if (entity instanceof EnderDragon dragon)
      for (EnderDragonPart part : dragon.getSubEntities())
        if (part.isPickable())
          Gizmos.cuboid(HitboxBoxes.hitCheckBox(part.getBoundingBox(), part.getPickRadius()), yellowStyle);
  }

  /** The integrated server's copy of the entity. Only exists in singleplayer or when hosting a LAN world. */
  private static void emitGreen(Minecraft minecraft, Entity entity)
  {
    IntegratedServer server = minecraft.getSingleplayerServer();
    if (server == null)
      return;
    ServerLevel level = server.getLevel(entity.level().dimension());
    if (level == null)
      return;
    Entity serverEntity;
    try
    {
      // Read from the render thread while the server thread may be adding or removing entities, the same way
      // vanilla's own server-hitbox debug code does. A torn read can at worst miss an entity or throw; in that
      // case skip the green box for this entity and frame.
      serverEntity = level.getEntity(entity.getId());
    }
    catch (RuntimeException e)
    {
      return;
    }
    if (serverEntity == null || serverEntity.getType() != entity.getType())
      return;
    Gizmos.cuboid(serverEntity.getBoundingBox(), greenStyle);
  }
}
