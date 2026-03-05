package dev.duckslock.enclave;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import dev.duckslock.grid.ArenaConstants;
import dev.duckslock.grid.GridSquare;
import dev.duckslock.grid.GridSquareType;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;

public class EnclaveManager {

    public static final String WORLD_NAME = "default";
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Enclave[] enclaves = new Enclave[ArenaConstants.ENCLAVE_COUNT];

    public EnclaveManager() {
        for (int i = 0; i < ArenaConstants.ENCLAVE_COUNT; i++) {
            enclaves[i] = new Enclave(i, EnclaveColor.fromIndex(i));
        }

        LOGGER.at(Level.INFO).log("Initialized %s enclaves.", ArenaConstants.ENCLAVE_COUNT);
        logLayout();

        World world = Universe.get().getWorld(WORLD_NAME);
        if (world == null) {
            LOGGER.at(Level.SEVERE).log("World '%s' not found, arena generation skipped.", WORLD_NAME);
            return;
        }

        world.execute(() -> generateArena(world));
    }

    private void generateArena(World world) {
        LOGGER.at(Level.INFO).log("Generating arena at Y=%s...", ArenaConstants.ARENA_FLOOR_Y);

        int originX = ArenaConstants.ARENA_ORIGIN_X;
        int originZ = ArenaConstants.ARENA_ORIGIN_Z;
        int floorY = ArenaConstants.ARENA_FLOOR_Y;
        int width = ArenaConstants.totalArenaWidth();
        int depth = ArenaConstants.totalArenaDepth();

        for (int x = originX; x < originX + width; x++) {
            for (int z = originZ; z < originZ + depth; z++) {
                placeBlock(world, x, floorY, z, ArenaConstants.BLOCK_PATH);
            }
        }

        for (Enclave enclave : enclaves) {
            generateEnclaveBlocks(world, enclave, floorY);
        }

        LOGGER.at(Level.INFO).log("Arena generation complete: %s x %s blocks.", width, depth);
    }

    private void generateEnclaveBlocks(World world, Enclave enclave, int floorY) {
        int column = enclave.getIndex() % ArenaConstants.ENCLAVES_PER_ROW;
        int row = enclave.getIndex() / ArenaConstants.ENCLAVES_PER_ROW;
        int borderX = ArenaConstants.enclaveWorldStartX(column);
        int borderZ = ArenaConstants.enclaveWorldStartZ(row);
        int borderSize = ArenaConstants.ENCLAVE_WORLD_SIZE;

        for (int x = borderX; x < borderX + borderSize; x++) {
            for (int z = borderZ; z < borderZ + borderSize; z++) {
                boolean onBorder = x == borderX
                        || x == borderX + borderSize - 1
                        || z == borderZ
                        || z == borderZ + borderSize - 1;

                if (onBorder) {
                    placeBlock(world, x, floorY, z, ArenaConstants.BLOCK_BORDER);
                }
            }
        }

        int squareSize = ArenaConstants.SQUARE_SIZE;
        for (GridSquare square : enclave.getAllSquares()) {
            String blockType = square.getType() == GridSquareType.BASE
                    ? ArenaConstants.BLOCK_BASE
                    : ArenaConstants.BLOCK_BUILDABLE;

            for (int dx = 0; dx < squareSize; dx++) {
                for (int dz = 0; dz < squareSize; dz++) {
                    placeBlock(world, square.getWorldX() + dx, floorY, square.getWorldZ() + dz, blockType);
                }
            }
        }
    }

    public void assignEnclaveToPlayer(Player player) {
        UUID uuid = player.getUuid();
        String name = player.getDisplayName();

        for (Enclave enclave : enclaves) {
            if (uuid.equals(enclave.getOwnerUuid())) {
                player.sendMessage(Message.raw("[TD] Welcome back " + name + "! You are "
                        + enclave.getColor().getDisplayName() + " (enclave " + enclave.getIndex() + ")."));
                LOGGER.at(Level.INFO).log("%s reconnected to enclave %s (%s).",
                        name, enclave.getIndex(), enclave.getColor().getDisplayName());
                return;
            }
        }

        Enclave next = getNextFreeEnclave();
        if (next == null) {
            player.sendMessage(Message.raw("[TD] Server full, all 8 enclaves taken."));
            LOGGER.at(Level.WARNING).log("Could not assign enclave to %s: all full.", name);
            return;
        }

        next.assignOwner(uuid, name);
        player.sendMessage(Message.raw("[TD] Welcome " + name + "! Your color is "
                + next.getColor().getDisplayName() + " (enclave " + next.getIndex() + ")."));
        LOGGER.at(Level.INFO).log("Assigned %s to enclave %s (%s).",
                name, next.getIndex(), next.getColor().getDisplayName());
    }

    public void releaseEnclave(UUID playerUuid) {
        for (Enclave enclave : enclaves) {
            if (playerUuid.equals(enclave.getOwnerUuid())) {
                LOGGER.at(Level.INFO).log("Released enclave %s from %s.",
                        enclave.getIndex(), enclave.getOwnerName());
                enclave.clearOwner();
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

    @Nullable
    private Enclave getNextFreeEnclave() {
        for (Enclave enclave : enclaves) {
            if (!enclave.isOwned()) {
                return enclave;
            }
        }
        return null;
    }

    private void logLayout() {
        for (Enclave enclave : enclaves) {
            int column = enclave.getIndex() % ArenaConstants.ENCLAVES_PER_ROW;
            int row = enclave.getIndex() / ArenaConstants.ENCLAVES_PER_ROW;
            LOGGER.at(Level.INFO).log("[%s,%s] %s inner=(%s,%s) center=(%s,%s)",
                    column,
                    row,
                    enclave.getColor().getDisplayName(),
                    enclave.getWorldStartX(),
                    enclave.getWorldStartZ(),
                    enclave.getCentreWorldX(),
                    enclave.getCentreWorldZ());
        }
    }

    // Single low-level write point for arena generation.
    private void placeBlock(World world, int x, int y, int z, String blockTypeName) {
        BlockType blockType = BlockType.getAssetMap().getAsset(blockTypeName);
        if (blockType == null) {
            LOGGER.at(Level.WARNING).log("Unknown block type: %s", blockTypeName);
            return;
        }

        int id = BlockType.getAssetMap().getIndex(blockTypeName);
        WorldChunk chunk = world.getNonTickingChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) {
            LOGGER.at(Level.WARNING).log("Chunk not loaded at %s %s", x, z);
            return;
        }

        chunk.setBlock(x, y, z, id, blockType, 0, 0, 0);
    }
}
