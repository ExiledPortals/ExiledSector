package exiledsector.console;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTerritoryTransfer;
import org.lazywizard.console.Console;

import java.util.List;

public class AtlasClaimCommand extends AtlasTargetCommand {

    static final String RELEASE = "release";

    @Override
    protected String usage() {
        return "ExiledSectorAtlasClaim <x> <y> | <site id> [<faction id> | release].";
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
            Console.showMessage("The Wormhole Gateway can't be claimed.");
            return CommandResult.ERROR;
        }
        String argument = rest.get(0);
        if (argument.equalsIgnoreCase(RELEASE)) {
            return release(generator, state, cell);
        }
        if (sector.getFaction(argument) == null) {
            Console.showMessage("No faction with the id " + argument + ".");
            return CommandResult.ERROR;
        }
        if (state.officerAt(cell) != null) {
            Console.showMessage(AtlasCommandText.cell(cell) + " is a command centre. Release it first.");
            return CommandResult.ERROR;
        }
        if (!AtlasTerritoryTransfer.neighbouringCentres(generator, state, cell).containsKey(argument)) {
            Console.showMessage("No " + argument + " territory borders " + AtlasCommandText.cell(cell) + ".");
            return CommandResult.ERROR;
        }
        AtlasTerritoryTransfer.transfer(generator, state, cell, argument);
        Console.showMessage("Claimed for " + argument + ".\n" + AtlasCommandText.node(generator, cell, state));
        return CommandResult.SUCCESS;
    }

    private static CommandResult release(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        if (!state.isClaimed(cell)) {
            Console.showMessage(AtlasCommandText.cell(cell) + " isn't claimed.");
            return CommandResult.ERROR;
        }
        boolean commandCentre = state.officerAt(cell) != null;
        AtlasTerritoryTransfer.vacate(generator, state, cell);
        Console.showMessage((commandCentre ? "Released, and its territory broke up.\n" : "Released.\n") + AtlasCommandText.node(generator, cell, state));
        return CommandResult.SUCCESS;
    }
}
