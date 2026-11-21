package exiledsector.harness;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.mission.FleetSide;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.run.cleanup.ChartRunCloser;

import java.util.List;

final class SuiteSteps {

    static final float TEARDOWN_TIMEOUT_SECONDS = 60f;
    static final float LEAK_SETTLE_SECONDS = 5f;

    static final class Baseline {
        private HarnessBaseline captured;

        HarnessBaseline captured() {
            return captured;
        }
    }

    private SuiteSteps() {
    }

    static final class CaptureBaselineStep implements HarnessStep {

        private final Baseline baseline;

        CaptureBaselineStep(Baseline baseline) {
            this.baseline = baseline;
        }

        @Override
        public String name() {
            return "captureBaseline";
        }

        @Override
        public StepOutcome advance(StepContext context) throws Throwable {
            StepOutcome notInCampaign = HarnessCampaign.notInCampaign(name());
            if (notInCampaign != null) {
                return notInCampaign;
            }
            baseline.captured = HarnessBaseline.capture(Global.getSector());
            return StepOutcome.passed("baseline " + baseline.captured.describe());
        }
    }

    static final class StartCaseStep implements HarnessStep {

        private final SuiteCase suiteCase;

        StartCaseStep(SuiteCase suiteCase) {
            this.suiteCase = suiteCase;
        }

        SuiteCase suiteCase() {
            return suiteCase;
        }

        @Override
        public String name() {
            return "case " + suiteCase.id();
        }

        @Override
        public StepOutcome advance(StepContext context) {
            context.results().startCase(suiteCase.id());
            return StepOutcome.passed("seed " + suiteCase.seed() + (suiteCase.tags().isEmpty() ? "" : ", tags " + suiteCase.tags()));
        }
    }

    static final class TeardownStep implements HarnessStep {

        private final SuiteCase suiteCase;
        private final Baseline baseline;
        private float restoredAtSeconds = -1f;

        TeardownStep(SuiteCase suiteCase, Baseline baseline) {
            this.suiteCase = suiteCase;
            this.baseline = baseline;
        }

        @Override
        public String name() {
            return "teardown " + suiteCase.id();
        }

        @Override
        public StepOutcome advance(StepContext context) throws Throwable {
            HarnessBaseline captured = baseline.captured();
            if (captured == null) {
                return finish(context, StepOutcome.failed("no baseline was captured before the case"));
            }
            if (Global.getCurrentState() == GameState.COMBAT) {
                endCombat();
                return new StepOutcome(false, false, "ending combat");
            }
            SectorAPI sector = Global.getSector();
            InteractionDialogAPI dialog = sector.getCampaignUI().getCurrentInteractionDialog();
            if (dialog != null) {
                dialog.dismiss();
                return new StepOutcome(false, false, "closing the interaction dialog");
            }
            if (restoredAtSeconds < 0f) {
                captured.restore(sector);
                closeRun(sector);
                ChartAnomalies.forgetHookFailures();
                restoredAtSeconds = context.seconds();
                return new StepOutcome(false, false, "restored the baseline");
            }
            ChartRun stillOpen = ChartRuns.live();
            List<String> leaks = captured.leaks(sector);
            if (stillOpen == null && leaks.isEmpty()) {
                return finish(context, StepOutcome.passed("restored the baseline with nothing left behind"));
            }
            if (context.seconds() - restoredAtSeconds < LEAK_SETTLE_SECONDS) {
                return new StepOutcome(false, false, "waiting for the run to close");
            }
            return finish(context, StepOutcome.failed((stillOpen == null ? "" : "run " + stillOpen.runId() + " is still open; ")
                    + (leaks.isEmpty() ? "" : "left behind " + leaks)));
        }

        private static void endCombat() {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine != null && !engine.isCombatOver()) {
                engine.endCombat(0f, FleetSide.PLAYER);
            }
        }

        private static void closeRun(SectorAPI sector) {
            ChartRun run = ChartRuns.live();
            if (run != null) {
                ChartRunCloser.close(sector, run);
            }
        }

        private StepOutcome finish(StepContext context, StepOutcome outcome) {
            context.results().endCase();
            return outcome;
        }

        @Override
        public float timeoutSeconds() {
            return TEARDOWN_TIMEOUT_SECONDS;
        }
    }
}
