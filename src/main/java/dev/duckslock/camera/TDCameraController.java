package dev.duckslock.camera;

import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;

/**
 * TDCameraController
 * <p>
 * Applies a locked top-down camera to each player on join.
 * Camera looks straight down, cursor visible, movement decoupled
 * from camera angle — the classic Warcraft III TD perspective.
 */
public class TDCameraController {

    public void apply(PlayerReadyEvent event) {
        // Get the Player, then PlayerRef, then PacketHandler
        // getPlayerRef() is deprecated but still functional in this EA build
        Player player = event.getPlayer();

        ServerCameraSettings settings = new ServerCameraSettings();

        // Smooth camera tracking
        settings.positionLerpSpeed = 0.2f;
        settings.rotationLerpSpeed = 0.2f;

        // Distance and perspective
        settings.distance = 20.0f;  // blocks above player — adjust to taste
        settings.isFirstPerson = false;
        settings.eyeOffset = true;
        settings.displayCursor = true;   // cursor must be visible for TD clicking

        // Straight down: -90 degrees pitch (-PI/2 radians)
        settings.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
        settings.rotationType = RotationType.Custom;
        settings.rotation = new Direction(0.0f, -1.5707964f, 0.0f);

        // Clicks register on the horizontal ground plane
        settings.mouseInputType = MouseInputType.LookAtPlane;
        settings.planeNormal = new Vector3f(0.0f, 1.0f, 0.0f);

        // Movement aligned to world grid, not camera angle (WC3 feel)
        settings.movementForceRotationType = MovementForceRotationType.Custom;
        settings.movementForceRotation = new Direction(0.0f, 0.0f, 0.0f);

        // Build and send the packet
        // SetServerCamera(view, isLocked, settings)
        // isLocked = true means player cannot rotate or tilt the camera
        SetServerCamera packet = new SetServerCamera(
                ClientCameraView.Custom,
                true,
                settings
        );

        //noinspection deprecation — getPlayerRef() is deprecated but still works in EA
        player.getPlayerRef().getPacketHandler().writeNoCache(packet);
    }
}