package dev.duckslock;

import com.hypixel.hytale.server.core.event.events.PrepareUniverseEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.WorldConfigProvider;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.VoidWorldGenProvider;
import dev.duckslock.camera.TDCameraController;
import dev.duckslock.commands.ArenaDebugRoundService;
import dev.duckslock.commands.DebugRoundCommand;
import dev.duckslock.commands.DebugRoundDefinitions;
import dev.duckslock.config.ModConfigHolder;
import dev.duckslock.config.TDConfig;
import dev.duckslock.config.TDConfigManager;
import dev.duckslock.enclave.EnclaveManager;
import dev.duckslock.enemy.EnemyType;
import dev.duckslock.grid.ArenaConstants;
import dev.duckslock.sprint.SprintMechanicController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Stream;

public class TowerDefensePlugin extends JavaPlugin {

    private static TowerDefensePlugin instance;
    private EnclaveManager enclaveManager;
    private ArenaDebugRoundService debugRoundService;
    private final TDCameraController cameraController = new TDCameraController();
    private TDConfig config;

    public TowerDefensePlugin(JavaPluginInit init) {
        super(init);
    }

    public static TowerDefensePlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        instance = this;
        config = TDConfigManager.loadOrCreate(getDataDirectory().resolve("config.json"), getLogger());
        ModConfigHolder.set(config);
        getLogger().at(Level.INFO).log("Using config at '%s'.", getDataDirectory().resolve("config.json"));
        ArenaConstants.apply(config.arena);
        EnclaveManager.setWorldName(config.world.name);
        EnemyType.applyConfig(config.enemies, getLogger());
        DebugRoundDefinitions.replaceFromConfig(config.debugRounds.rounds);
        SprintMechanicController.enable(this);

        getEventRegistry().register(PrepareUniverseEvent.class, this::onPrepareUniverse);
        getEventRegistry().register(PlayerReadyEvent.class, EnclaveManager.WORLD_NAME, this::onPlayerReady);
        getEventRegistry().register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

        getLogger().at(Level.INFO).log("Tower Defense plugin set up.");
    }

    @Override
    protected void start() {
        enclaveManager = new EnclaveManager();
        enclaveManager.beginWorldBootstrap();
        debugRoundService = new ArenaDebugRoundService(enclaveManager);
        debugRoundService.start();
        enclaveManager.setAssignmentListener(debugRoundService);
        getCommandRegistry().registerCommand(new DebugRoundCommand(debugRoundService));
        getLogger().at(Level.INFO).log("Tower Defense plugin started.");
    }

    @Override
    protected void shutdown() {
        if (debugRoundService != null) {
            debugRoundService.shutdown();
        }
        getLogger().at(Level.INFO).log("Tower Defense plugin shut down.");
    }

    private void onPlayerReady(PlayerReadyEvent event) {
        var player = event.getPlayer();
        if (player == null || enclaveManager == null) {
            return;
        }

        var entityRef = event.getPlayerRef();
        PlayerRef playerRef = entityRef.getStore().getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        cameraController.apply(playerRef.getPacketHandler(), config.camera);
        SprintMechanicController.applyPlayerSettings(player);
        enclaveManager.assignEnclaveToPlayer(event.getPlayerRef(), player, player.getUuid());
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (enclaveManager == null) {
            return;
        }
        UUID uuid = event.getPlayerRef().getUuid();
        enclaveManager.releaseEnclave(uuid);
    }

    public EnclaveManager getEnclaveManager() {
        return enclaveManager;
    }

    private void onPrepareUniverse(PrepareUniverseEvent event) {
        WorldConfigProvider delegate = event.getWorldConfigProvider();
        event.setWorldConfigProvider(new TDWorldConfigProvider(delegate, getLogger()));
    }

    private static final class TDWorldConfigProvider implements WorldConfigProvider {
        private final WorldConfigProvider delegate;
        private final com.hypixel.hytale.logger.HytaleLogger logger;

        private TDWorldConfigProvider(WorldConfigProvider delegate, com.hypixel.hytale.logger.HytaleLogger logger) {
            this.delegate = delegate;
            this.logger = logger;
        }

        @Override
        public CompletableFuture<WorldConfig> load(Path worldPath, String worldName) {
            return delegate.load(worldPath, worldName).thenApply(config -> {
                if (config == null || !EnclaveManager.WORLD_NAME.equals(worldName)) {
                    return config;
                }

                if (!hasGeneratedChunks(worldPath)) {
                    config.setWorldGenProvider(new VoidWorldGenProvider());
                    config.setSpawningNPC(false);
                    config.setIsSpawnMarkersEnabled(false);
                    config.markChanged();
                    logger.at(Level.INFO).log("Using VoidWorldGenProvider for new world '%s'.", worldName);
                }
                return config;
            });
        }

        @Override
        public CompletableFuture<Void> save(Path worldPath, WorldConfig config, World world) {
            return delegate.save(worldPath, config, world);
        }

        private boolean hasGeneratedChunks(Path worldPath) {
            Path chunks = worldPath.resolve("chunks");
            if (!Files.isDirectory(chunks)) {
                return false;
            }
            try (Stream<Path> paths = Files.list(chunks)) {
                return paths.findAny().isPresent();
            } catch (Exception ex) {
                logger.at(Level.WARNING).log("Failed to inspect chunks directory '%s': %s", chunks, ex.toString());
                return false;
            }
        }
    }
}
