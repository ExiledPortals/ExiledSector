package exiledsector.harness;

final class ClickStep implements HarnessStep {

    static final int HOLD_FRAMES = 3;

    @Override
    public String name() {
        return "click";
    }

    @Override
    public StepOutcome advance(StepContext context) throws Throwable {
        if (context.firstFrame()) {
            HarnessMouse.pressLeft();
            return StepOutcome.RUNNING;
        }
        if (context.frame() < HOLD_FRAMES) {
            return StepOutcome.RUNNING;
        }
        HarnessMouse.releaseLeft();
        return StepOutcome.passed("clicked the left button at " + Math.round(HarnessMouse.position().x) + ", " + Math.round(HarnessMouse.position().y));
    }
}
