# Findings

From reading the decompiled 26.3 client (Mojang names; 26.x ships
unobfuscated) for HitSync, plus the original mod and two hitbox mods. Re-check
everything against Loom's generated sources before relying on exact names.

## Where the "real" box is

- Hit checks use `entity.getBoundingBox()` (inflated by
  `entity.getPickRadius()`, which is 0 for players) via
  `ProjectileUtil.getEntityHitResult`. Both the per-frame crosshair pick and
  the per-tick pick before an attack use it.
- That box sits at `entity.position()`, which only changes on client ticks
  (20/s). The model, and vanilla's F3+B box, are drawn at
  `entity.getPosition(partialTick)`, interpolated between the previous and
  current position.
- So the yellow box is simply `entity.getBoundingBox()` drawn where it is,
  with no interpolation offset. It will move in steps; that is expected.
- Remote living entities use `SteppedInterpolationHandler` in 26.x (queued
  position steps, player step = 2 ticks). Whatever `getBoundingBox()`
  returns is by definition what gets hit-checked, so draw that.
- A sprinting player moves about 0.28 blocks per tick. Player boxes are 0.6
  wide, so the white and yellow boxes can be up to half a box apart.

## Rendering in 26.3

- Vanilla F3+B is drawn by `EntityHitboxDebugRenderer`. Combat Hitboxes+
  (Apache-2.0, github.com/TrueWulf/combat-hitboxes-plus) draws there with
  `Gizmos.cuboid(entity.getBoundingBox().move(delta), GizmoStyle.stroke(...))`
  where `delta = entity.getPosition(tickProgress) - entity.position()`.
  Drawing without `delta` gives the yellow box. Use this only as a lead;
  don't copy its code.
- The original mod (1.21.5) hooked `EntityRenderer.extractHitboxes` and
  filled `EntityRenderState.serverHitboxesRenderState` from
  `EntityRenderer.getServerSideEntity`, which reads the integrated server.
  Those names may have moved or changed in 26.3.
- Check whether vanilla 26.3 already exposes server-side hitboxes (for
  example in the F3 debug options screen). If it does, use or enable that
  for the green box instead of re-implementing it.
