package dev.duckslock.tower;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.duckslock.enclave.Enclave;
import dev.duckslock.enemy.Enemy;
import dev.duckslock.grid.GridSquare;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Tower {

    private final UUID id = UUID.randomUUID();
    private final TowerType type;
    private final GridSquare square;
    private final Enclave enclave;
    private final long createdAtMs = System.currentTimeMillis();

    private int tier = 0;
    private int spentUpgradeGold = 0;
    private double damageMultiplier = 1.0d;
    private double rangeMultiplier = 1.0d;
    private double attackSpeedMultiplier = 1.0d;
    private double slowPercentMultiplier = 1.0d;
    private long slowDurationBonusMs = 0L;

    @Nullable
    private UpgradePath chosenPath;

    @Nullable
    private Ref<EntityStore> entityRef;

    private long lastAttackTick;
    private TargetPriority targetPriority = TargetPriority.FIRST;

    @Nullable
    private Enemy currentTarget;

    public Tower(TowerType type, GridSquare square, Enclave enclave) {
        this.type = type;
        this.square = square;
        this.enclave = enclave;
        this.lastAttackTick = createdAtMs;
    }

    public boolean upgrade(UpgradePath path) {
        if (tier >= 2) {
            return false;
        }
        if (tier == 1 && chosenPath != path) {
            return false;
        }

        UpgradeTier upgradeTier = type.getUpgradeTier(path, tier);
        if (upgradeTier == null) {
            return false;
        }

        if (!enclave.spendGold(upgradeTier.getCost())) {
            return false;
        }

        applyStatDeltas(upgradeTier.getStatDeltas());
        if (tier == 0) {
            chosenPath = path;
        }
        tier++;
        spentUpgradeGold += upgradeTier.getCost();
        return true;
    }

    public int getSellRefund() {
        return Math.max(0, (type.getCost() + spentUpgradeGold) / 2);
    }

    public double getAttackIntervalMs() {
        return 1000.0d / Math.max(0.05d, getEffectiveAttackSpeed());
    }

    public int getEffectiveDamage() {
        return Math.max(1, (int) Math.round(type.getDamage() * damageMultiplier));
    }

    public double getEffectiveRange() {
        return Math.max(0.25d, type.getRange() * rangeMultiplier);
    }

    public double getEffectiveAttackSpeed() {
        return Math.max(0.05d, type.getAttackSpeed() * attackSpeedMultiplier);
    }

    public double getEffectiveSlowPercent() {
        return clamp(type.getSlowPercent() * slowPercentMultiplier, 0.0d, 0.95d);
    }

    public long getEffectiveSlowDurationMs() {
        return Math.max(0L, type.getSlowDurationMs() + slowDurationBonusMs);
    }

    private void applyStatDeltas(Map<String, Double> statDeltas) {
        if (statDeltas == null || statDeltas.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Double> entry : statDeltas.entrySet()) {
            String key = entry.getKey();
            Double value = entry.getValue();
            if (key == null || value == null) {
                continue;
            }

            String normalized = key.toLowerCase(Locale.ROOT).replace(" ", "");
            switch (normalized) {
                case "damage" -> damageMultiplier *= Math.max(0.01d, value);
                case "range" -> rangeMultiplier *= Math.max(0.01d, value);
                case "attackspeed", "attack_speed" -> attackSpeedMultiplier *= Math.max(0.01d, value);
                case "slowpercent", "slow_percent" -> slowPercentMultiplier *= Math.max(0.01d, value);
                case "slowdurationms", "slow_duration_ms" -> slowDurationBonusMs += Math.round(value);
                default -> {
                }
            }
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public UUID getId() {
        return id;
    }

    public TowerType getType() {
        return type;
    }

    public int getTier() {
        return tier;
    }

    @Nullable
    public UpgradePath getChosenPath() {
        return chosenPath;
    }

    public GridSquare getSquare() {
        return square;
    }

    public Enclave getEnclave() {
        return enclave;
    }

    @Nullable
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    public void setEntityRef(@Nullable Ref<EntityStore> entityRef) {
        this.entityRef = entityRef;
    }

    public long getLastAttackTick() {
        return lastAttackTick;
    }

    public void markAttack(long nowMs) {
        this.lastAttackTick = nowMs;
    }

    public TargetPriority getTargetPriority() {
        return targetPriority;
    }

    public void setTargetPriority(TargetPriority targetPriority) {
        if (targetPriority != null) {
            this.targetPriority = targetPriority;
        }
    }

    @Nullable
    public Enemy getCurrentTarget() {
        return currentTarget;
    }

    public void setCurrentTarget(@Nullable Enemy currentTarget) {
        this.currentTarget = currentTarget;
    }
}
