package dev.duckslock.grid;

/**
 * Represents a single cell in the grid.
 * Tracks its type and what tower (if any) is placed on it.
 */
public class GridSquare {

    private final int gridX;
    private final int gridZ;
    private final GridSquareType type;

    // Null if no tower is placed here.
    // Typed as Object for now — will be Tower once that class exists.
    private Object placedTower;

    public GridSquare(int gridX, int gridZ, GridSquareType type) {
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.type  = type;
    }

    public boolean isOccupied()         { return placedTower != null; }
    public void setTower(Object tower)  { this.placedTower = tower; }
    public void clearTower()            { this.placedTower = null; }

    public int getGridX()               { return gridX; }
    public int getGridZ()               { return gridZ; }
    public GridSquareType getType()     { return type; }
    public Object getPlacedTower()      { return placedTower; }
}