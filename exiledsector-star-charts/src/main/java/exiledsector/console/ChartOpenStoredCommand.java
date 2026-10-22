package exiledsector.console;

import com.fs.starfarer.api.Global;
import exiledsector.charts.ChartRun;
import exiledsector.charts.ChartRunActions;
import exiledsector.charts.ChartRunActions.OpenResult;
import exiledsector.charts.ChartRuns;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.StarCharts;
import exiledlib.items.Item;
import exiledlib.items.ItemStore;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.List;
import java.util.Random;

public class ChartOpenStoredCommand implements BaseCommand {

    private final Random random;

    public ChartOpenStoredCommand() {
        this(new Random());
    }

    ChartOpenStoredCommand(Random random) {
        this.random = random;
    }

    static Integer parseIndex(String args) {
        if (args == null || args.isBlank()) {
            return null;
        }
        try {
            int index = Integer.parseInt(args.trim());
            return index >= 1 ? index : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        Integer index = parseIndex(args);
        if (index == null) {
            Console.showMessage("Usage: ExiledSectorChartOpenStored <index>, where index is a chart's number in ExiledSectorChartList.");
            return CommandResult.BAD_SYNTAX;
        }
        StarCharts.absorbCharts(Global.getSector());
        ItemStore store = ItemStore.starChartsIfCreated();
        List<Item> charts = store == null ? List.of() : store.owned();
        if (index > charts.size()) {
            Console.showMessage("There is no stored Star Chart " + index + "; Star Chart storage holds " + charts.size() + ".");
            return CommandResult.ERROR;
        }
        Item chart = charts.get(index - 1);
        String chartText = ChartListCommand.describe(chart);
        int entries = StarChartSettings.wormholeEntries();
        OpenResult result = ChartRunActions.openFromStore(Global.getSector(), entries, random, store, chart);
        switch (result) {
            case OPENED -> {
                ChartRun run = ChartRuns.currentOrNull();
                Console.showMessage("Opened a Star Chart wormhole beside your fleet from " + chartText + "\nThe chart was used up. "
                        + ChartOpenCommand.describeLayout(run) + "\n" + ChartOpenCommand.describeContent(run));
                return CommandResult.SUCCESS;
            }
            case RUN_EXISTS -> Console.showMessage("A Star Chart run is already open. Collapse it or finish it first (see ExiledSectorChartStatus).");
            case NO_PLAYER_FLEET -> Console.showMessage("No player fleet found.");
            case PLAYER_IN_TRANSITION -> Console.showMessage("Your fleet is jumping. Try again once it arrives.");
            case PLAYER_IN_SYSTEM -> Console.showMessage("Your fleet is inside a Star Chart system.");
            case NO_CHART -> Console.showMessage("Stored chart " + index + " can't be opened: its definition is not loaded.");
            default -> Console.showMessage("Opening the wormhole failed, so the chart was kept; see starsector.log.");
        }
        return CommandResult.ERROR;
    }
}
