package exiledsector.ui.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoloFrameTest {

    @Test
    void chromeOnlyShowsOnceTheOpenAnimationHasFinished() {
        assertEquals(0f, HoloFrame.chromeAlpha(true, 0f));
        assertEquals(0f, HoloFrame.chromeAlpha(true, 0.99f));
        assertEquals(1f, HoloFrame.chromeAlpha(true, 1f));
    }

    @Test
    void chromeDisappearsTheMomentTheCloseStartsEvenAtFullContent() {
        assertEquals(0f, HoloFrame.chromeAlpha(false, 1f));
    }

    @Test
    void aClosingFrameKeepsBlockingUntilItHasFadedOut() {
        HoloFrame frame = new HoloFrame(HoloFrameTest.class, HoloFrame.Look.MODAL);
        frame.open();
        frame.advance(1f);
        assertEquals(1f, frame.chromeAlpha());

        frame.close();
        assertEquals(0f, frame.chromeAlpha());
        assertTrue(frame.isBlocking());

        frame.advance(0.1f);
        assertTrue(frame.isBlocking());

        frame.advance(1f);
        assertFalse(frame.isBlocking());
        assertTrue(frame.isFullyClosed());
    }

    @Test
    void advanceReportsWhetherTheAnimationMoved() {
        HoloFrame frame = new HoloFrame(HoloFrameTest.class, HoloFrame.Look.PANEL);
        frame.openInstantly();

        assertFalse(frame.advance(0.1f));
        frame.close();
        assertTrue(frame.advance(0.1f));
    }
}
