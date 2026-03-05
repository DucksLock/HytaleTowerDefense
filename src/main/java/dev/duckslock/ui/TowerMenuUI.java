package dev.duckslock.ui;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import dev.duckslock.grid.GridSquare;

public class TowerMenuUI {

    public static void openPlacementMenu(PlayerReadyEvent event, GridSquare square) {
        // Phase 3: show placement options for this square.
    }

    public static void openUpgradeMenu(PlayerReadyEvent event, GridSquare square) {
        // Phase 3: show upgrade options for an existing tower.
    }

    public static void closeMenu(PlayerReadyEvent event) {
        // Phase 3: close any open tower menu for this player.
    }
}
