package dev.duckslock.camera;

import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class TDCameraController {

    public void apply(PlayerRef playerRef) {
        var settings = new ServerCameraSettings();

        settings.positionLerpSpeed = 0.2f;
        settings.rotationLerpSpeed = 0.2f;

        settings.distance = 20.0f;
        settings.isFirstPerson = false;
        settings.eyeOffset = true;
        settings.displayCursor = true;

        settings.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
        settings.rotationType = RotationType.Custom;
        settings.rotation = new Direction(0.0f, -1.5707964f, 0.0f);

        settings.mouseInputType = MouseInputType.LookAtPlane;
        settings.planeNormal = new Vector3f(0.0f, 1.0f, 0.0f);

        settings.movementForceRotationType = MovementForceRotationType.Custom;
        settings.movementForceRotation = new Direction(0.0f, 0.0f, 0.0f);

        var packet = new SetServerCamera(ClientCameraView.Custom, true, settings);
        playerRef.getPacketHandler().writeNoCache(packet);
    }
}
