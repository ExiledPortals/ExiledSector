package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledlib.items.ItemData;
import exiledlib.items.ItemDefinition;
import exiledlib.items.ItemDefinitions;
import exiledsector.items.ItemDrops;
import exiledlib.items.ItemType;
import exiledsector.items.StarChart;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GrantStarChartsCommand implements BaseCommand {

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
            Integer count = tokens.length > 0 ? parseNumber(tokens[0]) : Integer.valueOf(DEFAULT_COUNT);
            Integer tier = tokens.length > 1 ? parseNumber(tokens[1]) : Integer.valueOf(DEFAULT_TIER);
            if (count == null || count < 1 || count > MAX_COUNT) {
                return problem("The count must be a whole number from 1 to " + MAX_COUNT + ".");
            }
            if (tier == null || tier < StarChart.LOWEST_TIER || tier > StarChart.HIGHEST_TIER) {
                return problem("The tier must be a whole number from " + StarChart.LOWEST_TIER + " to " + StarChart.HIGHEST_TIER + ".");
            }
            return new GrantArguments(count, tier, null);
        }

        private static GrantArguments problem(String problem) {
            return new GrantArguments(0, 0, problem);
        }

        private static Integer parseNumber(String token) {
            try {
                return Integer.parseInt(token);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        GrantArguments arguments = GrantArguments.parse(args);
        if (arguments.problem() != null) {
            Console.showMessage(arguments.problem() + "\nUsage: ExiledSectorGrantStarCharts [count] [tier], count 1 to " + MAX_COUNT
                    + " (default " + DEFAULT_COUNT + "), tier " + StarChart.LOWEST_TIER + " to " + StarChart.HIGHEST_TIER + " (default "
                    + DEFAULT_TIER + ").");
            return CommandResult.BAD_SYNTAX;
        }
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null || playerFleet.getCargo() == null) {
            Console.showMessage("No player fleet found.");
            return CommandResult.ERROR;
        }
        if (ItemDefinitions.ofType(ItemType.STAR_CHART).isEmpty()) {
            Console.showMessage("No Star Charts are defined.");
            return CommandResult.ERROR;
        }
        List<String> names = new ArrayList<>();
        for (int i = 0; i < arguments.count(); i++) {
            ItemDefinition definition = ItemDrops.pickChartAtTier(random, arguments.tier(), chart -> true);
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
