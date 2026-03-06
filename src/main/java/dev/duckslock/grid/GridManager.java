package dev.duckslock.grid;

import com.hypixel.hytale.math.vector.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GridManager {

    private final Map<Long, GridSquare> grid = new HashMap<>();
    private MapDefinition mapDefinition;

    public GridManager(MapDefinition mapDefinition) {
        loadMap(mapDefinition);
    }

    public final void loadMap(MapDefinition map) {
        this.mapDefinition = Objects.requireNonNull(map, "map");
        grid.clear();
        for (GridSquareData data : map.getSquares()) {
            GridSquareType type = data.getType() == null ? GridSquareType.BLOCKED : data.getType();
            int worldX = ArenaConstants.ARENA_ORIGIN_X + data.getGridX() * ArenaConstants.SQUARE_SIZE;
            int worldZ = ArenaConstants.ARENA_ORIGIN_Z + data.getGridZ() * ArenaConstants.SQUARE_SIZE;
            grid.put(
                    key(data.getGridX(), data.getGridZ()),
                    new GridSquare(data.getGridX(), data.getGridZ(), worldX, worldZ, type, -1)
            );
        }
    }

    public GridSquare getSquareAt(Vector3f worldPos) {
        return getSquareAtWorldPos(worldPos.x, worldPos.z);
    }

    public GridSquare getSquareAtWorldPos(float worldX, float worldZ) {
        GridCoordinate coordinate = worldToGrid(worldX, worldZ);
        return getSquare(coordinate.gridX(), coordinate.gridZ());
    }

    public GridSquare getSquare(int gx, int gz) {
        return grid.get(key(gx, gz));
    }

    public GridCoordinate worldToGrid(float worldX, float worldZ) {
        int gx = worldToGridX(worldX);
        int gz = worldToGridZ(worldZ);
        return new GridCoordinate(gx, gz);
    }

    public int worldToGridX(float worldX) {
        return (int) Math.floor((worldX - ArenaConstants.ARENA_ORIGIN_X) / ArenaConstants.SQUARE_SIZE);
    }

    public int worldToGridZ(float worldZ) {
        return (int) Math.floor((worldZ - ArenaConstants.ARENA_ORIGIN_Z) / ArenaConstants.SQUARE_SIZE);
    }

    public Vector3f gridToWorld(int gx, int gz) {
        float squareSize = ArenaConstants.SQUARE_SIZE;
        float wx = ArenaConstants.ARENA_ORIGIN_X + (gx * squareSize) + (squareSize / 2.0f);
        float wz = ArenaConstants.ARENA_ORIGIN_Z + (gz * squareSize) + (squareSize / 2.0f);
        return new Vector3f(wx, 0, wz);
    }

    public MapDefinition getMapDefinition() {
        return mapDefinition;
    }

    private long key(int gx, int gz) {
        return ((long) gx << 32) ^ (gz & 0xffffffffL);
    }

    public record GridCoordinate(int gridX, int gridZ) {
    }
}
