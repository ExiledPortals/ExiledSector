package exiledsector.ui.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoloTransitionTest {

    private static final float EPSILON = 1e-4f;

    @Test
    void contentOnlyAppearsAfterTheFrameHasUnfolded() {
        HoloTransition transition = new HoloTransition();
        transition.open();

        transition.advance(0.175f);
        assertEquals(0.5f, transition.progress(), EPSILON);
        assertEquals(0f, transition.contentAlpha(), EPSILON);
        assertTrue(transition.isAnimating());

        transition.advance(1f);
        assertEquals(1f, transition.contentAlpha(), EPSILON);
        assertFalse(transition.isAnimating());
    }

    @Test
    void closingPlaysBackwardsAndStaysVisibleUntilItFinishes() {
        HoloTransition transition = new HoloTransition();
        transition.open();
        transition.advance(1f);

        transition.close();
        transition.advance(0.11f);
        assertTrue(transition.isVisible());
        assertFalse(transition.isFullyClosed());

        transition.advance(1f);
        assertTrue(transition.isFullyClosed());
        assertFalse(transition.isVisible());
    }

    @Test
    void reopeningMidCloseContinuesFromWhereItWas() {
        HoloTransition transition = new HoloTransition();
        transition.open();
        transition.advance(1f);
        transition.close();
        transition.advance(0.11f);
        float midway = transition.progress();

        transition.open();
        transition.advance(0f);

        assertEquals(midway, transition.progress(), EPSILON);
        assertTrue(transition.isAnimating());
    }
}
