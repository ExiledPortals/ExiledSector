package exiledsector.charts.layout;

import java.util.Set;

public record ChartLayoutRecipe(ChartLook look, ChartShape shape) {

    public static final Set<String> ANOMALY_STARS = Set.of("black_hole", "star_neutron");
    public static final Set<String> ANOMALY_CATEGORIES = Set.of("cat_magfield");
    public static final ChartLayoutRecipe ORDINARY = new ChartLayoutRecipe(ChartLook.PLAIN, ChartShape.ANY);

    public ChartLayoutRecipe {
        look = look == null ? ChartLook.PLAIN : look;
        shape = shape == null ? ChartShape.ANY : shape;
    }

    public float starWeight(String starType, float vanillaWeight) {
        return ANOMALY_STARS.contains(starType) ? 0f : vanillaWeight * look.starWeights().getOrDefault(starType, 1f);
    }

    public float categoryWeight(String category, float vanillaWeight) {
        if (ANOMALY_CATEGORIES.contains(category)) {
            return 0f;
        }
        return vanillaWeight * look.planetWeights().getOrDefault(category, 1f) * shape.terrainWeights().getOrDefault(category, 1f);
    }
}
