package io.github.zigag1999.truehitbox.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

class HitboxBoxesTest
{
  private static final AABB ZOMBIE = new AABB(9.7, 64.0, -3.3, 10.3, 65.95, -2.7);

  @Test
  void zeroPickRadiusReturnsTheSameBoxWithoutAllocating()
  {
    assertSame(ZOMBIE, HitboxBoxes.hitCheckBox(ZOMBIE, 0.0F));
  }

  @Test
  void pickRadiusInflatesEverySide()
  {
    AABB box = HitboxBoxes.hitCheckBox(ZOMBIE, 1.0F);
    assertEquals(8.7, box.minX, 1e-9);
    assertEquals(63.0, box.minY, 1e-9);
    assertEquals(-4.3, box.minZ, 1e-9);
    assertEquals(11.3, box.maxX, 1e-9);
    assertEquals(66.95, box.maxY, 1e-9);
    assertEquals(-1.7, box.maxZ, 1e-9);
  }

  @Test
  void hitCheckBoxStaysAtTheCurrentPosition()
  {
    // Vanilla draws the white box moved by getPosition(partialTick) - position(); the yellow box is never moved,
    // so it stays centered on the current position regardless of partial tick.
    AABB box = HitboxBoxes.hitCheckBox(ZOMBIE, 0.0F);
    assertEquals(10.0, box.getCenter().x, 1e-9);
    assertEquals(-3.0, box.getCenter().z, 1e-9);
    assertEquals(64.0, box.minY, 1e-9);
  }
}
