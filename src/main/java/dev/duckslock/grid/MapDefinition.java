package dev.duckslock.grid;

/**
 * Defines a map's grid squares and enemy waypoints.
 * In Phase 4 this will be loaded from a JSON file.
 * For Phase 1 testing, MAP_1 is hardcoded.
 */
public class MapDefinition {

    // ------------------------------------------------------------------
    // MAP 1 — The Ruins (hardcoded for Phase 1 testing)
    // S-curve path, Zone 1 aesthetic, 10 waves
    // ------------------------------------------------------------------
    public static final MapDefinition MAP_1 = new MapDefinition(
            "The Ruins",
            new GridSquareData[]{
                    // Path squares (enemies walk these)
                    new GridSquareData(6, 1, GridSquareType.PATH),
                    new GridSquareData(7, 1, GridSquareType.PATH),
                    new GridSquareData(8, 1, GridSquareType.PATH),
                    new GridSquareData(8, 2, GridSquareType.PATH),
                    new GridSquareData(8, 3, GridSquareType.PATH),
                    new GridSquareData(7, 3, GridSquareType.PATH),
                    new GridSquareData(6, 3, GridSquareType.PATH),
                    new GridSquareData(5, 3, GridSquareType.PATH),
                    new GridSquareData(4, 3, GridSquareType.PATH),
                    new GridSquareData(3, 3, GridSquareType.PATH),
                    new GridSquareData(2, 3, GridSquareType.PATH),
                    new GridSquareData(1, 3, GridSquareType.PATH),
                    new GridSquareData(1, 4, GridSquareType.PATH),
                    new GridSquareData(1, 5, GridSquareType.PATH),
                    new GridSquareData(1, 6, GridSquareType.PATH),
                    new GridSquareData(2, 6, GridSquareType.PATH),
                    new GridSquareData(3, 6, GridSquareType.PATH),
                    new GridSquareData(4, 6, GridSquareType.PATH),
                    new GridSquareData(5, 6, GridSquareType.PATH),
                    new GridSquareData(6, 6, GridSquareType.PATH),
                    new GridSquareData(7, 6, GridSquareType.PATH),
                    new GridSquareData(8, 6, GridSquareType.PATH),
                    new GridSquareData(8, 7, GridSquareType.PATH),
                    new GridSquareData(8, 8, GridSquareType.PATH),

                    // Buildable squares (player places towers here)
                    new GridSquareData(1, 1, GridSquareType.BUILDABLE),
                    new GridSquareData(2, 1, GridSquareType.BUILDABLE),
                    new GridSquareData(3, 1, GridSquareType.BUILDABLE),
                    new GridSquareData(4, 1, GridSquareType.BUILDABLE),
                    new GridSquareData(5, 1, GridSquareType.BUILDABLE),
                    new GridSquareData(1, 2, GridSquareType.BUILDABLE),
                    new GridSquareData(2, 2, GridSquareType.BUILDABLE),
                    new GridSquareData(3, 2, GridSquareType.BUILDABLE),
                    new GridSquareData(4, 2, GridSquareType.BUILDABLE),
                    new GridSquareData(5, 2, GridSquareType.BUILDABLE),
                    new GridSquareData(2, 4, GridSquareType.BUILDABLE),
                    new GridSquareData(3, 4, GridSquareType.BUILDABLE),
                    new GridSquareData(4, 4, GridSquareType.BUILDABLE),
                    new GridSquareData(5, 4, GridSquareType.BUILDABLE),
                    new GridSquareData(6, 4, GridSquareType.BUILDABLE),
                    new GridSquareData(7, 4, GridSquareType.BUILDABLE),
                    new GridSquareData(2, 5, GridSquareType.BUILDABLE),
                    new GridSquareData(3, 5, GridSquareType.BUILDABLE),
                    new GridSquareData(4, 5, GridSquareType.BUILDABLE),
                    new GridSquareData(5, 5, GridSquareType.BUILDABLE),
                    new GridSquareData(6, 5, GridSquareType.BUILDABLE),
                    new GridSquareData(7, 5, GridSquareType.BUILDABLE),
            },
            new int[][]{
                    // Waypoints: enemies follow these grid coordinates in order
                    {8, 0},  // spawn (edge)
                    {8, 3},
                    {1, 3},
                    {1, 6},
                    {8, 6},
                    {8, 9},  // base (edge)
            }
    );
    private final String name;
    private final GridSquareData[] squares;
    private final int[][] waypoints; // {gridX, gridZ} pairs

    public MapDefinition(String name, GridSquareData[] squares, int[][] waypoints) {
        this.name = name;
        this.squares = squares;
        this.waypoints = waypoints;
    }

    public String getName() {
        return name;
    }

    public GridSquareData[] getSquares() {
        return squares;
    }

    public int[][] getWaypoints() {
        return waypoints;
    }
}
