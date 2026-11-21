package exiledsector.harness;

import com.fs.starfarer.api.combat.CombatEngineAPI;

final class AutopilotStep implements HarnessStep {

    private final boolean on;

    AutopilotStep(boolean on) {
        this.on = on;
    }

    @Override
    public String name() {
        return "autopilot " + (on ? "on" : "off");
    }

    @Override
    public StepOutcome advance(StepContext context) throws Throwable {
        CombatEngineAPI engine = SimulatorAccess.combat();
        if (engine == null) {
            return StepOutcome.failed("autopilot only works in combat; run a simulate step first");
        }
        if (engine.getPlayerShip() == null) {
            return new StepOutcome(false, false, "the combat has no player ship yet");
        }
        if (!SimulatorAccess.setAutopilot(engine, on)) {
            return StepOutcome.failed("the combat screen has no autopilot switch");
        }
        boolean nowOn = engine.getCombatUI().isAutopilotOn();
        String detail = "autopilot is " + (nowOn ? "on" : "off") + " for " + CombatShips.describe(engine.getPlayerShip());
        return nowOn == on ? StepOutcome.passed(detail) : StepOutcome.failed(detail);
    }
}
