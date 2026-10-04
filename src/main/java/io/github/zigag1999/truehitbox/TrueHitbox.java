package io.github.zigag1999.truehitbox;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.zigag1999.truehitbox.config.HitboxMode;
import io.github.zigag1999.truehitbox.config.TrueHitboxConfig;
import io.github.zigag1999.truehitbox.render.ExtraHitboxRenderer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TrueHitbox implements ClientModInitializer
{
  public static final String MOD_ID = "truehitbox";
  public static final Logger LOGGER = LoggerFactory.getLogger("TrueHitbox");

  private static TrueHitboxConfig config = TrueHitboxConfig.defaults();
  private static Path configFile;
  private static KeyMapping cycleKey;

  public static TrueHitboxConfig config()
  {
    return config;
  }

  /** The cycle keybind, exposed for the client game test. */
  public static KeyMapping cycleKey()
  {
    return cycleKey;
  }

  @Override
  public void onInitializeClient()
  {
    configFile = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".properties");
    config = loadConfig(configFile);
    ExtraHitboxRenderer.applyColors(config);

    KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, MOD_ID));
    cycleKey = KeyMappingHelper.registerKeyMapping(
        new KeyMapping("key.truehitbox.cycle_mode", InputConstants.UNKNOWN.getValue(), category));
    ClientTickEvents.END_CLIENT_TICK.register(TrueHitbox::onEndTick);
  }

  private static TrueHitboxConfig loadConfig(Path file)
  {
    try
    {
      boolean existed = Files.exists(file);
      TrueHitboxConfig loaded = TrueHitboxConfig.load(file);
      for (String problem : loaded.problems())
        LOGGER.warn("TrueHitbox config {}: {}", file, problem);
      // Write the file on first start so the colors can be found and edited.
      if (!existed)
        loaded.save(file);
      return loaded;
    }
    catch (IOException | RuntimeException e)
    {
      LOGGER.error("Could not read TrueHitbox config {}, using defaults", file, e);
      return TrueHitboxConfig.defaults();
    }
  }

  private static void onEndTick(Minecraft minecraft)
  {
    while (cycleKey.consumeClick())
      cycleMode(minecraft);
  }

  /** Advances all / yellow only / off, shows it above the hotbar and saves it. */
  public static void cycleMode(Minecraft minecraft)
  {
    HitboxMode mode = config.mode().next();
    config.setMode(mode);
    if (minecraft.player != null)
      minecraft.player.sendOverlayMessage(Component.translatable("truehitbox.mode." + mode.id()));
    try
    {
      config.save(configFile);
    }
    catch (IOException e)
    {
      LOGGER.error("Could not save TrueHitbox config {}", configFile, e);
    }
  }
}
