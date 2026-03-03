package dev.duckslock.grid;

import com.hypixel.hytale.protocol.Vector3f;
import java.util.HashMap;
import java.util.Map;

/**
 * Owns the logical grid for the current map.
 * Maps world positions to GridSquares so click detection is instant.
 */
public class GridManager {

    private final Map<String, GridSquare> grid = new HashMap<>();

    // Each grid square is this many blocks wide/deep
    public static final int SQUARE_SIZE = 2;

    /** Loads a map's grid. Call this when a game session starts. */
    public void loadMap(MapDefinition map) {
        grid.clear();
        for (GridSquareData data : map.getSquares()) {
            grid.put(key(data.gridX, data.gridZ),
                    new GridSquare(data.gridX, data.gridZ, data.type));
        }
    }

    /** Returns the GridSquare at a world position, or null if outside the grid. */
    public GridSquare getSquareAt(Vector3f worldPos) {
        int gx = worldToGrid(worldPos.x);
        int gz = worldToGrid(worldPos.z);
        return grid.get(key(gx, gz));
    }

    /** Returns the GridSquare at explicit grid coordinates. */
    public GridSquare getSquare(int gx, int gz) {
        return grid.get(key(gx, gz));
    }

    /** Returns the world-space centre of a grid square. */
    public Vector3f gridToWorld(int gx, int gz) {
        float wx = (gx * SQUARE_SIZE) + (SQUARE_SIZE / 2.0f);
        float wz = (gz * SQUARE_SIZE) + (SQUARE_SIZE / 2.0f);
        return new Vector3f(wx, 0, wz);
    }

    private int worldToGrid(float coord) { return (int) Math.floor(coord / SQUARE_SIZE); }
    private String key(int gx, int gz)   { return gx + "," + gz; }
}