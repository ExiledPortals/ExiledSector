package exiledsector.harness;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;

final class SaveGameStep implements HarnessStep {

    @Override
    public String name() {
        return "saveGame";
    }

    @Override
    public StepOutcome advance(StepContext context) {
        if (Global.getCurrentState() != GameState.CAMPAIGN) {
            return StepOutcome.failed("saveGame only works in the campaign");
        }
        if (!Global.getSector().getCampaignUI().cmdSave()) {
            return StepOutcome.failed("the campaign refused to save; the game log has its message");
        }
        return StepOutcome.passed("saved " + Global.getSector().getPlayerPerson().getNameString() + "'s game");
    }
}
