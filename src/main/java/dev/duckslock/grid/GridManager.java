package dev.duckslock.grid;

import com.hypixel.hytale.math.vector.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class GridManager {

    public static final int SQUARE_SIZE = 2;
    private final Map<String, GridSquare> grid = new HashMap<>();

    // load map squares into lookup table
    public void loadMap(MapDefinition map) {
        grid.clear();
        for (GridSquareData data : map.getSquares()) {
            grid.put(key(data.gridX, data.gridZ),
                    new GridSquare(data.gridX, data.gridZ, data.type));
        }
    }

    // find square at world position, null if outside grid
    public GridSquare getSquareAt(Vector3f worldPos) {
        int gx = worldToGrid(worldPos.x);
        int gz = worldToGrid(worldPos.z);
        return grid.get(key(gx, gz));
    }

    // find square by grid coords
    public GridSquare getSquare(int gx, int gz) {
        return grid.get(key(gx, gz));
    }

    // grid coords to world centre of that square
    public Vector3f gridToWorld(int gx, int gz) {
        float wx = (gx * SQUARE_SIZE) + (SQUARE_SIZE / 2.0f);
        float wz = (gz * SQUARE_SIZE) + (SQUARE_SIZE / 2.0f);
        return new Vector3f(wx, 0, wz);
    }

    private int worldToGrid(float coord) {
        return (int) Math.floor(coord / SQUARE_SIZE);
    }

    private String key(int gx, int gz) {
        return gx + "," + gz;
    }
}