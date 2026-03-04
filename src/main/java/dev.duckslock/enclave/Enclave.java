package dev.duckslock.enclave;

import dev.duckslock.grid.ArenaConstants;
import dev.duckslock.grid.GridSquare;
import dev.duckslock.grid.GridSquareType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One player's enclave on the TD arena.
 * <p>
 * An enclave owns a ENCLAVE_GRID_SIZE × ENCLAVE_GRID_SIZE grid of squares.
 * The back row (highest Z within the enclave) is typed BASE; all others start
 * as BUILDABLE.  When enemies reach a BASE square the owner loses lives.
 * <p>
 * World coordinate layout (inside the border):
 * <p>
 * (worldStartX, worldStartZ)
 * +-------- X -------->
 * |  [0,0] [1,0] ...
 * Z  [0,1] ...
 * |  ...
 * v  [0,9] [1,9] ... [9,9]  ← BASE row
 */
public class Enclave {

    private final int index;
    private final EnclaveColor color;

    /**
     * World block X of the first (west) inner square column.
     */
    private final int worldStartX;
    /**
     * World block Z of the first (north) inner square row.
     */
    private final int worldStartZ;

    /**
     * grid[x][z] covering the inner ENCLAVE_GRID_SIZE × ENCLAVE_GRID_SIZE area.
     */
    private final GridSquare[][] grid;

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private String ownerName;

    // -------------------------------------------------------------------------

    public Enclave(int index, EnclaveColor color) {
        this.index = index;
        this.color = color;

        int col = index % ArenaConstants.ENCLAVES_PER_ROW;
        int row = index / ArenaConstants.ENCLAVES_PER_ROW;

        // The inner area starts one border-thickness inside the enclave border block.
        this.worldStartX = ArenaConstants.enclaveWorldStartX(col) + ArenaConstants.BORDER_THICKNESS;
        this.worldStartZ = ArenaConstants.enclaveWorldStartZ(row) + ArenaConstants.BORDER_THICKNESS;

        int size = ArenaConstants.ENCLAVE_GRID_SIZE;
        grid = new GridSquare[size][size];

        for (int gx = 0; gx < size; gx++) {
            for (int gz = 0; gz < size; gz++) {
                int wx = worldStartX + gx * ArenaConstants.SQUARE_SIZE;
                int wz = worldStartZ + gz * ArenaConstants.SQUARE_SIZE;

                // Last Z row is the base line.
                GridSquareType type = (gz == size - 1)
                        ? GridSquareType.BASE
                        : GridSquareType.BUILDABLE;

                grid[gx][gz] = new GridSquare(gx, gz, wx, wz, type, index);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Ownership
    // -------------------------------------------------------------------------

    public boolean isOwned() {
        return ownerUuid != null;
    }

    public void assignOwner(UUID uuid, String name) {
        this.ownerUuid = uuid;
        this.ownerName = name;
    }

    public void clearOwner() {
        this.ownerUuid = null;
        this.ownerName = null;
    }

    // -------------------------------------------------------------------------
    // Grid helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the square at grid-local (gx, gz), or null if out of bounds.
     */
    @Nullable
    public GridSquare getSquare(int gx, int gz) {
        if (gx < 0 || gz < 0 || gx >= grid.length || gz >= grid[0].length) return null;
        return grid[gx][gz];
    }

    /**
     * Returns the square whose world footprint contains (wx, wz), or null if
     * the position is outside this enclave's inner area.
     */
    @Nullable
    public GridSquare getSquareAtWorldPos(int wx, int wz) {
        int relX = wx - worldStartX;
        int relZ = wz - worldStartZ;
        if (relX < 0 || relZ < 0) return null;
        int gx = relX / ArenaConstants.SQUARE_SIZE;
        int gz = relZ / ArenaConstants.SQUARE_SIZE;
        return getSquare(gx, gz);
    }

    /**
     * All squares in this enclave (all types).
     */
    public List<GridSquare> getAllSquares() {
        List<GridSquare> result = new ArrayList<>();
        for (GridSquare[] col : grid) for (GridSquare sq : col) result.add(sq);
        return result;
    }

    /**
     * All BUILDABLE squares that have no tower yet.
     */
    public List<GridSquare> getFreeBuildableSquares() {
        List<GridSquare> result = new ArrayList<>();
        for (GridSquare[] col : grid) {
            for (GridSquare sq : col) {
                if (sq.isBuildable()) result.add(sq);
            }
        }
        return result;
    }

    /**
     * Centre world X of the enclave's inner area (good for teleport / HUD).
     */
    public int getCentreWorldX() {
        return worldStartX + (ArenaConstants.ENCLAVE_GRID_SIZE * ArenaConstants.SQUARE_SIZE) / 2;
    }

    /**
     * Centre world Z of the enclave's inner area.
     */
    public int getCentreWorldZ() {
        return worldStartZ + (ArenaConstants.ENCLAVE_GRID_SIZE * ArenaConstants.SQUARE_SIZE) / 2;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public int getIndex() {
        return index;
    }

    public EnclaveColor getColor() {
        return color;
    }

    public int getWorldStartX() {
        return worldStartX;
    }

    public int getWorldStartZ() {
        return worldStartZ;
    }

    public GridSquare[][] getGrid() {
        return grid;
    }

    @Nullable
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    @Nullable
    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public String toString() {
        return "Enclave{index=" + index + " color=" + color.getDisplayName()
                + " owner=" + (ownerName != null ? ownerName : "none") + "}";
    }
}
