package dev.duckslock.commands;

import com.hypixel.hytale.math.vector.Vector3d;
import dev.duckslock.enemy.EnemyType;

import java.util.Collections;
import java.util.List;

public final class DebugRoundDefinition {

    private final String name;
    private final List<Vector3d> localWaypoints;
    private final List<SpawnBatch> batches;

    public DebugRoundDefinition(String name, List<Vector3d> localWaypoints, List<SpawnBatch> batches) {
        this.name = name;
        this.localWaypoints = Collections.unmodifiableList(localWaypoints);
        this.batches = Collections.unmodifiableList(batches);
    }

    public String getName() {
        return name;
    }

    public List<Vector3d> getLocalWaypoints() {
        return localWaypoints;
    }

    public List<SpawnBatch> getBatches() {
        return batches;
    }

    public static final class SpawnBatch {
        private final long delayMs;
        private final EnemyType enemyType;
        private final int count;

        public SpawnBatch(long delayMs, EnemyType enemyType, int count) {
            this.delayMs = delayMs;
            this.enemyType = enemyType;
            this.count = count;
        }

        public long getDelayMs() {
            return delayMs;
        }

        public EnemyType getEnemyType() {
            return enemyType;
        }

        public int getCount() {
            return count;
        }
    }
}
