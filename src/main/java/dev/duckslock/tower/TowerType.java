package dev.duckslock.tower;

import com.hypixel.hytale.logger.HytaleLogger;
import dev.duckslock.combat.ElementType;
import dev.duckslock.config.TDConfig;

import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Level;

public enum TowerType {

    ARROW("Outlander_Hunter", 50, 15, 1.2, 6.0, 0.0, 0L, false, ElementType.COMPOSITE,
            TowerAttackKind.DIRECT, 0.0d, 0.50d, 0, 0.0d, 1.0d, null),
    CANNON("Trork_Mauler", 80, 40, 0.5, 5.0, 0.0, 0L, false, ElementType.COMPOSITE,
            TowerAttackKind.SPLASH, 2.0d, 0.65d, 0, 0.0d, 1.0d, null),
    FROST("Skeleton_Soldier", 65, 8, 1.0, 5.0, 0.25, 1500L, false, ElementType.WATER,
            TowerAttackKind.DIRECT, 0.0d, 0.50d, 0, 0.0d, 1.0d, null),
    MAGIC("Outlander_Brute", 100, 25, 0.8, 6.0, 0.0, 0L, true, ElementType.DARKNESS,
            TowerAttackKind.CHAIN, 0.0d, 0.50d, 2, 3.0d, 0.75d, null),

    WC3_COLD("Skeleton_Soldier", 50, 18, 3.2258064516, 5.46875, 0.0, 0L, false, ElementType.WATER,
            TowerAttackKind.SPLASH, 1.5625d, 0.70d, 0, 0.0d, 1.0d, "n000"),
    WC3_ADV_COLD("Skeleton_Soldier", 125, 90, 3.2258064516, 5.46875, 0.0, 0L, false, ElementType.WATER,
            TowerAttackKind.SPLASH, 1.5625d, 0.70d, 0, 0.0d, 1.0d, "n001"),
    WC3_ROCK("Earthen_Golem", 50, 29, 1.0, 5.46875, 0.0, 0L, false, ElementType.EARTH,
            TowerAttackKind.SPLASH, 2.34375d, 0.65d, 0, 0.0d, 1.0d, "n005"),
    WC3_ADV_ROCK("Earthen_Golem", 125, 145, 1.0, 5.46875, 0.0, 0L, false, ElementType.EARTH,
            TowerAttackKind.SPLASH, 2.34375d, 0.65d, 0, 0.0d, 1.0d, "n002"),
    WC3_FIRE("Outlander_Brute", 50, 12, 3.2258064516, 3.90625, 0.0, 0L, false, ElementType.FIRE,
            TowerAttackKind.SPLASH, 2.34375d, 0.70d, 0, 0.0d, 1.0d, "n003"),
    WC3_ADV_FIRE("Outlander_Brute", 125, 60, 3.2258064516, 3.90625, 0.0, 0L, false, ElementType.FIRE,
            TowerAttackKind.SPLASH, 2.34375d, 0.70d, 0, 0.0d, 1.0d, "n007"),
    WC3_DEATH("Trork_Mauler", 50, 157, 0.6666666667, 8.7890625, 0.0, 0L, false, ElementType.DARKNESS,
            TowerAttackKind.DIRECT, 0.0d, 0.50d, 0, 0.0d, 1.0d, "n004"),
    WC3_ADV_DEATH("Trork_Mauler", 125, 785, 0.6666666667, 8.7890625, 0.0, 0L, false, ElementType.DARKNESS,
            TowerAttackKind.DIRECT, 0.0d, 0.50d, 0, 0.0d, 1.0d, "n006"),
    WC3_ENERGY("Outlander_Hunter", 50, 57, 1.5151515152, 11.71875, 0.0, 0L, false, ElementType.LIGHT,
            TowerAttackKind.CHAIN, 0.0d, 0.50d, 2, 4.0d, 0.70d, "n008"),
    WC3_ADV_ENERGY("Outlander_Hunter", 125, 285, 1.5151515152, 11.71875, 0.0, 0L, false, ElementType.LIGHT,
            TowerAttackKind.CHAIN, 0.0d, 0.50d, 4, 4.5d, 0.75d, "n009"),
    WC3_TRICKERY("Outlander_Hunter", 325, 0, 1.0, 5.46875, 0.0, 0L, true, ElementType.LIGHT,
            TowerAttackKind.SUPPORT_TRICKERY, 0.0d, 0.50d, 0, 0.0d, 1.0d, "h000"),
    WC3_FLESH_GOLEM("Earthen_Golem", 1000, 9000, 1.0, 3.90625, 0.0, 0L, true, ElementType.COMPOSITE,
            TowerAttackKind.DIRECT, 0.0d, 0.50d, 0, 0.0d, 1.0d, "n01C"),
    WC3_ABOMINATION("Earthen_Golem", 3500, 45000, 1.0, 3.90625, 0.0, 0L, true, ElementType.COMPOSITE,
            TowerAttackKind.DIRECT, 0.0d, 0.50d, 0, 0.0d, 1.0d, "n021"),
    WC3_RAINBOW("Outlander_Brute", 11225, 78750, 1.0, 7.03125, 0.0, 0L, true, ElementType.COMPOSITE,
            TowerAttackKind.SPLASH, 1.5625d, 0.70d, 0, 0.0d, 1.0d, "n02G");

    private final TDConfig.TowerConfig defaults;
    private final TowerAttackKind defaultAttackKind;
    private final double defaultSplashRadius;
    private final double defaultSplashFalloff;
    private final int defaultChainBounces;
    private final double defaultChainRange;
    private final double defaultChainFalloff;
    @Nullable
    private final String wc3UnitId;

    private String assetId;
    private int cost;
    private int damage;
    private double attackSpeed;
    private double range;
    private double slowPercent;
    private long slowDurationMs;
    private boolean ignoreArmor;
    private ElementType damageElement;
    private TowerAttackKind attackKind;
    private double splashRadius;
    private double splashFalloff;
    private int chainBounces;
    private double chainRange;
    private double chainFalloff;
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
            boolean ignoreArmor,
            ElementType damageElement,
            TowerAttackKind attackKind,
            double splashRadius,
            double splashFalloff,
            int chainBounces,
            double chainRange,
            double chainFalloff,
            @Nullable String wc3UnitId
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
                damageElement.name(),
                TDConfig.defaultPathAUpgrades(),
                TDConfig.defaultPathBUpgrades()
        );
        this.defaultAttackKind = attackKind;
        this.defaultSplashRadius = Math.max(0.0d, splashRadius);
        this.defaultSplashFalloff = clamp(splashFalloff, 0.0d, 1.0d);
        this.defaultChainBounces = Math.max(0, chainBounces);
        this.defaultChainRange = Math.max(0.0d, chainRange);
        this.defaultChainFalloff = clamp(chainFalloff, 0.0d, 1.0d);
        this.wc3UnitId = wc3UnitId == null || wc3UnitId.isBlank() ? null : wc3UnitId;
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

    public static TowerType parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String normalized = token.trim().toUpperCase(Locale.ROOT);
        try {
            return TowerType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
        }

        for (TowerType type : values()) {
            if (type.wc3UnitId != null && type.wc3UnitId.equalsIgnoreCase(token.trim())) {
                return type;
            }
        }
        return null;
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

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
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
        this.damageElement = ElementType.parseOrDefault(config.damageElement, ElementType.COMPOSITE);
        this.attackKind = defaultAttackKind;
        this.splashRadius = defaultSplashRadius;
        this.splashFalloff = defaultSplashFalloff;
        this.chainBounces = defaultChainBounces;
        this.chainRange = defaultChainRange;
        this.chainFalloff = defaultChainFalloff;
        this.pathAUpgrades = Collections.unmodifiableList(toUpgradeTiers(config.pathA));
        this.pathBUpgrades = Collections.unmodifiableList(toUpgradeTiers(config.pathB));
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

    public TDConfig.TowerConfig defaultConfigCopy() {
        return defaults.copy();
    }

    @Nullable
    public String getWc3UnitId() {
        return wc3UnitId;
    }

    public ElementType getDamageElement() {
        return damageElement;
    }

    public TowerAttackKind getAttackKind() {
        return attackKind;
    }

    public double getSplashRadius() {
        return splashRadius;
    }

    public double getSplashFalloff() {
        return splashFalloff;
    }

    public int getChainBounces() {
        return chainBounces;
    }

    public double getChainRange() {
        return chainRange;
    }

    public UpgradeTier getUpgradeTier(UpgradePath path, int currentTier) {
        List<UpgradeTier> tiers = path == UpgradePath.PATH_A ? pathAUpgrades : pathBUpgrades;
        if (currentTier < 0 || currentTier >= tiers.size()) {
            return null;
        }
        return tiers.get(currentTier);
    }

    public double getChainFalloff() {
        return chainFalloff;
    }

    public boolean isCompositeDamage() {
        return damageElement == ElementType.COMPOSITE;
    }
}
