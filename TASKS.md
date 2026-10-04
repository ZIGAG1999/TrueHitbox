# TrueHitbox tasks

- [x] 1. Update build to 26.3 (Loom, Gradle wrapper, loader, Fabric API, Java 25), rename to TrueHitbox (`truehitbox`, `2.0.0+26.3`), LICENSE line for fork
- [x] 2. Confirm mixin targets in Loom's generated 26.3 sources (hitbox renderer, server-side hitbox support)
- [x] 3. Yellow box (current, non-interpolated bounding box) alongside vanilla's
- [x] 4. Green box (server-side) ported to 26.3, singleplayer/LAN host only
- [x] 5. Config file (mode + colors) and unbound keybind cycling all / yellow only / off, with on-screen message
- [x] 6. JUnit tests for pure logic (box offset math, config parsing, mode cycling)
- [x] 7. README
- [x] 8. `gradlew build` passes, one jar
- [x] 9. In-game test in singleplayer (client game test) + screenshot; also a dedicated server run
- [x] 10. Test with the user's 26.3 mod set (Sodium, Iris, Lithium, FerriteCore, Entity Culling, More Culling, ImmediatelyFast, Fabric API, Mod Menu, YACL) with Complementary Reimagined on
- [x] 11. Fail-safe check: missing injection point logs once, no crash
- [x] 12. Self-review of the diff

## Notes
- Step 2: vanilla 26.3 has no F3 option for server hitboxes. The only built-in path is the JVM debug flag
  `MC_DEBUG_SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES` (needs `MC_DEBUG_ENABLED`), and it passes the client entity
  instead of the server entity to `showHitboxes`, so it draws the wrong box. Green is reimplemented.
- Hook: `@WrapOperation` on the `showHitboxes` call in `EntityHitboxDebugRenderer.emitGizmos`. Combat Hitboxes+
  cancels `showHitboxes` at HEAD, so injecting inside `showHitboxes` would never run alongside it.
- In-game test: `gradlew runClientGameTest`. Fabric's `takeScreenshot` re-renders outside the frame loop and
  drops all debug gizmos (vanilla's white boxes too), so the test uses vanilla's F2 screenshot instead.
- Mod set test: `gradlew runModSetGameTest -PextraMods=<folder>`; the More Culling jar also needs Cloth Config.
- Fail-safe: `defaultRequire: 1` crashed the game when the target was missing even with `"required": false`.
  Now `defaultRequire: 0` plus a mixin config plugin that logs once if the injection point is missing.
- Not tested: Combat Hitboxes / Combat Hitboxes+ installed together with TrueHitbox (no 26.3 jar here).
