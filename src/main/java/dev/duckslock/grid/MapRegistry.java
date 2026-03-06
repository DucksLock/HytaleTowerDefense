package dev.duckslock.grid;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime map catalog with a switchable active map.
 */
public class MapRegistry {

    private final Map<String, MapDefinition> mapsById = new ConcurrentHashMap<>();

    @Nullable
    private volatile String activeMapId;

    public void register(String mapId, MapDefinition mapDefinition) {
        if (mapId == null || mapId.isBlank()) {
            throw new IllegalArgumentException("mapId cannot be blank");
        }
        Objects.requireNonNull(mapDefinition, "mapDefinition");
        mapsById.put(normalize(mapId), mapDefinition);
    }

    public boolean setActiveMap(String mapId) {
        if (mapId == null || mapId.isBlank()) {
            return false;
        }
        String key = normalize(mapId);
        if (!mapsById.containsKey(key)) {
            return false;
        }
        activeMapId = key;
        return true;
    }

    @Nullable
    public MapDefinition getMap(String mapId) {
        if (mapId == null || mapId.isBlank()) {
            return null;
        }
        return mapsById.get(normalize(mapId));
    }

    @Nullable
    public MapDefinition getActiveMap() {
        String id = activeMapId;
        if (id == null) {
            return null;
        }
        return mapsById.get(id);
    }

    @Nullable
    public String getActiveMapId() {
        return activeMapId;
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(new TreeSet<>(mapsById.keySet()));
    }

    private String normalize(String mapId) {
        return mapId.trim().toLowerCase(Locale.ROOT);
    }
}
