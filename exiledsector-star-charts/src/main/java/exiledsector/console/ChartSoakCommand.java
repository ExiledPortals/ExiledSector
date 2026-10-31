package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.run.diagnostics.ChartCensus;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.run.diagnostics.ChartSoakScript;
import org.lazywizard.console.Console;

import java.util.Random;

public class ChartSoakCommand extends CampaignCommand {

    static final int DEFAULT_SYSTEMS = 10;
    static final int MAX_SYSTEMS = 500;

    @Override
    protected CommandResult runInCampaign(String args) {
        Integer systems = ConsoleArgs.count(args, DEFAULT_SYSTEMS, 1, MAX_SYSTEMS);
        if (systems == null) {
            Console.showMessage("Give a number of systems from 1 to " + MAX_SYSTEMS + ".");
            return CommandResult.BAD_SYNTAX;
        }
        SectorAPI sector = Global.getSector();
        if (ChartRuns.hasRun()) {
            Console.showMessage(ChartConsoleText.RUN_ALREADY_OPEN);
            return CommandResult.ERROR;
        }
        if (sector.hasTransientScript(ChartSoakScript.class)) {
            Console.showMessage("A Star Chart soak is already running.");
            return CommandResult.ERROR;
        }
        ChartCensus before = ChartCensus.take(sector);
        sector.addTransientScript(new ChartSoakScript(systems, before, ChartSoakCommand::report, new Random()));
        Console.showMessage("Soaking " + systems + " full Star Chart system(s) with their fleets and points of interest, a few frames each,"
                + " cycling seeds, system types and tiers 1 to 15. Close the console and keep the game paused so other fleets and intel do not change the counts; the result is shown"
                + " in the campaign messages, in the console and in starsector.log.\nBefore: " + before.describe());
        return CommandResult.SUCCESS;
    }

    private static void report(String report) {
        Console.showMessage(report);
        if (Global.getSector().getCampaignUI() != null) {
            for (String line : report.split("\n")) {
                Global.getSector().getCampaignUI().addMessage(line);
            }
        }
    }
}
