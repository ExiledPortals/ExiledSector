package exiledsector.harness;

import org.lwjgl.util.vector.Vector2f;

final class DragStep implements HarnessStep {

    static final int PRESS_FRAMES = 3;

    private final String target;
    private final float seconds;
    private final boolean rightButton;
    private Vector2f start;
    private float startSeconds = -1f;
    private boolean pressed;

    DragStep(String target, float seconds, String button) {
        if (!ScreenTargets.isKnown(target)) {
            throw new IllegalArgumentException("drag goes to " + ScreenTargets.DESCRIPTION + ", not \"" + target + "\"");
        }
        if (seconds < 0f) {
            throw new IllegalArgumentException("drag needs a duration of at least 0 seconds, not " + seconds);
        }
        if (!ClickStep.LEFT.equals(button) && !ClickStep.RIGHT.equals(button)) {
            throw new IllegalArgumentException("drag holds the " + ClickStep.LEFT + " or " + ClickStep.RIGHT + " button, not \"" + button + "\"");
        }
        this.target = target;
        this.seconds = seconds;
        this.rightButton = ClickStep.RIGHT.equals(button);
    }

    @Override
    public String name() {
        return (rightButton ? "right-drag" : "drag") + " to " + target + " over " + seconds + " s";
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
        if (!pressed) {
            start = HarnessMouse.position();
            HarnessMouse.press(buttonMask());
            pressed = true;
            return StepOutcome.RUNNING;
        }
        if (startSeconds < 0f) {
            if (context.frame() >= PRESS_FRAMES) {
                startSeconds = context.seconds();
            }
            return StepOutcome.RUNNING;
        }
        float progress = seconds <= 0f ? 1f : Math.min(1f, (context.seconds() - startSeconds) / seconds);
        float eased = progress * progress * (3f - 2f * progress);
        HarnessMouse.moveTo(start.x + (goal.x - start.x) * eased, start.y + (goal.y - start.y) * eased);
        if (progress < 1f) {
            return StepOutcome.RUNNING;
        }
        HarnessMouse.release(buttonMask());
        return StepOutcome.passed("dragged from " + Math.round(start.x) + ", " + Math.round(start.y) + " to " + target + " at "
                + Math.round(goal.x) + ", " + Math.round(goal.y));
    }

    private int buttonMask() {
        return rightButton ? HarnessMouse.RIGHT_BUTTON_MASK : HarnessMouse.LEFT_BUTTON_MASK;
    }

    @Override
    public float timeoutSeconds() {
        return Math.max(DEFAULT_TIMEOUT_SECONDS, seconds + DEFAULT_TIMEOUT_SECONDS);
    }
}
