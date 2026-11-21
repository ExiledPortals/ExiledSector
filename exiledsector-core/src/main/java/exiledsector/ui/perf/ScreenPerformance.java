package exiledsector.ui.perf;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class ScreenPerformance {

    static final int LOGGED_FIRST_FRAMES = 3;
    static final long SLOW_WORK_NANOS = 50_000_000L;
    static final long SLOW_FRAME_NANOS = 100_000_000L;
    static final int MAX_SLOW_FRAME_LINES = 20;
    static final int LOGGED_PHASES = 6;
    static final String PREFIX = "[ExiledSector] [perf] ";
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final Logger LOG = Logger.getLogger(ScreenPerformance.class);
    private static ScreenPerformance opening;

    private record Phase(String name, long nanos) {
    }

    private final LongSupplier nanoClock;
    private final Supplier<LoadCounters.Totals> loadTotals;
    private final Consumer<String> log;
    private final long openStartNanos;
    private final LoadCounters.Totals openStartLoads;
    private final List<Phase> phases = new ArrayList<>();
    private String screenName = "screen";
    private LoadCounters.Totals shownStartLoads;
    private boolean closed;
    private int frameCount;
    private long frameStartNanos = -1L;
    private long frameAdvanceNanos;
    private long frameRenderNanos;
    private long frameInputNanos;
    private LoadCounters.Totals frameStartLoads = LoadCounters.Totals.NONE;
    private int slowFrames;
    private int slowFrameLines;
    private int unloggedSlowFrames;
    private long slowestWorkNanos;
    private long slowestFrameNanos;

    ScreenPerformance(LongSupplier nanoClock, Supplier<LoadCounters.Totals> loadTotals, Consumer<String> log) {
        this.nanoClock = nanoClock;
        this.loadTotals = loadTotals;
        this.log = log;
        this.openStartNanos = nanoClock.getAsLong();
        this.openStartLoads = loadTotals.get();
    }

    public static ScreenPerformance begin() {
        return begin(new ScreenPerformance(System::nanoTime, LoadCounters::totals, LOG::info));
    }

    static ScreenPerformance begin(ScreenPerformance performance) {
        opening = performance;
        return performance;
    }

    public static <T> T phase(String phaseName, Supplier<T> work) {
        ScreenPerformance current = opening;
        if (current == null) {
            return work.get();
        }
        long startNanos = current.now();
        try {
            return work.get();
        } finally {
            current.phases.add(new Phase(phaseName, current.now() - startNanos));
        }
    }

    public static void phase(String phaseName, Runnable work) {
        phase(phaseName, () -> {
            work.run();
            return null;
        });
    }

    public void opened(String shownScreenName) {
        if (opening == this) {
            opening = null;
        }
        screenName = shownScreenName;
        long openNanos = now() - openStartNanos;
        shownStartLoads = loadTotals.get();
        log.accept(PREFIX + screenName + " opened in " + millis(openNanos) + " ms" + phaseSummary(openNanos)
                + loadSummary(shownStartLoads.minus(openStartLoads), "; "));
    }

    public void abandonIfUnopened() {
        if (opening == this) {
            opening = null;
        }
    }

    long now() {
        return nanoClock.getAsLong();
    }

    public void frameStarted() {
        long startNanos = now();
        if (frameStartNanos >= 0L) {
            finishFrame(startNanos);
        }
        frameCount++;
        frameStartNanos = startNanos;
        frameAdvanceNanos = 0L;
        frameRenderNanos = 0L;
        frameInputNanos = 0L;
        frameStartLoads = loadTotals.get();
    }

    public void advanced(long startNanos) {
        frameAdvanceNanos += now() - startNanos;
    }

    public void rendered(long startNanos) {
        frameRenderNanos += now() - startNanos;
    }

    public void handledInput(long startNanos) {
        frameInputNanos += now() - startNanos;
    }

    public void closed() {
        if (closed || shownStartLoads == null) {
            return;
        }
        closed = true;
        if (frameStartNanos >= 0L) {
            finishFrame(now());
        }
        String unlogged = unloggedSlowFrames == 0 ? "" : ", " + unloggedSlowFrames + " of them not logged";
        log.accept(PREFIX + screenName + " closed after " + frameCount + " frames: " + slowFrames + " slow" + unlogged + ", slowest frame "
                + millis(slowestFrameNanos) + " ms with " + millis(slowestWorkNanos) + " ms in this screen"
                + loadSummary(loadTotals.get().minus(shownStartLoads), "; while open "));
    }

    private void finishFrame(long endNanos) {
        long frameNanos = endNanos - frameStartNanos;
        long workNanos = frameAdvanceNanos + frameRenderNanos + frameInputNanos;
        boolean slow = workNanos >= SLOW_WORK_NANOS || frameNanos >= SLOW_FRAME_NANOS;
        if (slow) {
            slowFrames++;
        }
        slowestWorkNanos = Math.max(slowestWorkNanos, workNanos);
        slowestFrameNanos = Math.max(slowestFrameNanos, frameNanos);
        if (frameCount > LOGGED_FIRST_FRAMES && !slow) {
            return;
        }
        if (frameCount > LOGGED_FIRST_FRAMES && slowFrameLines >= MAX_SLOW_FRAME_LINES) {
            unloggedSlowFrames++;
            return;
        }
        if (frameCount > LOGGED_FIRST_FRAMES) {
            slowFrameLines++;
        }
        log.accept(PREFIX + screenName + " frame " + frameCount + (slow ? " (slow)" : "") + ": " + millis(frameNanos) + " ms whole frame, "
                + millis(workNanos) + " ms in this screen (advance " + millis(frameAdvanceNanos) + ", render " + millis(frameRenderNanos)
                + ", input " + millis(frameInputNanos) + ")" + loadSummary(loadTotals.get().minus(frameStartLoads), "; "));
    }

    private String phaseSummary(long openNanos) {
        if (phases.isEmpty()) {
            return "";
        }
        List<Phase> slowestFirst = new ArrayList<>(phases);
        slowestFirst.sort(Comparator.comparingLong(Phase::nanos).reversed());
        StringBuilder summary = new StringBuilder(":");
        long namedNanos = 0L;
        for (int index = 0; index < Math.min(LOGGED_PHASES, slowestFirst.size()); index++) {
            Phase phase = slowestFirst.get(index);
            summary.append(index == 0 ? " " : ", ").append(phase.name()).append(' ').append(millis(phase.nanos()));
            namedNanos += phase.nanos();
        }
        return summary.append(", other ").append(millis(Math.max(0L, openNanos - namedNanos))).toString();
    }

    private static String loadSummary(LoadCounters.Totals loads, String lead) {
        if (loads.isEmpty()) {
            return "";
        }
        return lead + "loaded " + loads.textureLoads() + " textures in " + millis(loads.textureNanos()) + " ms, read "
                + loads.hullFileReads() + " hull files in " + millis(loads.hullFileNanos()) + " ms";
    }

    private static long millis(long nanos) {
        return nanos / NANOS_PER_MILLI;
    }
}
