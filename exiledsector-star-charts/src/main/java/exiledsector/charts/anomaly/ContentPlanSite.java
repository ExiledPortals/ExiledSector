package exiledsector.charts.anomaly;

import exiledsector.charts.content.ChartFaction;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.table.BuiltInTable;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public record ContentPlanSite(String anomalyId, long seed, ChartTier tier, ChartGenContext context, List<ChartFaction> factions,
                              Predicate<String> factionLoaded) {

    public Random random(long salt) {
        return BuiltInTable.rollFor(seed, anomalyId, salt);
    }
}
