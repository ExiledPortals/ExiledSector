package exiledsector.harness;

final class ClickStep implements HarnessStep {

    static final int HOLD_FRAMES = 3;
    static final int CONTROL_LEAD_FRAMES = 2;
    static final String LEFT = "left";
    static final String RIGHT = "right";

    private final boolean rightButton;
    private final boolean holdingControl;

    ClickStep(String button, boolean holdingControl) {
        if (!LEFT.equals(button) && !RIGHT.equals(button)) {
            throw new IllegalArgumentException("click presses the " + LEFT + " or " + RIGHT + " button, not \"" + button + "\"");
        }
        this.rightButton = RIGHT.equals(button);
        this.holdingControl = holdingControl;
    }

    @Override
    public String name() {
        String click = holdingControl ? "ctrl-click" : "click";
        return rightButton ? click + " " + RIGHT : click;
    }

    @Override
    public StepOutcome advance(StepContext context) throws Throwable {
        int pressFrame = holdingControl ? CONTROL_LEAD_FRAMES : 0;
        int releaseFrame = pressFrame + HOLD_FRAMES;
        int frame = context.frame();
        if (holdingControl && frame == 0) {
            HarnessMouse.pressKey(HarnessMouse.CONTROL_KEY);
        }
        if (frame == pressFrame) {
            HarnessMouse.press(buttonMask());
        }
        if (frame < releaseFrame) {
            return StepOutcome.RUNNING;
        }
        HarnessMouse.release(buttonMask());
        if (holdingControl) {
            HarnessMouse.releaseKey(HarnessMouse.CONTROL_KEY);
        }
        return StepOutcome.passed(name() + " at " + Math.round(HarnessMouse.position().x) + ", " + Math.round(HarnessMouse.position().y));
    }

    private int buttonMask() {
        return rightButton ? HarnessMouse.RIGHT_BUTTON_MASK : HarnessMouse.LEFT_BUTTON_MASK;
    }
}
