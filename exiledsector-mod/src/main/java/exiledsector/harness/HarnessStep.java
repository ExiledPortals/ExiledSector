package exiledsector.harness;

interface HarnessStep {

    float DEFAULT_TIMEOUT_SECONDS = 30f;

    String name();

    StepOutcome advance(StepContext context) throws Throwable;

    default float timeoutSeconds() {
        return DEFAULT_TIMEOUT_SECONDS;
    }
}
