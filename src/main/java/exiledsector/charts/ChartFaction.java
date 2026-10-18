package exiledsector.charts;

public record ChartFaction(String factionId, float weight, int minTier, float fleetSizeBonus, float fleetQualityBonus) {

    static final String FALLBACK_FACTION_ID = "pirates";
    static final ChartFaction FALLBACK = new ChartFaction(FALLBACK_FACTION_ID, 1f, ChartTiers.LOWEST_TIER, 0f, 0f);

    public ChartFaction {
        weight = Math.max(0f, weight);
    }

    public boolean allowedAt(int tier) {
        return tier >= minTier;
    }
}
