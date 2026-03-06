# WC3-Inspired Tower Defense for Hytale

This mod is building a Warcraft III style multiplayer tower defense loop inside Hytale.

## Current Progress (as of March 6, 2026)

### Core systems working

- World/bootstrap pipeline runs and generates arena layout.
- Active map now loads from JSON resource at startup (`maps/ElementTD.json`).
- Map registry exists with an active-map concept (prepared for future map switching).
- Arena block generation now uses map square data (`PATH`, `BUILDABLE`, `BASE`, `BLOCKED`).
- Grid manager now uses loaded `MapDefinition` data for world/grid conversions.
- Enclave ownership regions are now derived from active-map bounds (partitioned map regions), not fixed 10x10 debug
  grids.
- Enclave ownership and reconnect flow works.
- Top-down camera + directional sprint tuning works.
- Wave loop state machine is implemented per enclave.
- Lives and gold economy are stored per enclave.
- Leak handling now follows WC3 loop behavior: leaks cost life and respawn at path start until killed.
- Kill handling resolves enemies and rewards bounty.
- Between-wave income is paid during cleanup.
- Interest payout is active every 15 seconds based on unspent gold.
- Element wheel damage modifiers are active in tower combat.
- Wave definitions can now be sourced from extracted WC3 reference data (`wc3/ElementTDReference.json`) with 60-wave
  HP/speed/bounty scaling.
- Debug rounds are routed through `WaveManager` instead of direct spawn calls.
- `/tddebuground` controls wave debug behavior through `GameManager`.
- Debug enemy movement can use active-map waypoints when present in map JSON.
- Every 5 cleared waves grants progression toward element picks (first free pick token, later boss-gated token flow).

### Milestone 2 now implemented

- Grid square occupancy model supports placed tower references.
- Tower config schema exists in `TDConfig` and is loaded/sanitized from JSON.
- Tower roster now includes legacy debug towers plus WC3-derived towers (`n000`..`n009`, `n01C`, `n021`, `n02G`) with
  copied baseline damage/range/cooldown/cost values.
- Tower runtime model supports tiering, path lock, upgrade deltas, and sell refund.
- Tower manager exists per enclave and is ticked with wave tick cadence.
- Towers now place with a WC3-style `2x2` footprint (all four squares must be buildable and free).
- Placement validation order is implemented:
  1. square in player enclave
  2. square type is `BUILDABLE`
  3. square is not occupied
  4. player has enough gold
- Targeting + attack loop is implemented (first/last/strongest/weakest).
- Attack profile support now includes `DIRECT`, `SPLASH`, and `CHAIN` behaviors for WC3-like tower attack styles.
- Trickery support flow exists (`h000` / `WC3_TRICKERY`): one-time support buff can be applied, and sell/rebuild can
  chain-buff multiple towers.
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
- `ElementTD.json` is now generated from WC3 `war3map.wpm` (640x384 pathing-grid parity).
- `ElementTD.json` still has an empty `waypoints` array and needs real route points added.
- Full tower-by-tower WC3 spell parity is still in progress (current Trickery support behavior is implemented as a
  simplified parity model).

## Class Responsibilities

### Plugin composition
- `dev.duckslock.TowerDefensePlugin`
  - Loads config, applies constants, applies enemy/tower stat config, wires services, registers commands.
  - Loads map resources into `MapRegistry`, sets an active map, and injects map services.
  - Owns lifecycle start/shutdown for enclave, wave, debug-round, tower, and map systems.

### Config
- `dev.duckslock.config.TDConfig`
  - Defines and sanitizes world/arena/camera/gameplay/debug/enemy/tower config.
  - Includes upgrade tier config for tower path A/B.
  - Includes combat element typing for enemies/towers and interest settings.
- `dev.duckslock.config.TDConfigManager`
  - Load-or-create config and persist sanitized output.
- `dev.duckslock.config.ModConfigHolder`
  - Global runtime config holder for systems initialized after plugin setup.

### Enclave + arena
- `dev.duckslock.enclave.Enclave`
  - Runtime player lane/base state: owner, grid, lives, gold, interest rate, and element-pick tokens.
- `dev.duckslock.enclave.EnclaveManager`
  - Arena generation/bootstrap, assignment/release/teleport, post-generation task queue.
  - Creates enclaves with configured starting lives/gold.
  - Generates arena blocks from the active map definition and preloads chunks for map bounds.
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
- `dev.duckslock.grid.GridSquareData`
  - JSON-backed square descriptor (`gridX`, `gridZ`, `type`).
- `dev.duckslock.grid.MapDefinition`
  - JSON map model (`name`, `gridWidth`, `gridHeight`, `squares`, `waypoints`) + classpath loader.
- `dev.duckslock.grid.MapRegistry`
  - Runtime map catalog with active-map selection (foundation for on-the-fly switching).
- `dev.duckslock.grid.GridManager`
  - Active-map square lookup and world<->grid coordinate conversion.

### Enemy + movement runtime
- `dev.duckslock.enemy.EnemyType`
  - Configurable enemy archetypes with speed/armor/bounty/damage and enemy element typing.
- `dev.duckslock.enemy.Enemy`
  - Runtime enemy state: hp, waypoint progress, distance-to-next-waypoint, slow state, entity ref.
  - Supports per-spawn stat overrides (used for WC3-derived wave HP/speed/bounty/leak damage).
- `dev.duckslock.enemy.EnemySpawnProfile`
  - Per-batch enemy stat override payload attached to wave spawn batches.
- `dev.duckslock.enemy.EnemySpawner`
  - Spawn/remove world entities for enemy runtime objects.
- `dev.duckslock.enemy.EnemyAssets`
  - Enemy model asset ID constants.
- `dev.duckslock.commands.ArenaDebugRoundService`
  - Internal spawn/movement worker for wave-driven debug rounds.
  - Tracks active enemies, moves by waypoints, applies facing/animation/movement states.
  - Uses active-map waypoints when available, otherwise falls back to per-round local waypoints.
  - Implements WC3 leak-loop rule by respawning leaked creeps at path start and deducting life.
  - Exposes enemy list + kill resolution hooks used by tower combat loop.

### Combat rules

- `dev.duckslock.combat.ElementType`
  - Damage element model (`FIRE/NATURE/EARTH/LIGHT/DARKNESS/WATER/COMPOSITE/NONE`).
- `dev.duckslock.combat.ElementWheel`
  - WC3-style elemental multiplier rules and composite-vs-non-elemental handling.

### Wave + game orchestration

- `dev.duckslock.wave.WaveManager`
  - Per-enclave wave state machine:
    - `IDLE -> PREPARE -> SPAWNING -> ACTIVE -> CLEANUP -> IDLE`
  - Handles countdown/cleanup timers, active enemy counts, early-wave flag, and resolve callbacks.
  - Adds 5-wave milestone progression with free/boss-gated element-pick token flow.
- `dev.duckslock.game.GameManager`
  - Owns per-enclave `WaveManager` + `TowerManager`.
  - Ticks both systems on one scheduler.
  - Pays periodic interest income from unspent gold.
  - Routes debug-round start requests.
  - Handles enclave loss reset behavior.

### Tower gameplay (Milestone 2)

- `dev.duckslock.tower.TowerType`
  - Config-driven base stats + upgrade path definitions + damage element type.
  - Includes WC3 unit-id mapping and built-in attack profile metadata.
- `dev.duckslock.tower.UpgradeTier`
  - Upgrade data record (`cost`, `name`, `statDeltas`).
- `dev.duckslock.tower.UpgradePath`
  - `PATH_A` / `PATH_B`.
- `dev.duckslock.tower.TowerAttackKind`
  - Runtime attack behavior selection (`DIRECT`, `SPLASH`, `CHAIN`, `SUPPORT_TRICKERY`).
- `dev.duckslock.tower.TargetPriority`
  - Target selection mode (`FIRST`, `LAST`, `STRONGEST`, `WEAKEST`).
- `dev.duckslock.tower.Tower`
  - Runtime placed tower state: type, tier, path, square/enclave ref, entity ref, target, attack timing.
  - Applies upgrade deltas and computes effective stats.
- `dev.duckslock.tower.TowerManager`
  - Per-enclave tower owner.
  - Placement validation and spawn, upgrade, sell/remove, attack tick, target selection.
  - Applies armor/slow/magic-ignore rules, element-wheel multipliers, and resolves kills through debug-round runtime.

### Commands
- `dev.duckslock.commands.DebugRoundDefinition`
  - Immutable debug round model.
- `dev.duckslock.commands.DebugRoundDefinitions`
  - Parse/validate configured debug rounds.
- `dev.duckslock.commands.DebugRoundCommand`
  - `/tddebuground` control surface routed to `GameManager`/`WaveManager`.
- `dev.duckslock.commands.TowerCommand`
  - `/tdtower` command surface for placement/upgrade/sell/status.
- `dev.duckslock.wc3.WC3ReferenceData`
  - Loader/model for extracted WC3 map reference JSON used to build runtime wave definitions.

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
  - `/tdtower place <type|wc3UnitId> <worldX> <worldZ>`
  - `/tdtower upgrade <worldX> <worldZ> <A|B>`
  - `/tdtower sell <worldX> <worldZ>`

## Converter Utilities

- `converter/wc3_wpm_to_hytale_map.py`
  - Converts WC3 `war3map.wpm` pathing data to `MapDefinition` JSON.
- `converter/extract_element_td_reference.py`
  - Extracts wave/tower reference data from converted WC3 files into `src/main/resources/wc3/ElementTDReference.json`.

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
3. Add runtime map switching API/command and safely reset active systems between map changes.
4. Improve reconnect/spectator/end-state handling.
5. Add repeatable balancing scenarios and stress tests.
