package dev.duckslock.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.duckslock.config.ModConfigHolder;
import dev.duckslock.config.TDConfig;
import dev.duckslock.enclave.Enclave;
import dev.duckslock.enclave.EnclaveManager;
import dev.duckslock.enemy.Enemy;
import dev.duckslock.enemy.EnemySpawner;
import dev.duckslock.grid.ArenaConstants;
import dev.duckslock.wave.WaveManager;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class ArenaDebugRoundService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final EnemySpawner enemySpawner = new EnemySpawner();
    private final long movementTickMs;
    private final double baseMoveBlocksPerSecond;
    private final double waypointReachedDistance;
    private final long enemySpawnSpacingMs;
    private final long entityRefWaitTimeoutMs;
    private final String[] walkAnimationCandidates;
    private final Map<UUID, TrackedEnemy> trackedEnemies = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "td-debug-rounds");
                t.setDaemon(true);
                return t;
            });

    public ArenaDebugRoundService(EnclaveManager enclaveManager) {
        TDConfig.DebugRoundsConfig config = ModConfigHolder.get().debugRounds;
        this.movementTickMs = config.movementTickMs;
        this.baseMoveBlocksPerSecond = config.baseMoveBlocksPerSecond;
        this.waypointReachedDistance = config.waypointReachedDistance;
        this.enemySpawnSpacingMs = config.enemySpawnSpacingMs;
        this.entityRefWaitTimeoutMs = config.entityRefWaitTimeoutMs;
        this.walkAnimationCandidates = config.walkAnimationCandidates.toArray(new String[0]);
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(this::scheduleMovementTick, movementTickMs, movementTickMs, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        scheduler.shutdownNow();
        for (TrackedEnemy tracked : trackedEnemies.values()) {
            enemySpawner.remove(tracked.enemy);
        }
        trackedEnemies.clear();
    }

    public boolean startDebugRound(Enclave enclave, DebugRoundDefinition round, WaveManager waveManager) {
        if (enclave == null || round == null || waveManager == null) {
            return false;
        }

        List<Vector3d> worldWaypoints = toWorldWaypoints(enclave, round.getLocalWaypoints());
        if (worldWaypoints.size() < 2) {
            LOGGER.at(Level.WARNING).log("Round %s has fewer than 2 waypoints; not spawning.", round.getName());
            return false;
        }

        int totalToSpawn = totalEnemies(round);
        if (totalToSpawn <= 0) {
            return false;
        }

        AtomicInteger spawnedCount = new AtomicInteger(0);
        for (DebugRoundDefinition.SpawnBatch batch : round.getBatches()) {
            int count = Math.max(1, batch.getCount());
            for (int i = 0; i < count; i++) {
                long delay = Math.max(0L, batch.getDelayMs()) + i * enemySpawnSpacingMs;
                scheduler.schedule(
                        () -> spawnOneEnemy(enclave, batch, worldWaypoints, waveManager, spawnedCount, totalToSpawn),
                        delay,
                        TimeUnit.MILLISECONDS
                );
            }
        }

        LOGGER.at(Level.INFO).log("Queued debug round '%s' for enclave %s.", round.getName(), enclave.getIndex());
        return true;
    }

    public void clearEnemiesForEnclave(int enclaveIndex) {
        List<TrackedEnemy> toRemove = new ArrayList<>();
        for (TrackedEnemy tracked : trackedEnemies.values()) {
            if (tracked.enemy.getEnclaveIndex() == enclaveIndex) {
                toRemove.add(tracked);
            }
        }

        for (TrackedEnemy tracked : toRemove) {
            trackedEnemies.remove(tracked.enemy.getId());
            enemySpawner.remove(tracked.enemy);
        }
    }

    public List<Enemy> getActiveEnemiesForEnclave(int enclaveIndex) {
        List<Enemy> result = new ArrayList<>();
        for (TrackedEnemy tracked : trackedEnemies.values()) {
            Enemy enemy = tracked.enemy;
            if (enemy.getEnclaveIndex() == enclaveIndex && !enemy.isDead()) {
                result.add(enemy);
            }
        }
        return result;
    }

    public boolean resolveEnemyKilledFromCombat(Enemy enemy) {
        if (enemy == null) {
            return false;
        }

        TrackedEnemy tracked = trackedEnemies.remove(enemy.getId());
        if (tracked == null) {
            return false;
        }

        resolveKilledEnemy(tracked, false);
        return true;
    }

    private void spawnOneEnemy(
            Enclave enclave,
            DebugRoundDefinition.SpawnBatch batch,
            List<Vector3d> worldWaypoints,
            WaveManager waveManager,
            AtomicInteger spawnedCount,
            int totalToSpawn
    ) {
        Vector3d start = worldWaypoints.get(0);
        Vector3f spawnPos = new Vector3f((float) start.x, (float) start.y, (float) start.z);
        Enemy enemy = enemySpawner.spawn(batch.getEnemyType(), enclave.getIndex(), spawnPos);

        trackedEnemies.put(
                enemy.getId(),
                new TrackedEnemy(enemy, worldWaypoints, 1, System.currentTimeMillis(), enclave, waveManager)
        );
        waveManager.onEnemySpawned();

        if (spawnedCount.incrementAndGet() >= totalToSpawn) {
            waveManager.onAllEnemiesSpawned();
        }
    }

    private int totalEnemies(DebugRoundDefinition round) {
        int total = 0;
        for (DebugRoundDefinition.SpawnBatch batch : round.getBatches()) {
            total += Math.max(1, batch.getCount());
        }
        return total;
    }

    private void scheduleMovementTick() {
        if (trackedEnemies.isEmpty()) {
            return;
        }

        World world = Universe.get().getWorld(EnclaveManager.WORLD_NAME);
        if (world == null) {
            return;
        }

        world.execute(() -> updateMovement(world));
    }

    private void updateMovement(World world) {
        long nowMs = System.currentTimeMillis();
        for (TrackedEnemy tracked : trackedEnemies.values()) {
            moveEnemy(world, tracked, nowMs);
        }
    }

    private void moveEnemy(World world, TrackedEnemy tracked, long nowMs) {
        if (trackedEnemies.get(tracked.enemy.getId()) != tracked) {
            return;
        }

        if (tracked.enemy.isDead()) {
            resolveKilledEnemy(tracked, true);
            return;
        }

        Ref<EntityStore> ref = tracked.enemy.getEntityRef();
        if (ref == null) {
            if (System.currentTimeMillis() - tracked.createdAtMs > entityRefWaitTimeoutMs) {
                resolveUnspawnedEnemy(tracked);
            }
            return;
        }
        if (!ref.isValid()) {
            resolveInvalidEnemy(tracked);
            return;
        }

        Store<EntityStore> store = world.getEntityStore().getStore();
        ensureWalkingAnimation(tracked, ref, store);
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            resolveInvalidEnemy(tracked);
            return;
        }

        if (tracked.nextWaypointIndex >= tracked.worldWaypoints.size()) {
            resolveLeakedEnemy(tracked);
            return;
        }

        Vector3d current = transform.getPosition();
        Vector3d target = tracked.worldWaypoints.get(tracked.nextWaypointIndex);

        double dx = target.x - current.x;
        double dy = target.y - current.y;
        double dz = target.z - current.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance <= waypointReachedDistance) {
            tracked.nextWaypointIndex++;
            if (tracked.nextWaypointIndex >= tracked.worldWaypoints.size()) {
                resolveLeakedEnemy(tracked);
                return;
            }

            target = tracked.worldWaypoints.get(tracked.nextWaypointIndex);
            dx = target.x - current.x;
            dy = target.y - current.y;
            dz = target.z - current.z;
            distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            tracked.enemy.advanceWaypoint();
        }

        if (distance <= 0.0001d) {
            tracked.enemy.setDistanceToNextWaypoint(distance);
            return;
        }

        tracked.enemy.setDistanceToNextWaypoint(distance);
        double maxStep = baseMoveBlocksPerSecond
                * tracked.enemy.getType().speed
                * tracked.enemy.getCurrentSpeedMultiplier(nowMs)
                * (movementTickMs / 1000.0d);
        double step = Math.min(distance, maxStep);
        double scale = step / distance;

        Vector3d next = new Vector3d(
                current.x + dx * scale,
                current.y + dy * scale,
                current.z + dz * scale
        );
        transform.teleportPosition(next);
        transform.teleportRotation(toYawRotation(dx, dz));
        setWalkingMovementState(store, ref);
        tracked.enemy.setPosition(next.toVector3f());
    }

    private void resolveKilledEnemy(TrackedEnemy tracked, boolean removeFromTrackedMap) {
        if (removeFromTrackedMap) {
            trackedEnemies.remove(tracked.enemy.getId());
        }
        enemySpawner.remove(tracked.enemy);

        int bounty = tracked.enemy.getType().getBounty();
        if (tracked.waveManager.isEarlyWaveTriggered()) {
            bounty = Math.max(1, Math.round(bounty * 1.1f));
        }
        tracked.enclave.addGold(bounty);
        tracked.waveManager.onEnemyResolved();
    }

    private void resolveLeakedEnemy(TrackedEnemy tracked) {
        trackedEnemies.remove(tracked.enemy.getId());
        enemySpawner.remove(tracked.enemy);

        boolean defeated = tracked.enclave.deductLives(tracked.enemy.getType().getDamage());
        tracked.waveManager.onEnemyResolved();
        if (defeated) {
            tracked.waveManager.onEnclaveDefeated();
        }
    }

    private void resolveInvalidEnemy(TrackedEnemy tracked) {
        trackedEnemies.remove(tracked.enemy.getId());
        enemySpawner.remove(tracked.enemy);
        tracked.waveManager.onEnemyResolved();
    }

    private void resolveUnspawnedEnemy(TrackedEnemy tracked) {
        trackedEnemies.remove(tracked.enemy.getId());
        enemySpawner.remove(tracked.enemy);
        tracked.waveManager.onEnemyResolved();
    }

    private void ensureWalkingAnimation(TrackedEnemy tracked, Ref<EntityStore> ref, Store<EntityStore> store) {
        if (tracked.walkAnimationSet) {
            return;
        }

        ModelComponent modelComponent = store.getComponent(ref, ModelComponent.getComponentType());
        if (modelComponent == null || modelComponent.getModel() == null) {
            return;
        }

        String animId = modelComponent.getModel().getFirstBoundAnimationId(walkAnimationCandidates);
        if (animId == null || animId.isBlank()) {
            animId = findAnyMovementLikeAnimation(modelComponent.getModel().getAnimationSetMap().keySet());
        }
        if (animId == null || animId.isBlank()) {
            return;
        }

        AnimationUtils.playAnimation(ref, AnimationSlot.Movement, animId, true, store);
        tracked.walkAnimationSet = true;
    }

    @Nullable
    private String findAnyMovementLikeAnimation(Collection<String> animationIds) {
        for (String id : animationIds) {
            String lower = id.toLowerCase();
            if (lower.contains("walk") || lower.contains("run") || lower.contains("move")) {
                return id;
            }
        }
        return null;
    }

    private Vector3f toYawRotation(double dx, double dz) {
        Vector3f lookRotation = Vector3f.lookAt(new Vector3d(dx, 0d, dz));
        return new Vector3f(0f, lookRotation.getYaw(), 0f);
    }

    private void setWalkingMovementState(Store<EntityStore> store, Ref<EntityStore> ref) {
        MovementStatesComponent statesComponent =
                store.ensureAndGetComponent(ref, MovementStatesComponent.getComponentType());
        MovementStates states = statesComponent.getMovementStates();
        if (states == null) {
            states = new MovementStates();
        }
        states.idle = false;
        states.horizontalIdle = false;
        states.walking = true;
        states.running = true;
        states.sprinting = true;
        states.onGround = true;
        statesComponent.setMovementStates(states);
    }

    private List<Vector3d> toWorldWaypoints(Enclave enclave, List<Vector3d> localWaypoints) {
        List<Vector3d> world = new ArrayList<>(localWaypoints.size());
        for (Vector3d local : localWaypoints) {
            world.add(new Vector3d(
                    enclave.getWorldStartX() + local.x,
                    ArenaConstants.ARENA_FLOOR_Y + local.y,
                    enclave.getWorldStartZ() + local.z
            ));
        }
        return world;
    }

    private static final class TrackedEnemy {
        private final Enemy enemy;
        private final List<Vector3d> worldWaypoints;
        private final long createdAtMs;
        private final Enclave enclave;
        private final WaveManager waveManager;
        private int nextWaypointIndex;
        private boolean walkAnimationSet;

        private TrackedEnemy(
                Enemy enemy,
                List<Vector3d> worldWaypoints,
                int nextWaypointIndex,
                long createdAtMs,
                Enclave enclave,
                WaveManager waveManager
        ) {
            this.enemy = enemy;
            this.worldWaypoints = worldWaypoints;
            this.nextWaypointIndex = nextWaypointIndex;
            this.createdAtMs = createdAtMs;
            this.enclave = enclave;
            this.waveManager = waveManager;
        }
    }
}
