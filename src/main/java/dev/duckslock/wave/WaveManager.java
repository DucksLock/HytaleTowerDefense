package dev.duckslock.wave;

import dev.duckslock.commands.ArenaDebugRoundService;
import dev.duckslock.commands.DebugRoundDefinition;
import dev.duckslock.enclave.Enclave;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class WaveManager {

    private final Enclave enclave;
    private final ArenaDebugRoundService debugRoundService;
    private final List<DebugRoundDefinition> waveDefinitions;
    private final Consumer<Enclave> enclaveLostCallback;
    private final int betweenWaveIncome;
    private final long prepareCountdownMs;
    private final long cleanupDurationMs;
    private State state = State.IDLE;
    private int currentWaveNumber = 0;
    private int activeEnemyCount = 0;
    private boolean earlyWaveTriggered = false;
    private long stateDeadlineMs = 0L;
    @Nullable
    private DebugRoundDefinition activeRound;

    public WaveManager(
            Enclave enclave,
            ArenaDebugRoundService debugRoundService,
            List<DebugRoundDefinition> waveDefinitions,
            Consumer<Enclave> enclaveLostCallback,
            int betweenWaveIncome,
            long prepareCountdownMs,
            long cleanupDurationMs
    ) {
        this.enclave = enclave;
        this.debugRoundService = debugRoundService;
        this.waveDefinitions = waveDefinitions;
        this.enclaveLostCallback = enclaveLostCallback;
        this.betweenWaveIncome = Math.max(0, betweenWaveIncome);
        this.prepareCountdownMs = Math.max(0L, prepareCountdownMs);
        this.cleanupDurationMs = Math.max(0L, cleanupDurationMs);
    }

    public void tick(long nowMs) {
        DebugRoundDefinition roundToSpawn = null;

        synchronized (this) {
            if (state == State.PREPARE && nowMs >= stateDeadlineMs) {
                roundToSpawn = activeRound;
                state = State.SPAWNING;
                stateDeadlineMs = 0L;
            } else if (state == State.CLEANUP && nowMs >= stateDeadlineMs) {
                enclave.addGold(betweenWaveIncome);
                currentWaveNumber++;
                state = State.IDLE;
                stateDeadlineMs = 0L;
                activeEnemyCount = 0;
                earlyWaveTriggered = false;
                activeRound = null;
            }
        }

        if (roundToSpawn != null && !debugRoundService.startDebugRound(enclave, roundToSpawn, this)) {
            forceResetToIdle();
        }
    }

    public boolean startNextWave() {
        DebugRoundDefinition nextRound = resolveNextRound();
        if (nextRound == null) {
            return false;
        }

        synchronized (this) {
            if (state != State.IDLE) {
                return false;
            }

            activeRound = nextRound;
            activeEnemyCount = 0;
            earlyWaveTriggered = false;
            state = State.PREPARE;
            stateDeadlineMs = System.currentTimeMillis() + prepareCountdownMs;
            return true;
        }
    }

    public boolean triggerEarlyStart() {
        DebugRoundDefinition roundToSpawn;

        synchronized (this) {
            if (state != State.PREPARE || activeRound == null) {
                return false;
            }

            earlyWaveTriggered = true;
            state = State.SPAWNING;
            stateDeadlineMs = 0L;
            roundToSpawn = activeRound;
        }

        if (!debugRoundService.startDebugRound(enclave, roundToSpawn, this)) {
            forceResetToIdle();
            return false;
        }
        return true;
    }

    public boolean startDebugRound(DebugRoundDefinition round) {
        if (round == null) {
            return false;
        }

        synchronized (this) {
            if (state != State.IDLE) {
                return false;
            }

            activeRound = round;
            activeEnemyCount = 0;
            earlyWaveTriggered = false;
            state = State.SPAWNING;
            stateDeadlineMs = 0L;
        }

        if (!debugRoundService.startDebugRound(enclave, round, this)) {
            forceResetToIdle();
            return false;
        }

        return true;
    }

    public synchronized void onEnemySpawned() {
        if (state == State.SPAWNING || state == State.ACTIVE) {
            activeEnemyCount++;
        }
    }

    public synchronized void onAllEnemiesSpawned() {
        if (state != State.SPAWNING) {
            return;
        }

        if (activeEnemyCount <= 0) {
            enterCleanup();
            return;
        }

        state = State.ACTIVE;
    }

    public synchronized void onEnemyResolved() {
        if (activeEnemyCount > 0) {
            activeEnemyCount--;
        } else {
            activeEnemyCount = 0;
        }

        if (state == State.ACTIVE && activeEnemyCount <= 0) {
            enterCleanup();
        }
    }

    public void onEnclaveDefeated() {
        enclaveLostCallback.accept(enclave);
    }

    public synchronized void forceResetToIdle() {
        state = State.IDLE;
        stateDeadlineMs = 0L;
        activeEnemyCount = 0;
        earlyWaveTriggered = false;
        activeRound = null;
    }

    public synchronized int getCurrentWaveNumber() {
        return currentWaveNumber;
    }

    public synchronized State getState() {
        return state;
    }

    public synchronized int getActiveEnemyCount() {
        return activeEnemyCount;
    }

    public synchronized boolean isEarlyWaveTriggered() {
        return earlyWaveTriggered;
    }

    public Enclave getEnclave() {
        return enclave;
    }

    @Nullable
    private synchronized DebugRoundDefinition resolveNextRound() {
        if (waveDefinitions == null || waveDefinitions.isEmpty()) {
            return null;
        }

        int nextWave = currentWaveNumber + 1;
        int index = Math.floorMod(nextWave - 1, waveDefinitions.size());
        return waveDefinitions.get(index);
    }

    private void enterCleanup() {
        state = State.CLEANUP;
        stateDeadlineMs = System.currentTimeMillis() + cleanupDurationMs;
    }

    public enum State {
        IDLE,
        PREPARE,
        SPAWNING,
        ACTIVE,
        CLEANUP
    }
}
