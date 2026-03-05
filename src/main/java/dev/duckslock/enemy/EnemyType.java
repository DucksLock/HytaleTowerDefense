package dev.duckslock.enemy;

// one entry per enemy type - stats + which model to spawn
public enum EnemyType {

    //                       assetId                        hp    speed  armor  bounty  damage  isBoss
    GRUNT(EnemyAssets.ZOMBIE, 80, 1.0f, 0.00f, 5, 1, false),
    RUNNER(EnemyAssets.SKELETON_SCOUT, 50, 1.6f, 0.00f, 8, 1, false),
    SOLDIER(EnemyAssets.SKELETON_SOLDIER, 120, 1.1f, 0.15f, 10, 1, false),
    BRUTE(EnemyAssets.TRORK_UNARMED, 200, 0.7f, 0.30f, 12, 2, false),
    SHIELDED(EnemyAssets.TRORK_MAULER, 150, 1.0f, 0.50f, 15, 2, false),
    HUNTER(EnemyAssets.OUTLANDER_HUNTER, 130, 1.5f, 0.10f, 18, 1, false),
    ELITE(EnemyAssets.OUTLANDER_BRUTE, 300, 0.9f, 0.40f, 25, 2, false),
    GOLEM(EnemyAssets.EARTHEN_GOLEM, 1500, 0.5f, 0.40f, 80, 5, true);

    public final String assetId;
    public final int maxHp;
    public final float speed;   // multiplier, 1.0 = normal
    public final float armor;   // 0.0-1.0 damage reduction
    public final int bounty;  // gold on kill
    public final int damage;  // lives deducted on base reach
    public final boolean isBoss;

    EnemyType(String assetId, int maxHp, float speed, float armor,
              int bounty, int damage, boolean isBoss) {
        this.assetId = assetId;
        this.maxHp = maxHp;
        this.speed = speed;
        this.armor = armor;
        this.bounty = bounty;
        this.damage = damage;
        this.isBoss = isBoss;
    }
}