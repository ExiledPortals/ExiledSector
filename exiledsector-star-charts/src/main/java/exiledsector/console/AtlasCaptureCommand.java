package exiledsector.console;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;
import org.lazywizard.console.Console;

import java.util.List;

public class AtlasCaptureCommand extends AtlasTargetCommand {

    static final String RELEASE = "release";

    @Override
    protected String usage() {
        return "ExiledSectorAtlasCapture <x> <y> | <site id> [<faction id> | release].";
    }

    @Override
    protected CommandResult runAt(SectorAPI sector, AtlasGenerator generator, AtlasState state, AtlasCell cell, List<String> rest) {
        if (rest.isEmpty()) {
            Console.showMessage(AtlasCommandText.node(generator, cell, state));
            return CommandResult.SUCCESS;
        }
        if (rest.size() > 1) {
            return badSyntax(rest);
        }
        if (cell.equals(AtlasCell.ORIGIN)) {
            Console.showMessage("The Wormhole Gateway can't be captured.");
            return CommandResult.ERROR;
        }
        String argument = rest.get(0);
        if (argument.equalsIgnoreCase(RELEASE)) {
            if (!state.release(cell)) {
                Console.showMessage(AtlasCommandText.cell(cell) + " isn't captured.");
                return CommandResult.ERROR;
            }
            Console.showMessage("Released.\n" + AtlasCommandText.node(generator, cell, state));
            return CommandResult.SUCCESS;
        }
        if (sector.getFaction(argument) == null) {
            Console.showMessage("No faction with the id " + argument + ".");
            return CommandResult.ERROR;
        }
        state.capture(generator, cell, argument);
        Console.showMessage("Captured for " + argument + ".\n" + AtlasCommandText.node(generator, cell, state));
        return CommandResult.SUCCESS;
    }
}
