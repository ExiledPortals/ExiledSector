package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.charts.loot.ChartDrops;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemType;
import exiledsector.items.StarChart;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GrantStarChartsCommand extends PlayerFleetCommand {

    static final int DEFAULT_COUNT = 5;
    static final int MAX_COUNT = 50;
    static final int DEFAULT_TIER = StarChart.LOWEST_TIER;

    private final Random random;

    public GrantStarChartsCommand() {
        this(new Random());
    }

    GrantStarChartsCommand(Random random) {
        this.random = random;
    }

    record GrantArguments(int count, int tier, String problem) {

        static GrantArguments parse(String args) {
            String[] tokens = args == null || args.isBlank() ? new String[0] : args.trim().split("\\s+");
            if (tokens.length > 2) {
                return problem("Give at most a count and a tier.");
            }
            Integer count = tokens.length > 0 ? ConsoleArgs.wholeNumber(tokens[0], 1, MAX_COUNT) : Integer.valueOf(DEFAULT_COUNT);
            Integer tier = tokens.length > 1 ? ConsoleArgs.wholeNumber(tokens[1], StarChart.LOWEST_TIER, StarChart.CORRUPTED_TIER)
                    : Integer.valueOf(DEFAULT_TIER);
            if (count == null) {
                return problem("The count must be a whole number from 1 to " + MAX_COUNT + ".");
            }
            if (tier == null) {
                return problem("The tier must be a whole number from " + StarChart.LOWEST_TIER + " to " + StarChart.CORRUPTED_TIER + ".");
            }
            return new GrantArguments(count, tier, null);
        }

        private static GrantArguments problem(String problem) {
            return new GrantArguments(0, 0, problem);
        }
    }

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        GrantArguments arguments = GrantArguments.parse(args);
        if (arguments.problem() != null) {
            Console.showMessage(arguments.problem() + "\nUsage: ExiledSectorGrantStarCharts [count] [tier], count 1 to " + MAX_COUNT
                    + " (default " + DEFAULT_COUNT + "), tier " + StarChart.LOWEST_TIER + " to " + StarChart.CORRUPTED_TIER + " (default "
                    + DEFAULT_TIER + ").");
            return CommandResult.BAD_SYNTAX;
        }
        if (ItemDefinitions.ofType(ItemType.STAR_CHART).isEmpty()) {
            Console.showMessage("No Star Charts are defined.");
            return CommandResult.ERROR;
        }
        List<String> names = new ArrayList<>();
        for (int i = 0; i < arguments.count(); i++) {
            ItemDefinition definition = ChartDrops.pickChartAtTier(random, arguments.tier());
            if (definition == null) {
                break;
            }
            ItemData chart = ItemData.rolledAtTier(definition, random.nextLong(), arguments.tier());
            playerFleet.getCargo().addSpecial(chart.toSpecialItem(), 1f);
            names.add(chart.preview() == null ? definition.id() : chart.preview().name());
        }
        Console.showMessage("Added " + names.size() + " tier " + arguments.tier() + " Star Chart(s) to your cargo: " + String.join(", ", names)
                + ". ExiledSectorChartList moves them into Star Chart storage and lists them.");
        return names.isEmpty() ? CommandResult.ERROR : CommandResult.SUCCESS;
    }
}
