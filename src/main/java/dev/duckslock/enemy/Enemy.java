package dev.duckslock.enemy;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.UUID;

// runtime state for one live enemy on the path
public class Enemy {

    private final UUID id = UUID.randomUUID();
    private final EnemyType type;
    private final int enclaveIndex;

    private int hp;
    private int waypointIndex = 0;
    private double distanceToNextWaypoint = Double.MAX_VALUE;
    private Vector3f position;
    private long slowUntilMs = 0L;
    private double slowPercent = 0d;

    @Nullable
    private Ref<EntityStore> entityRef;

    public Enemy(EnemyType type, int enclaveIndex, Vector3f spawnPos) {
        this.type = type;
        this.enclaveIndex = enclaveIndex;
        this.hp = type.maxHp;
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
        return Math.max(0d, Math.min(0.95d, type.armor));
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

    @Nullable
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    public void setEntityRef(Ref<EntityStore> ref) {
        this.entityRef = ref;
    }
}
