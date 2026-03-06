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
import dev.duckslock.enclave.EnclaveAssignmentListener;
import dev.duckslock.enclave.EnclaveManager;
import dev.duckslock.enemy.Enemy;
import dev.duckslock.enemy.EnemySpawner;
import dev.duckslock.grid.ArenaConstants;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ArenaDebugRoundService implements EnclaveAssignmentListener {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final EnclaveManager enclaveManager;
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

    private volatile boolean enabled;
    private volatile int activeRoundId;
    private volatile boolean triggerOnlyOnNewAssignment;

    public ArenaDebugRoundService(EnclaveManager enclaveManager) {
        this.enclaveManager = enclaveManager;
        TDConfig.DebugRoundsConfig config = ModConfigHolder.get().debugRounds;
        this.movementTickMs = config.movementTickMs;
        this.baseMoveBlocksPerSecond = config.baseMoveBlocksPerSecond;
        this.waypointReachedDistance = config.waypointReachedDistance;
        this.enemySpawnSpacingMs = config.enemySpawnSpacingMs;
        this.entityRefWaitTimeoutMs = config.entityRefWaitTimeoutMs;
        this.walkAnimationCandidates = config.walkAnimationCandidates.toArray(new String[0]);
        this.enabled = config.enabledByDefault;
        this.activeRoundId = config.activeRoundId;
        this.triggerOnlyOnNewAssignment = config.triggerOnlyOnNewAssignment;
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

    @Override
    public void onAssigned(Ref<EntityStore> playerEntityRef, UUID playerUuid, Enclave enclave, boolean newAssignment) {
        if (!enabled) {
            return;
        }
        if (triggerOnlyOnNewAssignment && !newAssignment) {
            return;
        }
        enclaveManager.runAfterArenaGenerated(() -> spawnRoundForEnclave(enclave.getIndex(), activeRoundId));
    }

    public boolean spawnRoundForEnclave(int enclaveIndex, int roundId) {
        if (enclaveIndex < 0 || enclaveIndex >= enclaveManager.getEnclaves().length) {
            return false;
        }

        Enclave enclave = enclaveManager.getEnclave(enclaveIndex);
        DebugRoundDefinition round = DebugRoundDefinitions.get(roundId);
        if (round == null) {
            return false;
        }

        List<Vector3d> worldWaypoints = toWorldWaypoints(enclave, round.getLocalWaypoints());
        if (worldWaypoints.size() < 2) {
            LOGGER.at(Level.WARNING).log("Round %s has fewer than 2 waypoints; not spawning.", roundId);
            return false;
        }

        for (DebugRoundDefinition.SpawnBatch batch : round.getBatches()) {
            scheduler.schedule(() -> spawnBatch(enclave, batch, worldWaypoints),
                    batch.getDelayMs(), TimeUnit.MILLISECONDS);
        }

        LOGGER.at(Level.INFO).log("Queued debug round %s (%s) for enclave %s.",
                roundId, round.getName(), enclaveIndex);
        return true;
    }

    @Nullable
    public Enclave findEnclaveForPlayer(UUID playerUuid) {
        return enclaveManager.getEnclaveForPlayer(playerUuid);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getActiveRoundId() {
        return activeRoundId;
    }

    public void setActiveRoundId(int activeRoundId) {
        this.activeRoundId = activeRoundId;
    }

    public boolean isTriggerOnlyOnNewAssignment() {
        return triggerOnlyOnNewAssignment;
    }

    public void setTriggerOnlyOnNewAssignment(boolean triggerOnlyOnNewAssignment) {
        this.triggerOnlyOnNewAssignment = triggerOnlyOnNewAssignment;
    }

    private void spawnBatch(Enclave enclave, DebugRoundDefinition.SpawnBatch batch, List<Vector3d> worldWaypoints) {
        Vector3d start = worldWaypoints.get(0);
        int count = Math.max(1, batch.getCount());

        for (int i = 0; i < count; i++) {
            long delay = i * enemySpawnSpacingMs;
            scheduler.schedule(() -> {
                Vector3f spawnPos = new Vector3f((float) start.x, (float) start.y, (float) start.z);
                Enemy enemy = enemySpawner.spawn(batch.getEnemyType(), enclave.getIndex(), spawnPos);
                trackedEnemies.put(enemy.getId(), new TrackedEnemy(enemy, worldWaypoints, 1, System.currentTimeMillis()));
            }, delay, TimeUnit.MILLISECONDS);
        }
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
        for (TrackedEnemy tracked : trackedEnemies.values()) {
            moveEnemy(world, tracked);
        }
    }

    private void moveEnemy(World world, TrackedEnemy tracked) {
        Ref<EntityStore> ref = tracked.enemy.getEntityRef();
        if (ref == null) {
            if (System.currentTimeMillis() - tracked.createdAtMs > entityRefWaitTimeoutMs) {
                trackedEnemies.remove(tracked.enemy.getId());
            }
            return;
        }
        if (!ref.isValid()) {
            trackedEnemies.remove(tracked.enemy.getId());
            return;
        }

        Store<EntityStore> store = world.getEntityStore().getStore();
        ensureWalkingAnimation(tracked, ref, store);
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            trackedEnemies.remove(tracked.enemy.getId());
            return;
        }

        if (tracked.nextWaypointIndex >= tracked.worldWaypoints.size()) {
            enemySpawner.remove(tracked.enemy);
            trackedEnemies.remove(tracked.enemy.getId());
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
                enemySpawner.remove(tracked.enemy);
                trackedEnemies.remove(tracked.enemy.getId());
                return;
            }

            target = tracked.worldWaypoints.get(tracked.nextWaypointIndex);
            dx = target.x - current.x;
            dy = target.y - current.y;
            dz = target.z - current.z;
            distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        if (distance <= 0.0001d) {
            return;
        }

        double maxStep = baseMoveBlocksPerSecond * tracked.enemy.getType().speed * (movementTickMs / 1000.0d);
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
        private int nextWaypointIndex;
        private boolean walkAnimationSet;

        private TrackedEnemy(Enemy enemy, List<Vector3d> worldWaypoints, int nextWaypointIndex, long createdAtMs) {
            this.enemy = enemy;
            this.worldWaypoints = worldWaypoints;
            this.nextWaypointIndex = nextWaypointIndex;
            this.createdAtMs = createdAtMs;
        }
    }
}
