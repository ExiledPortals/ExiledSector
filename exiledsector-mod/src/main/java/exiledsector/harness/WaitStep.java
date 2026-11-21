package exiledsector.harness;

final class WaitStep implements HarnessStep {

    private final int frames;
    private final float seconds;

    WaitStep(int frames, float seconds) {
        this.frames = frames;
        this.seconds = seconds;
    }

    @Override
    public String name() {
        return "wait " + (frames > 0 ? frames + " frames" : "") + (frames > 0 && seconds > 0f ? " and " : "") + (seconds > 0f ? seconds + " s" : "");
    }

    @Override
    public StepOutcome advance(StepContext context) {
        if (context.frame() >= frames && context.seconds() >= seconds) {
            return StepOutcome.passed("waited " + context.frame() + " frames over " + Math.round(context.seconds() * 1000f) + " ms");
        }
        return StepOutcome.RUNNING;
    }

    @Override
    public float timeoutSeconds() {
        return Math.max(DEFAULT_TIMEOUT_SECONDS, seconds + DEFAULT_TIMEOUT_SECONDS);
    }
}
