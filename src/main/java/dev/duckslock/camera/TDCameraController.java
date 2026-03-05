package dev.duckslock.camera;

import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.io.PacketHandler;
import dev.duckslock.config.TDConfig;

public class TDCameraController {

    public void apply(PacketHandler packetHandler, TDConfig.CameraConfig config) {
        TDConfig.CameraConfig camera = config == null ? new TDConfig.CameraConfig() : config;
        var settings = new ServerCameraSettings();

        settings.positionLerpSpeed = camera.positionLerpSpeed;
        settings.rotationLerpSpeed = camera.rotationLerpSpeed;

        settings.distance = camera.distance;
        settings.isFirstPerson = camera.isFirstPerson;
        settings.eyeOffset = camera.eyeOffset;
        settings.displayCursor = camera.displayCursor;

        settings.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
        settings.rotationType = RotationType.Custom;
        settings.rotation = new Direction(camera.rotationPitch, camera.rotationYaw, camera.rotationRoll);

        settings.mouseInputType = MouseInputType.LookAtPlane;
        settings.planeNormal = new Vector3f(camera.planeNormalX, camera.planeNormalY, camera.planeNormalZ);

        settings.movementForceRotationType = MovementForceRotationType.Custom;
        settings.movementForceRotation = new Direction(
                camera.movementForcePitch,
                camera.movementForceYaw,
                camera.movementForceRoll
        );

        var packet = new SetServerCamera(ClientCameraView.Custom, true, settings);
        packetHandler.writeNoCache(packet);
    }
}
