package dev.duckslock;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
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
 * NOTE: Replace onEnable / onDisable with the correct lifecycle method names
 * from PluginBase once PluginBase.class is decompiled.
 * Likely candidates: setup(), enable(), teardown(), disable()
 */
public class TowerDefensePlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static TowerDefensePlugin instance;
    private EnclaveManager enclaveManager;

    public TowerDefensePlugin(JavaPluginInit init) {
        super(init);
    }

    public static TowerDefensePlugin getInstance() { return instance;
    }

    // TODO: replace "onEnable" with the real PluginBase lifecycle method name
    // @Override
    public void onEnable() {
        instance = this;

        enclaveManager = new EnclaveManager();

        HytaleServer.get().getEventBus()
                .subscribe(PlayerReadyEvent.class, EnclaveManager.WORLD_NAME,
                        this::onPlayerReady);

        HytaleServer.get().getEventBus()
                .subscribe(PlayerDisconnectEvent.class, EnclaveManager.WORLD_NAME,
                        this::onPlayerDisconnect);

        LOGGER.at(Level.INFO).log("Tower Defense plugin enabled.");
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    // TODO: replace "onDisable" with the real PluginBase lifecycle method name
    // @Override
    public void onDisable() {
        LOGGER.at(Level.INFO).log("Tower Defense plugin disabled.");
    }

    private void onPlayerReady(PlayerReadyEvent event) {
        // PlayerReadyEvent extends PlayerEvent — getPlayer() should exist.
        var player = event.getPlayer();
        if (player == null) return;
        enclaveManager.assignEnclaveToPlayer(player);
    }

    // -------------------------------------------------------------------------
    // Static access
    // -------------------------------------------------------------------------

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        // PlayerDisconnectEvent has no getPlayer() — use getPlayerRef() instead.
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef == null) return;
        UUID uuid = playerRef.getUuid();
        enclaveManager.releaseEnclave(uuid);
    }

    public EnclaveManager getEnclaveManager() {
        return enclaveManager; }
}