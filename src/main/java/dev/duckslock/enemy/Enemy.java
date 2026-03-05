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
    private Vector3f position;

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
        float reduced = amount * (1.0f - type.armor);
        hp -= (int) Math.max(1, reduced);
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

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f pos) {
        this.position = pos;
    }

    public void advanceWaypoint() {
        waypointIndex++;
    }

    @Nullable
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    public void setEntityRef(Ref<EntityStore> ref) {
        this.entityRef = ref;
    }
}