package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.ChartGenContext;
import exiledsector.charts.ChartLayoutGenerator;
import exiledsector.charts.ChartLayoutRequest;
import exiledsector.charts.ChartRun;
import exiledsector.charts.ChartRunActions;
import exiledsector.charts.ChartRunActions.OpenResult;
import exiledsector.charts.ChartRunEffects;
import exiledsector.charts.ChartRuns;
import exiledsector.charts.ChartTiers;
import exiledsector.charts.StarChartSettings;
import exiledsector.items.StarChart;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChartOpenCommand implements BaseCommand {

    static final int DEFAULT_TIER = ChartTiers.LOWEST_TIER;
    static final Pattern EXPLICIT_TIER = Pattern.compile("(?:tier|t)=?(-?\\d+)", Pattern.CASE_INSENSITIVE);

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
        OpenArguments arguments = OpenArguments.parse(args);
        if (arguments.problem() != null) {
            Console.showMessage(arguments.problem() + "\nUsage: ExiledSectorChartOpen [seed] [type] [tier], where type is one of "
                    + ChartLayoutGenerator.ALLOWED_TYPES + " and tier is " + ChartTiers.LOWEST_TIER + " to " + ChartTiers.HIGHEST_TIER
                    + " (default " + DEFAULT_TIER + "). All are optional and can come in any order; write the tier as tier=N when the"
                    + " seed is also a small number.");
            return CommandResult.BAD_SYNTAX;
        }
        ChartLayoutRequest layoutRequest = new ChartLayoutRequest(arguments.seed() != null ? arguments.seed() : random.nextLong(),
                arguments.systemType(), arguments.tierOrDefault());
        int entries = StarChartSettings.wormholeEntries();
        OpenResult result = ChartRunActions.openBesidePlayer(Global.getSector(), entries, random, layoutRequest);
        switch (result) {
            case OPENED -> {
                Console.showMessage("Opened a Star Chart wormhole beside your fleet with " + entries + (entries == 1 ? " entry." : " entries."));
                ChartRun run = ChartRuns.currentOrNull();
                Console.showMessage(describeLayout(run));
                Console.showMessage(describeContent(run));
                return CommandResult.SUCCESS;
            }
            case RUN_EXISTS -> Console.showMessage("A Star Chart run is already open. Collapse it or finish it first (see ExiledSectorChartStatus).");
            case NO_PLAYER_FLEET -> Console.showMessage("No player fleet found.");
            case PLAYER_IN_TRANSITION -> Console.showMessage("Your fleet is jumping. Try again once it arrives.");
            case PLAYER_IN_SYSTEM -> Console.showMessage("Your fleet is inside a Star Chart system.");
            default -> Console.showMessage("Opening the wormhole failed; see starsector.log.");
        }
        return CommandResult.ERROR;
    }

    static String describeLayout(ChartRun run) {
        if (run == null || !run.hasLayout()) {
            return "The system's layout was not recorded.";
        }
        return "Layout seed " + run.layoutSeed() + ", type " + run.layoutType()
                + (run.isLayoutGenerated() ? "" : " (generation failed, so the system has a single star; see starsector.log)")
                + ". Reopen the same system with: ExiledSectorChartOpen " + run.layoutSeed() + " " + run.layoutType() + " tier=" + run.tier()
                + (run.hasChart() ? " (that leaves out the chart's modifiers)" : "");
    }

    static String describeChart(ChartRun run) {
        if (run == null || !run.hasChart()) {
            return "Chart: none (opened by ExiledSectorChartOpen, so no modifiers apply).";
        }
        StarChart chart = ChartRunEffects.chartOf(run);
        if (chart == null) {
            return "Chart: its data could not be read (" + run.chartData() + ").";
        }
        return "Chart: " + ChartListCommand.describe(chart) + "\nChart effects: " + ChartGenContext.from(chart).describe() + ".";
    }

    static String describeContent(ChartRun run) {
        if (run == null) {
            return "The system's content was not recorded.";
        }
        String objective = run.objective() == null ? "none (no target could be placed; see starsector.log)"
                : run.objective().name() + " (" + run.objectiveTargetIds().size() + " target(s))";
        return "Tier " + run.tier() + ", held by " + (run.contentFaction() == null ? "nobody" : run.contentFaction()) + ": "
                + run.contentFleets() + " fleet(s), " + run.contentPois() + " point(s) of interest"
                + (run.bossFleetId() != null ? ", a boss fleet" : "")
                + ". Objective: " + objective + ".";
    }

    record OpenArguments(Long seed, StarSystemType systemType, Integer tier, String problem) {

        static OpenArguments parse(String args) {
            if (args == null || args.isBlank()) {
                return new OpenArguments(null, null, null, null);
            }
            StarSystemType systemType = null;
            Integer explicitTier = null;
            List<Long> numbers = new ArrayList<>();
            for (String token : args.trim().split("\\s+")) {
                Matcher tierMatch = EXPLICIT_TIER.matcher(token);
                StarSystemType type = ChartLayoutGenerator.parseType(token);
                Long number = parseNumber(token);
                if (type != null && systemType == null) {
                    systemType = type;
                } else if (tierMatch.matches() && explicitTier == null) {
                    Long tierNumber = parseNumber(tierMatch.group(1));
                    if (tierNumber == null || !ChartTiers.isValid(tierNumber.intValue()) || tierNumber.intValue() != tierNumber) {
                        return problem("The tier in \"" + token + "\" must be " + ChartTiers.LOWEST_TIER + " to " + ChartTiers.HIGHEST_TIER + ".");
                    }
                    explicitTier = tierNumber.intValue();
                } else if (number != null) {
                    numbers.add(number);
                } else {
                    return problem("Can't use \"" + token + "\": give at most one seed, one system type and one tier.");
                }
            }
            return assign(numbers, systemType, explicitTier);
        }

        private static OpenArguments assign(List<Long> numbers, StarSystemType systemType, Integer explicitTier) {
            if (numbers.size() > 2 || explicitTier != null && numbers.size() > 1) {
                return problem("Too many numbers: give at most one seed and one tier.");
            }
            if (numbers.isEmpty()) {
                return new OpenArguments(null, systemType, explicitTier, null);
            }
            if (numbers.size() == 1) {
                return new OpenArguments(numbers.get(0), systemType, explicitTier, null);
            }
            long first = numbers.get(0);
            long second = numbers.get(1);
            if (isTier(second)) {
                return new OpenArguments(first, systemType, (int) second, null);
            }
            if (isTier(first)) {
                return new OpenArguments(second, systemType, (int) first, null);
            }
            return problem("Neither " + first + " nor " + second + " is a tier from " + ChartTiers.LOWEST_TIER + " to " + ChartTiers.HIGHEST_TIER + ".");
        }

        private static boolean isTier(long number) {
            return number >= ChartTiers.LOWEST_TIER && number <= ChartTiers.HIGHEST_TIER;
        }

        private static OpenArguments problem(String problem) {
            return new OpenArguments(null, null, null, problem);
        }

        int tierOrDefault() {
            return tier != null ? tier : DEFAULT_TIER;
        }

        private static Long parseNumber(String token) {
            try {
                return Long.parseLong(token.toLowerCase(Locale.ROOT));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
