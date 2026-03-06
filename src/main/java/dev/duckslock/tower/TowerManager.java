package dev.duckslock.tower;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.duckslock.commands.ArenaDebugRoundService;
import dev.duckslock.enclave.Enclave;
import dev.duckslock.enclave.EnclaveManager;
import dev.duckslock.enemy.Enemy;
import dev.duckslock.grid.ArenaConstants;
import dev.duckslock.grid.GridSquare;
import dev.duckslock.grid.GridSquareType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TowerManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Enclave enclave;
    private final ArenaDebugRoundService debugRoundService;
    private final List<Tower> towers = new CopyOnWriteArrayList<>();

    public TowerManager(Enclave enclave, ArenaDebugRoundService debugRoundService) {
        this.enclave = enclave;
        this.debugRoundService = debugRoundService;
    }

    public PlacementResult placeTower(TowerType type, int worldX, int worldZ) {
        GridSquare square = enclave.getSquareAtWorldPos(worldX, worldZ);
        if (square == null) {
            return PlacementResult.fail("That square is not inside your enclave.");
        }

        if (square.getType() != GridSquareType.BUILDABLE) {
            return PlacementResult.fail("You can only place towers on BUILDABLE squares.");
        }

        if (square.isOccupied()) {
            return PlacementResult.fail("That square is already occupied.");
        }

        if (!enclave.spendGold(type.getCost())) {
            return PlacementResult.fail("Not enough gold.");
        }

        Tower tower = new Tower(type, square, enclave);
        String spawnFailure = spawnTowerEntity(tower);
        if (spawnFailure != null) {
            enclave.addGold(type.getCost());
            return PlacementResult.fail(spawnFailure);
        }

        square.setTower(tower);
        square.setTowerOwner(enclave.getOwnerUuid());
        towers.add(tower);
        return PlacementResult.ok(tower);
    }

    public ActionResult upgradeTowerAt(int worldX, int worldZ, UpgradePath path) {
        GridSquare square = enclave.getSquareAtWorldPos(worldX, worldZ);
        if (square == null) {
            return ActionResult.fail("That square is not inside your enclave.");
        }

        Tower tower = square.getTower();
        if (tower == null) {
            return ActionResult.fail("No tower found on that square.");
        }

        if (tower.getTier() >= 2) {
            return ActionResult.fail("That tower is already at max tier.");
        }

        if (tower.getTier() == 1 && tower.getChosenPath() != path) {
            return ActionResult.fail("That tower is locked to a different upgrade path.");
        }

        UpgradeTier nextTier = tower.getType().getUpgradeTier(path, tower.getTier());
        if (nextTier == null) {
            return ActionResult.fail("No upgrade available for that path.");
        }

        if (enclave.getGold() < nextTier.getCost()) {
            return ActionResult.fail("Not enough gold for that upgrade.");
        }

        if (!tower.upgrade(path)) {
            return ActionResult.fail("Upgrade failed.");
        }

        return ActionResult.ok("Tower upgraded to tier " + tower.getTier() + " (" + path + ").");
    }

    public ActionResult sellTowerAt(int worldX, int worldZ) {
        GridSquare square = enclave.getSquareAtWorldPos(worldX, worldZ);
        if (square == null) {
            return ActionResult.fail("That square is not inside your enclave.");
        }

        Tower tower = square.getTower();
        if (tower == null) {
            return ActionResult.fail("No tower found on that square.");
        }

        int refund = tower.getSellRefund();
        enclave.addGold(refund);
        removeTower(tower);
        return ActionResult.ok("Tower sold for " + refund + "g.");
    }

    public void removeTower(Tower tower) {
        towers.remove(tower);
        tower.setCurrentTarget(null);
        tower.getSquare().clearTower();
        removeTowerEntity(tower);
    }

    public void clearAllTowers() {
        List<Tower> snapshot = new ArrayList<>(towers);
        for (Tower tower : snapshot) {
            removeTower(tower);
        }
    }

    public List<Tower> getTowersInRange(Enemy enemy) {
        List<Tower> result = new ArrayList<>();
        for (Tower tower : towers) {
            if (isEnemyInRange(tower, enemy)) {
                result.add(tower);
            }
        }
        return result;
    }

    public void tick(long nowMs) {
        if (towers.isEmpty()) {
            return;
        }

        List<Enemy> enemies = debugRoundService.getActiveEnemiesForEnclave(enclave.getIndex());
        if (enemies.isEmpty()) {
            for (Tower tower : towers) {
                tower.setCurrentTarget(null);
            }
            return;
        }

        for (Tower tower : towers) {
            processTowerTick(tower, enemies, nowMs);
        }
    }

    public List<Tower> getTowers() {
        return towers;
    }

    public Enclave getEnclave() {
        return enclave;
    }

    private void processTowerTick(Tower tower, List<Enemy> enemies, long nowMs) {
        Enemy target = tower.getCurrentTarget();
        if (target == null || target.isDead() || !isEnemyInRange(tower, target)) {
            target = pickTarget(tower, enemies);
            tower.setCurrentTarget(target);
        }

        if (target == null) {
            return;
        }

        long sinceLastAttack = nowMs - tower.getLastAttackTick();
        if (sinceLastAttack < tower.getAttackIntervalMs()) {
            return;
        }

        int rawDamage = tower.getEffectiveDamage();
        int actualDamage = rawDamage;
        if (!tower.getType().isIgnoreArmor()) {
            actualDamage = Math.max(1, (int) Math.round(rawDamage * (1.0d - target.getArmorFraction())));
        }

        boolean died = target.damage(actualDamage);
        if (tower.getType() == TowerType.FROST && tower.getEffectiveSlowPercent() > 0d) {
            target.applySlow(tower.getEffectiveSlowPercent(), tower.getEffectiveSlowDurationMs(), nowMs);
        }

        tower.markAttack(nowMs);
        if (died) {
            debugRoundService.resolveEnemyKilledFromCombat(target);
            tower.setCurrentTarget(null);
        }
    }

    @Nullable
    private Enemy pickTarget(Tower tower, List<Enemy> enemies) {
        Enemy best = null;
        for (Enemy enemy : enemies) {
            if (enemy.isDead()) {
                continue;
            }
            if (!isEnemyInRange(tower, enemy)) {
                continue;
            }

            if (best == null || compareTargets(tower.getTargetPriority(), enemy, best) > 0) {
                best = enemy;
            }
        }
        return best;
    }

    private int compareTargets(TargetPriority priority, Enemy a, Enemy b) {
        return switch (priority) {
            case FIRST -> compareFirst(a, b);
            case LAST -> compareFirst(b, a);
            case STRONGEST -> Integer.compare(a.getHp(), b.getHp());
            case WEAKEST -> Integer.compare(b.getHp(), a.getHp());
        };
    }

    private int compareFirst(Enemy a, Enemy b) {
        int byWaypoint = Integer.compare(a.getWaypointIndex(), b.getWaypointIndex());
        if (byWaypoint != 0) {
            return byWaypoint;
        }
        return Double.compare(b.getDistanceToNextWaypoint(), a.getDistanceToNextWaypoint());
    }

    private boolean isEnemyInRange(Tower tower, Enemy enemy) {
        Vector3f position = enemy.getPosition();
        if (position == null) {
            return false;
        }

        double squareCenterOffset = ArenaConstants.SQUARE_SIZE / 2.0d;
        double towerX = tower.getSquare().getWorldX() + squareCenterOffset;
        double towerZ = tower.getSquare().getWorldZ() + squareCenterOffset;
        double dx = position.x - towerX;
        double dz = position.z - towerZ;
        double distanceSq = dx * dx + dz * dz;

        double rangeBlocks = tower.getEffectiveRange() * ArenaConstants.SQUARE_SIZE;
        return distanceSq <= rangeBlocks * rangeBlocks;
    }

    @Nullable
    private String spawnTowerEntity(Tower tower) {
        World world = Universe.get().getWorld(EnclaveManager.WORLD_NAME);
        if (world == null) {
            return "World is not available.";
        }

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(tower.getType().getAssetId());
        if (modelAsset == null) {
            return "Tower model not found: " + tower.getType().getAssetId();
        }

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Model model = Model.createScaledModel(modelAsset, 1.0f);

            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
            double centerOffset = ArenaConstants.SQUARE_SIZE / 2.0d;
            Vector3d position = new Vector3d(
                    tower.getSquare().getWorldX() + centerOffset,
                    ArenaConstants.ARENA_FLOOR_Y + 1.0d,
                    tower.getSquare().getWorldZ() + centerOffset
            );
            Vector3f rotation = new Vector3f(0f, 0f, 0f);

            holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(position, rotation));
            holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
            holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(tower.getId()));
            holder.addComponent(NetworkId.getComponentType(), new NetworkId(
                    store.getExternalData().takeNextNetworkId()
            ));

            Ref<EntityStore> ref = store.addEntity(holder, AddReason.SPAWN);
            tower.setEntityRef(ref);
        });

        return null;
    }

    private void removeTowerEntity(Tower tower) {
        Ref<EntityStore> ref = tower.getEntityRef();
        if (ref == null) {
            return;
        }

        World world = Universe.get().getWorld(EnclaveManager.WORLD_NAME);
        if (world == null) {
            return;
        }

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Ref<EntityStore> towerRef = tower.getEntityRef();
            if (towerRef != null && towerRef.isValid()) {
                store.removeEntity(towerRef, RemoveReason.REMOVE);
            }
        });
    }

    public static final class PlacementResult {
        private final boolean ok;
        private final String message;
        @Nullable
        private final Tower tower;

        private PlacementResult(boolean ok, String message, @Nullable Tower tower) {
            this.ok = ok;
            this.message = message;
            this.tower = tower;
        }

        public static PlacementResult ok(Tower tower) {
            return new PlacementResult(true, "Tower placed.", tower);
        }

        public static PlacementResult fail(String message) {
            return new PlacementResult(false, message, null);
        }

        public boolean isOk() {
            return ok;
        }

        public String getMessage() {
            return message;
        }

        @Nullable
        public Tower getTower() {
            return tower;
        }
    }

    public static final class ActionResult {
        private final boolean ok;
        private final String message;

        private ActionResult(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }

        public static ActionResult ok(String message) {
            return new ActionResult(true, message);
        }

        public static ActionResult fail(String message) {
            return new ActionResult(false, message);
        }

        public boolean isOk() {
            return ok;
        }

        public String getMessage() {
            return message;
        }
    }
}
