package exiledsector.ui.decoration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import exiledsector.ui.perf.LoadCounters;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class DecorationWarmup {

    static final long FRAME_BUDGET_NANOS = 3_000_000L;
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final Logger LOG = Logger.getLogger(DecorationWarmup.class);

    private final String warmupName;
    private final Consumer<String> textureLoader;
    private final Consumer<String> hullLoader;
    private final LongSupplier nanoClock;
    private final Consumer<String> report;
    private final Set<String> queuedTextures = new HashSet<>();
    private final Set<String> queuedHulls = new HashSet<>();
    private final Queue<Runnable> steps = new ArrayDeque<>();
    private int stepsDone;
    private int framesUsed;
    private long firstFrameNanos;
    private long loadingNanos;

    public DecorationWarmup(String warmupName) {
        this(warmupName, DecorationWarmup::loadTexture, DecorationWarmup::loadHullArt, System::nanoTime, LOG::info);
    }

    DecorationWarmup(String warmupName, Consumer<String> textureLoader, Consumer<String> hullLoader, LongSupplier nanoClock,
                     Consumer<String> report) {
        this.warmupName = warmupName;
        this.textureLoader = textureLoader;
        this.hullLoader = hullLoader;
        this.nanoClock = nanoClock;
        this.report = report;
    }

    public void addTexture(String path) {
        if (path != null && !path.isEmpty() && queuedTextures.add(path)) {
            steps.add(() -> textureLoader.accept(path));
        }
    }

    public void addHulls(Collection<String> hullIds) {
        for (String hullId : hullIds) {
            if (hullId != null && !hullId.isEmpty() && queuedHulls.add(hullId)) {
                steps.add(() -> hullLoader.accept(hullId));
            }
        }
    }

    public boolean isDone() {
        return steps.isEmpty();
    }

    public void advance() {
        if (steps.isEmpty()) {
            return;
        }
        long frameStartNanos = nanoClock.getAsLong();
        if (framesUsed == 0) {
            firstFrameNanos = frameStartNanos;
        }
        framesUsed++;
        long nowNanos = frameStartNanos;
        Runnable step;
        while ((step = steps.poll()) != null) {
            step.run();
            stepsDone++;
            nowNanos = nanoClock.getAsLong();
            if (nowNanos - frameStartNanos >= FRAME_BUDGET_NANOS) {
                break;
            }
        }
        loadingNanos += nowNanos - frameStartNanos;
        if (steps.isEmpty()) {
            report.accept("[ExiledSector] [perf] " + warmupName + " warm-up finished: " + stepsDone + " assets over " + framesUsed + " frames and "
                    + (nowNanos - firstFrameNanos) / NANOS_PER_MILLI + " ms, " + loadingNanos / NANOS_PER_MILLI + " ms spent loading");
        }
    }

    private static void loadTexture(String path) {
        try {
            long startNanos = System.nanoTime();
            Global.getSettings().loadTexture(path);
            LoadCounters.textureLoaded(System.nanoTime() - startNanos);
        } catch (IOException | RuntimeException e) {
            LOG.debug("Couldn't warm up texture " + path + "; it loads when first drawn: " + e.getMessage());
        }
    }

    private static void loadHullArt(String hullId) {
        ShipHullSpecAPI hullSpec;
        try {
            hullSpec = Global.getSettings().getHullSpec(hullId);
        } catch (RuntimeException e) {
            LOG.debug("Couldn't warm up hull " + hullId + ": " + e.getMessage());
            return;
        }
        if (hullSpec == null) {
            return;
        }
        String spritePath = hullSpec.getSpriteName();
        if (spritePath != null && !spritePath.isEmpty()) {
            loadTexture(spritePath);
        }
        try {
            HullEngines.of(hullSpec);
        } catch (RuntimeException e) {
            LOG.debug("Couldn't warm up the engines and modules of hull " + hullId + ": " + e.getMessage());
        }
    }
}
