package dev.duckslock.enemy;

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
import dev.duckslock.enclave.EnclaveManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class EnemySpawner {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final List<Enemy> activeEnemies = new ArrayList<>();

    public Enemy spawn(EnemyType type, int enclaveIndex, Vector3f spawnPos) {
        return spawn(type, enclaveIndex, spawnPos, null);
    }

    public Enemy spawn(
            EnemyType type,
            int enclaveIndex,
            Vector3f spawnPos,
            EnemySpawnProfile profile
    ) {
        Enemy enemy = new Enemy(type, enclaveIndex, spawnPos, profile);

        World world = Universe.get().getWorld(EnclaveManager.WORLD_NAME);
        if (world == null) {
            LOGGER.at(Level.WARNING).log("Cannot spawn %s - world not found", type.name());
            return enemy;
        }

        world.execute(() -> spawnEntity(enemy, world, spawnPos));
        activeEnemies.add(enemy);

        LOGGER.at(Level.INFO).log("Spawned %s (enclave %s) at %.1f,%.1f",
                type.name(), enclaveIndex, spawnPos.x, spawnPos.z);
        return enemy;
    }

    private void spawnEntity(Enemy enemy, World world, Vector3f spawnPos) {
        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(enemy.getAssetId());
        if (modelAsset == null) {
            LOGGER.at(Level.WARNING).log("Model not found: %s", enemy.getAssetId());
            return;
        }

        Store<EntityStore> store = world.getEntityStore().getStore();
        Model model = Model.createScaledModel(modelAsset, 1.0f);

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        Vector3d position = new Vector3d(spawnPos.x, spawnPos.y, spawnPos.z);
        Vector3f rotation = new Vector3f(0f, 0f, 0f);

        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(position, rotation));
        holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
        holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(enemy.getId()));
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(
                store.getExternalData().takeNextNetworkId()
        ));

        Ref<EntityStore> ref = store.addEntity(holder, AddReason.SPAWN);
        enemy.setEntityRef(ref);
    }

    public void remove(Enemy enemy) {
        activeEnemies.remove(enemy);

        if (enemy.getEntityRef() == null) return;

        World world = Universe.get().getWorld(EnclaveManager.WORLD_NAME);
        if (world == null) return;

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Ref<EntityStore> ref = enemy.getEntityRef();
            if (ref != null && ref.isValid()) {
                store.removeEntity(ref, RemoveReason.REMOVE);
            }
        });
    }

    public List<Enemy> getActiveEnemies() {
        return activeEnemies;
    }

    public List<Enemy> getEnemiesForEnclave(int enclaveIndex) {
        List<Enemy> result = new ArrayList<>();
        for (Enemy e : activeEnemies) {
            if (e.getEnclaveIndex() == enclaveIndex) result.add(e);
        }
        return result;
    }
}
