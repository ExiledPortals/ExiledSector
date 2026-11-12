package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.TerritoryContext;
import exiledsector.charts.officer.turn.OfficerTurns;
import exiledsector.charts.officer.turn.TurnCause;
import exiledsector.charts.officer.turn.TurnResult;
import org.lazywizard.console.Console;

public class AtlasTurnCommand extends CampaignCommand {

    @Override
    protected CommandResult runInCampaign(String args) {
        SectorAPI sector = Global.getSector();
        AtlasState state = AtlasState.get(sector);
        TurnResult result = OfficerTurns.take(AtlasRuntime.generator(sector), state, TurnCause.forced(), TerritoryContext.current());
        Console.showMessage(AtlasLastTurnCommand.describe(state, result) + "\n" + AtlasIntentsCommand.describe(state));
        return CommandResult.SUCCESS;
    }
}
