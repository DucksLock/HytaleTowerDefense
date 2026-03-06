package dev.duckslock.enemy;

import com.hypixel.hytale.logger.HytaleLogger;
import dev.duckslock.combat.ElementType;
import dev.duckslock.config.TDConfig;

import java.util.Map;
import java.util.logging.Level;

// one entry per enemy type - stats + which model to spawn
public enum EnemyType {

    //                       assetId                        hp    speed  armor  bounty  damage  isBoss
    GRUNT(EnemyAssets.ZOMBIE, 80, 1.0f, 0.00f, 5, 1, false, ElementType.NONE),
    RUNNER(EnemyAssets.SKELETON_SCOUT, 50, 1.6f, 0.00f, 8, 1, false, ElementType.NONE),
    SOLDIER(EnemyAssets.SKELETON_SOLDIER, 120, 1.1f, 0.15f, 10, 1, false, ElementType.NONE),
    BRUTE(EnemyAssets.TRORK_UNARMED, 200, 0.7f, 0.30f, 12, 2, false, ElementType.NONE),
    SHIELDED(EnemyAssets.TRORK_MAULER, 150, 1.0f, 0.50f, 15, 2, false, ElementType.NONE),
    HUNTER(EnemyAssets.OUTLANDER_HUNTER, 130, 1.5f, 0.10f, 18, 1, false, ElementType.NONE),
    ELITE(EnemyAssets.OUTLANDER_BRUTE, 300, 0.9f, 0.40f, 25, 2, false, ElementType.NONE),
    GOLEM(EnemyAssets.EARTHEN_GOLEM, 1500, 0.5f, 0.40f, 80, 5, true, ElementType.EARTH);

    private final TDConfig.EnemyConfig defaults;

    public String assetId;
    public int maxHp;
    public float speed;   // multiplier, 1.0 = normal
    public float armor;   // 0.0-1.0 damage reduction
    public int bounty;    // gold on kill
    public int damage;    // lives deducted on base reach
    public boolean isBoss;
    public ElementType element;

    EnemyType(String assetId, int maxHp, float speed, float armor,
              int bounty, int damage, boolean isBoss, ElementType element) {
        this.defaults = new TDConfig.EnemyConfig(assetId, maxHp, speed, armor, bounty, damage, isBoss, element.name());
        apply(defaults);
    }

    public static void applyConfig(Map<String, TDConfig.EnemyConfig> configured, HytaleLogger logger) {
        for (EnemyType type : values()) {
            TDConfig.EnemyConfig config = configured != null ? configured.get(type.name()) : null;
            if (config == null) {
                config = type.defaults.copy();
            } else {
                config = config.copy();
            }
            config.sanitize();
            type.apply(config);
            logger.at(Level.INFO).log("Enemy '%s' configured: asset=%s hp=%s speed=%.3f",
                    type.name(), type.assetId, type.maxHp, type.speed);
        }
    }

    private void apply(TDConfig.EnemyConfig config) {
        this.assetId = config.assetId;
        this.maxHp = config.maxHp;
        this.speed = config.speed;
        this.armor = config.armor;
        this.bounty = config.bounty;
        this.damage = config.damage;
        this.isBoss = config.boss;
        this.element = ElementType.parseOrDefault(config.element, ElementType.NONE);
    }

    public int getDamage() {
        return damage;
    }

    public int getBounty() {
        return bounty;
    }

    public ElementType getElement() {
        return element;
    }
}
