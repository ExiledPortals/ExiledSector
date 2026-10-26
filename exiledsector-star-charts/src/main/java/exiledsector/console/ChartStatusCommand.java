package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import exiledsector.charts.run.diagnostics.ChartCensus;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.diagnostics.ChartSystemContents;
import exiledsector.charts.ChartRun;
import exiledsector.charts.ChartRuns;
import org.lazywizard.console.Console;

public class ChartStatusCommand extends CampaignCommand {

    @Override
    protected CommandResult runInCampaign(String args) {
        SectorAPI sector = Global.getSector();
        Console.showMessage(describeRun(sector, ChartRuns.currentOrNull()));
        Console.showMessage("Counts: " + ChartCensus.take(sector).describe());
        Console.showMessage("\"Charted Space\" reserved in the used procgen names: " + ChartCensus.systemNameReserved());
        return CommandResult.SUCCESS;
    }

    static String describeRun(SectorAPI sector, ChartRun run) {
        if (run == null) {
            return "No Star Chart run is open.";
        }
        StarSystemAPI system = sector.getStarSystem(run.systemId());
        boolean outerExists = sector.getEntityById(run.outerWormholeId()) != null;
        return "Star Chart run " + run.runId() + ": " + run.runState() + ", entries used " + run.entriesUsed() + " of " + run.entriesAllowed()
                + ", system " + (system != null ? "exists" : "missing") + ", outer wormhole " + (outerExists ? "exists" : "gone")
                + ", origin " + run.originLocationId() + " at (" + Math.round(run.originX()) + ", " + Math.round(run.originY()) + ")"
                + (run.isReturnRequested() ? ", returning" : "")
                + "\n" + ChartOpenCommand.describeLayout(run)
                + "\n" + ChartOpenCommand.describeContent(run)
                + "\n" + describeObjective(run)
                + "\n" + ChartOpenCommand.describeChart(run)
                + (system == null ? "" : "\nSystem contents: " + ChartSystemContents.of(system).describe()
                + "\nStill in the system: " + describeLiveContent(system));
    }

    static String describeObjective(ChartRun run) {
        if (run.objective() == null) {
            return "Objective: none.";
        }
        return "Objective " + run.objective().name() + ": " + run.objectiveTargetsDefeated() + " of " + run.objectiveTargetIds().size()
                + " target(s) done" + (run.isObjectiveComplete() ? ", complete" : "")
                + (run.isRewardClaimed() ? ", reward delivered" : run.isObjectiveComplete() ? ", reward NOT delivered" : "") + ".";
    }

    static String describeLiveContent(StarSystemAPI system) {
        int chartFleets = 0;
        for (CampaignFleetAPI fleet : system.getFleets()) {
            if (fleet.hasTag(ChartIds.CONTENT_TAG) && !fleet.isDespawning()) {
                chartFleets++;
            }
        }
        int pois = 0;
        for (SectorEntityToken entity : system.getEntitiesWithTag(ChartIds.CONTENT_TAG)) {
            if (!(entity instanceof CampaignFleetAPI) && !entity.hasTag(Tags.FADING_OUT_AND_EXPIRING)) {
                pois++;
            }
        }
        return chartFleets + " chart fleet(s) and " + pois + " point(s) of interest not yet salvaged.";
    }
}
