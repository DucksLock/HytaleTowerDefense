package dev.duckslock.commands;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import dev.duckslock.config.TDConfig;
import dev.duckslock.enemy.EnemyType;

import java.util.*;
import java.util.logging.Level;

public final class DebugRoundDefinitions {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static volatile Map<Integer, DebugRoundDefinition> rounds = Collections.emptyMap();

    static {
        replaceFromConfig(TDConfig.defaultDebugRounds());
    }

    private DebugRoundDefinitions() {
    }

    public static void replaceFromConfig(List<TDConfig.DebugRoundConfig> configuredRounds) {
        Map<Integer, DebugRoundDefinition> loaded = parse(configuredRounds);
        if (loaded.isEmpty()) {
            loaded = parse(TDConfig.defaultDebugRounds());
        }
        rounds = Collections.unmodifiableMap(loaded);
    }

    public static DebugRoundDefinition get(int roundId) {
        return rounds.get(roundId);
    }

    public static Set<Integer> ids() {
        return new TreeSet<>(rounds.keySet());
    }

    private static Map<Integer, DebugRoundDefinition> parse(Collection<TDConfig.DebugRoundConfig> configuredRounds) {
        Map<Integer, DebugRoundDefinition> loaded = new LinkedHashMap<>();
        if (configuredRounds == null) {
            return loaded;
        }

        for (TDConfig.DebugRoundConfig round : configuredRounds) {
            if (round == null || round.id <= 0) {
                continue;
            }

            List<Vector3d> waypoints = new ArrayList<>();
            if (round.localWaypoints != null) {
                for (TDConfig.Vec3 waypoint : round.localWaypoints) {
                    if (waypoint == null) {
                        continue;
                    }
                    waypoints.add(new Vector3d(waypoint.x, waypoint.y, waypoint.z));
                }
            }

            List<DebugRoundDefinition.SpawnBatch> batches = new ArrayList<>();
            if (round.batches != null) {
                for (TDConfig.SpawnBatchConfig batch : round.batches) {
                    if (batch == null) {
                        continue;
                    }

                    EnemyType enemyType;
                    try {
                        enemyType = EnemyType.valueOf(batch.enemyType.toUpperCase(Locale.ROOT));
                    } catch (Exception ex) {
                        LOGGER.at(Level.WARNING).log(
                                "Skipping debug round %s batch with invalid enemy type '%s'.",
                                round.id,
                                batch.enemyType
                        );
                        continue;
                    }

                    batches.add(new DebugRoundDefinition.SpawnBatch(
                            Math.max(0L, batch.delayMs),
                            enemyType,
                            Math.max(1, batch.count)
                    ));
                }
            }

            if (waypoints.size() < 2 || batches.isEmpty()) {
                LOGGER.at(Level.WARNING).log("Skipping debug round %s due to invalid waypoints or batches.", round.id);
                continue;
            }

            String name = (round.name == null || round.name.isBlank()) ? "Round " + round.id : round.name;
            loaded.put(round.id, new DebugRoundDefinition(name, waypoints, batches));
        }

        return loaded;
    }
}
