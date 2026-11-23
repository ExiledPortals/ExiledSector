package exiledsector.ui.slipstream;

import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlipstreamEventTest {

    private static final float STEP = 1f / 30f;
    private static final SlipstreamRoute ROUTE = new SlipstreamRoute(
            List.of(new Vector2f(-6000f, 0f), new Vector2f(0f, 800f), new Vector2f(6000f, 0f)), 400f);
    private static final float CROSSING_SECONDS = 9f;
    private static final float SPEED = ROUTE.length() / CROSSING_SECONDS;

    @Test
    void theStreamIsLaidBehindTheGhostsAndCoversTheRouteOnceTheyCross() {
        SlipstreamEvent event = new SlipstreamEvent(ROUTE, SPEED, new Random(4));
        advance(event, CROSSING_SECONDS / 2f);
        float halfway = event.stream().laidLength();
        assertTrue(halfway > ROUTE.length() * 0.4f && halfway < ROUTE.length() * 0.6f, "laid " + halfway);

        advance(event, CROSSING_SECONDS);
        assertEquals(ROUTE.length(), event.stream().laidLength(), 0.01f);
        assertTrue(event.stream().alphaAt(ROUTE.length() / 2f) > 0.9f);
    }

    @Test
    void theStreamLingersAboutThirtySecondsAfterItIsLaidThenDisappears() {
        SlipstreamEvent event = new SlipstreamEvent(ROUTE, SPEED, new Random(9));
        float lingerFloor = CROSSING_SECONDS + SlipstreamEvent.LINGER_SECONDS - SlipstreamEvent.LINGER_JITTER_SECONDS;
        advance(event, lingerFloor - 0.1f);
        assertFalse(event.stream().isDissolving());
        assertFalse(event.isOver());

        float elapsed = lingerFloor;
        while (!event.isOver() && elapsed < 90f) {
            event.advance(STEP);
            elapsed += STEP;
        }
        assertTrue(event.isOver());
        float latest = CROSSING_SECONDS + SlipstreamEvent.LINGER_SECONDS + SlipstreamEvent.LINGER_JITTER_SECONDS + Slipstream.DISSOLVE_SECONDS
                + Slipstream.MIN_SEGMENT_FADE_OUT_SECONDS + 0.5f;
        assertTrue(elapsed < latest, "lasted " + elapsed);
    }

    @Test
    void theEndsOfTheStreamAreAlwaysInvisible() {
        SlipstreamEvent event = new SlipstreamEvent(ROUTE, SPEED, new Random(2));
        advance(event, CROSSING_SECONDS * 2f);
        Slipstream stream = event.stream();

        assertEquals(0f, stream.alphaOf(0));
        assertEquals(0f, stream.alphaOf(stream.segments().size() - 1));
    }

    @Test
    void dissolveOrderSpansZeroToOne() {
        float[] order = Slipstream.dissolveOrder(40, new Random(5));
        float minimum = 1f;
        float maximum = 0f;
        for (float value : order) {
            minimum = Math.min(minimum, value);
            maximum = Math.max(maximum, value);
        }
        assertEquals(0f, minimum, 0.0001f);
        assertEquals(1f, maximum, 0.0001f);
    }

    private static void advance(SlipstreamEvent event, float seconds) {
        for (float elapsed = 0f; elapsed < seconds; elapsed += STEP) {
            event.advance(STEP);
        }
    }
}
