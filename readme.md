# TrueHitbox

A client-side Fabric mod for Minecraft 26.3 that shows where an entity's hitbox
really is, not just where its model is drawn.

Turn on hitboxes with **F3+B**. TrueHitbox adds up to two boxes next to the
vanilla one:

| Color | What it is | Where it works |
|-------|------------|----------------|
| **White** | Vanilla's hitbox, unchanged. Drawn where the model is drawn. | Everywhere |
| **Yellow** | The box your client actually hit-checks when you aim and attack. | Everywhere, including multiplayer servers |
| **Green** | The server's copy of the entity's box. | Only when your game runs the server: singleplayer, or hosting a LAN world |

## Why white and yellow differ

Entities move in steps, once per game tick (20 times a second). To look
smooth, the game draws the model *in between* the last two tick positions,
based on how far into the current tick the frame is. Vanilla's white box
follows the model.

Hit detection doesn't use that in-between position. The crosshair and attack
checks use the entity's bounding box at its *current* tick position. That box
is the yellow one. It moves in small jumps, once per tick, and is usually a
little ahead of the model when an entity moves. That gap is what you're
looking at. At sprinting speed it can be up to about half a player width.
Right after an entity turns around, the model can briefly be ahead of the
yellow box instead, because it is still catching up from the old direction.

For a few entities, such as fireballs and wind charges, the hit check uses a
slightly larger box (their "pick radius"), and the yellow box includes that.
Entities you can't hit (items, experience orbs, arrows and so on) have no
yellow box. For the ender dragon, the yellow boxes are its body parts, because
those are what gets hit.

## Green: the server-side box

When your game runs the server itself (singleplayer, or a LAN world you
host), TrueHitbox can read the server's copy of each entity and draw its box
in green. On someone else's server, LAN world or realm, your client doesn't
have that information, so there is no green box.

Green usually sits a bit ahead of yellow, because the client receives
positions from the server a little late and smooths them out.

## Keybind and config

**Cycle extra hitboxes** (Options → Controls → Key Binds → TrueHitbox) is
unbound by default. Each press switches between:

1. all boxes (yellow + green)
2. yellow only
3. off (vanilla's white box only)

A short message above the hotbar shows the new mode. The choice is saved to
`config/truehitbox.properties`, which you can also edit by hand:

```properties
# all, yellow_only or off
mode=all
# #RRGGBB or #AARRGGBB; restart the game after editing colors
yellow_color=#FFFFFF00
green_color=#FF00FF00
```

Vanilla also uses yellow for the small "riding position" box on mounted
entities. If that is confusing, change `yellow_color`.

## Purely visual

TrueHitbox only draws boxes. It does not change hitboxes, targeting, reach,
attacks or movement, sends nothing to the server and has no networking, so it
behaves the same on any server. It adds to vanilla's F3+B rendering instead
of replacing it, and is built to work next to other hitbox mods such as
Combat Hitboxes and Combat Hitboxes+ (not tested together yet).

## Requirements

- Minecraft 26.3
- Fabric Loader 0.19.5 or newer
- Fabric API

## Installing

TrueHitbox isn't on Modrinth or CurseForge. Build the jar yourself (see
Building below) and put `build/libs/truehitbox-<version>.jar` in your
instance's `mods` folder next to Fabric API.

## Credits

TrueHitbox is a fork of
[Entity Desync Viewer](https://github.com/crazysmc/entity-desync-viewer) by
**crazysmc**, which showed server-side hitboxes in green in singleplayer.
Both are under the MIT license, see [LICENSE](LICENSE).

## Building

```
gradlew build              # mod jar in build/libs, runs unit tests
gradlew runClientGameTest  # in-game test: singleplayer + dedicated server
```
