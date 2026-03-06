package dev.duckslock.wc3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class WC3ReferenceData {

    private static final Gson GSON = new GsonBuilder().create();

    private String source = "";
    private int waveCount = 0;
    private List<WaveRecord> waves = new ArrayList<>();
    private List<TowerRecord> towers = new ArrayList<>();

    public static WC3ReferenceData loadFromResource(String resourcePath) throws IOException {
        try (InputStream stream = WC3ReferenceData.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IOException("WC3 reference resource not found: " + resourcePath);
            }

            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                WC3ReferenceData parsed = GSON.fromJson(reader, WC3ReferenceData.class);
                if (parsed == null) {
                    throw new IOException("WC3 reference resource parsed to null: " + resourcePath);
                }
                parsed.sanitize();
                return parsed;
            }
        }
    }

    private void sanitize() {
        if (source == null) {
            source = "";
        }
        if (waves == null) {
            waves = new ArrayList<>();
        } else {
            waves = new ArrayList<>(waves);
        }
        if (towers == null) {
            towers = new ArrayList<>();
        } else {
            towers = new ArrayList<>(towers);
        }

        waves.removeIf(w -> w == null || w.wave <= 0);
        waves.sort(Comparator.comparingInt(w -> w.wave));
        waveCount = Math.max(waveCount, waves.size());
        towers.removeIf(t -> t == null || t.unitId == null || t.unitId.isBlank());
    }

    public String getSource() {
        return source;
    }

    public int getWaveCount() {
        return waveCount;
    }

    public List<WaveRecord> getWaves() {
        return Collections.unmodifiableList(waves);
    }

    public List<TowerRecord> getTowers() {
        return Collections.unmodifiableList(towers);
    }

    public static final class WaveRecord {
        private int wave;
        private String unitId;
        private String unitName;
        private Double baseHp;
        private Double baseMoveSpeed;
        private Integer bounty;
        private Double attackRange;
        private Double attackCooldown;

        public int getWave() {
            return wave;
        }

        public String getUnitId() {
            return unitId;
        }

        public String getUnitName() {
            return unitName;
        }

        public Double getBaseHp() {
            return baseHp;
        }

        public Double getBaseMoveSpeed() {
            return baseMoveSpeed;
        }

        public Integer getBounty() {
            return bounty;
        }

        public Double getAttackRange() {
            return attackRange;
        }

        public Double getAttackCooldown() {
            return attackCooldown;
        }
    }

    public static final class TowerRecord {
        private String unitId;
        private String name;
        private String missileArt;
        private Double missileSpeed;
        private Double missileArc;
        private String requires;
        private String requiresAmount;

        public String getUnitId() {
            return unitId;
        }

        public String getName() {
            return name;
        }

        public String getMissileArt() {
            return missileArt;
        }

        public Double getMissileSpeed() {
            return missileSpeed;
        }

        public Double getMissileArc() {
            return missileArc;
        }

        public String getRequires() {
            return requires;
        }

        public String getRequiresAmount() {
            return requiresAmount;
        }
    }
}
