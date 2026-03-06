package dev.duckslock.grid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapDefinition {

    private static final Gson GSON = new GsonBuilder().create();

    private String name = "Unnamed";
    private int gridWidth;
    private int gridHeight;
    private List<GridSquareData> squares = new ArrayList<>();
    private List<GridWaypoint> waypoints = new ArrayList<>();

    public static MapDefinition loadFromResource(String resourcePath) throws IOException {
        try (InputStream stream = MapDefinition.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IOException("Map resource not found: " + resourcePath);
            }

            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                MapDefinition parsed = GSON.fromJson(reader, MapDefinition.class);
                if (parsed == null) {
                    throw new IOException("Map resource parsed to null: " + resourcePath);
                }
                parsed.sanitize();
                return parsed;
            }
        }
    }

    private void sanitize() {
        if (name == null || name.isBlank()) {
            name = "Unnamed";
        }
        gridWidth = Math.max(1, gridWidth);
        gridHeight = Math.max(1, gridHeight);

        if (squares == null) {
            squares = new ArrayList<>();
        } else {
            squares = new ArrayList<>(squares);
        }

        if (waypoints == null) {
            waypoints = new ArrayList<>();
        } else {
            waypoints = new ArrayList<>(waypoints);
        }
    }

    public String getName() {
        return name;
    }

    public int getGridWidth() {
        return gridWidth;
    }

    public int getGridHeight() {
        return gridHeight;
    }

    public List<GridSquareData> getSquares() {
        return Collections.unmodifiableList(squares);
    }

    public List<GridWaypoint> getWaypoints() {
        return Collections.unmodifiableList(waypoints);
    }

    public boolean hasWaypoints() {
        return !waypoints.isEmpty();
    }

    public static final class GridWaypoint {
        private int gridX;
        private int gridZ;

        public GridWaypoint() {
        }

        public GridWaypoint(int gridX, int gridZ) {
            this.gridX = gridX;
            this.gridZ = gridZ;
        }

        public int getGridX() {
            return gridX;
        }

        public int getGridZ() {
            return gridZ;
        }
    }
}
