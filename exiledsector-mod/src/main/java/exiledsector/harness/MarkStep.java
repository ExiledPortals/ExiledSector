package exiledsector.harness;

import org.apache.log4j.Logger;

final class MarkStep implements HarnessStep {

    private static final Logger LOG = Logger.getLogger(MarkStep.class);

    private final String markText;

    MarkStep(String markText) {
        this.markText = markText;
    }

    @Override
    public String name() {
        return "mark " + markText;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        LOG.info(Harness.LOG_TAG + "mark: " + markText);
        return StepOutcome.passed(markText);
    }
}
