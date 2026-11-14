package exiledsector.charts.atlas;

import exiledsector.charts.anomaly.AtlasRule;
import exiledsector.charts.anomaly.ChartAnomalies;

import java.util.ArrayList;
import java.util.List;

public final class OpeningAnomalies {

    static final long OPENING_SALT = 0x4f50454e494e47L;

    private OpeningAnomalies() {
    }

    public static List<String> rolled(AtlasGenerator generator, AtlasState state, AtlasCell cell, long chartSeed, List<String> present) {
        if (cell.equals(AtlasCell.ORIGIN) || generator.isVoid(cell)) {
            return List.of();
        }
        AtlasRegion region = generator.regionOf(cell);
        AnomalyRolls<String> rolls = new AnomalyRolls<>();
        for (ChartAnomalies.Definition definition : ChartAnomalies.definitions()) {
            String anomalyId = definition.id();
            if (!definition.rollsOnOpening() || present.contains(anomalyId) || region.anomalyWeight(anomalyId) <= 0f) {
                continue;
            }
            double roll = AtlasHash.unit(AtlasHash.of(chartSeed, OPENING_SALT, anomalyId, cell.gridX(), cell.gridY()));
            rolls.offer(anomalyId, anomalyId, roll, definition.openingChance());
        }
        List<String> accepted = new ArrayList<>(present);
        List<String> added = new ArrayList<>();
        for (String anomalyId : rolls.lowestFirst()) {
            if (accepted.size() >= AtlasAnomalies.MAX_PER_SYSTEM) {
                break;
            }
            if (ChartAnomalies.accepts(accepted, anomalyId) && fits(generator, state, cell, anomalyId)) {
                accepted.add(anomalyId);
                added.add(anomalyId);
            }
        }
        return List.copyOf(added);
    }

    private static boolean fits(AtlasGenerator generator, AtlasState state, AtlasCell cell, String anomalyId) {
        return AtlasRule.of(ChartAnomalies.byId(anomalyId)).insertVerdict(generator, state, cell).allowed();
    }
}
