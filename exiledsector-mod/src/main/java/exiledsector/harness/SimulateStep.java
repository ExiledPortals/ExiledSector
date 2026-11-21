package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

final class SimulateStep implements HarnessStep {

    static final float TIMEOUT_SECONDS = 60f;
    static final int OPPONENT_PICKER_WAIT_FRAMES = 30;

    private final String shipSelector;
    private FleetMemberAPI member;
    private boolean simulationRequested;
    private int simulationFrames;
    private boolean opponentPickerHandled;
    private boolean opponentPickerClosed;

    SimulateStep(String shipSelector) {
        this.shipSelector = shipSelector;
    }

    @Override
    public String name() {
        return "simulate " + shipSelector;
    }

    @Override
    public StepOutcome advance(StepContext context) throws Throwable {
        if (context.firstFrame()) {
            member = OpenSkillTreeStep.member(Global.getSector().getPlayerFleet(), shipSelector);
            if (member == null) {
                return StepOutcome.failed("no fleet member matches \"" + shipSelector + "\"; use flagship, a fleet index, a hull id or a ship name");
            }
            Global.getSector().getCampaignUI().showCoreUITab(CoreUITabId.REFIT, member);
            return StepOutcome.RUNNING;
        }
        if (!simulationRequested) {
            simulationRequested = SimulatorAccess.pressRunSimulation();
            return new StepOutcome(false, false, "the refit screen never showed a Run simulation button for " + member.getShipName());
        }
        CombatEngineAPI engine = SimulatorAccess.simulation();
        if (engine == null) {
            return new StepOutcome(false, false, "the simulator never started for " + member.getShipName());
        }
        simulationFrames++;
        if (!opponentPickerHandled) {
            opponentPickerClosed = SimulatorAccess.closeOpponentPicker();
            opponentPickerHandled = opponentPickerClosed || simulationFrames > OPPONENT_PICKER_WAIT_FRAMES;
            return StepOutcome.RUNNING;
        }
        if (SimulatorAccess.showingDialog(engine)) {
            return new StepOutcome(false, false, "a dialog stayed open over the simulator");
        }
        ShipAPI playerShip = engine.getPlayerShip();
        if (playerShip == null) {
            return new StepOutcome(false, false, "the simulator has no player ship");
        }
        if (SimulatorAccess.showingMap(engine) || engine.isPaused()) {
            SimulatorAccess.closeMapAndUnpause(engine);
            return new StepOutcome(false, false, "the simulator stayed paused or on the map");
        }
        return StepOutcome.passed("simulating " + CombatShips.describe(playerShip)
                + (opponentPickerClosed ? " after closing the opponent picker" : " with no opponent picker shown") + ", map closed and unpaused");
    }

    @Override
    public float timeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
}
