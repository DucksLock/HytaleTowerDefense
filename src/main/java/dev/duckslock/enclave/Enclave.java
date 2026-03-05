package dev.duckslock.enclave;

import dev.duckslock.grid.ArenaConstants;
import dev.duckslock.grid.GridSquare;
import dev.duckslock.grid.GridSquareType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runtime model for one player's enclave.
 */
public class Enclave {

    private final int index;
    private final EnclaveColor color;
    private final int worldStartX;
    private final int worldStartZ;
    private final GridSquare[][] grid;

    @Nullable
    private UUID ownerUuid;

    @Nullable
    private String ownerName;

    public Enclave(int index, EnclaveColor color) {
        this.index = index;
        this.color = color;

        int column = index % ArenaConstants.ENCLAVES_PER_ROW;
        int row = index / ArenaConstants.ENCLAVES_PER_ROW;

        this.worldStartX = ArenaConstants.enclaveWorldStartX(column) + ArenaConstants.BORDER_THICKNESS;
        this.worldStartZ = ArenaConstants.enclaveWorldStartZ(row) + ArenaConstants.BORDER_THICKNESS;

        int size = ArenaConstants.ENCLAVE_GRID_SIZE;
        this.grid = new GridSquare[size][size];

        for (int gx = 0; gx < size; gx++) {
            for (int gz = 0; gz < size; gz++) {
                int worldX = worldStartX + gx * ArenaConstants.SQUARE_SIZE;
                int worldZ = worldStartZ + gz * ArenaConstants.SQUARE_SIZE;
                GridSquareType type = (gz == size - 1) ? GridSquareType.BASE : GridSquareType.BUILDABLE;
                grid[gx][gz] = new GridSquare(gx, gz, worldX, worldZ, type, index);
            }
        }
    }

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

    @Nullable
    public GridSquare getSquare(int gx, int gz) {
        if (gx < 0 || gz < 0 || gx >= grid.length || gz >= grid[0].length) {
            return null;
        }
        return grid[gx][gz];
    }

    @Nullable
    public GridSquare getSquareAtWorldPos(int worldX, int worldZ) {
        int relativeX = worldX - worldStartX;
        int relativeZ = worldZ - worldStartZ;
        if (relativeX < 0 || relativeZ < 0) {
            return null;
        }

        int gx = relativeX / ArenaConstants.SQUARE_SIZE;
        int gz = relativeZ / ArenaConstants.SQUARE_SIZE;
        return getSquare(gx, gz);
    }

    public List<GridSquare> getAllSquares() {
        int size = ArenaConstants.ENCLAVE_GRID_SIZE;
        List<GridSquare> result = new ArrayList<>(size * size);

        for (GridSquare[] column : grid) {
            for (GridSquare square : column) {
                result.add(square);
            }
        }

        return result;
    }

    public List<GridSquare> getFreeBuildableSquares() {
        int size = ArenaConstants.ENCLAVE_GRID_SIZE;
        List<GridSquare> result = new ArrayList<>(size * size);

        for (GridSquare[] column : grid) {
            for (GridSquare square : column) {
                if (square.isBuildable()) {
                    result.add(square);
                }
            }
        }

        return result;
    }

    public int getCentreWorldX() {
        return worldStartX + (ArenaConstants.ENCLAVE_GRID_SIZE * ArenaConstants.SQUARE_SIZE) / 2;
    }

    public int getCentreWorldZ() {
        return worldStartZ + (ArenaConstants.ENCLAVE_GRID_SIZE * ArenaConstants.SQUARE_SIZE) / 2;
    }

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
