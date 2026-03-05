package dev.duckslock.grid;

import com.hypixel.hytale.math.vector.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class GridManager {

    private final Map<Long, GridSquare> grid = new HashMap<>();

    public void loadMap(MapDefinition map) {
        grid.clear();
        for (GridSquareData data : map.getSquares()) {
            grid.put(key(data.gridX, data.gridZ), new GridSquare(data.gridX, data.gridZ, data.type));
        }
    }

    public GridSquare getSquareAt(Vector3f worldPos) {
        int gx = worldToGrid(worldPos.x);
        int gz = worldToGrid(worldPos.z);
        return grid.get(key(gx, gz));
    }

    public GridSquare getSquare(int gx, int gz) {
        return grid.get(key(gx, gz));
    }

    public Vector3f gridToWorld(int gx, int gz) {
        float squareSize = ArenaConstants.SQUARE_SIZE;
        float wx = (gx * squareSize) + (squareSize / 2.0f);
        float wz = (gz * squareSize) + (squareSize / 2.0f);
        return new Vector3f(wx, 0, wz);
    }

    private int worldToGrid(float coordinate) {
        return (int) Math.floor(coordinate / ArenaConstants.SQUARE_SIZE);
    }

    private long key(int gx, int gz) {
        return ((long) gx << 32) ^ (gz & 0xffffffffL);
    }
}
