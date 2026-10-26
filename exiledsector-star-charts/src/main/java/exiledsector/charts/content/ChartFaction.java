package exiledsector.charts.content;

public record ChartFaction(String factionId, float weight) {

    static final String FALLBACK_FACTION_ID = "pirates";
    static final ChartFaction FALLBACK = new ChartFaction(FALLBACK_FACTION_ID, 1f);

    public ChartFaction {
        weight = Math.max(0f, weight);
    }

    static ChartFaction unlisted(String factionId) {
        return new ChartFaction(factionId, 1f);
    }
}
