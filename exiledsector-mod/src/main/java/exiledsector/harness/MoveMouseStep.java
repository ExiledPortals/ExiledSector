package exiledsector.harness;

import org.lwjgl.util.vector.Vector2f;

final class MoveMouseStep implements HarnessStep {

    static final float ARRIVAL_TOLERANCE = 3f;

    private final String target;
    private final float seconds;
    private Vector2f start;
    private float startSeconds;

    MoveMouseStep(String target, float seconds) {
        if (!ScreenTargets.isKnown(target)) {
            throw new IllegalArgumentException("moveMouse goes to " + ScreenTargets.DESCRIPTION + ", not \"" + target + "\"");
        }
        if (seconds < 0f) {
            throw new IllegalArgumentException("moveMouse needs a duration of at least 0 seconds, not " + seconds);
        }
        this.target = target;
        this.seconds = seconds;
    }

    @Override
    public String name() {
        return "moveMouse to " + target + " over " + seconds + " s";
    }

    @Override
    public StepOutcome advance(StepContext context) throws Throwable {
        if (!HarnessMouse.isCalibrated()) {
            String failure = HarnessMouse.calibrate();
            return failure == null ? new StepOutcome(false, false, "calibrating the cursor") : StepOutcome.failed(failure);
        }
        Vector2f goal = ScreenTargets.locate(target);
        if (goal == null) {
            return new StepOutcome(false, false, target + " never appeared on screen");
        }
        if (start == null) {
            start = HarnessMouse.position();
            startSeconds = context.seconds();
        }
        float progress = seconds <= 0f ? 1f : Math.min(1f, (context.seconds() - startSeconds) / seconds);
        float eased = progress * progress * (3f - 2f * progress);
        HarnessMouse.moveTo(start.x + (goal.x - start.x) * eased, start.y + (goal.y - start.y) * eased);
        Vector2f now = HarnessMouse.position();
        if (progress >= 1f && Math.abs(now.x - goal.x) <= ARRIVAL_TOLERANCE && Math.abs(now.y - goal.y) <= ARRIVAL_TOLERANCE) {
            return StepOutcome.passed("the cursor reached " + target + " at " + Math.round(goal.x) + ", " + Math.round(goal.y));
        }
        return new StepOutcome(false, false, "the cursor never settled on " + target + "; it is at " + Math.round(now.x) + ", " + Math.round(now.y));
    }

    @Override
    public float timeoutSeconds() {
        return Math.max(DEFAULT_TIMEOUT_SECONDS, seconds + DEFAULT_TIMEOUT_SECONDS);
    }
}
