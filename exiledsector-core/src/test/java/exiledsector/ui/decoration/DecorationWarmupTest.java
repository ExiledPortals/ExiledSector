package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecorationWarmupTest {

    private final List<String> loaded = new ArrayList<>();
    private final List<String> reports = new ArrayList<>();
    private long nowNanos;

    private DecorationWarmup warmup(long nanosPerLoad) {
        return new DecorationWarmup("Test tree", path -> load("texture " + path, nanosPerLoad), hullId -> load("hull " + hullId, nanosPerLoad),
                () -> nowNanos, reports::add);
    }

    private void load(String asset, long nanosPerLoad) {
        loaded.add(asset);
        nowNanos += nanosPerLoad;
    }

    @Test
    void loadsInQueuedOrderUntilTheFrameBudgetIsSpent() {
        DecorationWarmup warmup = warmup(DecorationWarmup.FRAME_BUDGET_NANOS / 2);
        warmup.addHulls(List.of("hound", "wolf", "lasher"));
        warmup.addTexture("graphics/planets/barren.jpg");

        warmup.advance();

        assertEquals(List.of("hull hound", "hull wolf"), loaded);
        assertFalse(warmup.isDone());
        assertEquals(List.of(), reports);

        warmup.advance();

        assertEquals(List.of("hull hound", "hull wolf", "hull lasher", "texture graphics/planets/barren.jpg"), loaded);
        assertTrue(warmup.isDone());
        assertEquals(List.of("[ExiledSector] [perf] Test tree warm-up finished: 4 assets over 2 frames and 6 ms, 6 ms spent loading"), reports);

        warmup.advance();

        assertEquals(1, reports.size());
    }

    @Test
    void aSlowLoadStillLetsEachFrameMakeProgress() {
        DecorationWarmup warmup = warmup(DecorationWarmup.FRAME_BUDGET_NANOS * 4);
        warmup.addHulls(List.of("onslaught", "paragon"));

        warmup.advance();

        assertEquals(List.of("hull onslaught"), loaded);
    }

    @Test
    void queuesEachAssetOnceAndSkipsBlankOnes() {
        DecorationWarmup warmup = warmup(0L);
        warmup.addHulls(List.of("hound", "hound", ""));
        warmup.addHulls(List.of("hound"));
        warmup.addTexture("graphics/planets/barren.jpg");
        warmup.addTexture("graphics/planets/barren.jpg");
        warmup.addTexture(null);

        warmup.advance();

        assertEquals(List.of("hull hound", "texture graphics/planets/barren.jpg"), loaded);
        assertTrue(warmup.isDone());
    }
}
