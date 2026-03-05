package dev.duckslock.sprint;

import com.hypixel.hytale.protocol.packets.setup.ClientFeature;
import com.hypixel.hytale.server.core.plugin.PluginBase;

/**
 * Enables sprint-force client behavior so holding Shift allows sprint while moving
 * in any top-down direction (W/A/S/D), not only forward.
 */
public final class SprintMechanicController {

    private SprintMechanicController() {
    }

    public static void enable(PluginBase plugin) {
        plugin.getClientFeatureRegistry().register(ClientFeature.SprintForce);
    }
}
