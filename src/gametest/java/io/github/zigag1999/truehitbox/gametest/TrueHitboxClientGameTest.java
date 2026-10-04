package io.github.zigag1999.truehitbox.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.zigag1999.truehitbox.TrueHitbox;
import io.github.zigag1999.truehitbox.config.HitboxMode;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Plays the mod in a real client: a singleplayer world (yellow + green expected) and a dedicated server
 * (yellow only, since there is no integrated server to read). Walking and running mobs are driven back and forth in
 * front of the camera, F3+B and the cycle keybind are pressed for real, and each screenshot is scanned for
 * yellow and green box pixels.
 */
public class TrueHitboxClientGameTest implements FabricClientGameTest
{
  private static final String TAG = "truehitbox_test";
  private static final double SPEED_WALK = 1.0;
  private static final double SPEED_RUN = 2.0;
  private static final int MIN_PIXELS = 20;

  @Override
  public void runTest(ClientGameTestContext context)
  {
    context.getInput().resizeWindow(1280, 720);
    context.runOnClient(mc -> TrueHitbox.config().setMode(HitboxMode.ALL));
    // The cycle key is unbound by default; bind it to H for the test.
    context.runOnClient(mc -> {
      if (!TrueHitbox.cycleKey().isUnbound())
        throw new AssertionError("cycle key should be unbound by default");
      TrueHitbox.cycleKey().setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_H));
      KeyMapping.resetMapping();
    });

    try (TestSingleplayerContext singleplayer = context.worldBuilder().create())
    {
      TestServerConnection connection = singleplayer.getConnection();
      setUpScene(context, singleplayer.getServer(), connection);

      toggleHitboxes(context, true);
      driveMobs(context, singleplayer.getServer(), 60);
      Scan all = shoot(context, singleplayer.getServer(), "singleplayer_all");
      all.expect(true, true);
      // A few more frames, so at least one shows the yellow box clearly ahead of the model.
      for (int i = 0; i < 4; i++)
        shoot(context, singleplayer.getServer(), "singleplayer_all_" + i).expect(true, true);

      pressCycleKey(context, HitboxMode.YELLOW_ONLY);
      shoot(context, singleplayer.getServer(), "singleplayer_yellow_only").expect(true, false);

      pressCycleKey(context, HitboxMode.OFF);
      shoot(context, singleplayer.getServer(), "singleplayer_off").expect(false, false);

      pressCycleKey(context, HitboxMode.ALL);
      takeHudScreenshot(context, "singleplayer_message_hud");
      shoot(context, singleplayer.getServer(), "singleplayer_all_again").expect(true, true);

      toggleHitboxes(context, false);
      shoot(context, singleplayer.getServer(), "singleplayer_hitboxes_off").expect(false, false);
    }

    try (TestDedicatedServerContext server = context.worldBuilder().createServer())
    {
      try (TestDedicatedServerConnection connection = server.connect())
      {
        setUpScene(context, server, connection);
        toggleHitboxes(context, true);
        driveMobs(context, server, 60);
        // No integrated server: yellow must show, green must be skipped.
        shoot(context, server, "dedicated_all").expect(true, false);
        for (int i = 0; i < 2; i++)
          shoot(context, server, "dedicated_all_" + i).expect(true, false);
        toggleHitboxes(context, false);
      }
    }
  }

  private static void setUpScene(ClientGameTestContext context, TestServerContext server,
                                 TestServerConnection connection)
  {
    connection.waitForChunksRender();
    server.runCommand("gamemode creative @a");
    server.runCommand("time set noon");
    server.runCommand("kill @e[type=!player]");
    // Camera at the origin looking south (+Z) and down at a strip where the mobs run east-west.
    server.runCommand("tp @a 0.5 -60 0.5");
    server.runCommand("summon pig -3 -60 5 {Tags:[\"" + TAG + "\",\"walk\"],PersistenceRequired:1b}");
    server.runCommand("summon wolf 4 -60 6 {Tags:[\"" + TAG + "\",\"run\"],PersistenceRequired:1b}");
    server.runCommand("summon cow -3 -60 8 {Tags:[\"" + TAG + "\",\"run\"],PersistenceRequired:1b}");
    context.waitTicks(20);
    connection.waitForChunksRender();
    context.getInput().lookAt(new BlockPos(0, -61, 6));
    context.runOnClient(mc -> setHudHidden(mc, true));
  }

  /** Sends each test mob to the far end of its lane whenever it has arrived, so they keep moving. */
  private static void driveMobs(ClientGameTestContext context, TestServerContext server, int ticks)
  {
    for (int i = 0; i < ticks; i++)
    {
      server.runOnServer(TrueHitboxClientGameTest::steerMobs);
      context.waitTick();
    }
  }

  private static void steerMobs(MinecraftServer server)
  {
    ServerLevel level = server.overworld();
    for (Entity entity : level.getAllEntities())
    {
      if (!(entity instanceof Mob mob) || !mob.entityTags().contains(TAG))
        continue;
      // Only our steering moves them, so they stay in their lanes in front of the camera.
      mob.removeAllGoals(goal -> true);
      if (!mob.getNavigation().isDone())
        continue;
      double targetX = mob.getX() < 0.5 ? 7.5 : -6.5;
      double speed = mob.entityTags().contains("run") ? SPEED_RUN : SPEED_WALK;
      mob.getNavigation().moveTo(targetX, mob.getY(), mob.getZ(), speed);
    }
  }

  private static void toggleHitboxes(ClientGameTestContext context, boolean on)
  {
    boolean current = context.computeOnClient(mc -> mc.debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES));
    if (current != on)
    {
      // Real F3+B key presses, the same path a player uses.
      context.getInput().holdKey(options -> options.keyDebugModifier);
      context.getInput().pressKey(options -> options.keyDebugShowHitboxes);
      context.getInput().releaseKey(options -> options.keyDebugModifier);
      context.waitTick();
    }
    boolean now = context.computeOnClient(mc -> mc.debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES));
    if (now != on)
      throw new AssertionError("F3+B did not turn hitboxes " + (on ? "on" : "off"));
  }

  private static void pressCycleKey(ClientGameTestContext context, HitboxMode expected)
  {
    context.getInput().pressKey(TrueHitbox.cycleKey());
    context.waitTick();
    HitboxMode mode = context.computeOnClient(mc -> TrueHitbox.config().mode());
    if (mode != expected)
      throw new AssertionError("cycle key gave " + mode + ", expected " + expected);
    Path configFile = FabricLoader.getInstance().getConfigDir()
        .resolve("truehitbox.properties");
    try
    {
      String saved = Files.readString(configFile);
      if (!saved.contains("mode=" + expected.id()))
        throw new AssertionError("config file not saved with mode " + expected.id() + ":\n" + saved);
    }
    catch (IOException e)
    {
      throw new UncheckedIOException(e);
    }
  }

  private static Scan shoot(ClientGameTestContext context, TestServerContext server, String name)
  {
    driveMobs(context, server, 3);
    Path file = grabFrame(context, name);
    Scan scan = Scan.of(name, file);
    System.out.println(scan);
    return scan;
  }

  private static void takeHudScreenshot(ClientGameTestContext context, String name)
  {
    // Clear the "Saved screenshot" chat lines so the mode message above the hotbar is readable.
    context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
    context.runOnClient(mc -> setHudHidden(mc, false));
    context.waitTick();
    grabFrame(context, name);
    context.runOnClient(mc -> setHudHidden(mc, true));
  }

  /**
   * Takes a screenshot with vanilla's screenshot key. Fabric's {@code takeScreenshot} re-renders a frame outside
   * the normal frame loop, where vanilla's debug gizmos (all F3+B boxes, white included) are not collected, so it
   * can't be used to check hitboxes. F2 captures the frame the player actually sees.
   */
  private static Path grabFrame(ClientGameTestContext context, String name)
  {
    Path dir = context.computeOnClient(mc -> mc.gameDirectory.toPath().resolve("screenshots"));
    try
    {
      Files.createDirectories(dir);
      Set<Path> before = listPngs(dir);
      context.getInput().pressKey(options -> options.keyScreenshot);
      Path[] found = new Path[1];
      context.waitFor(mc -> {
        Set<Path> now = listPngs(dir);
        now.removeAll(before);
        if (now.isEmpty())
          return false;
        found[0] = now.iterator().next();
        try
        {
          // Written asynchronously; wait until the file is a complete PNG.
          return ImageIO.read(found[0].toFile()) != null;
        }
        catch (IOException e)
        {
          return false;
        }
      }, 200);
      Path target = dir.resolve("truehitbox_" + name + ".png");
      Files.move(found[0], target, StandardCopyOption.REPLACE_EXISTING);
      return target;
    }
    catch (IOException e)
    {
      throw new UncheckedIOException(e);
    }
  }

  private static Set<Path> listPngs(Path dir)
  {
    try (Stream<Path> files = Files.list(dir))
    {
      return files.filter(p -> p.getFileName().toString().endsWith(".png")
                               && !p.getFileName().toString().startsWith("truehitbox_"))
          .collect(Collectors.toCollection(HashSet::new));
    }
    catch (IOException e)
    {
      throw new UncheckedIOException(e);
    }
  }

  private static void setHudHidden(Minecraft minecraft, boolean hidden)
  {
    if (minecraft.gui.hud.isHidden() != hidden)
      minecraft.gui.hud.toggle();
  }

  /** Counts strongly yellow and strongly green pixels. Nothing else in the test scene has those colors. */
  private record Scan(String name, Path file, int yellow, int green)
  {
    static Scan of(String name, Path file)
    {
      try
      {
        BufferedImage image = ImageIO.read(file.toFile());
        int yellow = 0;
        int green = 0;
        for (int y = 0; y < image.getHeight(); y++)
          for (int x = 0; x < image.getWidth(); x++)
          {
            int rgb = image.getRGB(x, y);
            int r = rgb >> 16 & 0xFF;
            int g = rgb >> 8 & 0xFF;
            int b = rgb & 0xFF;
            if (r > 200 && g > 200 && b < 60)
              yellow++;
            else if (r < 60 && g > 200 && b < 60)
              green++;
          }
        return new Scan(name, file, yellow, green);
      }
      catch (IOException e)
      {
        throw new UncheckedIOException(e);
      }
    }

    void expect(boolean wantYellow, boolean wantGreen)
    {
      if ((yellow >= MIN_PIXELS) != wantYellow || (green >= MIN_PIXELS) != wantGreen)
        throw new AssertionError(String.format(Locale.ROOT, "%s: expected yellow=%s green=%s, got %s",
                                               name, wantYellow, wantGreen, this));
    }

    @Override
    public String toString()
    {
      return String.format(Locale.ROOT, "[TrueHitbox test] %s: yellow=%d green=%d (%s)", name, yellow, green, file);
    }
  }
}
