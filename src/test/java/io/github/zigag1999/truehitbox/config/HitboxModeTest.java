package io.github.zigag1999.truehitbox.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HitboxModeTest
{
  @Test
  void cyclesAllThenYellowOnlyThenOffThenAll()
  {
    assertEquals(HitboxMode.YELLOW_ONLY, HitboxMode.ALL.next());
    assertEquals(HitboxMode.OFF, HitboxMode.YELLOW_ONLY.next());
    assertEquals(HitboxMode.ALL, HitboxMode.OFF.next());
  }

  @Test
  void visibilityPerMode()
  {
    assertTrue(HitboxMode.ALL.showYellow());
    assertTrue(HitboxMode.ALL.showGreen());
    assertTrue(HitboxMode.YELLOW_ONLY.showYellow());
    assertFalse(HitboxMode.YELLOW_ONLY.showGreen());
    assertFalse(HitboxMode.OFF.showYellow());
    assertFalse(HitboxMode.OFF.showGreen());
  }

  @Test
  void byIdRoundTripsAndIsLenient()
  {
    for (HitboxMode mode : HitboxMode.values())
      assertEquals(mode, HitboxMode.byId(mode.id()));
    assertEquals(HitboxMode.YELLOW_ONLY, HitboxMode.byId("  Yellow_Only "));
    assertNull(HitboxMode.byId("green_only"));
    assertNull(HitboxMode.byId(""));
    assertNull(HitboxMode.byId(null));
  }
}
