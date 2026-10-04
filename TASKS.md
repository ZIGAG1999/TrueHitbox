# TrueHitbox tasks

- [x] 1. Update build to 26.3 (Loom, Gradle wrapper, loader, Fabric API, Java 25), rename to TrueHitbox (`truehitbox`, `2.0.0+26.3`), LICENSE line for fork
- [x] 2. Confirm mixin targets in Loom's generated 26.3 sources (hitbox renderer, server-side hitbox support)
- [ ] 3. Yellow box (current, non-interpolated bounding box) alongside vanilla's
- [ ] 4. Green box (server-side) ported to 26.3, singleplayer/LAN host only
- [ ] 5. Config file (mode + colors) and unbound keybind cycling all / yellow only / off, with on-screen message
- [ ] 6. JUnit tests for pure logic (box offset math, config parsing, mode cycling)
- [ ] 7. README
- [ ] 8. `gradlew build` passes, one jar
- [ ] 9. runClient singleplayer test + screenshot
- [ ] 10. Test with the user's 26.3 mod set (Sodium, Iris, Lithium, FerriteCore, Entity Culling, More Culling, ImmediatelyFast, Fabric API, Mod Menu, YACL)
- [ ] 11. Self-review of the diff

## Notes
- Step 2: vanilla 26.3 has no F3 option for server hitboxes. The only built-in path is the JVM debug flag
  `MC_DEBUG_SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES` (needs `MC_DEBUG_ENABLED`), and it passes the client entity
  instead of the server entity to `showHitboxes`, so it draws the wrong box. Green is reimplemented.
- Hook: `@WrapOperation` on the `showHitboxes` call in `EntityHitboxDebugRenderer.emitGizmos`. Combat Hitboxes+
  cancels `showHitboxes` at HEAD, so injecting inside `showHitboxes` would never run alongside it.
