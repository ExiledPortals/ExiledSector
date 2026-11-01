package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasDevTools;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasSpecial;
import exiledsector.charts.atlas.AtlasState;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;

abstract class AtlasTargetCommand extends CampaignCommand {

    record Target(AtlasCell cell, String siteId, List<String> rest, String problem) {

        static Target parse(String args) {
            List<String> tokens = ConsoleArgs.tokens(args);
            if (tokens.isEmpty()) {
                return problem("Give a system as two whole numbers (x y) or a special site id.");
            }
            if (tokens.size() >= 2 && isWholeNumber(tokens.get(0)) && isWholeNumber(tokens.get(1))) {
                AtlasCell cell = new AtlasCell(Integer.parseInt(tokens.get(0)), Integer.parseInt(tokens.get(1)));
                return new Target(cell, null, List.copyOf(tokens.subList(2, tokens.size())), null);
            }
            if (isWholeNumber(tokens.get(0))) {
                return problem("Give both coordinates of the system (x y).");
            }
            return new Target(null, tokens.get(0), List.copyOf(tokens.subList(1, tokens.size())), null);
        }

        private static boolean isWholeNumber(String token) {
            return token.matches("-?\\d{1,6}");
        }

        private static Target problem(String problem) {
            return new Target(null, null, List.of(), problem);
        }
    }

    @Override
    protected final CommandResult runInCampaign(String args) {
        Target target = Target.parse(args);
        if (target.problem() != null) {
            return badSyntax(target.problem());
        }
        SectorAPI sector = Global.getSector();
        AtlasState state = AtlasState.get(sector);
        AtlasGenerator generator = AtlasRuntime.generator(sector);
        AtlasCell cell = target.cell();
        if (cell == null) {
            cell = AtlasDevTools.nearestOpenSite(generator, state, target.siteId());
            if (cell == null) {
                Console.showMessage("No open " + target.siteId() + " within " + AtlasDevTools.SITE_SEARCH_RADIUS
                        + " cells of the Gateway. Special site ids: " + String.join(", ", siteIds()) + ".");
                return CommandResult.ERROR;
            }
        }
        if (generator.isVoid(cell)) {
            Console.showMessage(AtlasCommandText.cell(cell) + " is empty space, not a system.");
            return CommandResult.ERROR;
        }
        return runAt(sector, generator, state, cell, target.rest());
    }

    protected abstract String usage();

    protected final CommandResult badSyntax(List<String> rest) {
        return badSyntax("Can't use \"" + String.join(" ", rest) + "\".");
    }

    protected final CommandResult badSyntax(String problem) {
        Console.showMessage(problem + "\nUsage: " + usage());
        return CommandResult.BAD_SYNTAX;
    }

    protected abstract CommandResult runAt(SectorAPI sector, AtlasGenerator generator, AtlasState state, AtlasCell cell, List<String> rest);

    private static List<String> siteIds() {
        List<String> ids = new ArrayList<>();
        for (AtlasSpecial special : AtlasData.rules().specials()) {
            ids.add(special.id());
        }
        return ids;
    }
}
