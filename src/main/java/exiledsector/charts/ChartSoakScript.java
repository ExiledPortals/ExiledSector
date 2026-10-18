package exiledsector.charts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import org.apache.log4j.Logger;

import java.util.Random;
import java.util.function.Consumer;

public class ChartSoakScript implements EveryFrameScript {

    static final int MAX_FRAMES_PER_POCKET = 600;
    private static final Logger LOG = Logger.getLogger(ChartSoakScript.class);

    private final int pocketsToSoak;
    private final Consumer<String> reporter;
    private final Random random;
    private final ChartCensus before;
    private int pocketsDone;
    private int framesWaiting;
    private String pendingRunId;
    private StarSystemAPI pendingPocket;
    private boolean done;

    public ChartSoakScript(int pocketsToSoak, ChartCensus before, Consumer<String> reporter, Random random) {
        this.pocketsToSoak = pocketsToSoak;
        this.before = before;
        this.reporter = reporter;
        this.random = random;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        if (done) {
            return;
        }
        try {
            step(Global.getSector());
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] The Star Chart soak failed after " + pocketsDone + " pocket(s)", e);
            finish(Global.getSector(), "failed after " + pocketsDone + " pocket(s): " + e);
        }
    }

    void step(SectorAPI sector) {
        if (pendingRunId == null) {
            if (pocketsDone >= pocketsToSoak) {
                finish(sector, "finished");
                return;
            }
            openNext(sector);
            return;
        }
        framesWaiting++;
        boolean recordGone = ChartRuns.currentOrNull() == null;
        boolean systemGone = pendingPocket == null || !sector.getStarSystems().contains(pendingPocket);
        if (recordGone && systemGone) {
            pocketsDone++;
            pendingRunId = null;
            pendingPocket = null;
            framesWaiting = 0;
            return;
        }
        if (framesWaiting > MAX_FRAMES_PER_POCKET) {
            finish(sector, "stopped: pocket " + pendingRunId + " was still there after " + MAX_FRAMES_PER_POCKET + " frames");
        }
    }

    private void openNext(SectorAPI sector) {
        ChartRunActions.OpenResult result = ChartRunActions.openBesidePlayer(sector, 1, random);
        if (result != ChartRunActions.OpenResult.OPENED) {
            finish(sector, "stopped: opening pocket " + (pocketsDone + 1) + " returned " + result);
            return;
        }
        ChartRun run = ChartRuns.currentOrNull();
        pendingRunId = run.runId();
        pendingPocket = sector.getStarSystem(run.pocketSystemId());
        framesWaiting = 0;
        run.collapseOuter();
    }

    private void finish(SectorAPI sector, String outcome) {
        done = true;
        pendingPocket = null;
        ChartCensus after = ChartCensus.take(sector);
        String report = "Star Chart soak " + outcome + ", " + pocketsDone + " of " + pocketsToSoak + " pocket(s) opened and closed.\n"
                + "Before: " + before.describe() + "\nAfter:  " + after.describe();
        LOG.info("[ExiledSector] " + report);
        reporter.accept(report);
    }
}
