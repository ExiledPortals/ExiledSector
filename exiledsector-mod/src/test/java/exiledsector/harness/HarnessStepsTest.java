package exiledsector.harness;

import exiledsector.ui.CanvasCamera;
import exiledsector.ui.ShownScreens;
import exiledsector.ui.ZoomableScreen;
import org.apache.log4j.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarnessStepsTest {

    static StepContext context(HarnessLog log, int frame, float seconds) {
        return new StepContext(frame, seconds, log, 0, null, null);
    }

    private static final class AtlasTreeScreen implements ZoomableScreen {

        private final CanvasCamera camera = new CanvasCamera();

        @Override
        public CanvasCamera camera() {
            return camera;
        }
    }

    @AfterEach
    void forgetShownScreens() {
        ShownScreens.clear();
    }

    @Test
    void waitingNeedsBothItsFramesAndItsSeconds() {
        WaitStep wait = new WaitStep(3, 1f);
        HarnessLog log = new HarnessLog();

        assertFalse(wait.advance(context(log, 3, 0.5f)).finished());
        assertFalse(wait.advance(context(log, 2, 2f)).finished());
        assertTrue(wait.advance(context(log, 3, 1f)).passed());
        assertEquals(HarnessStep.DEFAULT_TIMEOUT_SECONDS + 90f, new WaitStep(0, 90f).timeoutSeconds());
    }

    @Test
    void anExpectedLogLineMayAppearUntilItsTimeIsUp() {
        ExpectLogStep expectLog = new ExpectLogStep("[perf] Atlas", 2f);
        HarnessLog log = new HarnessLog();

        assertFalse(expectLog.advance(context(log, 1, 1f)).finished());
        assertFalse(expectLog.advance(context(log, 2, 2f)).passed());
        log.doAppend(HarnessLogTest.event(Level.INFO, "[perf] Atlas opened"));
        assertTrue(expectLog.advance(context(log, 3, 3f)).passed());
    }

    @Test
    void expectNoErrorsOnlyCountsErrorsSinceTheLastCheck() {
        ErrorCheckStep.Cursor cursor = new ErrorCheckStep.Cursor();
        HarnessLog log = new HarnessLog();
        log.doAppend(HarnessLogTest.event(Level.ERROR, "at load"));

        StepOutcome first = ErrorCheckStep.expectNoErrors(cursor).advance(context(log, 0, 0f));
        StepOutcome second = ErrorCheckStep.expectNoErrors(cursor).advance(context(log, 0, 0f));

        assertFalse(first.passed());
        assertTrue(first.detail().contains("at load"), first.detail());
        assertTrue(second.passed(), second.detail());
    }

    @Test
    void expectScreenComparesTheTopShownScreenByNameOrAlias() {
        HarnessLog log = new HarnessLog();
        assertTrue(new ExpectScreenStep(ExpectScreenStep.NONE).advance(context(log, 0, 0f)).passed());

        ShownScreens.shown(new AtlasTreeScreen(), () -> { });

        assertTrue(new ExpectScreenStep("atlasTree").advance(context(log, 0, 0f)).passed());
        assertTrue(new ExpectScreenStep("AtlasTreeScreen").advance(context(log, 0, 0f)).passed());
        StepOutcome wrong = new ExpectScreenStep("skillTree").advance(context(log, 0, 0f));
        assertFalse(wrong.passed());
        assertEquals("expected SkillTreeCanvasPlugin but showing AtlasTreeScreen", wrong.detail());
    }

    @Test
    void closeScreenClosesTheTopScreenAndWaitsUntilItIsGone() {
        HarnessLog log = new HarnessLog();
        AtlasTreeScreen screen = new AtlasTreeScreen();
        AtomicInteger closes = new AtomicInteger();
        ShownScreens.shown("Atlas underneath", () -> { });
        ShownScreens.shown(screen, closes::incrementAndGet);
        CloseScreenStep close = new CloseScreenStep();

        assertFalse(close.advance(context(log, 0, 0f)).finished());
        assertFalse(close.advance(context(log, 1, 0f)).finished());
        ShownScreens.hidden(screen);
        StepOutcome closed = close.advance(context(log, 2, 0f));

        assertEquals(1, closes.get());
        assertTrue(closed.passed());
        assertEquals("closed AtlasTreeScreen, now showing String", closed.detail());
    }

    @Test
    void closeScreenFailsWhenNothingIsShown() {
        assertFalse(new CloseScreenStep().advance(context(new HarnessLog(), 0, 0f)).passed());
    }

    @Test
    void zoomJumpsTheShownCameraAndKeepsItsCentre() {
        AtlasTreeScreen screen = new AtlasTreeScreen();
        screen.camera().centreOn(120f, -40f);
        ShownScreens.shown(screen, () -> { });

        StepOutcome zoomed = new ZoomStep(0.5f).advance(context(new HarnessLog(), 0, 0f));

        assertTrue(zoomed.passed(), zoomed.detail());
        assertEquals(0.5f, screen.camera().zoom(), 1e-4f);
        assertEquals(120f, screen.camera().centreTreeX(), 1e-3f);
        assertEquals(-40f, screen.camera().centreTreeY(), 1e-3f);
    }

    @Test
    void zoomFailsOnAScreenWithoutACamera() {
        ShownScreens.shown("Crafting bench", () -> { });

        assertFalse(new ZoomStep(0.5f).advance(context(new HarnessLog(), 0, 0f)).passed());
    }

    @Test
    void screenshotNamesKeepOnlyFileSafeCharacters() {
        assertEquals("screenshot atlas_tree_zoomed_-1", new ScreenshotStep("atlas tree/zoomed:-1").name());
    }
}
