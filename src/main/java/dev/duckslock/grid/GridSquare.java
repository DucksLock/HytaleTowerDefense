package dev.duckslock.grid;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A single cell in the TD arena grid.
 * <p>
 * The 6-argument constructor is the canonical one used by {@link Enclave}.
 * The 3-argument constructor is kept for backward compatibility with the
 * old GridManager and will be removed once GridManager is deleted.
 */
public class GridSquare {

    private final int gridX;
    private final int gridZ;

    /**
     * World block position of this square's south-west corner (-1 if unknown).
     */
    private final int worldX;
    private final int worldZ;
    /**
     * Which enclave owns this square (-1 = shared / path / legacy).
     */
    private final int enclaveIndex;
    private GridSquareType type;
    /**
     * UUID of the player whose tower occupies this square, or null if empty.
     */
    @Nullable
    private UUID towerOwnerUuid;

    /**
     * Full constructor — used by {@link Enclave}.
     */
    public GridSquare(int gridX, int gridZ,
                      int worldX, int worldZ,
                      GridSquareType type,
                      int enclaveIndex) {
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.worldX = worldX;
        this.worldZ = worldZ;
        this.type = type;
        this.enclaveIndex = enclaveIndex;
    }

    /**
     * Legacy 3-arg constructor for the old GridManager.
     * worldX/worldZ default to -1; enclaveIndex defaults to -1.
     *
     * @deprecated Use the 6-arg constructor. This will be removed once
     * GridManager is replaced by EnclaveManager.
     */
    @Deprecated
    public GridSquare(int gridX, int gridZ, GridSquareType type) {
        this(gridX, gridZ, -1, -1, type, -1);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public boolean isOccupied() {
        return towerOwnerUuid != null;
    }

    public boolean isBuildable() {
        return type == GridSquareType.BUILDABLE && !isOccupied();
    }

    /**
     * Returns true if the given world-X/Z position falls inside this square's
     * SQUARE_SIZE × SQUARE_SIZE block footprint.
     */
    public boolean containsWorldPos(int wx, int wz) {
        if (worldX == -1 || worldZ == -1) return false;
        int s = ArenaConstants.SQUARE_SIZE;
        return wx >= worldX && wx < worldX + s
                && wz >= worldZ && wz < worldZ + s;
    }

    // -------------------------------------------------------------------------
    // Mutators
    // -------------------------------------------------------------------------

    public void clearTower() {
        this.towerOwnerUuid = null;
    }

    public int getEnclaveIndex() {
        return enclaveIndex;
    }

    public int getGridX() {
        return gridX;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public int getGridZ() {
        return gridZ;
    }

    public int getWorldX() {
        return worldX;
    }

    public int getWorldZ() {
        return worldZ;
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