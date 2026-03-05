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

/**
 * Central manager for all eight enclaves.
 *
 * Responsibilities:
 *   1. Create the eight {@link Enclave} objects at plugin start.
 *   2. Generate the arena blocks in the Hytale world (flat platform + colored
 *      enclave areas, all adjacent to each other).
 *   3. Assign enclaves to players as they join, in order (player 1 → RED,
 *      player 2 → BLUE, …).
 *   4. Expose helpers so other systems (wave spawner, tower placement, etc.)
 *      can query the grid state.
 *
 * ── Block placement note ────────────────────────────────────────────────────
 * Block placement uses {@code BlockModule.get().setBlock(world, x, y, z, id, state)}.
 * If that method signature does not match the version of the Hytale server you
 * are running, update the single helper method {@link #placeBlock} at the bottom
 * of this class — nothing else needs changing.
 * ────────────────────────────────────────────────────────────────────────────
 */
public class EnclaveManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String WORLD_NAME = "default";

    /**
     * The eight enclaves, indexed 0 (RED) through 7 (PINK).
     */
    private final Enclave[] enclaves = new Enclave[ArenaConstants.ENCLAVE_COUNT];

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    public EnclaveManager() {
        // Build the data model for all 8 enclaves.
        for (int i = 0; i < ArenaConstants.ENCLAVE_COUNT; i++) {
            enclaves[i] = new Enclave(i, EnclaveColor.fromIndex(i));
        }
        LOGGER.at(Level.INFO).log("Initialised %s enclaves.", ArenaConstants.ENCLAVE_COUNT);
        logLayout();

        // Queue arena generation on the world thread.
        World world = Universe.get().getWorld(WORLD_NAME);
        if (world != null) {
            world.execute(() -> generateArena(world));
        } else {
            LOGGER.at(Level.SEVERE).log("World '%s' not found — arena generation skipped.", WORLD_NAME);
        }
    }

    // -------------------------------------------------------------------------
    // Arena world generation
    // -------------------------------------------------------------------------

    /**
     * Builds the physical arena.  Must run on the world thread (called via
     * {@code world.execute(...)}).
     *
     * Layer overview (all at Y = ARENA_FLOOR_Y):
     *   • Stone sub-floor covering the full arena footprint (path/gap strips).
     *   • Per-enclave: marble border → buildable wooden floor → chalk base row.
     */
    private void generateArena(World world) {
        LOGGER.at(Level.INFO).log("Generating arena at Y=%s ...", ArenaConstants.ARENA_FLOOR_Y);

        int originX = ArenaConstants.ARENA_ORIGIN_X;
        int originZ = ArenaConstants.ARENA_ORIGIN_Z;
        int floorY = ArenaConstants.ARENA_FLOOR_Y;
        int width = ArenaConstants.totalArenaWidth();
        int depth = ArenaConstants.totalArenaDepth();

        // 1. Stone sub-floor — covers the entire arena rectangle.
        for (int x = originX; x < originX + width; x++) {
            for (int z = originZ; z < originZ + depth; z++) {
                placeBlock(world, x, floorY, z, ArenaConstants.BLOCK_PATH);
            }
        }

        // 2. Enclave areas (borders + inner squares), drawn on top.
        for (Enclave enc : enclaves) {
            generateEnclaveBlocks(world, enc, floorY);
        }

        LOGGER.at(Level.INFO).log("Arena generation complete — %s × %s blocks.", width, depth);
    }

    /**
     * Places the border wall and inner floor for one enclave.
     */
    private void generateEnclaveBlocks(World world, Enclave enc, int floorY) {
        int col = enc.getIndex() % ArenaConstants.ENCLAVES_PER_ROW;
        int row = enc.getIndex() / ArenaConstants.ENCLAVES_PER_ROW;

        int borderX = ArenaConstants.enclaveWorldStartX(col);
        int borderZ = ArenaConstants.enclaveWorldStartZ(row);
        int borderSize = ArenaConstants.ENCLAVE_WORLD_SIZE;

        // Border walls (1-block thick ring around the enclave).
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

        // Inner grid squares — use the Enclave's own GridSquare data.
        for (GridSquare sq : enc.getAllSquares()) {
            String blockName = sq.getType() == GridSquareType.BASE
                    ? ArenaConstants.BLOCK_BASE
                    : ArenaConstants.BLOCK_BUILDABLE;

            // Each grid square is SQUARE_SIZE × SQUARE_SIZE blocks.
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

    /**
     * Called by the plugin when a {@code PlayerReadyEvent} fires.
     *
     * If the player already owns an enclave (reconnect), they are reminded of
     * it.  Otherwise, the next free enclave (lowest index first) is assigned.
     */
    public void assignEnclaveToPlayer(Player player) {
        UUID uuid = player.getUuid();
        String name = player.getDisplayName();

        // Already has an enclave? Just remind them.
        for (Enclave enc : enclaves) {
            if (uuid.equals(enc.getOwnerUuid())) {
                player.sendMessage(Message.raw(
                        "[TD] Welcome back " + name + "! "
                                + "You are " + enc.getColor().getDisplayName()
                                + " (enclave " + enc.getIndex() + ")."
                ));
                LOGGER.at(Level.INFO).log("%s reconnected to enclave %s (%s).",
                        name, enc.getIndex(), enc.getColor().getDisplayName());
                return;
            }
        }

        // Assign next free enclave.
        Enclave next = getNextFreeEnclave();
        if (next == null) {
            player.sendMessage(Message.raw(
                    "[TD] The server is full (all 8 enclaves taken). Please wait."
            ));
            LOGGER.at(Level.WARNING).log("Could not assign enclave to %s: all full.", name);
            return;
        }

        next.assignOwner(uuid, name);
        player.sendMessage(Message.raw(
                "[TD] Welcome " + name + "! "
                        + "Your colour is " + next.getColor().getDisplayName()
                        + " — enclave " + next.getIndex() + "."
        ));
        LOGGER.at(Level.INFO).log("Assigned %s to enclave %s (%s).",
                name, next.getIndex(), next.getColor().getDisplayName());
    }

    /**
     * Releases a player's enclave when they disconnect.
     */
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
    // Queries used by other systems
    // -------------------------------------------------------------------------

    /** Returns the enclave owned by the given player, or null. */
    @Nullable
    public Enclave getEnclaveForPlayer(UUID playerUuid) {
        for (Enclave enc : enclaves) {
            if (playerUuid.equals(enc.getOwnerUuid())) return enc;
        }
        return null;
    }

    /**
     * Returns the enclave (and its square) that contains the given world XZ
     * position, or null if the position is not inside any enclave's inner area.
     */
    @Nullable
    public GridSquare getSquareAtWorldPos(int worldX, int worldZ) {
        for (Enclave enc : enclaves) {
            GridSquare sq = enc.getSquareAtWorldPos(worldX, worldZ);
            if (sq != null) return sq;
        }
        return null;
    }

    /**
     * Direct access to all eight enclaves (read-only usage preferred).
     */
    public Enclave[] getEnclaves() {
        return enclaves;
    }

    /**
     * Returns enclave by 0-based index.
     */
    public Enclave getEnclave(int index) {
        return enclaves[index]; }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    @Nullable
    private Enclave getNextFreeEnclave() {
        for (Enclave enc : enclaves) {
            if (!enc.isOwned()) return enc;
        }
        return null;
    }

    private void logLayout() {
        LOGGER.at(Level.INFO).log("Arena layout (col x row -> world origin):");
        for (Enclave enc : enclaves) {
            int col = enc.getIndex() % ArenaConstants.ENCLAVES_PER_ROW;
            int row = enc.getIndex() / ArenaConstants.ENCLAVES_PER_ROW;
            LOGGER.at(Level.INFO).log("  [%s,%s] %s  inner start=(%s, %s)  centre=(%s, %s)",
                    col, row,
                    enc.getColor().getDisplayName(),
                    enc.getWorldStartX(), enc.getWorldStartZ(),
                    enc.getCentreWorldX(), enc.getCentreWorldZ());
        }
    }

    // -------------------------------------------------------------------------
    // Block placement — update this one method if the API differs
    // -------------------------------------------------------------------------

    /**
     * Sets a single block in the world.
     *
     * MUST be called on the world thread (inside world.execute(...)).
     *
     * If this causes a compile error, check the BlockModule API for your
     * Hytale server version and adjust the body.  The rest of the codebase
     * only calls this method, so nothing else needs changing.
     */
    private void placeBlock(World world, int x, int y, int z, String blockTypeName) {
        // look up BlockType by name, get chunk, set block
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