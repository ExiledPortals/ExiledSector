package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.layout.ChartLayoutGenerator;
import exiledsector.charts.layout.ChartLayoutRequest;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunActions;
import exiledsector.charts.run.ChartRunActions.OpenResult;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.anomaly.breach.SpatialBreach;
import exiledsector.charts.anomaly.skirmish.FactionSkirmish;
import exiledsector.charts.anomaly.skirmish.SkirmishSide;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.items.StarChart;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChartOpenCommand extends CampaignCommand {

    static final int DEFAULT_TIER = ChartTiers.LOWEST_TIER;
    static final String BREACH_WORD = "breach";
    static final Pattern EXPLICIT_TIER = Pattern.compile("(?:tier|t)=?(-?\\d+)", Pattern.CASE_INSENSITIVE);
    static final Pattern ATLAS_SETTING = Pattern.compile("(region|layout|anomaly|side)=(\\S+)", Pattern.CASE_INSENSITIVE);

    private final Random random;

    public ChartOpenCommand() {
        this(new Random());
    }

    ChartOpenCommand(Random random) {
        this.random = random;
    }

    @Override
    protected CommandResult runInCampaign(String args) {
        OpenArguments arguments = OpenArguments.parse(args);
        if (arguments.problem() != null) {
            Console.showMessage(arguments.problem() + "\nUsage: ExiledSectorChartOpen [seed] [type] [tier] [breach], where type is one of "
                    + ChartLayoutGenerator.ALLOWED_TYPES + ", tier is " + ChartTiers.LOWEST_TIER + " to " + ChartTiers.CORRUPTED_TIER
                    + " (default " + DEFAULT_TIER + "), breach adds a Spatial Breach, region=ID and layout=ID build the system from the"
                    + " atlas tables, each anomaly=ID adds that anomaly, and side=defender|attacker|neither adds a Faction Skirmish joined on that side."
                    + " All are optional and can come in any order; write the tier as tier=N when the seed is also a"
                    + " small number.");
            return CommandResult.BAD_SYNTAX;
        }
        ChartLayoutRequest layoutRequest = new ChartLayoutRequest(arguments.seed() != null ? arguments.seed() : random.nextLong(),
                arguments.systemType(), arguments.tierOrDefault(), null);
        if (arguments.regionId() != null || arguments.layoutId() != null) {
            layoutRequest = layoutRequest.withRecipe(AtlasData.recipeFor(arguments.regionId(), arguments.layoutId()));
        }
        List<String> anomalyIds = arguments.forcedAnomalyIds();
        if (!anomalyIds.isEmpty()) {
            layoutRequest = layoutRequest.withAnomalies(anomalyIds);
        }
        layoutRequest = layoutRequest.withSkirmishSide(arguments.side());
        int entries = StarChartSettings.wormholeEntries();
        OpenResult result = ChartRunActions.openBesidePlayer(Global.getSector(), entries, random, layoutRequest);
        switch (result) {
            case OPENED -> {
                Console.showMessage("Opened a Star Chart wormhole beside your fleet with " + counted(entries, "entry", "entries") + ".");
                ChartRun run = ChartRuns.currentOrNull();
                Console.showMessage(describeLayout(run));
                Console.showMessage(describeContent(run));
                return CommandResult.SUCCESS;
            }
            default -> Console.showMessage(ChartConsoleText.openFailure(result, "Opening the wormhole failed; see starsector.log."));
        }
        return CommandResult.ERROR;
    }

    static String describeLayout(ChartRun run) {
        if (run == null || !run.hasLayout()) {
            return "The system's layout was not recorded.";
        }
        return "Layout seed " + run.layoutSeed() + ", type " + run.layoutType()
                + (run.isLayoutGenerated() ? "" : " (generation failed, so the system has a single star; see starsector.log)")
                + ". Reopen the same system with: ExiledSectorChartOpen " + run.layoutSeed()
                + (ChartLayoutGenerator.parseType(run.layoutType()) != null ? " " + run.layoutType() : " region=ID layout=ID") + " tier=" + run.tier()
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
                + (run.bossFleetId() != null ? ", a boss " + run.bossType().key() : "")
                + (run.anomalyIds().isEmpty() ? "" : ", anomalies " + String.join(", ", run.anomalyIds()))
                + ". Objective: " + objective + ".";
    }

    record OpenArguments(Long seed, StarSystemType systemType, Integer tier, String problem, boolean breach, String regionId, String layoutId,
                         List<String> anomalyIds, SkirmishSide side) {

        OpenArguments {
            anomalyIds = anomalyIds == null ? List.of() : List.copyOf(anomalyIds);
        }

        OpenArguments(Long seed, StarSystemType systemType, Integer tier, String problem, boolean breach, String regionId, String layoutId,
                      List<String> anomalyIds) {
            this(seed, systemType, tier, problem, breach, regionId, layoutId, anomalyIds, null);
        }

        OpenArguments(Long seed, StarSystemType systemType, Integer tier, String problem, boolean breach) {
            this(seed, systemType, tier, problem, breach, null, null, List.of());
        }

        OpenArguments(Long seed, StarSystemType systemType, Integer tier, String problem) {
            this(seed, systemType, tier, problem, false);
        }

        static OpenArguments parse(String args) {
            if (args == null || args.isBlank()) {
                return new OpenArguments(null, null, null, null);
            }
            String regionId = null;
            String layoutId = null;
            List<String> anomalyIds = new ArrayList<>();
            SkirmishSide side = null;
            List<String> rest = new ArrayList<>();
            for (String token : args.trim().split("\\s+")) {
                Matcher atlas = ATLAS_SETTING.matcher(token);
                if (!atlas.matches()) {
                    rest.add(token);
                } else if (atlas.group(1).equalsIgnoreCase("region")) {
                    if (regionId != null || !AtlasData.knowsRegion(atlas.group(2))) {
                        return problem("Can't use \"" + token + "\": give one region from atlas_regions.csv.");
                    }
                    regionId = atlas.group(2);
                } else if (atlas.group(1).equalsIgnoreCase("layout")) {
                    if (layoutId != null || !AtlasData.knowsLayout(atlas.group(2))) {
                        return problem("Can't use \"" + token + "\": give one layout from atlas_layouts.csv.");
                    }
                    layoutId = atlas.group(2);
                } else if (atlas.group(1).equalsIgnoreCase("side")) {
                    if (side != null || SkirmishSide.parse(atlas.group(2)) == null) {
                        return problem("Can't use \"" + token + "\": give one side, defender, attacker or neither.");
                    }
                    side = SkirmishSide.parse(atlas.group(2));
                } else {
                    if (anomalyIds.contains(atlas.group(2)) || ChartAnomalies.definition(atlas.group(2)) == null) {
                        return problem("Can't use \"" + token + "\": give an anomaly from chart_anomalies.csv, once each.");
                    }
                    anomalyIds.add(atlas.group(2));
                }
            }
            OpenArguments parsed = parseSettings(String.join(" ", rest));
            return parsed.problem() != null ? parsed
                    : new OpenArguments(parsed.seed(), parsed.systemType(), parsed.tier(), null, parsed.breach(), regionId, layoutId, anomalyIds, side);
        }

        private static OpenArguments parseSettings(String args) {
            if (args.isBlank()) {
                return new OpenArguments(null, null, null, null);
            }
            StarSystemType systemType = null;
            Integer explicitTier = null;
            boolean breach = false;
            List<Long> numbers = new ArrayList<>();
            for (String token : args.trim().split("\\s+")) {
                Matcher tierMatch = EXPLICIT_TIER.matcher(token);
                StarSystemType type = ChartLayoutGenerator.parseType(token);
                Long number = parseNumber(token);
                if (BREACH_WORD.equalsIgnoreCase(token) && !breach) {
                    breach = true;
                } else if (type != null && systemType == null) {
                    systemType = type;
                } else if (tierMatch.matches() && explicitTier == null) {
                    Long tierNumber = parseNumber(tierMatch.group(1));
                    if (tierNumber == null || !ChartTiers.isValid(tierNumber.intValue()) || tierNumber.intValue() != tierNumber) {
                        return problem("The tier in \"" + token + "\" must be " + ChartTiers.LOWEST_TIER + " to " + ChartTiers.CORRUPTED_TIER + ".");
                    }
                    explicitTier = tierNumber.intValue();
                } else if (number != null) {
                    numbers.add(number);
                } else {
                    return problem("Can't use \"" + token + "\": give at most one seed, one system type, one tier and one breach.");
                }
            }
            OpenArguments assigned = assign(numbers, systemType, explicitTier);
            return !breach || assigned.problem() != null ? assigned
                    : new OpenArguments(assigned.seed(), assigned.systemType(), assigned.tier(), null, true);
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
            return problem("Neither " + first + " nor " + second + " is a tier from " + ChartTiers.LOWEST_TIER + " to " + ChartTiers.CORRUPTED_TIER + ".");
        }

        private static boolean isTier(long number) {
            return number >= ChartTiers.LOWEST_TIER && number <= ChartTiers.CORRUPTED_TIER;
        }

        private static OpenArguments problem(String problem) {
            return new OpenArguments(null, null, null, problem);
        }

        int tierOrDefault() {
            return tier != null ? tier : DEFAULT_TIER;
        }

        List<String> forcedAnomalyIds() {
            List<String> ids = new ArrayList<>(anomalyIds);
            if (breach && !ids.contains(SpatialBreach.ANOMALY_ID)) {
                ids.add(SpatialBreach.ANOMALY_ID);
            }
            if (side != null && !ids.contains(FactionSkirmish.ANOMALY_ID)) {
                ids.add(FactionSkirmish.ANOMALY_ID);
            }
            return List.copyOf(ids);
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
