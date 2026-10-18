package exiledsector.console;

import com.fs.starfarer.api.Global;
import exiledsector.charts.ChartRunActions;
import exiledsector.charts.ChartRunActions.OpenResult;
import exiledsector.charts.StarChartSettings;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.Random;

public class ChartOpenCommand implements BaseCommand {

    private final Random random;

    public ChartOpenCommand() {
        this(new Random());
    }

    ChartOpenCommand(Random random) {
        this.random = random;
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        int entries = StarChartSettings.wormholeEntries();
        OpenResult result = ChartRunActions.openBesidePlayer(Global.getSector(), entries, random);
        switch (result) {
            case OPENED -> {
                Console.showMessage("Opened a Star Chart wormhole beside your fleet with " + entries + (entries == 1 ? " entry." : " entries."));
                return CommandResult.SUCCESS;
            }
            case RUN_EXISTS -> Console.showMessage("A Star Chart run is already open. Collapse it or finish it first (see ExiledSectorChartStatus).");
            case NO_PLAYER_FLEET -> Console.showMessage("No player fleet found.");
            case PLAYER_IN_TRANSITION -> Console.showMessage("Your fleet is jumping. Try again once it arrives.");
            case PLAYER_IN_POCKET -> Console.showMessage("Your fleet is inside a Star Chart pocket.");
            default -> Console.showMessage("Opening the wormhole failed; see starsector.log.");
        }
        return CommandResult.ERROR;
    }
}
