package exiledsector.charts.atlas;

public record AtlasMarkerRule(float chance, float chancePerStep, float maxChance) {

    public static final AtlasMarkerRule NEVER = new AtlasMarkerRule(0f, 0f, 0f);

    public float chanceAt(float distance) {
        return Math.max(0f, Math.min(Math.min(1f, maxChance), chance + chancePerStep * distance));
    }
}
