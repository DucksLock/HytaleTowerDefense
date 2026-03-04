package dev.duckslock;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.duckslock.enclave.EnclaveManager;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Hytale Tower Defense — main plugin.
 * <p>
 * Lifecycle (from PluginBase):
 * setup()    → one-time registration (codecs, registries, event subscriptions)
 * start()    → server is running, safe to interact with the world
 * shutdown() → cleanup
 */
public class TowerDefensePlugin extends JavaPlugin {

    private static TowerDefensePlugin instance;
    private EnclaveManager enclaveManager;

    public TowerDefensePlugin(JavaPluginInit init) {
        super(init);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public static TowerDefensePlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        instance = this;

        // subscribe = register in EventRegistry API
        getEventRegistry()
                .register(PlayerReadyEvent.class, EnclaveManager.WORLD_NAME,
                        this::onPlayerReady);

        getEventRegistry()
                .register(PlayerDisconnectEvent.class, EnclaveManager.WORLD_NAME,
                        this::onPlayerDisconnect);

        getLogger().at(Level.INFO).log("Tower Defense plugin set up.");
    }

    @Override
    protected void start() {
        // World is available — safe to create the EnclaveManager and generate arena.
        enclaveManager = new EnclaveManager();
        getLogger().at(Level.INFO).log("Tower Defense plugin started.");
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    private void onPlayerReady(PlayerReadyEvent event) {
        var player = event.getPlayer();
        if (player == null) return;
        enclaveManager.assignEnclaveToPlayer(player);
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef == null) return;
        UUID uuid = playerRef.getUuid();
        enclaveManager.releaseEnclave(uuid);
    }

    // -------------------------------------------------------------------------
    // Static access
    // -------------------------------------------------------------------------

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("Tower Defense plugin shut down.");
    }

    public EnclaveManager getEnclaveManager() {
        return enclaveManager;
    }
}