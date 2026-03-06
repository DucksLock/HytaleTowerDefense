package dev.duckslock.grid;

/**
 * JSON-backed value object for map square data.
 */
public class GridSquareData {

    private int gridX;
    private int gridZ;
    private GridSquareType type = GridSquareType.BLOCKED;

    public GridSquareData() {
    }

    public GridSquareData(int gridX, int gridZ, GridSquareType type) {
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.type = type;
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridZ() {
        return gridZ;
    }

    public GridSquareType getType() {
        return type;
    }
}
