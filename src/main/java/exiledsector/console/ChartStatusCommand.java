package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.ChartCensus;
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
                + (pocket == null ? "" : "\nPocket contents: " + ChartPocketContents.of(pocket).describe());
    }
}
