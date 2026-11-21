package exiledsector.harness;

final class ScrollStep implements HarnessStep {

    private final boolean out;
    private final int notches;
    private final float seconds;
    private int sent;

    ScrollStep(boolean out, int notches, float seconds) {
        if (notches < 1) {
            throw new IllegalArgumentException("scroll needs at least 1 notch, not " + notches);
        }
        if (seconds < 0f) {
            throw new IllegalArgumentException("scroll needs a duration of at least 0 seconds, not " + seconds);
        }
        this.out = out;
        this.notches = notches;
        this.seconds = seconds;
    }

    static boolean scrollsOut(String direction) {
        return switch (direction) {
            case "out" -> true;
            case "in" -> false;
            default -> throw new IllegalArgumentException("scroll goes \"in\" or \"out\", not \"" + direction + "\"");
        };
    }

    @Override
    public String name() {
        return "scroll " + (out ? "out " : "in ") + notches + " notches over " + seconds + " s";
    }

    @Override
    public StepOutcome advance(StepContext context) throws Throwable {
        float progress = seconds <= 0f ? 1f : Math.min(1f, context.seconds() / seconds);
        int due = Math.min(notches, 1 + (int) (progress * (notches - 1)));
        while (sent < due) {
            HarnessMouse.wheel(out ? 1 : -1);
            sent++;
        }
        return sent < notches ? StepOutcome.RUNNING : StepOutcome.passed("scrolled " + (out ? "out " : "in ") + sent + " notches");
    }

    @Override
    public float timeoutSeconds() {
        return Math.max(DEFAULT_TIMEOUT_SECONDS, seconds + DEFAULT_TIMEOUT_SECONDS);
    }
}
