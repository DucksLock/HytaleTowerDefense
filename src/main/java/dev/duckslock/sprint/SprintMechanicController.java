package dev.duckslock.sprint;

import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.protocol.packets.player.UpdateMovementSettings;
import com.hypixel.hytale.protocol.packets.setup.ClientFeature;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementConfig;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import dev.duckslock.config.ModConfigHolder;
import dev.duckslock.config.TDConfig;

/**
 * Handles sprint behavior tuning for top-down movement.
 */
public final class SprintMechanicController {

    private SprintMechanicController() {
    }

    public static void enable(PluginBase plugin) {
        TDConfig.SprintConfig sprint = ModConfigHolder.get().sprint;
        if (sprint.enableSprintForceFeature) {
            plugin.getClientFeatureRegistry().register(ClientFeature.SprintForce);
        }
    }

    public static void applyPlayerSettings(Player player) {
        TDConfig.SprintConfig sprint = ModConfigHolder.get().sprint;
        if (!sprint.applyDirectionalSprintFix) {
            return;
        }

        MovementSettings settings = MovementConfig.DEFAULT_MOVEMENT.toPacket();
        float fallbackSprintMultiplier = settings.forwardSprintSpeedMultiplier;

        settings.backwardRunSpeedMultiplier = sprint.backwardRunSpeedMultiplier > 0f
                ? sprint.backwardRunSpeedMultiplier
                : fallbackSprintMultiplier;
        settings.strafeRunSpeedMultiplier = sprint.strafeRunSpeedMultiplier > 0f
                ? sprint.strafeRunSpeedMultiplier
                : fallbackSprintMultiplier;

        player.getPlayerConnection().writeNoCache(new UpdateMovementSettings(settings));
    }
}
