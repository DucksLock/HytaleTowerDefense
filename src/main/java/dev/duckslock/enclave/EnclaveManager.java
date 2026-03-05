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
        // build 8 enclave data objects
        for (int i = 0; i < ArenaConstants.ENCLAVE_COUNT; i++) {
            enclaves[i] = new Enclave(i, EnclaveColor.fromIndex(i));
        }
        LOGGER.at(Level.INFO).log("Initialised %s enclaves.", ArenaConstants.ENCLAVE_COUNT);
        logLayout();

        // queue arena block generation on world thread
        World world = Universe.get().getWorld(WORLD_NAME);
        if (world != null) {
            world.execute(() -> generateArena(world));
        } else {
            LOGGER.at(Level.SEVERE).log("World '%s' not found — arena generation skipped.", WORLD_NAME);
        }
    }

    // -------------------------------------------------------------------------
    // Arena generation
    // -------------------------------------------------------------------------

    // builds full arena floor + enclave borders/squares, must run on world thread
    private void generateArena(World world) {
        LOGGER.at(Level.INFO).log("Generating arena at Y=%s ...", ArenaConstants.ARENA_FLOOR_Y);

        int originX = ArenaConstants.ARENA_ORIGIN_X;
        int originZ = ArenaConstants.ARENA_ORIGIN_Z;
        int floorY = ArenaConstants.ARENA_FLOOR_Y;
        int width = ArenaConstants.totalArenaWidth();
        int depth = ArenaConstants.totalArenaDepth();

        // stone sub-floor across whole arena
        for (int x = originX; x < originX + width; x++) {
            for (int z = originZ; z < originZ + depth; z++) {
                placeBlock(world, x, floorY, z, ArenaConstants.BLOCK_PATH);
            }
        }

        // enclave borders + inner squares on top
        for (Enclave enc : enclaves) {
            generateEnclaveBlocks(world, enc, floorY);
        }

        LOGGER.at(Level.INFO).log("Arena generation complete — %s x %s blocks.", width, depth);
    }

    // place border ring and inner floor for one enclave
    private void generateEnclaveBlocks(World world, Enclave enc, int floorY) {
        int col = enc.getIndex() % ArenaConstants.ENCLAVES_PER_ROW;
        int row = enc.getIndex() / ArenaConstants.ENCLAVES_PER_ROW;
        int borderX = ArenaConstants.enclaveWorldStartX(col);
        int borderZ = ArenaConstants.enclaveWorldStartZ(row);
        int borderSize = ArenaConstants.ENCLAVE_WORLD_SIZE;

        // 1-block thick border ring
        for (int x = borderX; x < borderX + borderSize; x++) {
            for (int z = borderZ; z < borderZ + borderSize; z++) {
                boolean onBorder = x == borderX || x == borderX + borderSize - 1
                        || z == borderZ || z == borderZ + borderSize - 1;
                if (onBorder) placeBlock(world, x, floorY, z, ArenaConstants.BLOCK_BORDER);
            }
        }

        // inner squares, base row uses different block
        for (GridSquare sq : enc.getAllSquares()) {
            String blockName = sq.getType() == GridSquareType.BASE
                    ? ArenaConstants.BLOCK_BASE
                    : ArenaConstants.BLOCK_BUILDABLE;
            int s = ArenaConstants.SQUARE_SIZE;
            for (int dx = 0; dx < s; dx++) {
                for (int dz = 0; dz < s; dz++) {
                    placeBlock(world, sq.getWorldX() + dx, floorY, sq.getWorldZ() + dz, blockName);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Player assignment
    // -------------------------------------------------------------------------

    // assign enclave on join, remind on reconnect
    public void assignEnclaveToPlayer(Player player) {
        UUID uuid = player.getUuid();
        String name = player.getDisplayName();

        for (Enclave enc : enclaves) {
            if (uuid.equals(enc.getOwnerUuid())) {
                player.sendMessage(Message.raw("[TD] Welcome back " + name + "! You are "
                        + enc.getColor().getDisplayName() + " (enclave " + enc.getIndex() + ")."));
                LOGGER.at(Level.INFO).log("%s reconnected to enclave %s (%s).",
                        name, enc.getIndex(), enc.getColor().getDisplayName());
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
        player.sendMessage(Message.raw("[TD] Welcome " + name + "! Your colour is "
                + next.getColor().getDisplayName() + " — enclave " + next.getIndex() + "."));
        LOGGER.at(Level.INFO).log("Assigned %s to enclave %s (%s).",
                name, next.getIndex(), next.getColor().getDisplayName());
    }

    // free enclave slot when player leaves
    public void releaseEnclave(UUID playerUuid) {
        for (Enclave enc : enclaves) {
            if (playerUuid.equals(enc.getOwnerUuid())) {
                LOGGER.at(Level.INFO).log("Released enclave %s from %s.",
                        enc.getIndex(), enc.getOwnerName());
                enc.clearOwner();
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Nullable
    public Enclave getEnclaveForPlayer(UUID playerUuid) {
        for (Enclave enc : enclaves) {
            if (playerUuid.equals(enc.getOwnerUuid())) return enc;
        }
        return null;
    }

    @Nullable
    public GridSquare getSquareAtWorldPos(int worldX, int worldZ) {
        for (Enclave enc : enclaves) {
            GridSquare sq = enc.getSquareAtWorldPos(worldX, worldZ);
            if (sq != null) return sq;
        }
        return null;
    }

    public Enclave[] getEnclaves() {
        return enclaves;
    }

    public Enclave getEnclave(int index) {
        return enclaves[index];
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @Nullable
    private Enclave getNextFreeEnclave() {
        for (Enclave enc : enclaves) {
            if (!enc.isOwned()) return enc;
        }
        return null;
    }

    private void logLayout() {
        for (Enclave enc : enclaves) {
            int col = enc.getIndex() % ArenaConstants.ENCLAVES_PER_ROW;
            int row = enc.getIndex() / ArenaConstants.ENCLAVES_PER_ROW;
            LOGGER.at(Level.INFO).log("[%s,%s] %s inner=(%s,%s) centre=(%s,%s)",
                    col, row, enc.getColor().getDisplayName(),
                    enc.getWorldStartX(), enc.getWorldStartZ(),
                    enc.getCentreWorldX(), enc.getCentreWorldZ());
        }
    }

    // place single block — look up type, get chunk, set block
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