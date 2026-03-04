package dev.duckslock.grid;

/**
 * Single source of truth for every sizing decision in the TD arena.
 * <p>
 * Layout (top-down, Z increases downward on screen):
 * <p>
 * col  0       1       2       3
 * row0 [RED]   [BLUE]  [TEAL]  [PURPLE]
 * (gap)
 * row1 [YELL]  [ORAN]  [GREEN] [PINK]
 * <p>
 * Between each enclave there is a ENCLAVE_GAP-wide strip of PATH blocks that
 * will eventually carry the enemy route.
 */
public final class ArenaConstants {

    /**
     * World blocks per side of one grid square (each square is N×N blocks).
     */
    public static final int SQUARE_SIZE = 2;

    // -------------------------------------------------------------------------
    // Sizing
    // -------------------------------------------------------------------------
    /**
     * Buildable grid squares wide AND deep per enclave inner area.
     */
    public static final int ENCLAVE_GRID_SIZE = 10;
    /**
     * Block-thick solid border wall around each enclave's inner area.
     */
    public static final int BORDER_THICKNESS = 1;
    /**
     * Block-wide gap (path strip) between adjacent enclaves.
     */
    public static final int ENCLAVE_GAP = 6;
    /**
     * Enclaves arranged in this many columns.
     */
    public static final int ENCLAVES_PER_ROW = 4;
    /**
     * Number of enclave rows.
     */
    public static final int ENCLAVE_ROWS = 2;
    /**
     * Total enclaves = ENCLAVES_PER_ROW × ENCLAVE_ROWS.
     */
    public static final int ENCLAVE_COUNT = ENCLAVES_PER_ROW * ENCLAVE_ROWS; // 8
    /**
     * Total world-block footprint of one enclave (inner blocks + border both sides).
     * = ENCLAVE_GRID_SIZE * SQUARE_SIZE + BORDER_THICKNESS * 2
     * = 10 * 2 + 2 = 22
     */
    public static final int ENCLAVE_WORLD_SIZE =
            ENCLAVE_GRID_SIZE * SQUARE_SIZE + BORDER_THICKNESS * 2;
    /**
     * Y level the arena floor sits at (flat stone platform).
     */
    public static final int ARENA_FLOOR_Y = 64;

    // -------------------------------------------------------------------------
    // World position
    // -------------------------------------------------------------------------
    /**
     * World X of the arena's north-west corner (enclave 0 border start).
     */
    public static final int ARENA_ORIGIN_X = 0;
    /**
     * World Z of the arena's north-west corner.
     */
    public static final int ARENA_ORIGIN_Z = 0;
    /**
     * The sub-floor and gap/path strips between enclaves.
     */
    public static final String BLOCK_PATH = "Rock_Stone";

    // -------------------------------------------------------------------------
    // Block type names  (used with TempAssetIdUtil.getBlockTypeIndex)
    // -------------------------------------------------------------------------
    /**
     * Buildable squares inside an enclave.
     */
    public static final String BLOCK_BUILDABLE = "Wood_Planks_Oak";
    /**
     * The 1-block border wall around each enclave.
     */
    public static final String BLOCK_BORDER = "Rock_Marble";
    /**
     * The back row of each enclave (base line — enemies breach here).
     */
    public static final String BLOCK_BASE = "Rock_Chalk";

    private ArenaConstants() {
    }

    // -------------------------------------------------------------------------
    // Coordinate helpers
    // -------------------------------------------------------------------------

    /**
     * World X of the border's west edge for the enclave in the given column.
     * The inner buildable area starts BORDER_THICKNESS blocks further east.
     */
    public static int enclaveWorldStartX(int col) {
        return ARENA_ORIGIN_X + col * (ENCLAVE_WORLD_SIZE + ENCLAVE_GAP);
    }

    /**
     * World Z of the border's north edge for the enclave in the given row.
     */
    public static int enclaveWorldStartZ(int row) {
        return ARENA_ORIGIN_Z + row * (ENCLAVE_WORLD_SIZE + ENCLAVE_GAP);
    }

    /**
     * Full width of the entire arena in world blocks.
     * Useful for clearing/resetting the arena area.
     */
    public static int totalArenaWidth() {
        return ENCLAVES_PER_ROW * ENCLAVE_WORLD_SIZE
                + (ENCLAVES_PER_ROW - 1) * ENCLAVE_GAP;
    }

    /**
     * Full depth of the entire arena in world blocks.
     */
    public static int totalArenaDepth() {
        return ENCLAVE_ROWS * ENCLAVE_WORLD_SIZE
                + (ENCLAVE_ROWS - 1) * ENCLAVE_GAP;
    }
}
