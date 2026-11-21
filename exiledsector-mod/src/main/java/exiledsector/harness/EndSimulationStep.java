package exiledsector.harness;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.combat.CombatEngineAPI;

import java.util.Locale;

final class EndSimulationStep implements HarnessStep {

    @Override
    public String name() {
        return "endSimulation";
    }

    @Override
    public StepOutcome advance(StepContext context) throws Throwable {
        if (context.firstFrame()) {
            CombatEngineAPI engine = SimulatorAccess.simulation();
            if (engine == null) {
                return StepOutcome.failed("no simulation is running");
            }
            engine.getTimeMult().unmodify(SpeedStep.TIME_SOURCE);
            if (!SimulatorAccess.leaveCombat(engine)) {
                return StepOutcome.failed("the simulator has no state to return to");
            }
            return StepOutcome.RUNNING;
        }
        if (Global.getCurrentState() == GameState.CAMPAIGN) {
            CoreUITabId tab = Global.getSector().getCampaignUI().getCurrentCoreTab();
            return StepOutcome.passed("back in the campaign" + (tab == null ? "" : " on the " + tab.name().toLowerCase(Locale.ROOT) + " screen"));
        }
        return new StepOutcome(false, false, "the simulator never closed");
    }
}
