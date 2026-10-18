package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.StarChart;

public final class ChartRunEffects {

    static final float SENSOR_MOD_DAYS = 0.1f;

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
        return ChartGenContext.from(chartOf(run));
    }

    public static boolean applySensorRange(CampaignFleetAPI fleet, float sensorRangeMult, String source) {
        MutableFleetStatsAPI stats = fleet == null ? null : fleet.getStats();
        if (stats == null) {
            return false;
        }
        if (sensorRangeMult == 1f) {
            removeFrom(fleet);
            return false;
        }
        stats.addTemporaryModMult(SENSOR_MOD_DAYS, ChartIds.SENSOR_MOD_ID, source, sensorRangeMult, stats.getSensorRangeMod());
        return true;
    }

    public static void removeFrom(CampaignFleetAPI fleet) {
        MutableFleetStatsAPI stats = fleet == null ? null : fleet.getStats();
        if (stats == null) {
            return;
        }
        stats.removeTemporaryMod(ChartIds.SENSOR_MOD_ID);
        if (stats.getSensorRangeMod() != null) {
            stats.getSensorRangeMod().unmodify(ChartIds.SENSOR_MOD_ID);
        }
    }
}
