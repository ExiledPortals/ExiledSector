package exiledsector.harness;

import com.fs.starfarer.api.combat.CombatEngineAPI;

final class SpeedStep implements HarnessStep {

    static final String TIME_SOURCE = "exiledSector_harness";

    private final float timeMult;

    SpeedStep(float timeMult) {
        if (timeMult <= 0f) {
            throw new IllegalArgumentException("speed needs a mult above 0, not " + timeMult);
        }
        this.timeMult = timeMult;
    }

    @Override
    public String name() {
        return "speed x" + timeMult;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        CombatEngineAPI engine = SimulatorAccess.combat();
        if (engine == null) {
            return StepOutcome.failed("speed only works in combat; run a simulate step first");
        }
        if (timeMult == 1f) {
            engine.getTimeMult().unmodify(TIME_SOURCE);
        } else {
            engine.getTimeMult().modifyMult(TIME_SOURCE, timeMult);
        }
        return StepOutcome.passed("combat time runs at x" + engine.getTimeMult().getModifiedValue());
    }
}
