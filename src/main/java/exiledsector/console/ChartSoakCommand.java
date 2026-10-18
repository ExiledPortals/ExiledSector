package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.ChartCensus;
import exiledsector.charts.ChartRuns;
import exiledsector.charts.ChartSoakScript;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.Random;

public class ChartSoakCommand implements BaseCommand {

    static final int DEFAULT_POCKETS = 10;
    static final int MAX_POCKETS = 500;

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        int pockets = parsePockets(args);
        if (pockets < 1 || pockets > MAX_POCKETS) {
            Console.showMessage("Give a number of pockets from 1 to " + MAX_POCKETS + ".");
            return CommandResult.BAD_SYNTAX;
        }
        SectorAPI sector = Global.getSector();
        if (ChartRuns.hasRun()) {
            Console.showMessage("A Star Chart run is already open. Collapse it or finish it first.");
            return CommandResult.ERROR;
        }
        if (sector.hasTransientScript(ChartSoakScript.class)) {
            Console.showMessage("A Star Chart soak is already running.");
            return CommandResult.ERROR;
        }
        ChartCensus before = ChartCensus.take(sector);
        sector.addTransientScript(new ChartSoakScript(pockets, before, ChartSoakCommand::report, new Random()));
        Console.showMessage("Soaking " + pockets + " Star Chart pocket(s), a few frames each. Close the console; the result is shown"
                + " in the campaign messages, in the console and in starsector.log.\nBefore: " + before.describe());
        return CommandResult.SUCCESS;
    }

    static int parsePockets(String args) {
        if (args == null || args.isBlank()) {
            return DEFAULT_POCKETS;
        }
        try {
            return Integer.parseInt(args.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
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
