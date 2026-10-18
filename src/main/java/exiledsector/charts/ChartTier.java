package exiledsector.charts;

public record ChartTier(int tier, float fleetPower, int fleetsMin, int fleetsMax, int officerLevelBonus, int officerCountBonus,
                        float quality, int averageSMods, int treePercentMin, int treePercentMax, float lootBudget, boolean boss,
                        float bossPower, float clearWeight, float bossWeight, float vaultWeight) {

    static final float FALLBACK_POWER_PER_TIER = 90f;
    static final float FALLBACK_FIRST_TIER_POWER = 60f;
    static final float FALLBACK_LOOT_PER_TIER = 3f;
    static final float FALLBACK_FIRST_TIER_LOOT = 6f;
    static final int FALLBACK_FIRST_BOSS_TIER = 4;

    public ChartTier {
        fleetPower = Math.max(0f, fleetPower);
        fleetsMin = Math.max(1, fleetsMin);
        fleetsMax = Math.max(fleetsMin, fleetsMax);
        officerLevelBonus = Math.max(0, officerLevelBonus);
        officerCountBonus = Math.max(0, officerCountBonus);
        quality = Math.max(0f, quality);
        averageSMods = Math.max(0, averageSMods);
        int lowerPercent = Math.max(0, Math.min(treePercentMin, treePercentMax));
        int upperPercent = Math.max(0, Math.max(treePercentMin, treePercentMax));
        treePercentMin = lowerPercent;
        treePercentMax = upperPercent;
        lootBudget = Math.max(0f, lootBudget);
        bossPower = Math.max(0f, bossPower);
        boss = boss && bossPower > 0f;
        clearWeight = Math.max(0f, clearWeight);
        bossWeight = Math.max(0f, bossWeight);
        vaultWeight = Math.max(0f, vaultWeight);
    }

    public ChartTier withLootBudget(float replacementLootBudget) {
        return new ChartTier(tier, fleetPower, fleetsMin, fleetsMax, officerLevelBonus, officerCountBonus, quality, averageSMods, treePercentMin,
                treePercentMax, replacementLootBudget, boss, bossPower, clearWeight, bossWeight, vaultWeight);
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
        int treePercentMin = Math.min(200, 50 + 10 * steps);
        return new ChartTier(tier, FALLBACK_FIRST_TIER_POWER + FALLBACK_POWER_PER_TIER * steps, 2 + steps / 3, 4 + steps / 3,
                steps / 3, steps / 3, Math.min(1f, 0.3f + 0.05f * steps), steps / 5, treePercentMin, Math.min(300, treePercentMin + 40),
                FALLBACK_FIRST_TIER_LOOT + FALLBACK_LOOT_PER_TIER * steps, hasBoss, hasBoss ? 80f + 25f * (tier - FALLBACK_FIRST_BOSS_TIER) : 0f,
                1f, hasBoss ? 1f : 0f, tier >= 3 ? 1f : 0f);
    }
}
