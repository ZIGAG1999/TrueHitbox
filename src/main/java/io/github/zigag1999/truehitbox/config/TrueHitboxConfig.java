package io.github.zigag1999.truehitbox.config;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * The small {@code truehitbox.properties} config file: display mode and box colors.
 * Bad values never throw; each falls back to its default and is reported in {@link #problems()}. A file that
 * isn't valid properties syntax at all (e.g. a broken unicode escape) throws {@link IllegalArgumentException}.
 */
public final class TrueHitboxConfig
{
  public static final HitboxMode DEFAULT_MODE = HitboxMode.ALL;
  /** Opaque yellow, ARGB. */
  public static final int DEFAULT_YELLOW = 0xFFFFFF00;
  /** Opaque green, ARGB, same as vanilla's server-entity debug color. */
  public static final int DEFAULT_GREEN = 0xFF00FF00;

  static final String KEY_MODE = "mode";
  static final String KEY_YELLOW = "yellow_color";
  static final String KEY_GREEN = "green_color";

  private HitboxMode mode;
  private final int yellowColor;
  private final int greenColor;
  private final List<String> problems;

  public TrueHitboxConfig(HitboxMode mode, int yellowColor, int greenColor)
  {
    this(mode, yellowColor, greenColor, List.of());
  }

  private TrueHitboxConfig(HitboxMode mode, int yellowColor, int greenColor, List<String> problems)
  {
    this.mode = mode;
    this.yellowColor = yellowColor;
    this.greenColor = greenColor;
    this.problems = problems;
  }

  public static TrueHitboxConfig defaults()
  {
    return new TrueHitboxConfig(DEFAULT_MODE, DEFAULT_YELLOW, DEFAULT_GREEN);
  }

  public HitboxMode mode()
  {
    return mode;
  }

  public void setMode(HitboxMode mode)
  {
    this.mode = mode;
  }

  public int yellowColor()
  {
    return yellowColor;
  }

  public int greenColor()
  {
    return greenColor;
  }

  /** Human-readable descriptions of values that were invalid and replaced by defaults. */
  public List<String> problems()
  {
    return problems;
  }

  public static TrueHitboxConfig parse(String text)
  {
    try
    {
      return parse(new StringReader(text));
    }
    catch (IOException e)
    {
      throw new IllegalStateException("StringReader cannot fail", e);
    }
  }

  public static TrueHitboxConfig parse(Reader reader) throws IOException
  {
    Properties properties = new Properties();
    properties.load(reader);
    List<String> problems = new ArrayList<>();

    HitboxMode mode = DEFAULT_MODE;
    String modeValue = properties.getProperty(KEY_MODE);
    if (modeValue != null)
    {
      HitboxMode parsed = HitboxMode.byId(modeValue);
      if (parsed != null)
        mode = parsed;
      else
        problems.add("unknown " + KEY_MODE + " '" + modeValue + "', using " + DEFAULT_MODE.id());
    }

    int yellow = parseColorProperty(properties, KEY_YELLOW, DEFAULT_YELLOW, problems);
    int green = parseColorProperty(properties, KEY_GREEN, DEFAULT_GREEN, problems);
    return new TrueHitboxConfig(mode, yellow, green, List.copyOf(problems));
  }

  private static int parseColorProperty(Properties properties, String key, int fallback, List<String> problems)
  {
    String value = properties.getProperty(key);
    if (value == null)
      return fallback;
    try
    {
      return parseColor(value);
    }
    catch (IllegalArgumentException e)
    {
      problems.add("invalid " + key + " '" + value + "', using " + formatColor(fallback));
      return fallback;
    }
  }

  /**
   * Parses {@code #RRGGBB} or {@code #AARRGGBB} (the {@code #} is optional, {@code 0x} also accepted) into an
   * ARGB int. Six-digit colors are fully opaque.
   *
   * @throws IllegalArgumentException if the value is not one of those forms
   */
  public static int parseColor(String value)
  {
    String hex = value.trim();
    if (hex.startsWith("#"))
      hex = hex.substring(1);
    else if (hex.startsWith("0x") || hex.startsWith("0X"))
      hex = hex.substring(2);
    if (hex.length() != 6 && hex.length() != 8)
      throw new IllegalArgumentException("expected 6 or 8 hex digits: " + value);
    for (int i = 0; i < hex.length(); i++)
      if (Character.digit(hex.charAt(i), 16) < 0)
        throw new IllegalArgumentException("not a hex digit in: " + value);
    int argb = Integer.parseUnsignedInt(hex, 16);
    return hex.length() == 6 ? 0xFF000000 | argb : argb;
  }

  /** Formats an ARGB int as {@code #AARRGGBB}. */
  public static String formatColor(int argb)
  {
    return String.format(Locale.ROOT, "#%08X", argb);
  }

  public void write(Writer writer) throws IOException
  {
    writer.write("# TrueHitbox config\n");
    writer.write("# mode: all (yellow + green), yellow_only, or off. Vanilla's white box is never changed.\n");
    writer.write(KEY_MODE + "=" + mode.id() + "\n");
    writer.write("# Colors: #RRGGBB or #AARRGGBB. Restart the game after editing colors.\n");
    writer.write(KEY_YELLOW + "=" + formatColor(yellowColor) + "\n");
    writer.write(KEY_GREEN + "=" + formatColor(greenColor) + "\n");
  }

  /** Loads the config, or returns defaults if the file doesn't exist. */
  public static TrueHitboxConfig load(Path file) throws IOException
  {
    if (!Files.exists(file))
      return defaults();
    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
    {
      return parse(reader);
    }
  }

  /** Writes the config to a temp file first and then moves it into place, so a crash can't leave half a file. */
  public void save(Path file) throws IOException
  {
    Path parent = file.toAbsolutePath().getParent();
    Files.createDirectories(parent);
    Path temp = parent.resolve(file.getFileName() + ".tmp");
    try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8))
    {
      write(writer);
    }
    try
    {
      Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
    catch (IOException e)
    {
      Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
