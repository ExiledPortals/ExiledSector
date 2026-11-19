package exiledsector.charts.atlas;

public record AtlasAnomalyChance(float chance, float chancePerStep, float maxChance) {

    public static final AtlasAnomalyChance NEVER = new AtlasAnomalyChance(0f, 0f, 0f);

    public AtlasAnomalyChance increasedBy(float increasedPercent) {
        float mult = Math.max(0f, 1f + increasedPercent / 100f);
        return new AtlasAnomalyChance(chance * mult, chancePerStep * mult, maxChance * mult);
    }

    public float chanceAt(float distance) {
        return Math.max(0f, Math.min(Math.min(1f, maxChance), chance + chancePerStep * distance));
    }
}
