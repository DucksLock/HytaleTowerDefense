package dev.duckslock.tower;

import com.hypixel.hytale.logger.HytaleLogger;
import dev.duckslock.config.TDConfig;

import java.util.*;
import java.util.logging.Level;

public enum TowerType {

    ARROW("Outlander_Hunter", 50, 15, 1.2, 6.0, 0.0, 0L, false),
    CANNON("Trork_Mauler", 80, 40, 0.5, 5.0, 0.0, 0L, false),
    FROST("Skeleton_Soldier", 65, 8, 1.0, 5.0, 0.25, 1500L, false),
    MAGIC("Outlander_Brute", 100, 25, 0.8, 6.0, 0.0, 0L, true);

    private final TDConfig.TowerConfig defaults;

    private String assetId;
    private int cost;
    private int damage;
    private double attackSpeed;
    private double range;
    private double slowPercent;
    private long slowDurationMs;
    private boolean ignoreArmor;
    private List<UpgradeTier> pathAUpgrades = List.of();
    private List<UpgradeTier> pathBUpgrades = List.of();

    TowerType(
            String assetId,
            int cost,
            int damage,
            double attackSpeed,
            double range,
            double slowPercent,
            long slowDurationMs,
            boolean ignoreArmor
    ) {
        this.defaults = new TDConfig.TowerConfig(
                assetId,
                cost,
                damage,
                attackSpeed,
                range,
                slowPercent,
                slowDurationMs,
                ignoreArmor,
                TDConfig.defaultPathAUpgrades(),
                TDConfig.defaultPathBUpgrades()
        );
        apply(defaults);
    }

    public static void applyConfig(Map<String, TDConfig.TowerConfig> configured, HytaleLogger logger) {
        for (TowerType type : values()) {
            TDConfig.TowerConfig config = configured != null ? configured.get(type.name()) : null;
            if (config == null) {
                config = type.defaults.copy();
            } else {
                config = config.copy();
            }
            config.sanitize();
            type.apply(config);
            logger.at(Level.INFO).log(
                    "Tower '%s' configured: asset=%s cost=%s dmg=%s aps=%.3f range=%.3f",
                    type.name(),
                    type.assetId,
                    type.cost,
                    type.damage,
                    type.attackSpeed,
                    type.range
            );
        }
    }

    private void apply(TDConfig.TowerConfig config) {
        this.assetId = config.assetId;
        this.cost = config.cost;
        this.damage = config.damage;
        this.attackSpeed = config.attackSpeed;
        this.range = config.range;
        this.slowPercent = config.slowPercent;
        this.slowDurationMs = config.slowDurationMs;
        this.ignoreArmor = config.ignoreArmor;
        this.pathAUpgrades = Collections.unmodifiableList(toUpgradeTiers(config.pathA));
        this.pathBUpgrades = Collections.unmodifiableList(toUpgradeTiers(config.pathB));
    }

    private List<UpgradeTier> toUpgradeTiers(List<TDConfig.UpgradeTierConfig> configured) {
        List<UpgradeTier> upgrades = new ArrayList<>();
        if (configured == null) {
            return upgrades;
        }

        for (TDConfig.UpgradeTierConfig tier : configured) {
            if (tier == null) {
                continue;
            }

            Map<String, Double> deltas = new LinkedHashMap<>();
            if (tier.statDeltas != null) {
                for (Map.Entry<String, Double> entry : tier.statDeltas.entrySet()) {
                    String key = entry.getKey();
                    Double value = entry.getValue();
                    if (key == null || key.isBlank() || value == null) {
                        continue;
                    }
                    deltas.put(key.toLowerCase(Locale.ROOT), value);
                }
            }

            upgrades.add(new UpgradeTier(tier.cost, tier.name, deltas));
        }

        return upgrades;
    }

    public String getAssetId() {
        return assetId;
    }

    public int getCost() {
        return cost;
    }

    public int getDamage() {
        return damage;
    }

    public double getAttackSpeed() {
        return attackSpeed;
    }

    public double getRange() {
        return range;
    }

    public double getSlowPercent() {
        return slowPercent;
    }

    public long getSlowDurationMs() {
        return slowDurationMs;
    }

    public boolean isIgnoreArmor() {
        return ignoreArmor;
    }

    public UpgradeTier getUpgradeTier(UpgradePath path, int currentTier) {
        List<UpgradeTier> tiers = path == UpgradePath.PATH_A ? pathAUpgrades : pathBUpgrades;
        if (currentTier < 0 || currentTier >= tiers.size()) {
            return null;
        }
        return tiers.get(currentTier);
    }
}
