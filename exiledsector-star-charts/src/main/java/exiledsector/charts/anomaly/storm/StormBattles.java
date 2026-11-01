package exiledsector.charts.anomaly.storm;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;

public final class StormBattles {

    private StormBattles() {
    }

    public static StormFrontTerrainPlugin playerFront() {
        SectorAPI sector = Global.getSector();
        return sector == null ? null : StormFront.frontHolding(sector.getPlayerFleet());
    }

    public static boolean playerInStorm() {
        return playerFront() != null;
    }

    static ChartRun runOf(StormFrontTerrainPlugin front) {
        return front == null ? null : ChartRuns.live(front.runId());
    }

    static ChartGenContext contextOf(ChartRun run) {
        return run == null ? ChartGenContext.none() : ChartRunEffects.contextOf(run);
    }
}
