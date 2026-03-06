package dev.duckslock.enemy;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.duckslock.combat.ElementType;

import javax.annotation.Nullable;
import java.util.UUID;

// runtime state for one live enemy on the path
public class Enemy {

    private final UUID id = UUID.randomUUID();
    private final EnemyType type;
    private final int enclaveIndex;
    private final String assetId;
    private final float speed;
    private final float armor;
    private final int bounty;
    private final int leakDamage;
    private final boolean boss;
    private final ElementType element;
    @Nullable
    private final String sourceUnitId;

    private int hp;
    private int waypointIndex = 0;
    private double distanceToNextWaypoint = Double.MAX_VALUE;
    private Vector3f position;
    private long slowUntilMs = 0L;
    private double slowPercent = 0d;

    @Nullable
    private Ref<EntityStore> entityRef;

    public Enemy(EnemyType type, int enclaveIndex, Vector3f spawnPos, @Nullable EnemySpawnProfile profile) {
        this.type = type;
        this.enclaveIndex = enclaveIndex;
        this.assetId = resolveAssetId(type, profile);
        this.speed = resolveSpeed(type, profile);
        this.armor = resolveArmor(type, profile);
        this.bounty = resolveBounty(type, profile);
        this.leakDamage = resolveLeakDamage(type, profile);
        this.boss = resolveBoss(type, profile);
        this.element = resolveElement(type, profile);
        this.sourceUnitId = profile == null ? null : profile.getSourceUnitId();
        this.hp = resolveMaxHp(type, profile);
        this.position = spawnPos;
    }

    // apply damage, returns true if enemy died
    public boolean damage(int amount) {
        hp -= Math.max(1, amount);
        return hp <= 0;
    }

    public boolean isDead() {
        return hp <= 0;
    }

    public UUID getId() {
        return id;
    }

    public EnemyType getType() {
        return type;
    }

    public String getAssetId() {
        return assetId;
    }

    public float getSpeedMultiplier() {
        return speed;
    }

    public int getEnclaveIndex() {
        return enclaveIndex;
    }

    public int getHp() {
        return hp;
    }

    public int getWaypointIndex() {
        return waypointIndex;
    }

    public void setWaypointIndex(int waypointIndex) {
        this.waypointIndex = Math.max(0, waypointIndex);
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f pos) {
        this.position = pos;
    }

    public void advanceWaypoint() {
        waypointIndex++;
    }

    public double getDistanceToNextWaypoint() {
        return distanceToNextWaypoint;
    }

    public void setDistanceToNextWaypoint(double distanceToNextWaypoint) {
        this.distanceToNextWaypoint = Math.max(0d, distanceToNextWaypoint);
    }

    public double getArmorFraction() {
        return Math.max(0d, Math.min(0.95d, armor));
    }

    public void applySlow(double slowPercent, long durationMs, long nowMs) {
        if (durationMs <= 0L || slowPercent <= 0d) {
            return;
        }

        double clampedSlow = Math.max(0d, Math.min(0.95d, slowPercent));
        long until = nowMs + durationMs;
        if (until > slowUntilMs || clampedSlow > this.slowPercent) {
            slowUntilMs = until;
            this.slowPercent = clampedSlow;
        }
    }

    public double getCurrentSpeedMultiplier(long nowMs) {
        if (nowMs >= slowUntilMs) {
            return 1.0d;
        }
        return Math.max(0.05d, 1.0d - slowPercent);
    }

    public void resetPathProgress() {
        waypointIndex = 0;
        distanceToNextWaypoint = Double.MAX_VALUE;
    }

    public void clearTemporaryEffects() {
        slowUntilMs = 0L;
        slowPercent = 0d;
    }

    public int getBounty() {
        return bounty;
    }

    public int getLeakDamage() {
        return leakDamage;
    }

    public boolean isBoss() {
        return boss;
    }

    public ElementType getElement() {
        return element;
    }

    @Nullable
    public String getSourceUnitId() {
        return sourceUnitId;
    }

    @Nullable
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    public void setEntityRef(Ref<EntityStore> ref) {
        this.entityRef = ref;
    }

    private String resolveAssetId(EnemyType type, @Nullable EnemySpawnProfile profile) {
        if (profile != null && profile.getAssetId() != null && !profile.getAssetId().isBlank()) {
            return profile.getAssetId();
        }
        return type.assetId;
    }

    private int resolveMaxHp(EnemyType type, @Nullable EnemySpawnProfile profile) {
        if (profile != null && profile.getMaxHp() != null) {
            return Math.max(1, profile.getMaxHp());
        }
        return type.maxHp;
    }

    private float resolveSpeed(EnemyType type, @Nullable EnemySpawnProfile profile) {
        if (profile != null && profile.getSpeed() != null) {
            return Math.max(0.05f, profile.getSpeed());
        }
        return type.speed;
    }

    private float resolveArmor(EnemyType type, @Nullable EnemySpawnProfile profile) {
        if (profile != null && profile.getArmor() != null) {
            return Math.max(0f, Math.min(0.95f, profile.getArmor()));
        }
        return type.armor;
    }

    private int resolveBounty(EnemyType type, @Nullable EnemySpawnProfile profile) {
        if (profile != null && profile.getBounty() != null) {
            return Math.max(0, profile.getBounty());
        }
        return type.bounty;
    }

    private int resolveLeakDamage(EnemyType type, @Nullable EnemySpawnProfile profile) {
        if (profile != null && profile.getLeakDamage() != null) {
            return Math.max(1, profile.getLeakDamage());
        }
        return Math.max(1, type.damage);
    }

    private boolean resolveBoss(EnemyType type, @Nullable EnemySpawnProfile profile) {
        if (profile != null && profile.getBoss() != null) {
            return profile.getBoss();
        }
        return type.isBoss;
    }

    private ElementType resolveElement(EnemyType type, @Nullable EnemySpawnProfile profile) {
        if (profile != null && profile.getElement() != null) {
            return profile.getElement();
        }
        return type.getElement();
    }
}
