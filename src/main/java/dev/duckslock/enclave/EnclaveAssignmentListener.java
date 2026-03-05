package dev.duckslock.enclave;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

public interface EnclaveAssignmentListener {

    void onAssigned(Ref<EntityStore> playerEntityRef, UUID playerUuid, Enclave enclave, boolean newAssignment);
}
