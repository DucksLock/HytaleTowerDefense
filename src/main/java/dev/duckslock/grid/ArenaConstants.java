package dev.duckslock.grid;

import dev.duckslock.config.TDConfig;

/**
 * Shared sizing and block constants for the arena layout.
 */
public final class ArenaConstants {

    public static int SQUARE_SIZE = 2;
    public static int ENCLAVE_GRID_SIZE = 10;
    public static int BORDER_THICKNESS = 1;
    public static int ENCLAVE_GAP = 6;

    public static int ENCLAVES_PER_ROW = 4;
    public static int ENCLAVE_ROWS = 2;
    public static int ENCLAVE_COUNT = ENCLAVES_PER_ROW * ENCLAVE_ROWS;

    public static int ENCLAVE_WORLD_SIZE =
            ENCLAVE_GRID_SIZE * SQUARE_SIZE + BORDER_THICKNESS * 2;

    public static int ARENA_FLOOR_Y = 200;
    public static int ARENA_ORIGIN_X = 0;
    public static int ARENA_ORIGIN_Z = 0;

    public static String BLOCK_PATH = "Rock_Stone";
    public static String BLOCK_BUILDABLE = "Wood_Hardwood_Planks";
    public static String BLOCK_BORDER = "Rock_Marble";
    public static String BLOCK_BASE = "Rock_Chalk";

    private ArenaConstants() {
    }

    public static void apply(TDConfig.ArenaConfig config) {
        if (config == null) {
            return;
        }

        SQUARE_SIZE = config.squareSize;
        ENCLAVE_GRID_SIZE = config.enclaveGridSize;
        BORDER_THICKNESS = config.borderThickness;
        ENCLAVE_GAP = config.enclaveGap;
        ENCLAVES_PER_ROW = config.enclavesPerRow;
        ENCLAVE_ROWS = config.enclaveRows;
        ARENA_FLOOR_Y = config.arenaFloorY;
        ARENA_ORIGIN_X = config.arenaOriginX;
        ARENA_ORIGIN_Z = config.arenaOriginZ;
        BLOCK_PATH = config.blockPath;
        BLOCK_BUILDABLE = config.blockBuildable;
        BLOCK_BORDER = config.blockBorder;
        BLOCK_BASE = config.blockBase;
        recomputeDerived();
    }

    private static void recomputeDerived() {
        ENCLAVE_COUNT = ENCLAVES_PER_ROW * ENCLAVE_ROWS;
        ENCLAVE_WORLD_SIZE = ENCLAVE_GRID_SIZE * SQUARE_SIZE + BORDER_THICKNESS * 2;
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
