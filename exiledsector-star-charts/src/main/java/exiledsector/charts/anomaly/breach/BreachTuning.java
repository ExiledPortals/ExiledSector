package exiledsector.charts.anomaly.breach;

import exiledsector.charts.generation.ChartGenContext;
import org.lwjgl.util.vector.Vector2f;

public record BreachTuning(float heldDrainPercent, boolean drifts, boolean wakesAnomalousFleets, Vector2f incursionAt) {

    public static final BreachTuning NONE = new BreachTuning(0f, false, false, null);

    static BreachTuning from(ChartGenContext context, Vector2f incursionAt) {
        return new BreachTuning(context.anomalyPercent(BreachLines.HELD_DRAIN_KEY), BreachLines.on(context, BreachLines.DRIFT_KEY),
                BreachLines.on(context, BreachLines.WAKES_ANOMALOUS_KEY), incursionAt);
    }
}
