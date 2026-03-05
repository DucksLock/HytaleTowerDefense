package dev.duckslock.grid;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Represents a single grid cell in the tower defense arena.
 */
public class GridSquare {

    private final int gridX;
    private final int gridZ;
    private final int worldX;
    private final int worldZ;
    private final int enclaveIndex;

    private GridSquareType type;

    @Nullable
    private UUID towerOwnerUuid;

    public GridSquare(int gridX, int gridZ, int worldX, int worldZ, GridSquareType type, int enclaveIndex) {
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.worldX = worldX;
        this.worldZ = worldZ;
        this.type = type;
        this.enclaveIndex = enclaveIndex;
    }

    @Deprecated
    public GridSquare(int gridX, int gridZ, GridSquareType type) {
        this(gridX, gridZ, -1, -1, type, -1);
    }

    public boolean isOccupied() {
        return towerOwnerUuid != null;
    }

    public boolean isBuildable() {
        return type == GridSquareType.BUILDABLE && !isOccupied();
    }

    public boolean containsWorldPos(int wx, int wz) {
        if (worldX == -1 || worldZ == -1) {
            return false;
        }

        int size = ArenaConstants.SQUARE_SIZE;
        return wx >= worldX
                && wx < worldX + size
                && wz >= worldZ
                && wz < worldZ + size;
    }

    public void clearTower() {
        towerOwnerUuid = null;
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridZ() {
        return gridZ;
    }

    public int getWorldX() {
        return worldX;
    }

    public int getWorldZ() {
        return worldZ;
    }

    public int getEnclaveIndex() {
        return enclaveIndex;
    }

    public GridSquareType getType() {
        return type;
    }

    public void setType(GridSquareType type) {
        this.type = type;
    }

    @Nullable
    public UUID getTowerOwner() {
        return towerOwnerUuid;
    }

    public void setTowerOwner(@Nullable UUID uuid) {
        this.towerOwnerUuid = uuid;
    }

    @Override
    public String toString() {
        return "GridSquare{grid=(" + gridX + "," + gridZ + ")"
                + " world=(" + worldX + "," + worldZ + ")"
                + " type=" + type
                + " enclave=" + enclaveIndex + "}";
    }
}
