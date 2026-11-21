package exiledsector.harness;

record StepOutcome(boolean finished, boolean passed, String detail) {

    static final StepOutcome RUNNING = new StepOutcome(false, false, "");

    static StepOutcome passed(String detail) {
        return new StepOutcome(true, true, detail);
    }

    static StepOutcome failed(String detail) {
        return new StepOutcome(true, false, detail);
    }
}
