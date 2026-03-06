package dev.duckslock.config;

import com.hypixel.hytale.logger.HytaleLogger;
import dev.duckslock.enemy.EnemyType;

import java.util.*;
import java.util.logging.Level;

public final class TDConfig {

    public WorldConfig world = new WorldConfig();
    public ArenaConfig arena = new ArenaConfig();
    public CameraConfig camera = new CameraConfig();
    public GameplayConfig gameplay = new GameplayConfig();
    public DebugRoundsConfig debugRounds = new DebugRoundsConfig();
    public SprintConfig sprint = new SprintConfig();
    public Map<String, EnemyConfig> enemies = defaultEnemies();

    public static TDConfig defaults() {
        return new TDConfig();
    }

    private static int positiveInt(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private static double positiveDouble(double value, double fallback) {
        return value > 0d ? value : fallback;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static List<String> defaultWalkAnimationCandidates() {
        return List.of("Walk", "walk", "Run", "run", "Movement", "movement", "Locomotion", "locomotion");
    }

    public static Map<String, EnemyConfig> defaultEnemies() {
        Map<String, EnemyConfig> map = new LinkedHashMap<>();
        map.put("GRUNT", new EnemyConfig("Zombie", 80, 1.0f, 0.00f, 5, 1, false));
        map.put("RUNNER", new EnemyConfig("Skeleton_Scout", 50, 1.6f, 0.00f, 8, 1, false));
        map.put("SOLDIER", new EnemyConfig("Skeleton_Soldier", 120, 1.1f, 0.15f, 10, 1, false));
        map.put("BRUTE", new EnemyConfig("Trork_Unarmed", 200, 0.7f, 0.30f, 12, 2, false));
        map.put("SHIELDED", new EnemyConfig("Trork_Mauler", 150, 1.0f, 0.50f, 15, 2, false));
        map.put("HUNTER", new EnemyConfig("Outlander_Hunter", 130, 1.5f, 0.10f, 18, 1, false));
        map.put("ELITE", new EnemyConfig("Outlander_Brute", 300, 0.9f, 0.40f, 25, 2, false));
        map.put("GOLEM", new EnemyConfig("Earthen_Golem", 1500, 0.5f, 0.40f, 80, 5, true));
        return map;
    }

    public static List<DebugRoundConfig> defaultDebugRounds() {
        List<DebugRoundConfig> rounds = new ArrayList<>();

        rounds.add(new DebugRoundConfig(
                1,
                "Starter",
                List.of(
                        new Vec3(1.5, 1.0, 1.5),
                        new Vec3(18.5, 1.0, 1.5),
                        new Vec3(18.5, 1.0, 10.5),
                        new Vec3(10.5, 1.0, 18.5),
                        new Vec3(1.5, 1.0, 18.5)
                ),
                List.of(
                        new SpawnBatchConfig(0L, "GRUNT", 6),
                        new SpawnBatchConfig(2500L, "RUNNER", 4)
                )
        ));

        rounds.add(new DebugRoundConfig(
                2,
                "Mixed",
                List.of(
                        new Vec3(1.5, 1.0, 1.5),
                        new Vec3(18.5, 1.0, 1.5),
                        new Vec3(18.5, 1.0, 18.5),
                        new Vec3(1.5, 1.0, 18.5)
                ),
                List.of(
                        new SpawnBatchConfig(0L, "GRUNT", 8),
                        new SpawnBatchConfig(3000L, "SOLDIER", 4),
                        new SpawnBatchConfig(6000L, "RUNNER", 6)
                )
        ));

        rounds.add(new DebugRoundConfig(
                3,
                "Heavy",
                List.of(
                        new Vec3(1.5, 1.0, 1.5),
                        new Vec3(10.5, 1.0, 1.5),
                        new Vec3(18.5, 1.0, 10.5),
                        new Vec3(10.5, 1.0, 18.5),
                        new Vec3(1.5, 1.0, 10.5)
                ),
                List.of(
                        new SpawnBatchConfig(0L, "BRUTE", 2),
                        new SpawnBatchConfig(2500L, "SHIELDED", 4),
                        new SpawnBatchConfig(6000L, "ELITE", 2)
                )
        ));

        return rounds;
    }

    public void sanitize(HytaleLogger logger) {
        if (world == null) {
            world = new WorldConfig();
        }
        if (isBlank(world.name)) {
            world.name = "default";
        }
        if (isBlank(world.arenaMarkerFile)) {
            world.arenaMarkerFile = "td_arena_generated.marker";
        }
        world.preloadMarginChunks = Math.max(0, world.preloadMarginChunks);
        world.bootstrapPollMs = Math.max(50L, world.bootstrapPollMs);

        if (arena == null) {
            arena = new ArenaConfig();
        }
        arena.squareSize = positiveInt(arena.squareSize, 2);
        arena.enclaveGridSize = positiveInt(arena.enclaveGridSize, 10);
        arena.borderThickness = Math.max(0, arena.borderThickness);
        arena.enclaveGap = Math.max(0, arena.enclaveGap);
        arena.enclavesPerRow = positiveInt(arena.enclavesPerRow, 4);
        arena.enclaveRows = positiveInt(arena.enclaveRows, 2);
        if (isBlank(arena.blockPath)) {
            arena.blockPath = "Rock_Stone";
        }
        if (isBlank(arena.blockBuildable)) {
            arena.blockBuildable = "Wood_Hardwood_Planks";
        }
        if (isBlank(arena.blockBorder)) {
            arena.blockBorder = "Rock_Marble";
        }
        if (isBlank(arena.blockBase)) {
            arena.blockBase = "Rock_Chalk";
        }

        if (camera == null) {
            camera = new CameraConfig();
        }

        if (gameplay == null) {
            gameplay = new GameplayConfig();
        }
        gameplay.startingLives = positiveInt(gameplay.startingLives, 20);
        gameplay.startingGold = Math.max(0, gameplay.startingGold);
        gameplay.betweenWaveIncome = Math.max(0, gameplay.betweenWaveIncome);
        gameplay.prepareCountdownMs = Math.max(0L, gameplay.prepareCountdownMs);
        gameplay.cleanupDurationMs = Math.max(0L, gameplay.cleanupDurationMs);
        gameplay.waveTickMs = Math.max(20L, gameplay.waveTickMs);

        if (debugRounds == null) {
            debugRounds = new DebugRoundsConfig();
        }
        debugRounds.activeRoundId = positiveInt(debugRounds.activeRoundId, 1);
        debugRounds.movementTickMs = Math.max(20L, debugRounds.movementTickMs);
        debugRounds.baseMoveBlocksPerSecond = positiveDouble(debugRounds.baseMoveBlocksPerSecond, 2.6d);
        debugRounds.waypointReachedDistance = positiveDouble(debugRounds.waypointReachedDistance, 0.35d);
        debugRounds.enemySpawnSpacingMs = Math.max(0L, debugRounds.enemySpawnSpacingMs);
        debugRounds.entityRefWaitTimeoutMs = Math.max(100L, debugRounds.entityRefWaitTimeoutMs);
        if (debugRounds.walkAnimationCandidates == null || debugRounds.walkAnimationCandidates.isEmpty()) {
            debugRounds.walkAnimationCandidates = defaultWalkAnimationCandidates();
        } else {
            List<String> candidates = new ArrayList<>();
            for (String value : debugRounds.walkAnimationCandidates) {
                if (!isBlank(value)) {
                    candidates.add(value);
                }
            }
            debugRounds.walkAnimationCandidates = candidates.isEmpty() ? defaultWalkAnimationCandidates() : candidates;
        }

        debugRounds.rounds = sanitizeRounds(debugRounds.rounds, logger);
        if (!hasRound(debugRounds.rounds, debugRounds.activeRoundId)) {
            debugRounds.activeRoundId = debugRounds.rounds.get(0).id;
        }

        if (sprint == null) {
            sprint = new SprintConfig();
        }
        sprint.backwardRunSpeedMultiplier = Math.max(0f, sprint.backwardRunSpeedMultiplier);
        sprint.strafeRunSpeedMultiplier = Math.max(0f, sprint.strafeRunSpeedMultiplier);

        Map<String, EnemyConfig> configuredEnemies = enemies == null
                ? Collections.emptyMap()
                : new LinkedHashMap<>(enemies);
        Map<String, EnemyConfig> defaultEnemies = defaultEnemies();
        Map<String, EnemyConfig> sanitizedEnemies = new LinkedHashMap<>();
        for (EnemyType type : EnemyType.values()) {
            EnemyConfig config = configuredEnemies.get(type.name());
            if (config == null) {
                config = defaultEnemies.get(type.name()).copy();
            }
            config.sanitize();
            sanitizedEnemies.put(type.name(), config);
        }
        enemies = sanitizedEnemies;
    }

    private List<DebugRoundConfig> sanitizeRounds(List<DebugRoundConfig> configured, HytaleLogger logger) {
        List<DebugRoundConfig> source = configured == null || configured.isEmpty()
                ? defaultDebugRounds()
                : configured;

        List<DebugRoundConfig> sanitized = new ArrayList<>();
        for (DebugRoundConfig round : source) {
            if (round == null || round.id <= 0) {
                continue;
            }

            DebugRoundConfig copy = new DebugRoundConfig();
            copy.id = round.id;
            copy.name = isBlank(round.name) ? "Round " + round.id : round.name;
            copy.localWaypoints = sanitizeWaypoints(round.localWaypoints);
            copy.batches = sanitizeBatches(round.batches, logger, round.id);

            if (copy.localWaypoints.size() < 2 || copy.batches.isEmpty()) {
                continue;
            }
            sanitized.add(copy);
        }

        if (sanitized.isEmpty()) {
            sanitized = sanitizeRounds(defaultDebugRounds(), logger);
        }

        sanitized.sort(Comparator.comparingInt(r -> r.id));
        return sanitized;
    }

    private List<Vec3> sanitizeWaypoints(List<Vec3> configured) {
        if (configured == null || configured.isEmpty()) {
            return List.of();
        }
        List<Vec3> result = new ArrayList<>(configured.size());
        for (Vec3 waypoint : configured) {
            if (waypoint == null) {
                continue;
            }
            result.add(new Vec3(waypoint.x, waypoint.y, waypoint.z));
        }
        return result;
    }

    private List<SpawnBatchConfig> sanitizeBatches(List<SpawnBatchConfig> configured, HytaleLogger logger, int roundId) {
        if (configured == null || configured.isEmpty()) {
            return List.of();
        }
        List<SpawnBatchConfig> result = new ArrayList<>(configured.size());
        for (SpawnBatchConfig batch : configured) {
            if (batch == null) {
                continue;
            }

            String enemyType = batch.enemyType == null ? "" : batch.enemyType.toUpperCase(Locale.ROOT);
            try {
                EnemyType.valueOf(enemyType);
            } catch (IllegalArgumentException ex) {
                logger.at(Level.WARNING).log("Skipping invalid enemy type '%s' in round %s.", batch.enemyType, roundId);
                continue;
            }

            SpawnBatchConfig copy = new SpawnBatchConfig();
            copy.delayMs = Math.max(0L, batch.delayMs);
            copy.enemyType = enemyType;
            copy.count = Math.max(1, batch.count);
            result.add(copy);
        }
        return result;
    }

    private boolean hasRound(List<DebugRoundConfig> rounds, int roundId) {
        for (DebugRoundConfig round : rounds) {
            if (round.id == roundId) {
                return true;
            }
        }
        return false;
    }

    public static final class WorldConfig {
        public String name = "default";
        public String arenaMarkerFile = "td_arena_generated.marker";
        public int preloadMarginChunks = 2;
        public long bootstrapPollMs = 500L;
    }

    public static final class ArenaConfig {
        public int squareSize = 2;
        public int enclaveGridSize = 10;
        public int borderThickness = 1;
        public int enclaveGap = 6;
        public int enclavesPerRow = 4;
        public int enclaveRows = 2;
        public int arenaFloorY = 200;
        public int arenaOriginX = 0;
        public int arenaOriginZ = 0;
        public String blockPath = "Rock_Stone";
        public String blockBuildable = "Wood_Hardwood_Planks";
        public String blockBorder = "Rock_Marble";
        public String blockBase = "Rock_Chalk";
    }

    public static final class CameraConfig {
        public float positionLerpSpeed = 0.2f;
        public float rotationLerpSpeed = 0.2f;
        public float distance = 20.0f;
        public boolean isFirstPerson = false;
        public boolean eyeOffset = true;
        public boolean displayCursor = true;
        public float rotationPitch = 0.0f;
        public float rotationYaw = -1.5707964f;
        public float rotationRoll = 0.0f;
        public float movementForcePitch = 0.0f;
        public float movementForceYaw = 0.0f;
        public float movementForceRoll = 0.0f;
        public float planeNormalX = 0.0f;
        public float planeNormalY = 1.0f;
        public float planeNormalZ = 0.0f;
    }

    public static final class GameplayConfig {
        public int startingLives = 20;
        public int startingGold = 150;
        public int betweenWaveIncome = 10;
        public long prepareCountdownMs = 5000L;
        public long cleanupDurationMs = 1500L;
        public long waveTickMs = 100L;
    }

    public static final class DebugRoundsConfig {
        public boolean enabledByDefault = false;
        public int activeRoundId = 1;
        public boolean triggerOnlyOnNewAssignment = true;
        public long movementTickMs = 100L;
        public double baseMoveBlocksPerSecond = 2.6d;
        public double waypointReachedDistance = 0.35d;
        public long enemySpawnSpacingMs = 300L;
        public long entityRefWaitTimeoutMs = 4000L;
        public List<String> walkAnimationCandidates = defaultWalkAnimationCandidates();
        public List<DebugRoundConfig> rounds = defaultDebugRounds();
    }

    public static final class SprintConfig {
        public boolean enableSprintForceFeature = true;
        public boolean applyDirectionalSprintFix = true;
        public float backwardRunSpeedMultiplier = 0.0f;
        public float strafeRunSpeedMultiplier = 0.0f;
    }

    public static final class EnemyConfig {
        public String assetId;
        public int maxHp;
        public float speed;
        public float armor;
        public int bounty;
        public int damage;
        public boolean boss;

        public EnemyConfig() {
        }

        public EnemyConfig(String assetId, int maxHp, float speed, float armor, int bounty, int damage, boolean boss) {
            this.assetId = assetId;
            this.maxHp = maxHp;
            this.speed = speed;
            this.armor = armor;
            this.bounty = bounty;
            this.damage = damage;
            this.boss = boss;
        }

        public EnemyConfig copy() {
            return new EnemyConfig(assetId, maxHp, speed, armor, bounty, damage, boss);
        }

        public void sanitize() {
            if (isBlank(assetId)) {
                assetId = "Zombie";
            }
            maxHp = positiveInt(maxHp, 1);
            speed = speed > 0f ? speed : 1.0f;
            armor = Math.max(0f, Math.min(0.95f, armor));
            bounty = Math.max(0, bounty);
            damage = Math.max(0, damage);
        }
    }

    public static final class DebugRoundConfig {
        public int id;
        public String name;
        public List<Vec3> localWaypoints = new ArrayList<>();
        public List<SpawnBatchConfig> batches = new ArrayList<>();

        public DebugRoundConfig() {
        }

        public DebugRoundConfig(int id, String name, List<Vec3> localWaypoints, List<SpawnBatchConfig> batches) {
            this.id = id;
            this.name = name;
            this.localWaypoints = new ArrayList<>(localWaypoints);
            this.batches = new ArrayList<>(batches);
        }
    }

    public static final class SpawnBatchConfig {
        public long delayMs;
        public String enemyType;
        public int count;

        public SpawnBatchConfig() {
        }

        public SpawnBatchConfig(long delayMs, String enemyType, int count) {
            this.delayMs = delayMs;
            this.enemyType = enemyType;
            this.count = count;
        }
    }

    public static final class Vec3 {
        public double x;
        public double y;
        public double z;

        public Vec3() {
        }

        public Vec3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
