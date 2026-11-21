package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.objective.ChartObjectives;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;

import java.util.ArrayList;
import java.util.List;

final class DefeatChartBossStep implements HarnessStep {

    private final List<String> defeated = new ArrayList<>();

    @Override
    public String name() {
        return "defeatChartBoss";
    }

    @Override
    public StepOutcome advance(StepContext context) {
        if (context.firstFrame()) {
            StepOutcome notInCampaign = HarnessCampaign.notInCampaign(name());
            if (notInCampaign != null) {
                return notInCampaign;
            }
        }
        SectorAPI sector = Global.getSector();
        ChartRun run = ChartRuns.live();
        if (run == null) {
            return StepOutcome.failed("there is no live Star Chart run");
        }
        if (run.isObjectiveComplete()) {
            return StepOutcome.passed("the objective of run " + run.runId() + " is complete after destroying " + defeated);
        }
        if (!run.hasBoss()) {
            return StepOutcome.failed("run " + run.runId() + " has no boss to defeat");
        }
        CampaignFleetAPI boss = ChartObjectives.bossFleet(sector, run);
        if (boss != null && !boss.isDespawning()) {
            defeated.add(boss.getName());
            boss.despawn(FleetDespawnReason.DESTROYED_BY_BATTLE, null);
        }
        return new StepOutcome(false, false, "destroyed " + defeated + " and the objective is still open");
    }
}
