package exiledsector.charts;

public record ChartTier(int tier, float fleetSize, int fleetsMin, int fleetsMax, int officerLevelBonus, int officerCountBonus,
                        float fleetQuality, int fleetStrengthMin, int fleetStrengthMax, float lootBudget, boolean boss,
                        float wardenSize, float clearWeight, float bossWeight, float vaultWeight) {

    public static final float FLEET_SIZE_BASE_POINTS = 50f;
    static final float PERCENT = 100f;
    static final float FALLBACK_FIRST_TIER_FLEET_SIZE = 100f;
    static final float FALLBACK_FLEET_SIZE_PER_TIER = 32f;
    static final float FALLBACK_FIRST_WARDEN_SIZE = 300f;
    static final float FALLBACK_WARDEN_SIZE_PER_TIER = 70f;
    static final float FALLBACK_LOOT_PER_TIER = 3f;
    static final float FALLBACK_FIRST_TIER_LOOT = 6f;
    static final int FALLBACK_FIRST_BOSS_TIER = 4;

    public ChartTier {
        fleetSize = Math.max(0f, fleetSize);
        fleetsMin = Math.max(1, fleetsMin);
        fleetsMax = Math.max(fleetsMin, fleetsMax);
        officerLevelBonus = Math.max(0, officerLevelBonus);
        officerCountBonus = Math.max(0, officerCountBonus);
        fleetQuality = Math.max(0f, fleetQuality);
        int lowerPercent = Math.max(0, Math.min(fleetStrengthMin, fleetStrengthMax));
        int upperPercent = Math.max(0, Math.max(fleetStrengthMin, fleetStrengthMax));
        fleetStrengthMin = lowerPercent;
        fleetStrengthMax = upperPercent;
        lootBudget = Math.max(0f, lootBudget);
        wardenSize = Math.max(0f, wardenSize);
        boss = boss && wardenSize > 0f;
        clearWeight = Math.max(0f, clearWeight);
        bossWeight = Math.max(0f, bossWeight);
        vaultWeight = Math.max(0f, vaultWeight);
    }

    public static float fleetPoints(float sizePercent) {
        return FLEET_SIZE_BASE_POINTS * Math.max(0f, sizePercent) / PERCENT;
    }

    public ChartTier withLootBudget(float replacementLootBudget) {
        return new ChartTier(tier, fleetSize, fleetsMin, fleetsMax, officerLevelBonus, officerCountBonus, fleetQuality, fleetStrengthMin,
                fleetStrengthMax, replacementLootBudget, boss, wardenSize, clearWeight, bossWeight, vaultWeight);
    }

    public float weightFor(ChartObjective objective) {
        return switch (objective) {
            case CLEAR -> clearWeight;
            case BOSS -> boss ? bossWeight : 0f;
            case VAULT -> vaultWeight;
        };
    }

    static ChartTier fallback(int tier) {
        int steps = Math.max(0, tier - 1);
        boolean hasBoss = tier >= FALLBACK_FIRST_BOSS_TIER;
        int strengthMin = Math.min(200, 50 + 10 * steps);
        return new ChartTier(tier, FALLBACK_FIRST_TIER_FLEET_SIZE + FALLBACK_FLEET_SIZE_PER_TIER * steps, 2 + steps / 3, 4 + steps / 3,
                steps / 3, steps / 3, Math.min(100f, 30f + 5f * steps), strengthMin, Math.min(300, strengthMin + 40),
                FALLBACK_FIRST_TIER_LOOT + FALLBACK_LOOT_PER_TIER * steps, hasBoss,
                hasBoss ? FALLBACK_FIRST_WARDEN_SIZE + FALLBACK_WARDEN_SIZE_PER_TIER * (tier - FALLBACK_FIRST_BOSS_TIER) : 0f,
                1f, hasBoss ? 1f : 0f, tier >= 3 ? 1f : 0f);
    }
}
