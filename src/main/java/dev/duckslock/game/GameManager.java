package dev.duckslock.game;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import dev.duckslock.grid.GridManager;
import dev.duckslock.wave.WaveManager;

/**
 * Stub — full implementation in Phase 2
 */
public class GameManager {
    private final GridManager gridManager;
    private final WaveManager waveManager;

    public GameManager(GridManager gridManager, WaveManager waveManager) {
        this.gridManager = gridManager;
        this.waveManager = waveManager;
    }

    public void onPlayerReady(PlayerReadyEvent event) {
        // TODO Phase 2: seat player into game session
    }

    public void shutdown() {
        // TODO Phase 2
    }
}