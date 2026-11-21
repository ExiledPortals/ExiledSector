package exiledsector.harness;

final class ExpectLogStep implements HarnessStep {

    private final String expectedText;
    private final float withinSeconds;

    ExpectLogStep(String expectedText, float withinSeconds) {
        this.expectedText = expectedText;
        this.withinSeconds = withinSeconds;
    }

    @Override
    public String name() {
        return "expectLog \"" + expectedText + "\"";
    }

    @Override
    public StepOutcome advance(StepContext context) {
        String line = context.log().firstLineContaining(expectedText, 0);
        if (line != null) {
            return StepOutcome.passed(line);
        }
        if (context.seconds() >= withinSeconds) {
            return StepOutcome.failed("no log line contained \"" + expectedText + "\" within " + withinSeconds + " s");
        }
        return StepOutcome.RUNNING;
    }

    @Override
    public float timeoutSeconds() {
        return withinSeconds + DEFAULT_TIMEOUT_SECONDS;
    }
}
