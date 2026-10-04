package io.github.zigag1999.truehitbox.config;

import java.util.Locale;

/**
 * Which of TrueHitbox's extra boxes are drawn. Vanilla's white box is never affected.
 */
public enum HitboxMode
{
  /** Yellow (client hit-check) and green (server-side) boxes. */
  ALL("all"),
  /** Only the yellow (client hit-check) box. */
  YELLOW_ONLY("yellow_only"),
  /** No extra boxes. */
  OFF("off");

  private static final HitboxMode[] VALUES = values();

  private final String id;

  HitboxMode(String id)
  {
    this.id = id;
  }

  public String id()
  {
    return id;
  }

  public boolean showYellow()
  {
    return this != OFF;
  }

  public boolean showGreen()
  {
    return this == ALL;
  }

  /** The mode after this one in the keybind cycle: all, yellow only, off, all, ... */
  public HitboxMode next()
  {
    return VALUES[(ordinal() + 1) % VALUES.length];
  }

  /** Parses a mode id, ignoring case and surrounding whitespace. Returns null if unknown. */
  public static HitboxMode byId(String id)
  {
    if (id == null)
      return null;
    String normalized = id.trim().toLowerCase(Locale.ROOT);
    for (HitboxMode mode : VALUES)
      if (mode.id.equals(normalized))
        return mode;
    return null;
  }
}
