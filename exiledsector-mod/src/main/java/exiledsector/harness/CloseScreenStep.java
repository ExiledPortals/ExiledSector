package exiledsector.harness;

import exiledsector.ui.ShownScreens;

final class CloseScreenStep implements HarnessStep {

    private Object closingScreen;

    @Override
    public String name() {
        return "closeScreen";
    }

    @Override
    public StepOutcome advance(StepContext context) {
        ShownScreens.Shown shown = ShownScreens.top();
        if (context.firstFrame()) {
            if (shown == null) {
                return StepOutcome.failed("no Exiled Sector screen is open");
            }
            closingScreen = shown.screen();
            shown.close().run();
            return StepOutcome.RUNNING;
        }
        if (shown == null || shown.screen() != closingScreen) {
            return StepOutcome.passed("closed " + Harness.describe(closingScreen) + ", now showing " + Harness.describe(shown == null ? null : shown.screen()));
        }
        return new StepOutcome(false, false, Harness.describe(closingScreen) + " never closed");
    }
}
