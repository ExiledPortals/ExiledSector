package exiledsector.ui.slipstream;

import exiledsector.ui.TreeViewport;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlipstreamSkyTest {

    private static final SlipstreamSky.Pace PACE = new SlipstreamSky.Pace(5f, 5f, 50f, 50f, 1f);
    private static final TreeViewport VIEWPORT = new TreeViewport(500f, 400f, 0.5f, 0f, 0f, 1000f, 800f);
    private static final SlipstreamRoute ROUTE = new SlipstreamRoute(List.of(new Vector2f(-3000f, 0f), new Vector2f(3000f, 0f)), 200f);

    @Test
    void nothingIsPlannedBeforeAViewHasBeenSeen() {
        AtomicInteger plans = new AtomicInteger();
        SlipstreamSky sky = new SlipstreamSky((view, random) -> {
            plans.incrementAndGet();
            return ROUTE;
        }, PACE, new Random(1));

        advance(sky, 10f);

        assertEquals(0, plans.get());
        assertFalse(sky.isActive());
    }

    @Test
    void aSlipstreamStartsAfterTheFirstWaitAndRestsAfterItEnds() {
        SlipstreamSky sky = new SlipstreamSky((view, random) -> ROUTE, PACE, new Random(1));
        sky.render(VIEWPORT, 0f);

        advance(sky, 4.9f);
        assertFalse(sky.isActive());
        advance(sky, 0.2f);
        assertTrue(sky.isActive());

        advance(sky, 45f);
        assertFalse(sky.isActive());
        advance(sky, 35f);
        assertFalse(sky.isActive());
    }

    @Test
    void aSlowerPaceLaysTheStreamMoreSlowly() {
        SlipstreamSky brisk = new SlipstreamSky((view, random) -> ROUTE, PACE, new Random(1));
        SlipstreamSky slow = new SlipstreamSky((view, random) -> ROUTE, PACE.crossingViewIn(3f), new Random(1));
        brisk.render(VIEWPORT, 0f);
        slow.render(VIEWPORT, 0f);

        advance(brisk, 6f);
        advance(slow, 6f);

        float briskLaid = brisk.event().stream().laidLength();
        float slowLaid = slow.event().stream().laidLength();
        assertTrue(briskLaid > slowLaid * 2.5f, "brisk " + briskLaid + " slow " + slowLaid);
    }

    @Test
    void plansAreMadeFromTheLastRenderedView() {
        ViewWindow[] planned = new ViewWindow[1];
        SlipstreamSky sky = new SlipstreamSky((view, random) -> {
            planned[0] = view;
            return null;
        }, PACE, new Random(1));
        sky.render(VIEWPORT, 0f);

        advance(sky, 6f);

        assertEquals(new ViewWindow(-1000f, -800f, 1000f, 800f), planned[0]);
        assertFalse(sky.isActive());
    }

    @Test
    void aFailedPlanIsRetriedSoon() {
        AtomicInteger plans = new AtomicInteger();
        SlipstreamSky sky = new SlipstreamSky((view, random) -> {
            plans.incrementAndGet();
            return null;
        }, PACE, new Random(1));
        sky.render(VIEWPORT, 0f);

        advance(sky, 5.1f + SlipstreamSky.MAX_RETRY_SECONDS);

        assertEquals(2, plans.get());
    }

    private static void advance(SlipstreamSky sky, float seconds) {
        float step = 1f / 30f;
        for (float elapsed = 0f; elapsed < seconds; elapsed += step) {
            sky.advance(step);
        }
    }
}
