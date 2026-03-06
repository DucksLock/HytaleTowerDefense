# WC3-Inspired Tower Defense for Hytale

This project is building a Warcraft III style multiplayer tower defense experience inside Hytale.

## Current Progress (as of March 6, 2026)

### Working now

- Plugin bootstraps cleanly and loads config from disk.
- Arena layout is generated automatically in the configured world.
- 8 player enclaves are created and assigned by color (WC3 style player slots/colors).
- Players are teleported into their own enclave on join.
- Top-down camera is applied server-side.
- Sprint behavior is tuned for top-down movement and now behaves consistently on `W/A/S/D`.
- Debug rounds can spawn enemy batches with waypoint movement.
- Enemies animate while moving and now face movement direction while running.
- Enemy stats and round definitions are configurable through JSON.

### Partially implemented / placeholder

- `WaveManager` is still a stub.
- `GameManager` session flow is still a stub.
- `TowerMenuUI` is still a stub.
- No real tower placement/build/upgrade/sell pipeline yet.
- No combat pipeline (targeting, projectile/hit logic, leak damage, economy loop) yet.

## Project Structure: What Each Class Does and Why

### Core plugin

- `dev.duckslock.TowerDefensePlugin`
  - What: Main plugin entrypoint. Loads config, applies constants, registers events and commands, starts runtime
    services.
  - Why: Central composition root so the mod lifecycle is controlled from one place.

### Camera and movement

- `dev.duckslock.camera.TDCameraController`
  - What: Applies server camera settings for top-down control.
  - Why: WC3 TD feel depends on an RTS-like perspective and movement plane.
- `dev.duckslock.sprint.SprintMechanicController`
  - What: Enables sprint feature and adjusts directional run multipliers for top-down controls.
  - Why: Default movement tuning does not feel right for camera-forced top-down gameplay.

### Config

- `dev.duckslock.config.TDConfig`
  - What: Full config schema, defaults, and sanitization for world, arena, camera, rounds, sprint, and enemy stats.
  - Why: Keeps balancing and iteration data-driven instead of hardcoded.
- `dev.duckslock.config.TDConfigManager`
  - What: Reads/writes `config.json`, falls back to defaults, sanitizes before use.
  - Why: Reliable config boot process and persistent editable settings.
- `dev.duckslock.config.ModConfigHolder`
  - What: Global runtime holder for loaded config.
  - Why: Simple access to config from systems that do not own plugin init.

### Enclave system (player lanes/bases)

- `dev.duckslock.enclave.Enclave`
  - What: Runtime model for one player's grid, owner, color, and world-space footprint.
  - Why: Core unit of TD gameplay where each player builds and defends.
- `dev.duckslock.enclave.EnclaveColor`
  - What: WC3-style player color metadata.
  - Why: Color identity is a key part of multiplayer TD readability.
- `dev.duckslock.enclave.EnclaveAssignmentListener`
  - What: Callback contract fired when a player is assigned/reassigned to an enclave.
  - Why: Lets systems react to assignment without hard coupling.
- `dev.duckslock.enclave.EnclaveManager`
  - What: Generates arena blocks, preloads chunks, handles assignment/reconnect/release, teleports players, and tracks
    post-generation tasks.
  - Why: Owns world-space arena lifecycle and player ownership routing.

### Grid and map model

- `dev.duckslock.grid.ArenaConstants`
  - What: Runtime sizing/layout/block constants applied from config.
  - Why: Shared source of truth for world dimensions and block palette.
- `dev.duckslock.grid.GridSquare`
  - What: Single build/path/base cell model with occupancy and world mapping.
  - Why: Fundamental unit for future placement and path validation.
- `dev.duckslock.grid.GridSquareType`
  - What: Cell role enum (`BUILDABLE`, `PATH`, `BLOCKED`, `BASE`).
  - Why: Encodes gameplay semantics per cell.
- `dev.duckslock.grid.GridSquareData`
  - What: Immutable DTO for map square declarations.
  - Why: Clean input format for static or file-loaded maps.
- `dev.duckslock.grid.MapDefinition`
  - What: Static sample map data with squares and waypoint list.
  - Why: Seed content and format reference before full map tooling.
- `dev.duckslock.grid.GridManager`
  - What: In-memory grid lookup and coordinate conversion.
  - Why: Fast square queries for placement and game interactions.

### Enemy system

- `dev.duckslock.enemy.EnemyAssets`
  - What: Central asset ID constants for enemy models.
  - Why: Decouples visuals from behavior definitions.
- `dev.duckslock.enemy.EnemyType`
  - What: Enum of enemy archetypes with runtime-configurable stats.
  - Why: Defines roster and balancing knobs for wave composition.
- `dev.duckslock.enemy.Enemy`
  - What: Runtime state of one enemy (hp, waypoint progress, world ref).
  - Why: Needed to track damage, movement, and despawn lifecycle.
- `dev.duckslock.enemy.EnemySpawner`
  - What: Spawns/removes model entities in-world and tracks active enemies.
  - Why: Dedicated bridge between logical enemies and actual world entities.

### Debug wave tooling

- `dev.duckslock.commands.DebugRoundDefinition`
  - What: Immutable round model (waypoints + spawn batches).
  - Why: Separates round data from execution logic.
- `dev.duckslock.commands.DebugRoundDefinitions`
  - What: Loads/validates configured debug rounds and exposes lookup APIs.
  - Why: Centralized round parsing and validation.
- `dev.duckslock.commands.ArenaDebugRoundService`
  - What: Schedules and runs debug rounds, moves enemies by waypoint, applies movement animation/state, and handles
    cleanup.
  - Why: Fast gameplay iteration loop before full production wave system.
- `dev.duckslock.commands.DebugRoundCommand`
  - What: `/tddebuground` command interface for enabling, selecting, and spawning rounds.
  - Why: In-game manual control for balancing and rapid testing.

### Future gameplay scaffolding

- `dev.duckslock.game.GameManager`
  - What: Placeholder for session-level game flow.
  - Why: Intended home for round lifecycle, players, win/loss states.
- `dev.duckslock.wave.WaveManager`
  - What: Placeholder for production wave progression system.
  - Why: Will replace debug-only flow with real progression.
- `dev.duckslock.ui.TowerMenuUI`
  - What: Placeholder for placement/upgrade/sell UI.
  - Why: Required for actual tower gameplay loop.

## Useful Commands During Development

- Start dev server:
  - `.\gradlew.bat devServer`
- Compile:
  - `.\gradlew.bat compileJava`
- Debug rounds command:
  - `/tddebuground status`
  - `/tddebuground enable`
  - `/tddebuground set <roundId>`
  - `/tddebuground spawn [enclaveIndex] [roundId]`

## Next Steps To Make This Truly WC3 Tower Defense in Hytale

### Milestone 1: Playable core loop

1. Implement `WaveManager` with wave state machine (`prepare -> spawn -> cleanup -> next`).
2. Add player lives/base health per enclave and leak damage when enemies reach base.
3. Add gold income, bounty payout, and between-wave prep windows.
4. Move debug spawning logic behind a production wave API (keep debug as override mode).

### Milestone 2: Real tower gameplay

1. Implement tower placement validation on `GridSquare` (`BUILDABLE`, ownership, occupancy).
2. Add tower archetypes (single target, splash, slow, support aura) with WC3-like identities.
3. Add targeting rules (first/last/strong/close), attack cadence, and damage pipeline.
4. Add upgrade tiers and sell/refund behavior.

### Milestone 3: Pathing and mazing rules

1. Replace static waypoint movement with pathfinding over enclave grid.
2. Enforce "path must remain open" validation when placing towers.
3. Support dynamic path recompute for enemies already in the lane.
4. Add blocked-cell and special terrain behavior for map design variety.

### Milestone 4: WC3 polish systems

1. Add armor/damage typing and resist interactions.
2. Add special creep traits (fast, armored, magic immune, boss phases, summons).
3. Add game pacing tools: interest ticks, bonus rounds, difficulty scaling.
4. Add per-player and match-wide UI (lives, wave timer, net worth, leak alerts).

### Milestone 5: Multiplayer quality and content

1. Build robust reconnection and late-join spectator behavior.
2. Add deterministic simulation checks and stress/performance profiling.
3. Add map packs, presets, and modifiable wave scripts.
4. Add saveable presets for balance iteration and automated test scenarios.

## Definition of "WC3-feel" for this project

The game should feel WC3-like when:

- Players make meaningful economy vs defense decisions every wave.
- Tower identities are distinct and upgrade paths are strategic.
- Mazing is skillful but constrained by fair path rules.
- Leaks and wave spikes create pressure and comeback moments.
- Team readability is instant via color/lane/UI and creep behavior clarity.
