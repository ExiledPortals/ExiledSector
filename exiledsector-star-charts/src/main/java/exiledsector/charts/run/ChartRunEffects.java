package exiledsector.charts.run;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.effect.ChartFleetConditions;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.StarChart;

import java.util.List;
import java.util.Objects;

public final class ChartRunEffects {

    static final float SENSOR_MOD_DAYS = 0.1f;
    static final String VANILLA_NAV_BUOY_MOD_ID = "nav_buoy";
    static final String VANILLA_SENSOR_ARRAY_MOD_ID = "sensor_array";
    private static ChartRun contextRun;
    private static String contextChartData;
    private static String contextNodeData;
    private static List<String> contextAnomalies;
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
                || !run.anomalyIds().equals(contextAnomalies)) {
            context = ChartGenContext.from(chartOf(run)).withAtlasNode(run.atlasNode()).withAnomalyEffects(run.anomalyIds());
            contextRun = run;
            contextChartData = run.chartData();
            contextNodeData = run.atlasNodeData();
            contextAnomalies = run.anomalyIds();
        }
        return context;
    }

    public static void apply(CampaignFleetAPI fleet, ChartRun run, String sensorSource) {
        ChartGenContext runContext = contextOf(run);
        MutableFleetStatsAPI stats = fleet == null ? null : fleet.getStats();
        if (stats != null && runContext.sensorRangeMult() != 1f) {
            stats.addTemporaryModMult(SENSOR_MOD_DAYS, ChartIds.SENSOR_MOD_ID, sensorSource, runContext.sensorRangeMult(), stats.getSensorRangeMod());
        }
        ChartFleetConditions.set(runContext.fleetStatMults(), fleet);
        ChartFleetConditions.setBattleSizeBonus(runContext.battleSizeBonus());
    }

    public static void lift(CampaignFleetAPI fleet) {
        ChartFleetConditions.clear(fleet);
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
