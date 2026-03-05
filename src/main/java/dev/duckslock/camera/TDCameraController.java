package dev.duckslock.camera;

import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class TDCameraController {

    // send locked top-down camera packet to player
    public void apply(PlayerRef playerRef) {
        ServerCameraSettings settings = new ServerCameraSettings();

        // smooth follow
        settings.positionLerpSpeed = 0.2f;
        settings.rotationLerpSpeed = 0.2f;

        // third person, 20 blocks above, cursor on
        settings.distance = 20.0f;
        settings.isFirstPerson = false;
        settings.eyeOffset = true;
        settings.displayCursor = true;

        // aim straight down (-PI/2 pitch)
        settings.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
        settings.rotationType = RotationType.Custom;
        settings.rotation = new Direction(0.0f, -1.5707964f, 0.0f);

        // clicks hit the ground plane
        settings.mouseInputType = MouseInputType.LookAtPlane;
        settings.planeNormal = new Vector3f(0.0f, 1.0f, 0.0f);

        // movement aligned to world grid not camera angle
        settings.movementForceRotationType = MovementForceRotationType.Custom;
        settings.movementForceRotation = new Direction(0.0f, 0.0f, 0.0f);

        SetServerCamera packet = new SetServerCamera(ClientCameraView.Custom, true, settings);
        playerRef.getPacketHandler().writeNoCache(packet);
    }
}