package exiledsector.charts.layout;

import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.StarAnomalies;

import java.util.Set;

public record ChartLayoutRecipe(ChartLook look, ChartShape shape) {

    public static final Set<String> ANOMALY_CATEGORIES = Set.of("cat_magfield");
    public static final ChartLayoutRecipe ORDINARY = new ChartLayoutRecipe(ChartLook.PLAIN, ChartShape.ANY);

    public ChartLayoutRecipe {
        look = look == null ? ChartLook.PLAIN : look;
        shape = shape == null ? ChartShape.ANY : shape;
    }

    public float starWeight(String starType, float vanillaWeight) {
        return isAnomalyStar(starType) ? 0f : vanillaWeight * look.starWeights().getOrDefault(starType, 1f);
    }

    public static boolean isAnomalyStar(String starType) {
        for (StarAnomalies.ForcedStar star : ChartAnomalies.with(StarAnomalies.ForcedStar.class)) {
            if (starType != null && starType.equals(star.primaryStar())) {
                return true;
            }
        }
        return false;
    }

    public float categoryWeight(String category, float vanillaWeight) {
        if (ANOMALY_CATEGORIES.contains(category)) {
            return 0f;
        }
        return vanillaWeight * look.planetWeights().getOrDefault(category, 1f) * shape.terrainWeights().getOrDefault(category, 1f);
    }
}
