package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.ChartLayoutGenerator;
import exiledsector.charts.ChartLayoutRequest;
import exiledsector.charts.ChartRun;
import exiledsector.charts.ChartRunActions;
import exiledsector.charts.ChartRunActions.OpenResult;
import exiledsector.charts.ChartRuns;
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
        OpenArguments arguments = OpenArguments.parse(args);
        if (arguments.problem() != null) {
            Console.showMessage(arguments.problem() + "\nUsage: ExiledSectorChartOpen [seed] [type], where type is one of "
                    + ChartLayoutGenerator.ALLOWED_TYPES + ". Both are optional.");
            return CommandResult.BAD_SYNTAX;
        }
        ChartLayoutRequest layoutRequest = new ChartLayoutRequest(arguments.seed() != null ? arguments.seed() : random.nextLong(),
                arguments.systemType());
        int entries = StarChartSettings.wormholeEntries();
        OpenResult result = ChartRunActions.openBesidePlayer(Global.getSector(), entries, random, layoutRequest);
        switch (result) {
            case OPENED -> {
                Console.showMessage("Opened a Star Chart wormhole beside your fleet with " + entries + (entries == 1 ? " entry." : " entries."));
                Console.showMessage(describeLayout(ChartRuns.currentOrNull()));
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

    static String describeLayout(ChartRun run) {
        if (run == null || !run.hasLayout()) {
            return "The pocket's layout was not recorded.";
        }
        return "Layout seed " + run.layoutSeed() + ", type " + run.layoutType()
                + (run.isLayoutGenerated() ? "" : " (generation failed, so the pocket has a single star; see starsector.log)")
                + ". Reopen the same layout with: ExiledSectorChartOpen " + run.layoutSeed() + " " + run.layoutType();
    }

    record OpenArguments(Long seed, StarSystemType systemType, String problem) {

        static OpenArguments parse(String args) {
            Long seed = null;
            StarSystemType systemType = null;
            if (args == null || args.isBlank()) {
                return new OpenArguments(null, null, null);
            }
            for (String token : args.trim().split("\\s+")) {
                Long number = parseSeed(token);
                StarSystemType type = ChartLayoutGenerator.parseType(token);
                if (number != null && seed == null) {
                    seed = number;
                } else if (type != null && systemType == null) {
                    systemType = type;
                } else {
                    return new OpenArguments(null, null, "Can't use \"" + token + "\": give at most one seed (a whole number) and one system type.");
                }
            }
            return new OpenArguments(seed, systemType, null);
        }

        private static Long parseSeed(String token) {
            try {
                return Long.parseLong(token);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
