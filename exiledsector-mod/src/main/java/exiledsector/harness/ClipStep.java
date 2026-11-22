package exiledsector.harness;

final class ClipStep implements HarnessStep {

    private final String clipName;

    private ClipStep(String clipName) {
        this.clipName = clipName;
    }

    static ClipStep start(String clipName) {
        return new ClipStep(clipName);
    }

    static ClipStep end() {
        return new ClipStep(null);
    }

    @Override
    public String name() {
        return clipName == null ? "endClip" : "startClip " + clipName;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        long nowMillis = System.currentTimeMillis();
        if (clipName == null) {
            String ended = context.results().endClip(nowMillis);
            return ended == null ? StepOutcome.failed("no clip was started") : StepOutcome.passed("ended clip " + ended);
        }
        String interrupted = context.results().startClip(clipName, nowMillis);
        return StepOutcome.passed("started clip " + clipName + (interrupted == null ? "" : ", ending clip " + interrupted));
    }
}
