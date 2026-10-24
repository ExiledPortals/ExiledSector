package exiledsector.console;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasDevTools;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;
import org.lazywizard.console.Console;

import java.util.List;

public class AtlasFailCommand extends AtlasTargetCommand {

    @Override
    protected String usage() {
        return "ExiledSectorAtlasFail <x> <y> | <site id>.";
    }

    @Override
    protected CommandResult runAt(SectorAPI sector, AtlasGenerator generator, AtlasState state, AtlasCell cell, List<String> rest) {
        if (!rest.isEmpty()) {
            Console.showMessage("Can't use \"" + String.join(" ", rest) + "\".\nUsage: " + usage());
            return CommandResult.BAD_SYNTAX;
        }
        if (state.isCompleted(cell)) {
            Console.showMessage(AtlasCommandText.cell(cell) + " is already completed.");
            return CommandResult.ERROR;
        }
        AtlasDevTools.fail(state, cell);
        Console.showMessage("Marked " + AtlasCommandText.node(generator.node(cell), state) + " as failed.");
        return CommandResult.SUCCESS;
    }
}
