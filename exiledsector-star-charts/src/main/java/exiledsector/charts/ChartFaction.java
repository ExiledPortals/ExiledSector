package exiledsector.charts;

public record ChartFaction(String factionId, float weight, float fleetSizeBonus, float fleetQualityBonus) {

    static final String FALLBACK_FACTION_ID = "pirates";
    static final ChartFaction FALLBACK = new ChartFaction(FALLBACK_FACTION_ID, 1f, 0f, 0f);

    public ChartFaction {
        weight = Math.max(0f, weight);
    }

    static ChartFaction unlisted(String factionId) {
        return new ChartFaction(factionId, 1f, 0f, 0f);
    }
}
