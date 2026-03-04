package dev.duckslock.grid;

/**
 * Simple data container for a grid square definition.
 */
public class GridSquareData {
    public final int gridX;
    public final int gridZ;
    public final GridSquareType type;

    public GridSquareData(int gridX, int gridZ, GridSquareType type) {
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.type = type;
    }
}
