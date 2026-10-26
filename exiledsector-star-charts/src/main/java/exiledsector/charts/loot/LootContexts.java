package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.charts.ChartGenContext;
import exiledsector.charts.ChartRun;
import exiledsector.charts.ChartRunEffects;

public final class LootContexts {

    private LootContexts() {
    }

    public static ChartGenContext forFleet(CampaignFleetAPI fleet) {
        ChartRun run = fleet == null || !ChartFleetLoot.isChartFleet(fleet) ? null : ChartFleetLoot.liveRunOf(fleet);
        return run == null ? global() : ChartRunEffects.contextOf(run);
    }

    public static ChartGenContext global() {
        return ChartGenContext.none();
    }
}
