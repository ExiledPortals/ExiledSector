package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.run.ChartEntryBans;
import exiledsector.charts.run.ChartEntryFuel;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunActions;
import exiledsector.charts.run.ChartRuns;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

final class OpenChartStep implements HarnessStep {

    static final int ENTRIES_ALLOWED = 1;
    static final float ENTRY_TIMEOUT_SECONDS = 60f;

    private final long seed;
    private ChartRun run;

    OpenChartStep(long seed) {
        this.seed = seed;
    }

    @Override
    public String name() {
        return "openChart seed " + seed;
    }

    @Override
    public StepOutcome advance(StepContext context) throws JSONException {
        SectorAPI sector = Global.getSector();
        if (context.firstFrame()) {
            StepOutcome notInCampaign = HarnessCampaign.notInCampaign("openChart");
            return notInCampaign != null ? notInCampaign : openAndEnter(sector);
        }
        HarnessCampaign.unpause(sector);
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        StarSystemAPI system = sector.getStarSystem(run.systemId());
        if (playerFleet.isInHyperspaceTransition()) {
            return new StepOutcome(false, false, "the player fleet is still jumping into run " + run.runId());
        }
        if (system == null || playerFleet.getContainingLocation() != system) {
            return new StepOutcome(false, false, "the player fleet is in " + playerFleet.getContainingLocation().getName() + ", not run " + run.runId());
        }
        context.results().addData("chartRun", runData(run, system));
        return StepOutcome.passed("entered Star Chart run " + run.runId() + " in " + system.getName() + " (tier " + run.tier()
                + (run.hasBoss() ? ", with a boss" : ", with no boss") + ")");
    }

    private StepOutcome openAndEnter(SectorAPI sector) {
        ChartRunActions.OpenResult opened = ChartRunActions.openBesidePlayer(sector, ENTRIES_ALLOWED, new Random(seed));
        if (opened != ChartRunActions.OpenResult.OPENED) {
            return StepOutcome.failed("could not open a Star Chart run: " + opened);
        }
        run = ChartRuns.live();
        SectorEntityToken outerWormhole = sector.getEntityById(run.outerWormholeId());
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (outerWormhole == null) {
            return StepOutcome.failed("run " + run.runId() + " opened without an outer wormhole");
        }
        if (!ChartRunActions.enter(sector, run, outerWormhole)) {
            return StepOutcome.failed("could not enter run " + run.runId() + ": banned hull sizes in the fleet " + ChartEntryBans.breached(run, playerFleet)
                    + ", fuel " + (ChartEntryFuel.canAfford(run, playerFleet) ? "enough" : "short of " + ChartEntryFuel.cost(run)));
        }
        return StepOutcome.RUNNING;
    }

    private static JSONObject runData(ChartRun run, StarSystemAPI system) throws JSONException {
        JSONObject data = new JSONObject();
        data.put("runId", run.runId());
        data.put("system", system.getName());
        data.put("tier", run.tier());
        data.put("hasBoss", run.hasBoss());
        data.put("planets", system.getPlanets().stream().filter(planet -> !planet.isStar()).count());
        return data;
    }

    @Override
    public float timeoutSeconds() {
        return ENTRY_TIMEOUT_SECONDS;
    }
}
