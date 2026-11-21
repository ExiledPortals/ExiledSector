package exiledsector.harness;

import exiledsector.ui.ShownScreens;

import java.util.Map;

final class ExpectScreenStep implements HarnessStep {

    static final String NONE = "none";
    private static final Map<String, String> SCREEN_ALIASES = Map.of(
            "atlas", "AtlasScreen",
            "atlasTree", "AtlasTreeScreen",
            "skillTree", "SkillTreeCanvasPlugin",
            "craftingBench", "CraftingBenchScreen");

    private final String expectedScreen;

    ExpectScreenStep(String requestedScreen) {
        this.expectedScreen = SCREEN_ALIASES.getOrDefault(requestedScreen, requestedScreen);
    }

    @Override
    public String name() {
        return "expectScreen " + expectedScreen;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        ShownScreens.Shown shown = ShownScreens.top();
        String shownScreen = Harness.describe(shown == null ? null : shown.screen());
        return shownScreen.equalsIgnoreCase(expectedScreen) ? StepOutcome.passed("showing " + shownScreen)
                : StepOutcome.failed("expected " + expectedScreen + " but showing " + shownScreen);
    }
}
