package exiledsector.console;

import com.fs.starfarer.api.Global;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.turn.Intent;
import exiledsector.charts.officer.turn.OfficerTurns;
import org.lazywizard.console.Console;

import java.util.List;

public class AtlasIntentsCommand extends CampaignCommand {

    @Override
    protected CommandResult runInCampaign(String args) {
        Console.showMessage(describe(AtlasState.get(Global.getSector())));
        return CommandResult.SUCCESS;
    }

    static String describe(AtlasState state) {
        List<Intent> intents = OfficerTurns.declaredIntents(state);
        StringBuilder text = new StringBuilder("Intents declared for the next officer turn: " + intents.size());
        for (Intent intent : intents) {
            text.append("\n").append(officer(state, intent.officerRecordId())).append(" ").append(intent.kind().id());
            if (intent.target() != null) {
                text.append(" ").append(AtlasCommandText.cell(intent.target()));
            }
            if (intent.targetRecordId() != Intent.NO_OFFICER) {
                text.append(" against ").append(officer(state, intent.targetRecordId()));
            }
        }
        return text.toString();
    }

    static String officer(AtlasState state, int recordId) {
        if (recordId == Intent.NO_OFFICER) {
            return "the player";
        }
        AtlasOfficer officer = state.officer(recordId);
        return officer == null ? "#" + recordId : "#" + recordId + " " + officer.factionId() + " " + officer.rankId();
    }
}
