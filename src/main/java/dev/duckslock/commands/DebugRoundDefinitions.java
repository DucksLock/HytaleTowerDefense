package dev.duckslock.commands;

import com.hypixel.hytale.math.vector.Vector3d;
import dev.duckslock.enemy.EnemyType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Keep debug rounds in one place so you can tweak composition/path quickly.
 * <p>
 * Coordinates are LOCAL to the enclave inner area:
 * - x/z: offset from enclave.getWorldStartX/Z()
 * - y: offset from ArenaConstants.ARENA_FLOOR_Y
 */
public final class DebugRoundDefinitions {

    private static final Map<Integer, DebugRoundDefinition> ROUNDS = Map.of(
            1, new DebugRoundDefinition(
                    "Starter",
                    List.of(
                            new Vector3d(1.5, 1.0, 1.5),
                            new Vector3d(18.5, 1.0, 1.5),
                            new Vector3d(18.5, 1.0, 10.5),
                            new Vector3d(10.5, 1.0, 18.5),
                            new Vector3d(1.5, 1.0, 18.5)
                    ),
                    List.of(
                            new DebugRoundDefinition.SpawnBatch(0L, EnemyType.GRUNT, 6),
                            new DebugRoundDefinition.SpawnBatch(2500L, EnemyType.RUNNER, 4)
                    )
            ),
            2, new DebugRoundDefinition(
                    "Mixed",
                    List.of(
                            new Vector3d(1.5, 1.0, 1.5),
                            new Vector3d(18.5, 1.0, 1.5),
                            new Vector3d(18.5, 1.0, 18.5),
                            new Vector3d(1.5, 1.0, 18.5)
                    ),
                    List.of(
                            new DebugRoundDefinition.SpawnBatch(0L, EnemyType.GRUNT, 8),
                            new DebugRoundDefinition.SpawnBatch(3000L, EnemyType.SOLDIER, 4),
                            new DebugRoundDefinition.SpawnBatch(6000L, EnemyType.RUNNER, 6)
                    )
            ),
            3, new DebugRoundDefinition(
                    "Heavy",
                    List.of(
                            new Vector3d(1.5, 1.0, 1.5),
                            new Vector3d(10.5, 1.0, 1.5),
                            new Vector3d(18.5, 1.0, 10.5),
                            new Vector3d(10.5, 1.0, 18.5),
                            new Vector3d(1.5, 1.0, 10.5)
                    ),
                    List.of(
                            new DebugRoundDefinition.SpawnBatch(0L, EnemyType.BRUTE, 2),
                            new DebugRoundDefinition.SpawnBatch(2500L, EnemyType.SHIELDED, 4),
                            new DebugRoundDefinition.SpawnBatch(6000L, EnemyType.ELITE, 2)
                    )
            )
    );

    private DebugRoundDefinitions() {
    }

    public static DebugRoundDefinition get(int roundId) {
        return ROUNDS.get(roundId);
    }

    public static Set<Integer> ids() {
        return new TreeSet<>(ROUNDS.keySet());
    }
}
