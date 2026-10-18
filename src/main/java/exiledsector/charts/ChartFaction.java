package exiledsector.charts;

public record ChartFaction(String factionId, float weight, int minTier, int maxTier, float powerMult, float qualityBonus) {

    static final String FALLBACK_FACTION_ID = "pirates";
    static final ChartFaction FALLBACK = new ChartFaction(FALLBACK_FACTION_ID, 1f, ChartTiers.LOWEST_TIER, ChartTiers.HIGHEST_TIER, 1f, 0f);

    public ChartFaction {
        weight = Math.max(0f, weight);
        powerMult = powerMult > 0f ? powerMult : 1f;
    }

    public boolean allowedAt(int tier) {
        return tier >= minTier && tier <= maxTier;
    }
}
