package exiledsector.console;

import com.fs.starfarer.api.Global;
import exiledsector.charts.atlas.AtlasState;
import org.lazywizard.console.Console;

public class AtlasGrantCommand extends CampaignCommand {

    static final int DEFAULT_POINTS = 1;
    static final int MAX_GRANT = 100;

    record GrantArguments(int points, int fragments, String problem) {

        static GrantArguments parse(String args) {
            String[] tokens = args == null || args.isBlank() ? new String[0] : args.trim().split("\\s+");
            if (tokens.length > 2) {
                return new GrantArguments(0, 0, "Give at most a number of atlas points and a number of key fragments.");
            }
            Integer points = tokens.length > 0 ? ConsoleArgs.wholeNumber(tokens[0], 0, MAX_GRANT) : Integer.valueOf(DEFAULT_POINTS);
            Integer fragments = tokens.length > 1 ? ConsoleArgs.wholeNumber(tokens[1], 0, MAX_GRANT) : Integer.valueOf(0);
            if (points == null || fragments == null) {
                return new GrantArguments(0, 0, "Points and fragments must be whole numbers from 0 to " + MAX_GRANT + ".");
            }
            return new GrantArguments(points, fragments, null);
        }
    }

    @Override
    protected CommandResult runInCampaign(String args) {
        GrantArguments arguments = GrantArguments.parse(args);
        if (arguments.problem() != null) {
            Console.showMessage(arguments.problem() + "\nUsage: ExiledSectorAtlasGrant [points] [fragments], points default " + DEFAULT_POINTS
                    + ", fragments default 0.");
            return CommandResult.BAD_SYNTAX;
        }
        AtlasState state = AtlasState.get(Global.getSector());
        state.addAtlasPoints(arguments.points());
        for (int fragment = 0; fragment < arguments.fragments(); fragment++) {
            state.addKeyFragment();
        }
        Console.showMessage("Atlas points now " + state.atlasPoints() + ", key fragments now " + state.keyFragments() + ".");
        return CommandResult.SUCCESS;
    }
}
