package dev.duckslock.enemy;

import dev.duckslock.combat.ElementType;

import javax.annotation.Nullable;

/**
 * Optional per-spawn overrides used for WC3-derived wave data.
 */
public final class EnemySpawnProfile {

    @Nullable
    private final String sourceUnitId;
    @Nullable
    private final String assetId;
    @Nullable
    private final Integer maxHp;
    @Nullable
    private final Float speed;
    @Nullable
    private final Float armor;
    @Nullable
    private final Integer bounty;
    @Nullable
    private final Integer leakDamage;
    @Nullable
    private final Boolean boss;
    @Nullable
    private final ElementType element;

    public EnemySpawnProfile(
            @Nullable String sourceUnitId,
            @Nullable String assetId,
            @Nullable Integer maxHp,
            @Nullable Float speed,
            @Nullable Float armor,
            @Nullable Integer bounty,
            @Nullable Integer leakDamage,
            @Nullable Boolean boss,
            @Nullable ElementType element
    ) {
        this.sourceUnitId = sourceUnitId;
        this.assetId = assetId;
        this.maxHp = maxHp;
        this.speed = speed;
        this.armor = armor;
        this.bounty = bounty;
        this.leakDamage = leakDamage;
        this.boss = boss;
        this.element = element;
    }

    @Nullable
    public String getSourceUnitId() {
        return sourceUnitId;
    }

    @Nullable
    public String getAssetId() {
        return assetId;
    }

    @Nullable
    public Integer getMaxHp() {
        return maxHp;
    }

    @Nullable
    public Float getSpeed() {
        return speed;
    }

    @Nullable
    public Float getArmor() {
        return armor;
    }

    @Nullable
    public Integer getBounty() {
        return bounty;
    }

    @Nullable
    public Integer getLeakDamage() {
        return leakDamage;
    }

    @Nullable
    public Boolean getBoss() {
        return boss;
    }

    @Nullable
    public ElementType getElement() {
        return element;
    }
}
