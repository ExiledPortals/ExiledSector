package exiledsector.ui.perf;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScreenPerformanceTest {

    private static final long MILLI = 1_000_000L;

    private final List<String> lines = new ArrayList<>();
    private long nowNanos;
    private LoadCounters.Totals loads = LoadCounters.Totals.NONE;

    private ScreenPerformance begin() {
        return ScreenPerformance.begin(new ScreenPerformance(() -> nowNanos, () -> loads, lines::add));
    }

    @AfterEach
    void forgetOpeningScreen() {
        ScreenPerformance.begin((ScreenPerformance) null);
    }

    private void frame(ScreenPerformance performance, long advanceMillis, long renderMillis, long idleMillis) {
        performance.frameStarted();
        long advanceStart = performance.now();
        nowNanos += advanceMillis * MILLI;
        performance.advanced(advanceStart);
        long renderStart = performance.now();
        nowNanos += renderMillis * MILLI;
        performance.rendered(renderStart);
        nowNanos += idleMillis * MILLI;
    }

    @Test
    void theOpenLineListsPhasesSlowestFirstAndTheLoadsTheyCaused() {
        ScreenPerformance performance = begin();
        String built = ScreenPerformance.phase("tree view", () -> {
            nowNanos += 40 * MILLI;
            loads = new LoadCounters.Totals(12, 30 * MILLI, 2, 5 * MILLI);
            return "built";
        });
        ScreenPerformance.phase("chrome", () -> nowNanos += 10 * MILLI);
        nowNanos += 5 * MILLI;

        performance.opened("Atlas tree");

        assertEquals("built", built);
        assertEquals(List.of("[ExiledSector] [perf] Atlas tree opened in 55 ms: tree view 40, chrome 10, other 5; "
                + "loaded 12 textures in 30 ms, read 2 hull files in 5 ms"), lines);
    }

    @Test
    void phasesOutsideAnOpeningScreenAreNotRecorded() {
        ScreenPerformance earlier = begin();
        earlier.opened("Atlas");
        lines.clear();

        assertEquals(7, ScreenPerformance.phase("unrelated", () -> 7));

        ScreenPerformance later = begin();
        later.opened("Atlas tree");
        assertEquals(List.of("[ExiledSector] [perf] Atlas tree opened in 0 ms"), lines);
    }

    @Test
    void abandoningAnEarlierOpenLeavesTheCurrentOneCollecting() {
        ScreenPerformance failed = begin();
        ScreenPerformance current = begin();

        failed.abandonIfUnopened();
        ScreenPerformance.phase("tree view", () -> nowNanos += 3 * MILLI);
        current.opened("Skill tree");

        assertEquals(List.of("[ExiledSector] [perf] Skill tree opened in 3 ms: tree view 3, other 0"), lines);
    }

    @Test
    void logsTheFirstFramesThenOnlySlowOnesAndSummarisesOnClose() {
        ScreenPerformance performance = begin();
        performance.opened("Atlas tree");
        lines.clear();

        frame(performance, 2, 3, 11);
        frame(performance, 1, 1, 14);
        frame(performance, 1, 1, 14);
        frame(performance, 1, 1, 14);
        frame(performance, 30, 25, 5);
        frame(performance, 1, 1, 150);
        frame(performance, 1, 1, 14);
        performance.closed();
        performance.closed();

        assertEquals(List.of(
                "[ExiledSector] [perf] Atlas tree frame 1: 16 ms whole frame, 5 ms in this screen (advance 2, render 3, input 0)",
                "[ExiledSector] [perf] Atlas tree frame 2: 16 ms whole frame, 2 ms in this screen (advance 1, render 1, input 0)",
                "[ExiledSector] [perf] Atlas tree frame 3: 16 ms whole frame, 2 ms in this screen (advance 1, render 1, input 0)",
                "[ExiledSector] [perf] Atlas tree frame 5 (slow): 60 ms whole frame, 55 ms in this screen (advance 30, render 25, input 0)",
                "[ExiledSector] [perf] Atlas tree frame 6 (slow): 152 ms whole frame, 2 ms in this screen (advance 1, render 1, input 0)",
                "[ExiledSector] [perf] Atlas tree closed after 7 frames: 2 slow, slowest frame 152 ms with 55 ms in this screen"), lines);
    }

    @Test
    void aFrameNamesTheLoadsThatHappenedDuringIt() {
        ScreenPerformance performance = begin();
        performance.opened("Atlas tree");
        lines.clear();

        performance.frameStarted();
        long renderStart = performance.now();
        nowNanos += 70 * MILLI;
        loads = new LoadCounters.Totals(4, 60 * MILLI, 1, 8 * MILLI);
        performance.rendered(renderStart);
        performance.closed();

        assertEquals(List.of(
                "[ExiledSector] [perf] Atlas tree frame 1 (slow): 70 ms whole frame, 70 ms in this screen (advance 0, render 70, input 0); "
                        + "loaded 4 textures in 60 ms, read 1 hull files in 8 ms",
                "[ExiledSector] [perf] Atlas tree closed after 1 frames: 1 slow, slowest frame 70 ms with 70 ms in this screen; "
                        + "while open loaded 4 textures in 60 ms, read 1 hull files in 8 ms"), lines);
    }

    @Test
    void capsSlowFrameLinesAndCountsTheRest() {
        ScreenPerformance performance = begin();
        performance.opened("Skill tree");
        for (int frame = 0; frame < ScreenPerformance.LOGGED_FIRST_FRAMES; frame++) {
            frame(performance, 1, 1, 14);
        }
        int extraSlowFrames = 3;
        for (int frame = 0; frame < ScreenPerformance.MAX_SLOW_FRAME_LINES + extraSlowFrames; frame++) {
            frame(performance, 60, 0, 0);
        }
        lines.clear();

        performance.closed();

        int frames = ScreenPerformance.LOGGED_FIRST_FRAMES + ScreenPerformance.MAX_SLOW_FRAME_LINES + extraSlowFrames;
        assertEquals(List.of("[ExiledSector] [perf] Skill tree closed after " + frames + " frames: "
                + (ScreenPerformance.MAX_SLOW_FRAME_LINES + extraSlowFrames) + " slow, " + extraSlowFrames
                + " of them not logged, slowest frame 60 ms with 60 ms in this screen"), lines);
    }

    @Test
    void closingAScreenThatNeverOpenedLogsNothing() {
        ScreenPerformance performance = begin();
        performance.abandonIfUnopened();

        performance.closed();

        assertEquals(List.of(), lines);
    }
}
