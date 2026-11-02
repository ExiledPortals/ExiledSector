package exiledsector.charts.atlas;

public record AtlasAnomalyChance(float chance, float chancePerStep, float maxChance) {

    public static final AtlasAnomalyChance NEVER = new AtlasAnomalyChance(0f, 0f, 0f);

    public float chanceAt(float distance) {
        return Math.max(0f, Math.min(Math.min(1f, maxChance), chance + chancePerStep * distance));
    }
}
