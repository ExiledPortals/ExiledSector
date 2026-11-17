package exiledsector.charts.run;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.effect.ChartPlayerModifiers;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.starchart.StarChart;

import java.util.Objects;

public final class ChartRunEffects {

    static final String VANILLA_NAV_BUOY_MOD_ID = "nav_buoy";
    static final String VANILLA_SENSOR_ARRAY_MOD_ID = "sensor_array";
    private static ChartRun contextRun;
    private static String contextChartData;
    private static String contextNodeData;
    private static String contextTreeLinesData;
    private static String contextAnomaliesData;
    private static ChartGenContext context = ChartGenContext.none();

    private ChartRunEffects() {
    }

    public static StarChart chartOf(ChartRun run) {
        if (run == null || !run.hasChart()) {
            return null;
        }
        ItemData chartData = ItemData.parse(run.chartData());
        Item preview = chartData == null ? null : chartData.preview();
        return preview instanceof StarChart chart ? chart : null;
    }

    public static ChartGenContext contextOf(ChartRun run) {
        if (run == null) {
            return ChartGenContext.none();
        }
        if (run != contextRun || !Objects.equals(run.chartData(), contextChartData) || !Objects.equals(run.atlasNodeData(), contextNodeData)
                || !Objects.equals(run.atlasTreeLinesData(), contextTreeLinesData) || !Objects.equals(run.anomaliesData(), contextAnomaliesData)) {
            context = ChartGenContext.from(chartOf(run)).withAtlasNode(run.atlasNode()).withEffects(run.atlasTreeLines())
                    .withAnomalyEffects(run.anomalyIds());
            contextRun = run;
            contextChartData = run.chartData();
            contextNodeData = run.atlasNodeData();
            contextTreeLinesData = run.atlasTreeLinesData();
            contextAnomaliesData = run.anomaliesData();
        }
        return context;
    }

    public static void apply(CampaignFleetAPI fleet, ChartGenContext runContext, String sensorSource) {
        MutableFleetStatsAPI stats = fleet == null ? null : fleet.getStats();
        if (stats != null && stats.getSensorRangeMod() != null) {
            if (runContext.sensorRangeMult() != 1f) {
                stats.getSensorRangeMod().modifyMult(ChartIds.SENSOR_MOD_ID, runContext.sensorRangeMult(), sensorSource);
            } else {
                stats.getSensorRangeMod().unmodify(ChartIds.SENSOR_MOD_ID);
            }
        }
        ChartPlayerModifiers.set(runContext.fleetStatMults(), fleet);
        ChartPlayerModifiers.setBattleRules(runContext.battleRules());
    }

    public static void lift(CampaignFleetAPI fleet) {
        ChartPlayerModifiers.clear(fleet);
        ChartAnomalies.clearFrom(fleet);
        MutableFleetStatsAPI stats = fleet == null ? null : fleet.getStats();
        if (stats == null) {
            return;
        }
        stats.removeTemporaryMod(ChartIds.SENSOR_MOD_ID);
        stats.removeTemporaryMod(VANILLA_NAV_BUOY_MOD_ID);
        stats.removeTemporaryMod(VANILLA_SENSOR_ARRAY_MOD_ID);
        if (stats.getSensorRangeMod() != null) {
            stats.getSensorRangeMod().unmodify(ChartIds.SENSOR_MOD_ID);
            stats.getSensorRangeMod().unmodify(VANILLA_SENSOR_ARRAY_MOD_ID);
        }
        if (stats.getFleetwideMaxBurnMod() != null) {
            stats.getFleetwideMaxBurnMod().unmodify(VANILLA_NAV_BUOY_MOD_ID);
        }
    }
}
