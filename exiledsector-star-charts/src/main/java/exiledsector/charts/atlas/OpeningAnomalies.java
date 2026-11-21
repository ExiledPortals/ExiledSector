package exiledsector.charts.atlas;

import exiledsector.charts.anomaly.AtlasRule;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.generation.ChartGenContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OpeningAnomalies {

    static final long OPENING_SALT = 0x4f50454e494e47L;

    private OpeningAnomalies() {
    }

    public static List<String> rolled(AtlasGenerator generator, AtlasState state, AtlasCell cell, long chartSeed, List<String> present) {
        return rolled(generator, state, cell, chartSeed, present, List.of());
    }

    public static List<String> rolled(AtlasGenerator generator, AtlasState state, AtlasCell cell, long chartSeed, List<String> present,
                                      List<ChartGenContext.AppliedEffect> treeLines) {
        if (cell.equals(AtlasCell.ORIGIN) || generator.isVoid(cell)) {
            return List.of();
        }
        AtlasRegion region = generator.regionOf(cell);
        Map<String, Float> addedChances = addedChances(treeLines);
        AnomalyRolls<String> rolls = new AnomalyRolls<>();
        for (ChartAnomalies.Definition definition : ChartAnomalies.definitions()) {
            String anomalyId = definition.id();
            if (!definition.rollsOnOpening() || present.contains(anomalyId) || region.anomalyWeight(anomalyId) <= 0f) {
                continue;
            }
            double roll = AtlasHash.unit(AtlasHash.of(chartSeed, OPENING_SALT, anomalyId, cell.gridX(), cell.gridY()));
            rolls.offer(anomalyId, anomalyId, roll, chance(definition, addedChances));
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

    static Map<String, Float> addedChances(List<ChartGenContext.AppliedEffect> treeLines) {
        Map<String, Float> addedChances = new HashMap<>();
        for (ChartGenContext.AppliedEffect line : treeLines) {
            String anomalyId = line.effect().openingChanceAnomalyId();
            if (anomalyId != null) {
                addedChances.merge(anomalyId, line.value() / 100f, Float::sum);
            }
        }
        return addedChances;
    }

    private static double chance(ChartAnomalies.Definition definition, Map<String, Float> addedChances) {
        return Math.max(0f, Math.min(1f, definition.openingChance() + addedChances.getOrDefault(definition.id(), 0f)));
    }

    private static boolean fits(AtlasGenerator generator, AtlasState state, AtlasCell cell, String anomalyId) {
        return AtlasRule.of(ChartAnomalies.byId(anomalyId)).insertVerdict(generator, state, cell).allowed();
    }
}
