# WC3-Inspired Tower Defense for Hytale

This mod is building a Warcraft III style multiplayer tower defense loop inside Hytale.

## Current Progress (as of March 6, 2026)

### Core systems working

- World/bootstrap pipeline runs and generates arena layout.
- Enclave ownership and reconnect flow works.
- Top-down camera + directional sprint tuning works.
- Wave loop state machine is implemented per enclave.
- Lives and gold economy are stored per enclave.
- Leak handling deducts lives and ends enclave run at 0 lives.
- Kill handling resolves enemies and rewards bounty.
- Between-wave income is paid during cleanup.
- Debug rounds are routed through `WaveManager` instead of direct spawn calls.
- `/tddebuground` controls wave debug behavior through `GameManager`.

### Milestone 2 now implemented

- Grid square occupancy model supports placed tower references.
- Tower config schema exists in `TDConfig` and is loaded/sanitized from JSON.
- Tower roster exists (`ARROW`, `CANNON`, `FROST`, `MAGIC`) and stats are config-driven.
- Tower runtime model supports tiering, path lock, upgrade deltas, and sell refund.
- Tower manager exists per enclave and is ticked with wave tick cadence.
- Placement validation order is implemented:
  1. square in player enclave
  2. square type is `BUILDABLE`
  3. square is not occupied
  4. player has enough gold
- Targeting + attack loop is implemented (first/last/strongest/weakest).
- Armor reduction is applied for non-magic towers.
- `MAGIC` ignores armor.
- `FROST` applies timed slow that affects movement speed.
- Sell flow is implemented with 50% refund of base + upgrade spend.
- Command surface is available:
  - `/tdtower place <type> <worldX> <worldZ>`
  - `/tdtower upgrade <worldX> <worldZ> <A|B>`
  - `/tdtower sell <worldX> <worldZ>`
  - `/tdtower status`

### Still placeholder / next work

- `TowerMenuUI` is still placeholder (command-driven flow is active).
- Projectile visuals and VFX are not implemented yet (current combat is instant-hit).
- Full path-blocking validation/mazing constraints are not implemented yet.

## Class Responsibilities

### Plugin composition
- `dev.duckslock.TowerDefensePlugin`
  - Loads config, applies constants, applies enemy/tower stat config, wires services, registers commands.
  - Owns lifecycle start/shutdown for enclave, wave, debug-round, and tower systems.

### Config
- `dev.duckslock.config.TDConfig`
  - Defines and sanitizes world/arena/camera/gameplay/debug/enemy/tower config.
  - Includes upgrade tier config for tower path A/B.
- `dev.duckslock.config.TDConfigManager`
  - Load-or-create config and persist sanitized output.
- `dev.duckslock.config.ModConfigHolder`
  - Global runtime config holder for systems initialized after plugin setup.

### Enclave + arena
- `dev.duckslock.enclave.Enclave`
  - Runtime player lane/base state: owner, grid, lives, gold, economy/lives reset helpers.
- `dev.duckslock.enclave.EnclaveManager`
  - Arena generation/bootstrap, assignment/release/teleport, post-generation task queue.
  - Creates enclaves with configured starting lives/gold.
- `dev.duckslock.enclave.EnclaveAssignmentListener`
  - Callback for assignment events.
- `dev.duckslock.enclave.EnclaveColor`
  - WC3 color slot metadata.
- `dev.duckslock.grid.ArenaConstants`
  - Shared layout constants derived from config.
- `dev.duckslock.grid.GridSquare`
  - Build cell state including `occupied`, `tower`, and `isAvailable()`.
- `dev.duckslock.grid.GridSquareType`
  - Cell role enum (`BUILDABLE`, `PATH`, `BLOCKED`, `BASE`).
- `dev.duckslock.grid.GridSquareData`, `MapDefinition`, `GridManager`
  - Static map/grid utility model used by broader arena/grid logic.

### Enemy + movement runtime
- `dev.duckslock.enemy.EnemyType`
  - Configurable enemy archetypes with speed/armor/bounty/damage.
- `dev.duckslock.enemy.Enemy`
  - Runtime enemy state: hp, waypoint progress, distance-to-next-waypoint, slow state, entity ref.
- `dev.duckslock.enemy.EnemySpawner`
  - Spawn/remove world entities for enemy runtime objects.
- `dev.duckslock.enemy.EnemyAssets`
  - Enemy model asset ID constants.
- `dev.duckslock.commands.ArenaDebugRoundService`
  - Internal spawn/movement worker for wave-driven debug rounds.
  - Tracks active enemies, moves by waypoints, applies facing/animation/movement states.
  - Exposes enemy list + kill resolution hooks used by tower combat loop.

### Wave + game orchestration

- `dev.duckslock.wave.WaveManager`
  - Per-enclave wave state machine:
    - `IDLE -> PREPARE -> SPAWNING -> ACTIVE -> CLEANUP -> IDLE`
  - Handles countdown/cleanup timers, active enemy counts, early-wave flag, and resolve callbacks.
- `dev.duckslock.game.GameManager`
  - Owns per-enclave `WaveManager` + `TowerManager`.
  - Ticks both systems on one scheduler.
  - Routes debug-round start requests.
  - Handles enclave loss reset behavior.

### Tower gameplay (Milestone 2)

- `dev.duckslock.tower.TowerType`
  - Config-driven base stats + upgrade path definitions.
- `dev.duckslock.tower.UpgradeTier`
  - Upgrade data record (`cost`, `name`, `statDeltas`).
- `dev.duckslock.tower.UpgradePath`
  - `PATH_A` / `PATH_B`.
- `dev.duckslock.tower.TargetPriority`
  - Target selection mode (`FIRST`, `LAST`, `STRONGEST`, `WEAKEST`).
- `dev.duckslock.tower.Tower`
  - Runtime placed tower state: type, tier, path, square/enclave ref, entity ref, target, attack timing.
  - Applies upgrade deltas and computes effective stats.
- `dev.duckslock.tower.TowerManager`
  - Per-enclave tower owner.
  - Placement validation and spawn, upgrade, sell/remove, attack tick, target selection.
  - Applies armor/slow/magic-ignore rules and resolves kills through debug-round runtime.

### Commands
- `dev.duckslock.commands.DebugRoundDefinition`
  - Immutable debug round model.
- `dev.duckslock.commands.DebugRoundDefinitions`
  - Parse/validate configured debug rounds.
- `dev.duckslock.commands.DebugRoundCommand`
  - `/tddebuground` control surface routed to `GameManager`/`WaveManager`.
- `dev.duckslock.commands.TowerCommand`
  - `/tdtower` command surface for placement/upgrade/sell/status.

### Camera + movement control

- `dev.duckslock.camera.TDCameraController`
  - Applies custom top-down camera packet settings.
- `dev.duckslock.sprint.SprintMechanicController`
  - Applies directional sprint fixes for top-down movement.

### UI placeholder
- `dev.duckslock.ui.TowerMenuUI`
  - Reserved for menu-driven placement/upgrade/sell flow (not active yet).

## Development Commands

- Start dev server:
  - `.\gradlew.bat devServer`
- Compile:
  - `.\gradlew.bat build`
- Wave debug:
  - `/tddebuground status`
  - `/tddebuground enable`
  - `/tddebuground set <roundId>`
  - `/tddebuground spawn [enclaveIndex] [roundId]`
- Tower debug:
  - `/tdtower status`
  - `/tdtower place <type> <worldX> <worldZ>`
  - `/tdtower upgrade <worldX> <worldZ> <A|B>`
  - `/tdtower sell <worldX> <worldZ>`

## Next Steps

### Milestone 3 (Pathing and Mazing Rules)

1. Replace static waypoint-only routing with grid pathfinding.
2. Enforce "path must remain open" validation during placement.
3. Recompute active enemy paths safely when layout changes.
4. Add dedicated blocked/special terrain lane behavior.

### Milestone 4 (Polish and Game Feel)

1. Add projectile visuals and hit VFX.
2. Add richer armor/damage typing interactions.
3. Add stronger wave pacing controls and difficulty scaling.
4. Surface full game HUD and tower UI (replace command-only flow).

### Milestone 5 (Content and Multiplayer Robustness)

1. Add more tower rosters and enemy variants/boss behaviors.
2. Add map presets and wave scripting packs.
3. Improve reconnect/spectator/end-state handling.
4. Add repeatable balancing scenarios and stress tests.
