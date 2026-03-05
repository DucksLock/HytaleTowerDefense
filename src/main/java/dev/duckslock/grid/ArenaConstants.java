package dev.duckslock.grid;

/**
 * Shared sizing and block constants for the arena layout.
 */
public final class ArenaConstants {

    public static final int SQUARE_SIZE = 2;
    public static final int ENCLAVE_GRID_SIZE = 10;
    public static final int BORDER_THICKNESS = 1;
    public static final int ENCLAVE_GAP = 6;

    public static final int ENCLAVES_PER_ROW = 4;
    public static final int ENCLAVE_ROWS = 2;
    public static final int ENCLAVE_COUNT = ENCLAVES_PER_ROW * ENCLAVE_ROWS;

    public static final int ENCLAVE_WORLD_SIZE =
            ENCLAVE_GRID_SIZE * SQUARE_SIZE + BORDER_THICKNESS * 2;

    public static final int ARENA_FLOOR_Y = 64;
    public static final int ARENA_ORIGIN_X = 0;
    public static final int ARENA_ORIGIN_Z = 0;

    public static final String BLOCK_PATH = "Rock_Stone";
    public static final String BLOCK_BUILDABLE = "Wood_Planks_Oak";
    public static final String BLOCK_BORDER = "Rock_Marble";
    public static final String BLOCK_BASE = "Rock_Chalk";

    private ArenaConstants() {
    }

    public static int enclaveWorldStartX(int column) {
        return ARENA_ORIGIN_X + column * (ENCLAVE_WORLD_SIZE + ENCLAVE_GAP);
    }

    public static int enclaveWorldStartZ(int row) {
        return ARENA_ORIGIN_Z + row * (ENCLAVE_WORLD_SIZE + ENCLAVE_GAP);
    }

    public static int totalArenaWidth() {
        return ENCLAVES_PER_ROW * ENCLAVE_WORLD_SIZE
                + (ENCLAVES_PER_ROW - 1) * ENCLAVE_GAP;
    }

    public static int totalArenaDepth() {
        return ENCLAVE_ROWS * ENCLAVE_WORLD_SIZE
                + (ENCLAVE_ROWS - 1) * ENCLAVE_GAP;
    }
}
