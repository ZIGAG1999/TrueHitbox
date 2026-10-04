# TrueHitbox

Client-side Fabric mod for Minecraft 26.3. Fork of Entity Desync Viewer by
crazysmc (MIT). With hitboxes on (F3+B) it shows where an entity's hitbox
really is, not just where its model is drawn:

- White: vanilla hitbox, drawn at the model's interpolated position.
- Yellow (new): the box the client actually hit-checks, at the entity's
  current, non-interpolated position. Works on any server.
- Green (from the original): the server-side box. Only available when the
  game runs the server itself (singleplayer, or hosting a LAN world).

RESEARCH.md has the vanilla code findings this is based on.

## Rules

- Purely visual. The mod never changes hitboxes, targeting, reach, attacks,
  movement or anything that is sent to a server. No custom packets, no
  networking.
- Client-only (`"environment": "client"`). Dependencies: Fabric Loader and
  Fabric API only. Mod Menu / YACL integration may be optional, never
  required.
- Stay compatible with other hitbox mods (Combat Hitboxes, Combat
  Hitboxes+): add to vanilla's hitbox rendering, don't cancel or replace it.
- Performance: no allocations in per-frame hot paths, no logging in hot
  paths, no reflection at runtime. Mixins stay narrow; prefer
  `@Inject`/`@WrapOperation` over `@Overwrite`.
- Fail safe: if a mixin target is missing or anything throws inside mod
  code, log once and skip the extra boxes. Never crash the game.
- Licensing: keep the original MIT copyright line in LICENSE and add one for
  this fork. README and fabric.mod.json credit crazysmc and link the
  original repo.
- Build with official Fabric tooling and Maven repos only. No network calls
  at runtime, no telemetry.
- Don't put files in the Prism instance until told. Build the jar; I install
  it.
- Commit as ZIGAG1999 with the GitHub noreply email (already set in this
  repo's git config). `origin` is my fork. Don't push until told. Never push
  to `upstream` (push is disabled there on purpose).
