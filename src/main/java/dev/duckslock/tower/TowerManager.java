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
import dev.duckslock.combat.ElementWheel;
import dev.duckslock.commands.ArenaDebugRoundService;
import dev.duckslock.enclave.Enclave;
import dev.duckslock.enclave.EnclaveManager;
import dev.duckslock.enemy.Enemy;
import dev.duckslock.grid.ArenaConstants;
import dev.duckslock.grid.GridSquare;
import dev.duckslock.grid.GridSquareType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        GridSquare anchor = enclave.getSquareAtWorldPos(worldX, worldZ);
        if (anchor == null) {
            return PlacementResult.fail("That square is not inside your enclave.");
        }

        List<GridSquare> footprint = resolveTwoByTwoFootprint(anchor);
        if (footprint == null) {
            return PlacementResult.fail("Tower footprint must fit a 2x2 area inside your enclave.");
        }

        for (GridSquare square : footprint) {
            if (square.getType() != GridSquareType.BUILDABLE) {
                return PlacementResult.fail("All 2x2 footprint squares must be BUILDABLE.");
            }
            if (square.isOccupied()) {
                return PlacementResult.fail("One of the 2x2 footprint squares is already occupied.");
            }
        }

        if (!enclave.spendGold(type.getCost())) {
            return PlacementResult.fail("Not enough gold.");
        }

        Tower tower = new Tower(type, anchor, footprint, enclave);
        String spawnFailure = spawnTowerEntity(tower);
        if (spawnFailure != null) {
            enclave.addGold(type.getCost());
            return PlacementResult.fail(spawnFailure);
        }

        for (GridSquare square : footprint) {
            square.setTower(tower);
            square.setTowerOwner(enclave.getOwnerUuid());
        }
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
        for (GridSquare square : tower.getOccupiedSquares()) {
            square.clearTower();
        }
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
                if (tower.getAttackKind() == TowerAttackKind.SUPPORT_TRICKERY) {
                    processTrickerySupportTick(tower);
                } else {
                    tower.setCurrentTarget(null);
                }
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
        if (tower.getAttackKind() == TowerAttackKind.SUPPORT_TRICKERY) {
            processTrickerySupportTick(tower);
            return;
        }

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
        if (rawDamage <= 0) {
            return;
        }

        switch (tower.getAttackKind()) {
            case DIRECT -> applyDamage(tower, target, 1.0d, nowMs);
            case SPLASH -> applySplashDamage(tower, target, enemies, nowMs);
            case CHAIN -> applyChainDamage(tower, target, enemies, nowMs);
            case SUPPORT_TRICKERY -> processTrickerySupportTick(tower);
        }
        tower.markAttack(nowMs);
        if (target.isDead()) {
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

        double towerX = tower.getCenterWorldX();
        double towerZ = tower.getCenterWorldZ();
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
            double centerOffset = ArenaConstants.SQUARE_SIZE;
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
            tower.setEntityRef(null);
        });
    }

    @Nullable
    private List<GridSquare> resolveTwoByTwoFootprint(GridSquare anchor) {
        List<GridSquare> footprint = new ArrayList<>(4);
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                GridSquare square = enclave.getSquare(anchor.getGridX() + dx, anchor.getGridZ() + dz);
                if (square == null) {
                    return null;
                }
                footprint.add(square);
            }
        }
        return footprint;
    }

    private void applySplashDamage(Tower tower, Enemy primary, List<Enemy> enemies, long nowMs) {
        Vector3f primaryPosition = primary.getPosition();
        if (primaryPosition == null) {
            applyDamage(tower, primary, 1.0d, nowMs);
            return;
        }

        double splashRadiusBlocks = tower.getEffectiveSplashRadius() * ArenaConstants.SQUARE_SIZE;
        double splashRadiusSq = splashRadiusBlocks * splashRadiusBlocks;
        for (Enemy enemy : enemies) {
            if (enemy.isDead()) {
                continue;
            }
            Vector3f position = enemy.getPosition();
            if (position == null) {
                continue;
            }

            double dx = position.x - primaryPosition.x;
            double dz = position.z - primaryPosition.z;
            if (dx * dx + dz * dz > splashRadiusSq) {
                continue;
            }

            double multiplier = enemy == primary ? 1.0d : tower.getSplashFalloff();
            applyDamage(tower, enemy, multiplier, nowMs);
        }
    }

    private void applyChainDamage(Tower tower, Enemy primary, List<Enemy> enemies, long nowMs) {
        int remainingBounces = Math.max(0, tower.getChainBounces());
        double chainRangeBlocks = tower.getEffectiveChainRange() * ArenaConstants.SQUARE_SIZE;
        double damageScale = 1.0d;
        Enemy current = primary;
        Set<Enemy> visited = new HashSet<>();

        while (current != null) {
            visited.add(current);
            applyDamage(tower, current, damageScale, nowMs);
            if (remainingBounces <= 0) {
                break;
            }
            current = pickNearestChainTarget(current, enemies, visited, chainRangeBlocks);
            damageScale = Math.max(0.05d, damageScale * tower.getChainFalloff());
            remainingBounces--;
        }
    }

    @Nullable
    private Enemy pickNearestChainTarget(Enemy from, List<Enemy> enemies, Set<Enemy> visited, double chainRangeBlocks) {
        Vector3f fromPosition = from.getPosition();
        if (fromPosition == null) {
            return null;
        }

        double bestDistanceSq = chainRangeBlocks * chainRangeBlocks;
        Enemy best = null;
        for (Enemy candidate : enemies) {
            if (candidate.isDead() || visited.contains(candidate)) {
                continue;
            }
            Vector3f position = candidate.getPosition();
            if (position == null) {
                continue;
            }
            double dx = position.x - fromPosition.x;
            double dz = position.z - fromPosition.z;
            double distanceSq = dx * dx + dz * dz;
            if (distanceSq <= bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = candidate;
            }
        }
        return best;
    }

    private void applyDamage(Tower tower, Enemy target, double damageMultiplier, long nowMs) {
        if (target.isDead()) {
            return;
        }

        double rawDamage = tower.getEffectiveDamage() * Math.max(0.05d, damageMultiplier);
        double damageAfterElement = rawDamage * ElementWheel.damageMultiplier(
                tower.getDamageElement(),
                target.getElement()
        );
        if (!tower.getType().isIgnoreArmor()) {
            damageAfterElement *= (1.0d - target.getArmorFraction());
        }
        int actualDamage = Math.max(1, (int) Math.round(damageAfterElement));

        boolean died = target.damage(actualDamage);
        if (tower.getType() == TowerType.FROST && tower.getEffectiveSlowPercent() > 0d) {
            target.applySlow(tower.getEffectiveSlowPercent(), tower.getEffectiveSlowDurationMs(), nowMs);
        }
        if (died) {
            debugRoundService.resolveEnemyKilledFromCombat(target);
        }
    }

    private void processTrickerySupportTick(Tower supportTower) {
        if (supportTower.isSupportBuffApplied()) {
            return;
        }

        Tower best = pickBestTrickeryTarget(supportTower);
        if (best == null) {
            return;
        }

        best.applyExternalDamageMultiplier(1.35d);
        best.markTrickeryBuffed();
        supportTower.markSupportBuffApplied();
    }

    @Nullable
    private Tower pickBestTrickeryTarget(Tower supportTower) {
        Tower best = null;
        for (Tower candidate : towers) {
            if (candidate == supportTower) {
                continue;
            }
            if (candidate.getAttackKind() == TowerAttackKind.SUPPORT_TRICKERY) {
                continue;
            }
            if (candidate.isTrickeryBuffed()) {
                continue;
            }
            if (!isTowerInRange(supportTower, candidate, supportTower.getEffectiveRange())) {
                continue;
            }

            if (best == null || candidate.getEffectiveDamage() > best.getEffectiveDamage()) {
                best = candidate;
            }
        }
        return best;
    }

    private boolean isTowerInRange(Tower source, Tower target, double rangeSquares) {
        double rangeBlocks = rangeSquares * ArenaConstants.SQUARE_SIZE;
        double dx = source.getCenterWorldX() - target.getCenterWorldX();
        double dz = source.getCenterWorldZ() - target.getCenterWorldZ();
        return (dx * dx + dz * dz) <= rangeBlocks * rangeBlocks;
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
