package exiledsector.console;

import com.fs.starfarer.api.Global;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.atlas.AtlasDevTools;
import exiledsector.charts.atlas.AtlasState;
import org.lazywizard.console.Console;

public class AtlasResetCommand extends CampaignCommand {

    static final String SAME_WORD = "same";

    record ResetArguments(Long seed, boolean sameSeed, String problem) {

        static ResetArguments parse(String args) {
            String text = args == null ? "" : args.trim();
            if (text.isEmpty()) {
                return new ResetArguments(null, false, null);
            }
            if (SAME_WORD.equalsIgnoreCase(text)) {
                return new ResetArguments(null, true, null);
            }
            try {
                return new ResetArguments(Long.parseLong(text), false, null);
            } catch (NumberFormatException e) {
                return new ResetArguments(null, false, "Can't use \"" + text + "\": give a whole number seed or the word " + SAME_WORD + ".");
            }
        }
    }

    @Override
    protected CommandResult runInCampaign(String args) {
        ResetArguments arguments = ResetArguments.parse(args);
        if (arguments.problem() != null) {
            Console.showMessage(arguments.problem() + "\nUsage: ExiledSectorAtlasReset [seed | " + SAME_WORD + "].");
            return CommandResult.BAD_SYNTAX;
        }
        if (ChartRuns.hasRun()) {
            Console.showMessage("A Star Chart run is open. Finish or collapse it before resetting the atlas (see ExiledSectorChartStatus).");
            return CommandResult.ERROR;
        }
        Long seed = arguments.sameSeed() ? Long.valueOf(AtlasState.get(Global.getSector()).seed()) : arguments.seed();
        AtlasState state = AtlasDevTools.reset(Global.getSector(), seed);
        Console.showMessage("Reset the atlas with seed " + state.seed() + ". All progress, points and key fragments are gone.");
        return CommandResult.SUCCESS;
    }
}
