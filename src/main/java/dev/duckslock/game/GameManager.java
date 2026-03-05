package dev.duckslock.game;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import dev.duckslock.grid.GridManager;
import dev.duckslock.wave.WaveManager;

public class GameManager {

    private final GridManager gridManager;
    private final WaveManager waveManager;

    public GameManager(GridManager gridManager, WaveManager waveManager) {
        this.gridManager = gridManager;
        this.waveManager = waveManager;
    }

    public void onPlayerReady(PlayerReadyEvent event) {
        // Phase 2: seat player into a game session.
    }

    public void shutdown() {
        // Phase 2: stop systems and clean up state.
    }
}
