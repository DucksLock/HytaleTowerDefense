package dev.duckslock;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.duckslock.camera.TDCameraController;
import dev.duckslock.enclave.EnclaveManager;

import java.util.UUID;
import java.util.logging.Level;

public class TowerDefensePlugin extends JavaPlugin {

    private static TowerDefensePlugin instance;
    private EnclaveManager enclaveManager;
    private final TDCameraController cameraController = new TDCameraController();

    public TowerDefensePlugin(JavaPluginInit init) {
        super(init);
    }

    public static TowerDefensePlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        instance = this;

        getEventRegistry().register(PlayerReadyEvent.class, EnclaveManager.WORLD_NAME, this::onPlayerReady);
        getEventRegistry().register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

        getLogger().at(Level.INFO).log("Tower Defense plugin set up.");
    }

    @Override
    protected void start() {
        enclaveManager = new EnclaveManager();
        getLogger().at(Level.INFO).log("Tower Defense plugin started.");
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("Tower Defense plugin shut down.");
    }

    private void onPlayerReady(PlayerReadyEvent event) {
        var player = event.getPlayer();
        if (player == null || enclaveManager == null) {
            return;
        }

        // PlayerReadyEvent still exposes PlayerRef via deprecated API.
        //noinspection deprecation
        cameraController.apply(player.getPlayerRef());
        enclaveManager.assignEnclaveToPlayer(player);
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (enclaveManager == null) {
            return;
        }
        UUID uuid = event.getPlayerRef().getUuid();
        enclaveManager.releaseEnclave(uuid);
    }

    public EnclaveManager getEnclaveManager() {
        return enclaveManager;
    }
}
