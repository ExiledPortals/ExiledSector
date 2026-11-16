package exiledsector.charts.anomaly.debris;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;

public record DebrisTuning(int extraFields, boolean singleField, boolean breachField, float fleetSizePercent, float corruptedFleetSizePercent,
                           int corruptedEngrams, int twiceCorruptedEngrams, boolean secondCorruption, boolean alwaysTwiceCorrupted, boolean awake,
                           boolean roaming, SleeperShips.Shares sleeperShares) {

    public static DebrisTuning of(ChartGenContext context) {
        ChartGenContext lines = ChartGenContext.orNone(context);
        return new DebrisTuning(count(lines, ChartModifier.CHART_EXTRA_ANOMALOUS_DEBRIS), raised(lines, ChartModifier.CHART_ANOMALOUS_DEBRIS_SINGLE_FIELD),
                raised(lines, ChartModifier.CHART_ANOMALOUS_DEBRIS_BREACH_FIELD), percent(lines, ChartModifier.CHART_ANOMALOUS_DEBRIS_FLEET_SIZE_PERCENT),
                percent(lines, ChartModifier.CHART_ANOMALOUS_DEBRIS_CORRUPTED_FLEET_SIZE_PERCENT), count(lines, ChartModifier.CHART_ANOMALOUS_DEBRIS_CORRUPTED_ENGRAMS),
                count(lines, ChartModifier.CHART_ANOMALOUS_DEBRIS_TWICE_CORRUPTED_ENGRAMS), raised(lines, ChartModifier.CHART_ANOMALOUS_DEBRIS_SECOND_CORRUPTION),
                raised(lines, ChartModifier.CHART_ANOMALOUS_DEBRIS_ALWAYS_TWICE_CORRUPTED), raised(lines, ChartModifier.CHART_ANOMALOUS_DEBRIS_AWAKE),
                raised(lines, ChartModifier.CHART_ANOMALOUS_DEBRIS_ROAMING),
                new SleeperShips.Shares(percent(lines, ChartModifier.CHART_ANOMALOUS_DEBRIS_REMNANT_SHIP_SHARE),
                        percent(lines, ChartModifier.CHART_ANOMALOUS_DEBRIS_THREAT_SHIP_SHARE),
                        percent(lines, ChartModifier.CHART_ANOMALOUS_DEBRIS_DWELLER_SHIP_SHARE)));
    }

    private static float percent(ChartGenContext lines, ChartModifier effect) {
        return lines.anomalyPercent(effect.anomalyTuningKey());
    }

    private static int count(ChartGenContext lines, ChartModifier effect) {
        return Math.round(percent(lines, effect));
    }

    private static boolean raised(ChartGenContext lines, ChartModifier effect) {
        return percent(lines, effect) > 0f;
    }

    public int forcedCorruptions() {
        if (alwaysTwiceCorrupted) {
            return DebrisRules.MOST_CORRUPTIONS;
        }
        return awake ? 1 : 0;
    }

    public int mostCorruptions() {
        return alwaysTwiceCorrupted || secondCorruption ? DebrisRules.MOST_CORRUPTIONS : 1;
    }

    public float increasedSizePercent(int corruptions) {
        return fleetSizePercent + (corruptions > 0 ? corruptedFleetSizePercent : 0f);
    }

    public int engramsFor(int corruptions) {
        if (corruptions <= 0) {
            return 0;
        }
        int engrams = DebrisRules.CRAFTING_ITEM_PER_FLEET + Math.max(0, corruptedEngrams);
        return corruptions >= DebrisRules.MOST_CORRUPTIONS ? engrams + Math.max(0, twiceCorruptedEngrams) : engrams;
    }
}
