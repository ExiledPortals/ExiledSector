package exiledsector.console;

import com.fs.starfarer.api.Global;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunActions;
import exiledsector.charts.run.ChartRunActions.OpenResult;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.StarCharts;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import org.lazywizard.console.Console;

import java.util.List;
import java.util.Random;

public class ChartOpenStoredCommand extends CampaignCommand {

    private final Random random;

    public ChartOpenStoredCommand() {
        this(new Random());
    }

    ChartOpenStoredCommand(Random random) {
        this.random = random;
    }

    @Override
    protected CommandResult runInCampaign(String args) {
        Integer index = ConsoleArgs.wholeNumber(args, 1, Integer.MAX_VALUE);
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
            case NO_CHART -> Console.showMessage("Stored chart " + index + " can't be opened: its definition is not loaded.");
            default -> Console.showMessage(ChartConsoleText.openFailure(result, "Opening the wormhole failed, so the chart was kept; see starsector.log."));
        }
        return CommandResult.ERROR;
    }
}
