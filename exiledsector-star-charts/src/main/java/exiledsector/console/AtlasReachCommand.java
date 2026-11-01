package exiledsector.console;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasDevTools;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;
import org.lazywizard.console.Console;

import java.util.List;

public class AtlasReachCommand extends AtlasTargetCommand {

    @Override
    protected String usage() {
        return "ExiledSectorAtlasReach <x> <y> | <site id>, for example ExiledSectorAtlasReach 4 -7 or ExiledSectorAtlasReach remnant_nexus.";
    }

    @Override
    protected CommandResult runAt(SectorAPI sector, AtlasGenerator generator, AtlasState state, AtlasCell cell, List<String> rest) {
        if (!rest.isEmpty()) {
            return badSyntax(rest);
        }
        if (state.isCompleted(cell)) {
            Console.showMessage(AtlasCommandText.cell(cell) + " is already completed.");
            return CommandResult.SUCCESS;
        }
        List<AtlasCell> path = AtlasDevTools.reach(generator, state, cell);
        if (path == null) {
            Console.showMessage("No lane path reaches " + AtlasCommandText.cell(cell) + " without passing through another special site.");
            return CommandResult.ERROR;
        }
        Console.showMessage(path.isEmpty() ? AtlasCommandText.cell(cell) + " was already reachable."
                : "Completed " + counted(path.size(), "system", "systems") + " on the way, so " + AtlasCommandText.cell(cell)
                + " is now reachable: " + AtlasCommandText.node(generator, cell, state));
        return CommandResult.SUCCESS;
    }
}
