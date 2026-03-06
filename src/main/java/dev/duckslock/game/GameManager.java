package dev.duckslock.game;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.duckslock.commands.ArenaDebugRoundService;
import dev.duckslock.commands.DebugRoundDefinition;
import dev.duckslock.commands.DebugRoundDefinitions;
import dev.duckslock.config.ModConfigHolder;
import dev.duckslock.config.TDConfig;
import dev.duckslock.enclave.Enclave;
import dev.duckslock.enclave.EnclaveAssignmentListener;
import dev.duckslock.enclave.EnclaveManager;
import dev.duckslock.wave.WaveManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class GameManager implements EnclaveAssignmentListener {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final EnclaveManager enclaveManager;
    private final ArenaDebugRoundService debugRoundService;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "td-wave-manager");
                t.setDaemon(true);
                return t;
            });
    private final Map<Integer, WaveManager> waveManagers = new ConcurrentHashMap<>();
    private final long tickMs;

    private volatile boolean enabled;
    private volatile int activeRoundId;
    private volatile boolean triggerOnlyOnNewAssignment;

    public GameManager(EnclaveManager enclaveManager, ArenaDebugRoundService debugRoundService) {
        this.enclaveManager = enclaveManager;
        this.debugRoundService = debugRoundService;

        TDConfig config = ModConfigHolder.get();
        this.tickMs = config.gameplay.waveTickMs;
        this.enabled = config.debugRounds.enabledByDefault;
        this.activeRoundId = config.debugRounds.activeRoundId;
        this.triggerOnlyOnNewAssignment = config.debugRounds.triggerOnlyOnNewAssignment;

        List<DebugRoundDefinition> waveDefinitions = buildWaveDefinitions();
        for (Enclave enclave : enclaveManager.getEnclaves()) {
            waveManagers.put(
                    enclave.getIndex(),
                    new WaveManager(
                            enclave,
                            debugRoundService,
                            waveDefinitions,
                            this::onEnclaveLost,
                            config.gameplay.betweenWaveIncome,
                            config.gameplay.prepareCountdownMs,
                            config.gameplay.cleanupDurationMs
                    )
            );
        }
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(this::tickWaveManagers, tickMs, tickMs, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        scheduler.shutdownNow();
        for (WaveManager waveManager : waveManagers.values()) {
            waveManager.forceResetToIdle();
        }
    }

    @Override
    public void onAssigned(Ref<EntityStore> playerEntityRef, UUID playerUuid, Enclave enclave, boolean newAssignment) {
        if (!enabled) {
            return;
        }
        if (triggerOnlyOnNewAssignment && !newAssignment) {
            return;
        }

        enclaveManager.runAfterArenaGenerated(() -> startNextWave(enclave.getIndex()));
    }

    public boolean startNextWave(int enclaveIndex) {
        WaveManager waveManager = waveManagers.get(enclaveIndex);
        if (waveManager == null) {
            return false;
        }
        return waveManager.startNextWave();
    }

    public boolean startDebugRound(int enclaveIndex, int roundId) {
        WaveManager waveManager = waveManagers.get(enclaveIndex);
        if (waveManager == null) {
            return false;
        }
        if (waveManager.getState() != WaveManager.State.IDLE) {
            return false;
        }

        DebugRoundDefinition round = DebugRoundDefinitions.get(roundId);
        if (round == null) {
            return false;
        }

        return waveManager.startDebugRound(round);
    }

    public boolean triggerEarlyStart(int enclaveIndex) {
        WaveManager waveManager = waveManagers.get(enclaveIndex);
        if (waveManager == null) {
            return false;
        }
        return waveManager.triggerEarlyStart();
    }

    public void onEnclaveLost(Enclave enclave) {
        LOGGER.at(Level.WARNING).log(
                "Enclave %s (%s) has lost all lives. Resetting enclave state.",
                enclave.getIndex(),
                enclave.getColor().getDisplayName()
        );

        WaveManager waveManager = waveManagers.get(enclave.getIndex());
        if (waveManager != null) {
            waveManager.forceResetToIdle();
        }

        debugRoundService.clearEnemiesForEnclave(enclave.getIndex());
        enclave.resetEconomyAndLives();
    }

    @Nullable
    public Enclave findEnclaveForPlayer(UUID playerUuid) {
        return enclaveManager.getEnclaveForPlayer(playerUuid);
    }

    @Nullable
    public WaveManager getWaveManager(int enclaveIndex) {
        return waveManagers.get(enclaveIndex);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getActiveRoundId() {
        return activeRoundId;
    }

    public void setActiveRoundId(int activeRoundId) {
        this.activeRoundId = activeRoundId;
    }

    public boolean isTriggerOnlyOnNewAssignment() {
        return triggerOnlyOnNewAssignment;
    }

    public void setTriggerOnlyOnNewAssignment(boolean triggerOnlyOnNewAssignment) {
        this.triggerOnlyOnNewAssignment = triggerOnlyOnNewAssignment;
    }

    private void tickWaveManagers() {
        long nowMs = System.currentTimeMillis();
        for (WaveManager waveManager : waveManagers.values()) {
            waveManager.tick(nowMs);
        }
    }

    private List<DebugRoundDefinition> buildWaveDefinitions() {
        List<DebugRoundDefinition> rounds = new ArrayList<>();
        for (Integer id : DebugRoundDefinitions.ids()) {
            DebugRoundDefinition round = DebugRoundDefinitions.get(id);
            if (round != null) {
                rounds.add(round);
            }
        }
        return rounds;
    }
}
