package io.github.zigag1999.truehitbox.render;

import net.minecraft.world.phys.AABB;

/**
 * Box math, kept free of game state so it can be unit tested.
 */
public final class HitboxBoxes
{
  private HitboxBoxes()
  {
  }

  /**
   * The box the client ray-tests when picking an entity under the crosshair: the bounding box at the entity's
   * current (non-interpolated) position, inflated by its pick radius (see {@code ProjectileUtil.getEntityHitResult}).
   * Returns {@code boundingBox} itself when there is nothing to inflate, so the common case does not allocate.
   */
  public static AABB hitCheckBox(AABB boundingBox, float pickRadius)
  {
    return pickRadius == 0.0F ? boundingBox : boundingBox.inflate(pickRadius);
  }
}
