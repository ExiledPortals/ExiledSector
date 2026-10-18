package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import exiledsector.charts.ChartCensus;
import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartPocketContents;
import exiledsector.charts.ChartRun;
import exiledsector.charts.ChartRuns;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class ChartStatusCommand implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        SectorAPI sector = Global.getSector();
        Console.showMessage(describeRun(sector, ChartRuns.currentOrNull()));
        Console.showMessage("Counts: " + ChartCensus.take(sector).describe());
        Console.showMessage("\"Charted Space\" reserved in the used procgen names: " + ChartCensus.pocketNameReserved());
        return CommandResult.SUCCESS;
    }

    static String describeRun(SectorAPI sector, ChartRun run) {
        if (run == null) {
            return "No Star Chart run is open.";
        }
        StarSystemAPI pocket = sector.getStarSystem(run.pocketSystemId());
        boolean outerExists = sector.getEntityById(run.outerWormholeId()) != null;
        return "Star Chart run " + run.runId() + ": " + run.runState() + ", entries used " + run.entriesUsed() + " of " + run.entriesAllowed()
                + ", pocket " + (pocket != null ? "exists" : "missing") + ", outer wormhole " + (outerExists ? "exists" : "gone")
                + ", origin " + run.originLocationId() + " at (" + Math.round(run.originX()) + ", " + Math.round(run.originY()) + ")"
                + (run.isReturnRequested() ? ", returning" : "")
                + "\n" + ChartOpenCommand.describeLayout(run)
                + "\n" + ChartOpenCommand.describeContent(run)
                + "\n" + describeObjective(run)
                + (pocket == null ? "" : "\nPocket contents: " + ChartPocketContents.of(pocket).describe()
                + "\nStill in the pocket: " + describeLiveContent(pocket));
    }

    static String describeObjective(ChartRun run) {
        if (run.objective() == null) {
            return "Objective: none.";
        }
        return "Objective " + run.objective().name() + ": " + run.objectiveTargetsDefeated() + " of " + run.objectiveTargetIds().size()
                + " target(s) done" + (run.isObjectiveComplete() ? ", complete" : "")
                + (run.isRewardClaimed() ? ", reward delivered" : run.isObjectiveComplete() ? ", reward NOT delivered" : "") + ".";
    }

    static String describeLiveContent(StarSystemAPI pocket) {
        int chartFleets = 0;
        for (CampaignFleetAPI fleet : pocket.getFleets()) {
            if (fleet.hasTag(ChartIds.CONTENT_TAG) && !fleet.isDespawning()) {
                chartFleets++;
            }
        }
        int pois = 0;
        for (SectorEntityToken entity : pocket.getEntitiesWithTag(ChartIds.CONTENT_TAG)) {
            if (!(entity instanceof CampaignFleetAPI) && !entity.hasTag(Tags.FADING_OUT_AND_EXPIRING)) {
                pois++;
            }
        }
        return chartFleets + " chart fleet(s) and " + pois + " point(s) of interest not yet salvaged.";
    }
}
