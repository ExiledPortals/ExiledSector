package exiledsector.console;

import com.fs.starfarer.api.Global;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.AtlasOfficer;
import org.lazywizard.console.Console;

public class AtlasTerritoriesCommand extends CampaignCommand {

    @Override
    protected CommandResult runInCampaign(String args) {
        Console.showMessage(describe(AtlasState.get(Global.getSector())));
        return CommandResult.SUCCESS;
    }

    static String describe(AtlasState state) {
        StringBuilder text = new StringBuilder();
        int waiting = 0;
        int fled = 0;
        for (AtlasOfficer officer : state.officers()) {
            if (officer.isActive()) {
                text.append("\n#").append(officer.recordId()).append(" ").append(officer.factionId()).append(" ").append(officer.rankId())
                        .append(" at ").append(AtlasCommandText.cell(officer.commandCentre())).append(", ")
                        .append(state.claimsOf(officer.commandCentre()).size()).append(" systems");
            } else if (officer.hasFled()) {
                fled++;
            } else {
                waiting++;
            }
        }
        String header = "Officers: " + state.officers().size() + " on the board, " + fled + " fled, " + waiting + " never placed.";
        return text.isEmpty() ? header + "\nNo territories yet." : header + text;
    }
}
