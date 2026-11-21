package exiledsector.harness;

final class ScreenshotStep implements HarnessStep {

    private final String screenshotName;

    ScreenshotStep(String requestedName) {
        this.screenshotName = requestedName.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    @Override
    public String name() {
        return "screenshot " + screenshotName;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        ScreenCapture capture = context.capture();
        if (context.firstFrame()) {
            capture.request(screenshotName);
            return StepOutcome.RUNNING;
        }
        if (capture.failure() != null) {
            return StepOutcome.failed(capture.failure());
        }
        HarnessResults.Screenshot taken = capture.taken();
        if (taken != null && !capture.pending()) {
            return StepOutcome.passed(taken.name() + " " + taken.width() + "x" + taken.height() + " from the " + taken.source());
        }
        return StepOutcome.RUNNING;
    }
}
