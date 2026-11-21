package exiledsector.harness;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;

import java.util.List;

final class HarnessRun implements EveryFrameScript {

    private static final Logger LOG = Logger.getLogger(HarnessRun.class);
    private static final float NANOS_PER_SECOND = 1_000_000_000f;
    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final List<HarnessStep> steps;
    private final boolean quitWhenDone;
    private final HarnessLog log;
    private final HarnessResults results;
    private final Runnable quit;
    private final ScreenCapture capture;
    private int stepIndex;
    private int stepFrame;
    private long stepStartNanos;
    private int stepFirstLogLine;
    private boolean done;

    HarnessRun(HarnessScenario scenario, HarnessLog log, HarnessResults results, Runnable quit) {
        this.steps = scenario.steps();
        this.quitWhenDone = scenario.quitWhenDone();
        this.log = log;
        this.results = results;
        this.quit = quit;
        this.capture = new ScreenCapture(results);
        Global.getSector().getListenerManager().addListener(capture, true);
        startStep();
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    // java:S1181: a step drives game code that may throw anything, and the run must record it and carry on
    @SuppressWarnings("java:S1181")
    @Override
    public void advance(float amount) {
        if (done) {
            return;
        }
        capture.advance();
        if (stepIndex >= steps.size()) {
            finish();
            return;
        }
        HarnessStep step = steps.get(stepIndex);
        float seconds = (System.nanoTime() - stepStartNanos) / NANOS_PER_SECOND;
        StepOutcome outcome;
        try {
            outcome = step.advance(new StepContext(stepFrame, seconds, log, stepFirstLogLine, capture, results));
        } catch (Throwable e) {
            LOG.error(Harness.LOG_TAG + "step " + stepIndex + " " + step.name() + " threw", e);
            outcome = StepOutcome.failed("threw " + e);
        }
        if (!outcome.finished() && seconds >= step.timeoutSeconds()) {
            outcome = StepOutcome.failed("timed out after " + Math.round(seconds) + " s" + (outcome.detail().isEmpty() ? "" : ": " + outcome.detail()));
        }
        stepFrame++;
        if (outcome.finished()) {
            long millis = (System.nanoTime() - stepStartNanos) / NANOS_PER_MILLI;
            results.add(new HarnessResults.StepResult(stepIndex, step.name(), outcome.passed(), outcome.detail(), millis));
            stepIndex++;
            startStep();
        }
    }

    void renderInCombat() {
        if (!done) {
            capture.renderInCombat();
        }
    }

    private void startStep() {
        stepFrame = 0;
        stepStartNanos = System.nanoTime();
        stepFirstLogLine = log.lineCount();
        if (stepIndex < steps.size()) {
            LOG.info(Harness.LOG_TAG + "step " + stepIndex + " " + steps.get(stepIndex).name());
        }
    }

    private void finish() {
        done = true;
        Global.getSector().getListenerManager().removeListener(capture);
        results.finish();
        LOG.info(Harness.LOG_TAG + "scenario finished");
        if (quitWhenDone) {
            quit.run();
        }
    }
}
