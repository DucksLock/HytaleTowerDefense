package dev.duckslock.enclave;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.duckslock.config.ModConfigHolder;
import dev.duckslock.config.TDConfig;
import dev.duckslock.grid.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class EnclaveManager {

    public static String WORLD_NAME = "default";
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final MapRegistry mapRegistry;
    private final GridManager gridManager;
    private final Enclave[] enclaves = new Enclave[ArenaConstants.ENCLAVE_COUNT];
    private final String arenaMarkerFile;
    private final int preloadMarginChunks;
    private final long bootstrapPollMs;
    private final Set<String> missingBlockTypeNames = new HashSet<>();
    private final List<Runnable> pendingAfterGeneration = new ArrayList<>();
    private final ScheduledExecutorService bootstrapExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "td-arena-bootstrap");
                t.setDaemon(true);
                return t;
            });
    private final AtomicBoolean bootstrapLoopStarted = new AtomicBoolean(false);
    private final AtomicBoolean generationQueued = new AtomicBoolean(false);
    private boolean arenaGenerated = false;
    @Nullable
    private EnclaveAssignmentListener assignmentListener;

    public EnclaveManager(MapRegistry mapRegistry) {
        this.mapRegistry = Objects.requireNonNull(mapRegistry, "mapRegistry");
        MapDefinition activeMap = requireActiveMap();
        this.gridManager = new GridManager(activeMap);

        TDConfig.WorldConfig worldConfig = ModConfigHolder.get().world;
        TDConfig.GameplayConfig gameplayConfig = ModConfigHolder.get().gameplay;
        this.arenaMarkerFile = worldConfig.arenaMarkerFile;
        this.preloadMarginChunks = worldConfig.preloadMarginChunks;
        this.bootstrapPollMs = worldConfig.bootstrapPollMs;

        for (int i = 0; i < ArenaConstants.ENCLAVE_COUNT; i++) {
            enclaves[i] = new Enclave(
                    i,
                    EnclaveColor.fromIndex(i),
                    gameplayConfig.startingLives,
                    gameplayConfig.startingGold,
                    gameplayConfig.baseInterestPercent,
                    activeMap
            );
        }

        LOGGER.at(Level.INFO).log("Initialized %s enclaves.", ArenaConstants.ENCLAVE_COUNT);
        logLayout();
    }

    public static void setWorldName(String worldName) {
        if (worldName != null && !worldName.isBlank()) {
            WORLD_NAME = worldName;
        }
    }

    private void tryBootstrapArena() {
        if (arenaGenerated) {
            stopBootstrapLoop();
            return;
        }

        World world = Universe.get().getWorld(WORLD_NAME);
        if (world == null) {
            return;
        }

        queueArenaGeneration(world);
    }

    private void generateArena(World world) {
        LOGGER.at(Level.INFO).log("Generating arena at Y=%s...", ArenaConstants.ARENA_FLOOR_Y);

        int floorY = ArenaConstants.ARENA_FLOOR_Y;
        MapDefinition map = requireActiveMap();
        gridManager.loadMap(map);

        int width = map.getGridWidth() * ArenaConstants.SQUARE_SIZE;
        int depth = map.getGridHeight() * ArenaConstants.SQUARE_SIZE;

        for (GridSquareData squareData : map.getSquares()) {
            GridSquareType squareType = squareData.getType() == null
                    ? GridSquareType.BLOCKED
                    : squareData.getType();
            if (squareType == GridSquareType.BLOCKED) {
                continue;
            }

            String blockType = switch (squareType) {
                case PATH -> ArenaConstants.BLOCK_PATH;
                case BUILDABLE -> ArenaConstants.BLOCK_BUILDABLE;
                case BASE -> ArenaConstants.BLOCK_BASE;
                case BLOCKED -> null;
            };

            if (blockType == null) {
                continue;
            }

            int worldStartX = ArenaConstants.ARENA_ORIGIN_X + squareData.getGridX() * ArenaConstants.SQUARE_SIZE;
            int worldStartZ = ArenaConstants.ARENA_ORIGIN_Z + squareData.getGridZ() * ArenaConstants.SQUARE_SIZE;
            int squareSize = ArenaConstants.SQUARE_SIZE;
            for (int dx = 0; dx < squareSize; dx++) {
                for (int dz = 0; dz < squareSize; dz++) {
                    placeBlock(world, worldStartX + dx, floorY, worldStartZ + dz, blockType);
                }
            }
        }

        LOGGER.at(Level.INFO).log(
                "Arena generation complete for map '%s': %s x %s blocks.",
                map.getName(),
                width,
                depth
        );
    }

    // signature now takes playerEntityRef so we can teleport
    public void assignEnclaveToPlayer(Ref<EntityStore> playerEntityRef, Player player, UUID uuid) {
        beginWorldBootstrap();
        World world = Universe.get().getWorld(WORLD_NAME);
        boolean newAssignment = getEnclaveForPlayer(uuid) == null;

        Enclave assigned = resolveOrAssignEnclave(player, uuid);
        if (assigned == null) {
            return;
        }

        if (assignmentListener != null) {
            try {
                assignmentListener.onAssigned(playerEntityRef, uuid, assigned, newAssignment);
            } catch (RuntimeException ex) {
                LOGGER.at(Level.WARNING).log("Assignment listener failed: %s", ex.toString());
            }
        }

        if (arenaGenerated) {
            if (world != null) {
                world.execute(() -> doTeleport(playerEntityRef, assigned));
            }
            return;
        }

        enqueueAfterGeneration(() -> doTeleport(playerEntityRef, assigned));
        if (world == null) {
            LOGGER.at(Level.WARNING).log("World '%s' not available yet, delaying arena generation.", WORLD_NAME);
            return;
        }

        queueArenaGeneration(world);
    }

    // resolves reconnect or assigns new enclave, sends chat msg, returns the enclave
    @Nullable
    private Enclave resolveOrAssignEnclave(Player player, UUID uuid) {
        String name = player.getDisplayName();

        for (Enclave enclave : enclaves) {
            if (uuid.equals(enclave.getOwnerUuid())) {
                player.sendMessage(Message.raw("[TD] Welcome back " + name + "! You are "
                        + enclave.getColor().getDisplayName() + " (enclave " + enclave.getIndex() + ")."));
                LOGGER.at(Level.INFO).log("%s reconnected to enclave %s (%s).",
                        name, enclave.getIndex(), enclave.getColor().getDisplayName());
                return enclave;
            }
        }

        Enclave next = getNextFreeEnclave();
        if (next == null) {
            player.sendMessage(Message.raw("[TD] Server full, all enclaves taken."));
            LOGGER.at(Level.WARNING).log("Could not assign enclave to %s: all full.", name);
            return null;
        }

        next.assignOwner(uuid, name);
        next.resetEconomyAndLives();
        player.sendMessage(Message.raw("[TD] Welcome " + name + "! Your color is "
                + next.getColor().getDisplayName() + " (enclave " + next.getIndex() + ")."));
        LOGGER.at(Level.INFO).log("Assigned %s to enclave %s (%s).",
                name, next.getIndex(), next.getColor().getDisplayName());
        return next;
    }

    // teleport player to centre of their enclave, 1 above floor - must run on world thread
    private void doTeleport(Ref<EntityStore> ref, Enclave enclave) {
        if (!ref.isValid()) return;
        Store<EntityStore> store = ref.getStore();
        double cx = enclave.getCentreWorldX() + 0.5;
        double cy = ArenaConstants.ARENA_FLOOR_Y + 1.0;
        double cz = enclave.getCentreWorldZ() + 0.5;
        store.addComponent(ref, EntityModule.get().getTeleportComponentType(),
                new Teleport(new Vector3d(cx, cy, cz), new Vector3f(0f, 0f, 0f)));
        LOGGER.at(Level.INFO).log("Teleported to enclave %s at (%.1f, %.1f, %.1f)",
                enclave.getIndex(), cx, cy, cz);
    }

    public void releaseEnclave(UUID playerUuid) {
        for (Enclave enclave : enclaves) {
            if (playerUuid.equals(enclave.getOwnerUuid())) {
                LOGGER.at(Level.INFO).log("Released enclave %s from %s.",
                        enclave.getIndex(), enclave.getOwnerName());
                enclave.clearOwner();
                enclave.resetEconomyAndLives();
                return;
            }
        }
    }

    @Nullable
    public Enclave getEnclaveForPlayer(UUID playerUuid) {
        for (Enclave enclave : enclaves) {
            if (playerUuid.equals(enclave.getOwnerUuid())) {
                return enclave;
            }
        }
        return null;
    }

    @Nullable
    public GridSquare getSquareAtWorldPos(int worldX, int worldZ) {
        for (Enclave enclave : enclaves) {
            GridSquare square = enclave.getSquareAtWorldPos(worldX, worldZ);
            if (square != null) {
                return square;
            }
        }
        return null;
    }

    public Enclave[] getEnclaves() {
        return enclaves;
    }

    public Enclave getEnclave(int index) {
        return enclaves[index];
    }

    public MapRegistry getMapRegistry() {
        return mapRegistry;
    }

    public GridManager getGridManager() {
        return gridManager;
    }

    public void setAssignmentListener(@Nullable EnclaveAssignmentListener listener) {
        this.assignmentListener = listener;
    }

    public boolean isArenaGenerated() {
        return arenaGenerated;
    }

    public void runAfterArenaGenerated(Runnable task) {
        if (arenaGenerated) {
            task.run();
            return;
        }
        enqueueAfterGeneration(task);
    }

    @Nullable
    private Enclave getNextFreeEnclave() {
        for (Enclave enclave : enclaves) {
            if (!enclave.isOwned()) return enclave;
        }
        return null;
    }

    private void queueArenaGeneration(World world) {
        if (arenaGenerated || !generationQueued.compareAndSet(false, true)) {
            return;
        }

        preloadArenaChunks(world).whenComplete((ignored, preloadError) -> {
            if (preloadError != null) {
                LOGGER.at(Level.WARNING).log("Chunk preload failed before arena generation: %s", preloadError.toString());
            }

            world.execute(() -> {
                try {
                    ensureArenaGenerated(world);
                } finally {
                    generationQueued.set(false);
                    if (arenaGenerated) {
                        stopBootstrapLoop();
                    }
                }
            });
        });
    }

    public void beginWorldBootstrap() {
        if (!bootstrapLoopStarted.compareAndSet(false, true)) {
            return;
        }

        bootstrapExecutor.scheduleWithFixedDelay(this::tryBootstrapArena,
                0L, bootstrapPollMs, TimeUnit.MILLISECONDS);
    }

    private CompletableFuture<Void> preloadArenaChunks(World world) {
        int minX = ArenaConstants.ARENA_ORIGIN_X - preloadMarginChunks * ChunkUtil.SIZE;
        int minZ = ArenaConstants.ARENA_ORIGIN_Z - preloadMarginChunks * ChunkUtil.SIZE;
        int maxX = ArenaConstants.ARENA_ORIGIN_X + getArenaWidthBlocks() - 1 + preloadMarginChunks * ChunkUtil.SIZE;
        int maxZ = ArenaConstants.ARENA_ORIGIN_Z + getArenaDepthBlocks() - 1 + preloadMarginChunks * ChunkUtil.SIZE;

        int minChunkX = ChunkUtil.chunkCoordinate(minX);
        int minChunkZ = ChunkUtil.chunkCoordinate(minZ);
        int maxChunkX = ChunkUtil.chunkCoordinate(maxX);
        int maxChunkZ = ChunkUtil.chunkCoordinate(maxZ);

        List<CompletableFuture<WorldChunk>> futures = new ArrayList<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                futures.add(world.getNonTickingChunkAsync(ChunkUtil.indexChunk(cx, cz)));
            }
        }

        if (futures.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        LOGGER.at(Level.INFO).log("Preloading %s chunks for arena bootstrap.", futures.size());
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private void writeArenaMarker(Path markerPath) {
        try {
            Files.createDirectories(markerPath.getParent());
            Files.writeString(markerPath, "generatedAt=" + Instant.now() + System.lineSeparator());
        } catch (IOException ex) {
            LOGGER.at(Level.WARNING).log("Failed to write arena marker '%s': %s", markerPath, ex.toString());
        }
    }

    private void enqueueAfterGeneration(Runnable task) {
        synchronized (pendingAfterGeneration) {
            pendingAfterGeneration.add(task);
        }
    }

    private void runPendingAfterGeneration() {
        List<Runnable> tasks;
        synchronized (pendingAfterGeneration) {
            if (pendingAfterGeneration.isEmpty()) {
                return;
            }
            tasks = new ArrayList<>(pendingAfterGeneration);
            pendingAfterGeneration.clear();
        }

        for (Runnable task : tasks) {
            try {
                task.run();
            } catch (RuntimeException ex) {
                LOGGER.at(Level.WARNING).log("Post-generation task failed: %s", ex.toString());
            }
        }
    }

    private void stopBootstrapLoop() {
        if (!bootstrapExecutor.isShutdown()) {
            bootstrapExecutor.shutdown();
        }
    }

    private int getArenaWidthBlocks() {
        MapDefinition map = mapRegistry.getActiveMap();
        if (map == null) {
            return ArenaConstants.totalArenaWidth();
        }
        return Math.max(1, map.getGridWidth() * ArenaConstants.SQUARE_SIZE);
    }

    private int getArenaDepthBlocks() {
        MapDefinition map = mapRegistry.getActiveMap();
        if (map == null) {
            return ArenaConstants.totalArenaDepth();
        }
        return Math.max(1, map.getGridHeight() * ArenaConstants.SQUARE_SIZE);
    }

    private MapDefinition requireActiveMap() {
        MapDefinition map = mapRegistry.getActiveMap();
        if (map == null) {
            throw new IllegalStateException("No active map selected in MapRegistry.");
        }
        return map;
    }

    private void ensureArenaGenerated(World world) {
        if (arenaGenerated) {
            runPendingAfterGeneration();
            return;
        }

        Path marker = world.getSavePath().resolve(arenaMarkerFile);
        if (Files.exists(marker)) {
            arenaGenerated = true;
            LOGGER.at(Level.INFO).log("Arena already generated (marker found at '%s').", marker);
            runPendingAfterGeneration();
            return;
        }

        generateArena(world);
        arenaGenerated = true;
        writeArenaMarker(marker);
        runPendingAfterGeneration();
    }

    private void logLayout() {
        for (Enclave enclave : enclaves) {
            int column = enclave.getIndex() % ArenaConstants.ENCLAVES_PER_ROW;
            int row = enclave.getIndex() / ArenaConstants.ENCLAVES_PER_ROW;
            LOGGER.at(Level.INFO).log("[%s,%s] %s inner=(%s,%s) center=(%s,%s)",
                    column, row,
                    enclave.getColor().getDisplayName(),
                    enclave.getWorldStartX(), enclave.getWorldStartZ(),
                    enclave.getCentreWorldX(), enclave.getCentreWorldZ());
        }
    }

    private void placeBlock(World world, int x, int y, int z, String blockTypeName) {
        BlockType blockType = getBlockType(blockTypeName);
        if (blockType == null) {
            return;
        }

        int id = BlockType.getAssetMap().getIndex(blockTypeName);
        if (id < 0) {
            LOGGER.at(Level.WARNING).log("Block type '%s' has invalid index: %s", blockTypeName, id);
            return;
        }

        WorldChunk chunk = world.getNonTickingChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) {
            LOGGER.at(Level.WARNING).log("Chunk not loaded at %s %s", x, z);
            return;
        }

        chunk.setBlock(x, y, z, id, blockType, 0, 0, 0);
    }

    @Nullable
    private BlockType getBlockType(String blockTypeName) {
        Map<String, BlockType> blockMap;
        try {
            blockMap = BlockType.getAssetMap().getAssetMap();
        } catch (RuntimeException ex) {
            LOGGER.at(Level.SEVERE).log("Failed to access block asset map for '%s': %s",
                    blockTypeName, ex.toString());
            return null;
        }

        if (blockMap == null || blockMap.isEmpty()) {
            LOGGER.at(Level.WARNING).log("Block asset map is empty while resolving '%s'.", blockTypeName);
            return null;
        }

        BlockType blockType = blockMap.get(blockTypeName);
        if (blockType == null && missingBlockTypeNames.add(blockTypeName)) {
            LOGGER.at(Level.WARNING).log("Unknown block type: %s", blockTypeName);
            logMatchingBlockTypeCandidates(blockMap, blockTypeName);
        }
        return blockType;
    }

    private void logMatchingBlockTypeCandidates(Map<String, BlockType> blockMap, String requestedName) {
        String requestedLower = requestedName.toLowerCase(Locale.ROOT);
        int logged = 0;
        for (String name : blockMap.keySet()) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (!lower.contains("wood") && !lower.contains("plank")
                    && !lower.contains("oak") && !lower.contains("hardwood")) {
                continue;
            }
            if (!requestedLower.contains("wood") && !lower.contains(requestedLower)) {
                continue;
            }
            LOGGER.at(Level.INFO).log("Block candidate: %s", name);
            logged++;
            if (logged >= 40) {
                break;
            }
        }

        if (logged == 0) {
            LOGGER.at(Level.INFO).log("No matching block candidates found for '%s'.", requestedName);
        }
    }
}
