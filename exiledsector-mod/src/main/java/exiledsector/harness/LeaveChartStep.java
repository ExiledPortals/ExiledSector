package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunActions;
import exiledsector.charts.run.ChartRuns;
import org.lwjgl.util.vector.Vector2f;

final class LeaveChartStep implements HarnessStep {

    static final float LEAVE_TIMEOUT_SECONDS = 60f;

    private String runId;

    @Override
    public String name() {
        return "leaveChart";
    }

    @Override
    public StepOutcome advance(StepContext context) {
        SectorAPI sector = Global.getSector();
        if (context.firstFrame()) {
            StepOutcome notInCampaign = HarnessCampaign.notInCampaign(name());
            return notInCampaign != null ? notInCampaign : leave(sector);
        }
        HarnessCampaign.unpause(sector);
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (playerFleet.isInHyperspaceTransition()) {
            return new StepOutcome(false, false, "the player fleet is still jumping out of run " + runId);
        }
        if (ChartRuns.unclosed(runId) != null) {
            return new StepOutcome(false, false, "run " + runId + " hasn't closed");
        }
        return StepOutcome.passed("left run " + runId + ", which closed, and arrived in " + playerFleet.getContainingLocation().getName());
    }

    private StepOutcome leave(SectorAPI sector) {
        ChartRun run = HarnessCampaign.runAroundPlayer(sector);
        if (run == null) {
            return StepOutcome.failed("the player fleet isn't in a live Star Chart run");
        }
        runId = run.runId();
        SectorEntityToken innerWormhole = sector.getEntityById(run.innerWormholeId());
        if (innerWormhole != null) {
            Vector2f wormholeLocation = innerWormhole.getLocation();
            sector.getPlayerFleet().setLocation(wormholeLocation.x, wormholeLocation.y);
        }
        if (innerWormhole == null || !ChartRunActions.returnToOrigin(sector, run, innerWormhole)) {
            return StepOutcome.failed("could not return from run " + runId + (innerWormhole == null ? ", which has no inner wormhole" : ""));
        }
        return StepOutcome.RUNNING;
    }

    @Override
    public float timeoutSeconds() {
        return LEAVE_TIMEOUT_SECONDS;
    }
}
