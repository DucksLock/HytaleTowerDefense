package dev.duckslock.commands;

import com.hypixel.hytale.math.vector.Vector3d;
import dev.duckslock.enemy.EnemySpawnProfile;
import dev.duckslock.enemy.EnemyType;

import javax.annotation.Nullable;
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
        @Nullable
        private final EnemySpawnProfile profile;

        public SpawnBatch(long delayMs, EnemyType enemyType, int count) {
            this(delayMs, enemyType, count, null);
        }

        public SpawnBatch(long delayMs, EnemyType enemyType, int count, @Nullable EnemySpawnProfile profile) {
            this.delayMs = delayMs;
            this.enemyType = enemyType;
            this.count = count;
            this.profile = profile;
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

        @Nullable
        public EnemySpawnProfile getProfile() {
            return profile;
        }
    }
}
