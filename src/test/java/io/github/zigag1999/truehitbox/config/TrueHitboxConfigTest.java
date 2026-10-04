package io.github.zigag1999.truehitbox.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TrueHitboxConfigTest
{
  @Test
  void emptyFileGivesDefaults()
  {
    TrueHitboxConfig config = TrueHitboxConfig.parse("");
    assertEquals(HitboxMode.ALL, config.mode());
    assertEquals(0xFFFFFF00, config.yellowColor());
    assertEquals(0xFF00FF00, config.greenColor());
    assertTrue(config.problems().isEmpty());
  }

  @Test
  void parsesAllKeys()
  {
    TrueHitboxConfig config = TrueHitboxConfig.parse("""
        # comment
        mode = off
        yellow_color=#80FFAA00
        green_color=00ff00
        """);
    assertEquals(HitboxMode.OFF, config.mode());
    assertEquals(0x80FFAA00, config.yellowColor());
    assertEquals(0xFF00FF00, config.greenColor());
    assertTrue(config.problems().isEmpty());
  }

  @Test
  void badValuesFallBackPerKeyAndAreReported()
  {
    TrueHitboxConfig config = TrueHitboxConfig.parse("""
        mode=sideways
        yellow_color=#12345
        green_color=#FF0000
        """);
    assertEquals(HitboxMode.ALL, config.mode());
    assertEquals(TrueHitboxConfig.DEFAULT_YELLOW, config.yellowColor());
    assertEquals(0xFFFF0000, config.greenColor());
    assertEquals(2, config.problems().size());
  }

  @Test
  void parseColorForms()
  {
    assertEquals(0xFFFFFF00, TrueHitboxConfig.parseColor("#FFFF00"));
    assertEquals(0xFFFFFF00, TrueHitboxConfig.parseColor("ffff00"));
    assertEquals(0x40FFFF00, TrueHitboxConfig.parseColor("0x40ffff00"));
    assertEquals(0x00000000, TrueHitboxConfig.parseColor(" #00000000 "));
    assertThrows(IllegalArgumentException.class, () -> TrueHitboxConfig.parseColor(""));
    assertThrows(IllegalArgumentException.class, () -> TrueHitboxConfig.parseColor("#FFF"));
    assertThrows(IllegalArgumentException.class, () -> TrueHitboxConfig.parseColor("#GGGGGG"));
    // Integer.parseUnsignedInt would accept a sign; colors must not.
    assertThrows(IllegalArgumentException.class, () -> TrueHitboxConfig.parseColor("+FFFFFF"));
    assertThrows(IllegalArgumentException.class, () -> TrueHitboxConfig.parseColor("#FFFFFFFFF"));
  }

  @Test
  void formatColorRoundTrips()
  {
    assertEquals("#FFFFFF00", TrueHitboxConfig.formatColor(0xFFFFFF00));
    assertEquals("#0000FF00", TrueHitboxConfig.formatColor(0x0000FF00));
    for (int argb : new int[] {0, -1, 0x80123456, 0xFF00FF00})
      assertEquals(argb, TrueHitboxConfig.parseColor(TrueHitboxConfig.formatColor(argb)));
  }

  @Test
  void writeThenParseRoundTrips() throws IOException
  {
    TrueHitboxConfig original = new TrueHitboxConfig(HitboxMode.YELLOW_ONLY, 0x80ABCDEF, 0xFF123456);
    StringWriter out = new StringWriter();
    original.write(out);
    TrueHitboxConfig parsed = TrueHitboxConfig.parse(out.toString());
    assertEquals(original.mode(), parsed.mode());
    assertEquals(original.yellowColor(), parsed.yellowColor());
    assertEquals(original.greenColor(), parsed.greenColor());
    assertTrue(parsed.problems().isEmpty());
  }

  @Test
  void loadMissingFileGivesDefaultsAndSaveCreatesIt(@TempDir Path dir) throws IOException
  {
    Path file = dir.resolve("sub").resolve("truehitbox.properties");
    TrueHitboxConfig config = TrueHitboxConfig.load(file);
    assertEquals(HitboxMode.ALL, config.mode());
    assertFalse(Files.exists(file));

    config.setMode(HitboxMode.OFF);
    config.save(file);
    config.setMode(HitboxMode.YELLOW_ONLY);
    config.save(file);
    assertEquals(HitboxMode.YELLOW_ONLY, TrueHitboxConfig.load(file).mode());
    assertFalse(Files.exists(file.resolveSibling("truehitbox.properties.tmp")));
  }
}
