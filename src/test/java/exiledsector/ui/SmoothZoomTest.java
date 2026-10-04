package exiledsector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothZoomTest {

    @Test
    void scrollingMovesTheTargetButNotTheCurrentZoomUntilItAdvances() {
        SmoothZoom zoom = new SmoothZoom(1f);

        zoom.scroll(true);

        assertEquals(SmoothZoom.STEP, zoom.target(), 1e-6f);
        assertEquals(1f, zoom.current());
    }

    @Test
    void theZoomEasesTowardTheTargetAndSettlesExactlyOnIt() {
        SmoothZoom zoom = new SmoothZoom(1f);
        zoom.scroll(true);

        zoom.advance(1f / 60f);
        float afterOneFrame = zoom.current();
        assertTrue(afterOneFrame > 1f && afterOneFrame < SmoothZoom.STEP);

        for (int frame = 0; frame < 120; frame++) {
            zoom.advance(1f / 60f);
        }
        assertEquals(zoom.target(), zoom.current());
    }

    @Test
    void severalTicksInOneFrameStackOnTheTargetInsteadOfJumping() {
        SmoothZoom zoom = new SmoothZoom(1f);

        zoom.scroll(false);
        zoom.scroll(false);
        zoom.scroll(false);

        assertEquals(1f / (SmoothZoom.STEP * SmoothZoom.STEP * SmoothZoom.STEP), zoom.target(), 1e-5f);
        assertEquals(1f, zoom.current());
    }

    @Test
    void theTargetStaysWithinTheZoomLimits() {
        SmoothZoom zoom = new SmoothZoom(1f);
        for (int tick = 0; tick < 100; tick++) {
            zoom.scroll(true);
        }
        assertEquals(SmoothZoom.MAX_ZOOM, zoom.target());

        for (int tick = 0; tick < 200; tick++) {
            zoom.scroll(false);
        }
        assertEquals(SmoothZoom.MIN_ZOOM, zoom.target());
    }

    @Test
    void jumpingSetsBothTheCurrentZoomAndTheTarget() {
        SmoothZoom zoom = new SmoothZoom(1f);
        zoom.scroll(true);

        zoom.jumpTo(0.5f);

        assertEquals(0.5f, zoom.current());
        assertEquals(0.5f, zoom.target());
    }
}
