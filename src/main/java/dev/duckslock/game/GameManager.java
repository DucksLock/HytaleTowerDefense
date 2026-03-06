package dev.duckslock.game;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.duckslock.combat.ElementType;
import dev.duckslock.commands.ArenaDebugRoundService;
import dev.duckslock.commands.DebugRoundDefinition;
import dev.duckslock.commands.DebugRoundDefinitions;
import dev.duckslock.config.ModConfigHolder;
import dev.duckslock.config.TDConfig;
import dev.duckslock.enclave.Enclave;
import dev.duckslock.enclave.EnclaveAssignmentListener;
import dev.duckslock.enclave.EnclaveManager;
import dev.duckslock.enemy.EnemyAssets;
import dev.duckslock.enemy.EnemySpawnProfile;
import dev.duckslock.tower.TowerManager;
import dev.duckslock.tower.TowerType;
import dev.duckslock.tower.UpgradePath;
import dev.duckslock.wave.WaveManager;
import dev.duckslock.wc3.WC3ReferenceData;

import javax.annotation.Nullable;
import java.io.IOException;
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
    private final Map<Integer, TowerManager> towerManagers = new ConcurrentHashMap<>();
    private final long tickMs;
    private final long interestIntervalMs;
    private final double interestUpgradePercent;
    private final boolean useWc3ReferenceWaves;
    private final int wc3WaveDefaultCount;
    private final int wc3WavePeriodicCount;

    private volatile boolean enabled;
    private volatile int activeRoundId;
    private volatile boolean triggerOnlyOnNewAssignment;
    private volatile long nextInterestPayoutAtMs;

    public GameManager(EnclaveManager enclaveManager, ArenaDebugRoundService debugRoundService) {
        this.enclaveManager = enclaveManager;
        this.debugRoundService = debugRoundService;

        TDConfig config = ModConfigHolder.get();
        this.tickMs = config.gameplay.waveTickMs;
        this.interestIntervalMs = config.gameplay.interestIntervalMs;
        this.interestUpgradePercent = config.gameplay.interestUpgradePercent;
        this.useWc3ReferenceWaves = config.gameplay.useWc3ReferenceWaves;
        this.wc3WaveDefaultCount = Math.max(1, config.gameplay.wc3WaveDefaultCount);
        this.wc3WavePeriodicCount = Math.max(1, config.gameplay.wc3WavePeriodicCount);
        this.enabled = config.debugRounds.enabledByDefault;
        this.activeRoundId = config.debugRounds.activeRoundId;
        this.triggerOnlyOnNewAssignment = config.debugRounds.triggerOnlyOnNewAssignment;

        List<DebugRoundDefinition> waveDefinitions = buildWaveDefinitions();
        for (Enclave enclave : enclaveManager.getEnclaves()) {
            towerManagers.put(
                    enclave.getIndex(),
                    new TowerManager(enclave, debugRoundService)
            );
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
        nextInterestPayoutAtMs = System.currentTimeMillis() + interestIntervalMs;
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

    public TowerManager.PlacementResult placeTower(int enclaveIndex, TowerType type, int worldX, int worldZ) {
        TowerManager towerManager = towerManagers.get(enclaveIndex);
        if (towerManager == null) {
            return TowerManager.PlacementResult.fail("Tower manager not found for enclave " + enclaveIndex + ".");
        }
        return towerManager.placeTower(type, worldX, worldZ);
    }

    public TowerManager.ActionResult upgradeTower(int enclaveIndex, int worldX, int worldZ, UpgradePath path) {
        TowerManager towerManager = towerManagers.get(enclaveIndex);
        if (towerManager == null) {
            return TowerManager.ActionResult.fail("Tower manager not found for enclave " + enclaveIndex + ".");
        }
        return towerManager.upgradeTowerAt(worldX, worldZ, path);
    }

    public TowerManager.ActionResult sellTower(int enclaveIndex, int worldX, int worldZ) {
        TowerManager towerManager = towerManagers.get(enclaveIndex);
        if (towerManager == null) {
            return TowerManager.ActionResult.fail("Tower manager not found for enclave " + enclaveIndex + ".");
        }
        return towerManager.sellTowerAt(worldX, worldZ);
    }

    public boolean upgradeInterestRate(int enclaveIndex) {
        if (enclaveIndex < 0 || enclaveIndex >= enclaveManager.getEnclaves().length) {
            return false;
        }
        Enclave enclave = enclaveManager.getEnclave(enclaveIndex);
        if (!enclave.consumeElementPickToken()) {
            return false;
        }
        enclave.increaseInterest(interestUpgradePercent);
        return true;
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
        TowerManager towerManager = towerManagers.get(enclave.getIndex());
        if (towerManager != null) {
            towerManager.clearAllTowers();
        }
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
        for (TowerManager towerManager : towerManagers.values()) {
            towerManager.tick(nowMs);
        }
        tickInterest(nowMs);
    }

    private void tickInterest(long nowMs) {
        if (interestIntervalMs <= 0L || nowMs < nextInterestPayoutAtMs) {
            return;
        }

        while (nextInterestPayoutAtMs <= nowMs) {
            nextInterestPayoutAtMs += interestIntervalMs;
        }

        for (Enclave enclave : enclaveManager.getEnclaves()) {
            if (enclave.getLives() <= 0) {
                continue;
            }
            enclave.payoutInterest();
        }
    }

    private List<DebugRoundDefinition> buildWaveDefinitions() {
        if (useWc3ReferenceWaves) {
            try {
                WC3ReferenceData wc3 = WC3ReferenceData.loadFromResource("wc3/ElementTDReference.json");
                List<DebugRoundDefinition> converted = buildWaveDefinitionsFromWc3(wc3);
                if (!converted.isEmpty()) {
                    LOGGER.at(Level.INFO).log(
                            "Loaded %s WC3-derived wave definitions from %s.",
                            converted.size(),
                            wc3.getSource()
                    );
                    return converted;
                }
            } catch (IOException ex) {
                LOGGER.at(Level.WARNING).log("Failed to load wc3/ElementTDReference.json: %s", ex.toString());
            }
        }

        List<DebugRoundDefinition> rounds = new ArrayList<>();
        for (Integer id : DebugRoundDefinitions.ids()) {
            DebugRoundDefinition round = DebugRoundDefinitions.get(id);
            if (round != null) {
                rounds.add(round);
            }
        }
        return rounds;
    }

    private List<DebugRoundDefinition> buildWaveDefinitionsFromWc3(WC3ReferenceData wc3) {
        List<WC3ReferenceData.WaveRecord> waveRecords = wc3.getWaves();
        if (waveRecords.isEmpty()) {
            return List.of();
        }

        List<Vector3d> fallbackWaypoints = resolveFallbackWaypoints();
        List<DebugRoundDefinition> rounds = new ArrayList<>(waveRecords.size());

        for (WC3ReferenceData.WaveRecord wave : waveRecords) {
            int waveNumber = wave.getWave();
            int hp = Math.max(1, (int) Math.round(wave.getBaseHp() == null ? 100d : wave.getBaseHp()));
            float speed = wave.getBaseMoveSpeed() == null
                    ? 1.0f
                    : Math.max(0.1f, (float) (wave.getBaseMoveSpeed() / 300.0d));
            int bounty = Math.max(0, wave.getBounty() == null ? 1 : wave.getBounty());
            int count = waveNumber % 5 == 0 ? wc3WavePeriodicCount : wc3WaveDefaultCount;

            EnemySpawnProfile profile = new EnemySpawnProfile(
                    wave.getUnitId(),
                    pickWaveAsset(waveNumber),
                    hp,
                    speed,
                    0.0f,
                    bounty,
                    1,
                    false,
                    ElementType.NONE
            );

            DebugRoundDefinition.SpawnBatch batch = new DebugRoundDefinition.SpawnBatch(
                    0L,
                    dev.duckslock.enemy.EnemyType.GRUNT,
                    count,
                    profile
            );

            String unitName = wave.getUnitName() == null || wave.getUnitName().isBlank()
                    ? wave.getUnitId()
                    : wave.getUnitName().replace("\"", "");
            String name = "Wave " + waveNumber + " - " + unitName;
            rounds.add(new DebugRoundDefinition(name, fallbackWaypoints, List.of(batch)));
        }

        return rounds;
    }

    private List<Vector3d> resolveFallbackWaypoints() {
        DebugRoundDefinition first = DebugRoundDefinitions.get(activeRoundId);
        if (first != null && first.getLocalWaypoints().size() >= 2) {
            return first.getLocalWaypoints();
        }

        for (Integer id : DebugRoundDefinitions.ids()) {
            DebugRoundDefinition candidate = DebugRoundDefinitions.get(id);
            if (candidate != null && candidate.getLocalWaypoints().size() >= 2) {
                return candidate.getLocalWaypoints();
            }
        }

        return List.of(
                new Vector3d(1.5d, 1.0d, 1.5d),
                new Vector3d(18.5d, 1.0d, 18.5d)
        );
    }

    private String pickWaveAsset(int waveNumber) {
        String[] assets = {
                EnemyAssets.ZOMBIE,
                EnemyAssets.SKELETON_SCOUT,
                EnemyAssets.SKELETON_SOLDIER,
                EnemyAssets.TRORK_UNARMED,
                EnemyAssets.OUTLANDER_HUNTER,
                EnemyAssets.OUTLANDER_BRUTE,
                EnemyAssets.EARTHEN_GOLEM
        };
        return assets[Math.floorMod(waveNumber - 1, assets.length)];
    }
}
