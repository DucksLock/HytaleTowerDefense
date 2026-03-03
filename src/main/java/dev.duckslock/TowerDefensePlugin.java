package dev.duckslock;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import dev.duckslock.camera.TDCameraController;
import dev.duckslock.game.GameManager;
import dev.duckslock.grid.GridManager;
import dev.duckslock.wave.WaveManager;
import javax.annotation.Nonnull;

public class TowerDefensePlugin extends JavaPlugin {

    private static TowerDefensePlugin instance;

    private GridManager gridManager;
    private WaveManager waveManager;
    private GameManager gameManager;
    private TDCameraController cameraController;

    public TowerDefensePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        instance = this;

        gridManager      = new GridManager();
        waveManager      = new WaveManager();
        gameManager      = new GameManager(gridManager, waveManager);
        cameraController = new TDCameraController();

        // Apply camera and seat player into game when they're ready in the world
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, event -> {
            cameraController.apply(event);
            gameManager.onPlayerReady(event);
        });
    }

    public static TowerDefensePlugin getInstance() { return instance; }
    public GridManager getGridManager()            { return gridManager; }
    public WaveManager getWaveManager()            { return waveManager; }
    public GameManager getGameManager()            { return gameManager; }
}