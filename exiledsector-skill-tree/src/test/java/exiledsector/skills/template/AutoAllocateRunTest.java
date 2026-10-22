package exiledsector.skills.template;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoAllocateRunTest {

    private static List<TemplateStep> steps(int count) {
        List<TemplateStep> steps = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            steps.add(new TemplateStep("node_" + i, null));
        }
        return steps;
    }

    private static Function<TemplateStep, StepVerdict> pattern(StepVerdict... verdicts) {
        return step -> verdicts[Integer.parseInt(step.nodeId().substring(5)) % verdicts.length];
    }

    private static float secondsToFinish(int count, float frameSeconds, Function<TemplateStep, StepVerdict> attempt) {
        AutoAllocateRun run = new AutoAllocateRun(steps(count), count);
        float elapsed = 0f;
        while (!run.isFinished() && elapsed < 60f) {
            run.advance(frameSeconds, attempt, () -> true);
            elapsed += frameSeconds;
        }
        return elapsed;
    }

    @Test
    void theFirstNodeIsAllocatedOnTheFirstFrame() {
        AutoAllocateRun run = new AutoAllocateRun(steps(10), 10);
        List<String> attempted = new ArrayList<>();

        run.advance(0.001f, step -> {
            attempted.add(step.nodeId());
            return StepVerdict.ALLOCATE;
        }, () -> true);

        assertEquals(List.of("node_0"), attempted);
    }

    @Test
    void stepsAreSpacedSoTheWholeRunFitsTheTarget() {
        assertEquals(AutoAllocateRun.BASE_STEP_SECONDS, new AutoAllocateRun(steps(5), 5).stepSeconds(), 1e-6f);
        assertEquals(AutoAllocateRun.TARGET_TOTAL_SECONDS / 60f, new AutoAllocateRun(steps(60), 60).stepSeconds(), 1e-6f);
    }

    @Test
    void theWholeRunNeverTakesMoreThanThreeSeconds() {
        for (int count : new int[]{1, 5, 20, 60, 120}) {
            for (float frame : new float[]{1f / 60f, 1f / 15f}) {
                assertTrue(secondsToFinish(count, frame, step -> StepVerdict.ALLOCATE) <= 3f, count + " allocations at " + frame);
                assertTrue(secondsToFinish(count, frame, step -> StepVerdict.BLOCKED) <= 3f, count + " blocked at " + frame);
                assertTrue(secondsToFinish(count, frame, pattern(StepVerdict.ALLOCATE, StepVerdict.BLOCKED, StepVerdict.NOT_ALLOCATABLE,
                        StepVerdict.ALREADY_ALLOCATED)) <= 3f, count + " mixed at " + frame);
            }
        }
    }

    @Test
    void cheapSkipsCostNoTimeAndBlockedChecksAreLimitedPerFrame() {
        AutoAllocateRun cheap = new AutoAllocateRun(steps(50), 50);
        cheap.advance(0.001f, step -> StepVerdict.NOT_ALLOCATABLE, () -> true);
        assertTrue(cheap.isFinished());

        AutoAllocateRun blocked = new AutoAllocateRun(steps(50), 50);
        blocked.advance(0.001f, step -> StepVerdict.BLOCKED, () -> true);
        assertFalse(blocked.isFinished());
        assertEquals(AutoAllocateRun.MAX_BLOCKED_CHECKS_PER_FRAME, blocked.summary().skipped());
    }

    @Test
    void theRunStopsBeforeAnotherAttemptOnceNoPointsAreLeft() {
        AutoAllocateRun run = new AutoAllocateRun(steps(10), 10);
        List<String> attempted = new ArrayList<>();
        int[] budget = {2};

        for (int frame = 0; frame < 100 && !run.isFinished(); frame++) {
            run.advance(0.2f, step -> {
                attempted.add(step.nodeId());
                budget[0]--;
                return StepVerdict.ALLOCATE;
            }, () -> budget[0] > 0);
        }

        assertEquals(List.of("node_0", "node_1"), attempted);
        assertEquals(new AutoAllocateRun.Summary(AutoAllocateRun.Status.OUT_OF_POINTS, 2, 0), run.summary());
    }

    @Test
    void theRunEndsAtTheEndOfThePathAndCountsWhatItDid() {
        AutoAllocateRun run = new AutoAllocateRun(steps(4), 4);

        for (int frame = 0; frame < 100 && !run.isFinished(); frame++) {
            run.advance(0.5f, pattern(StepVerdict.ALLOCATE, StepVerdict.ITEM_COST, StepVerdict.ALREADY_ALLOCATED), () -> true);
        }

        assertEquals(new AutoAllocateRun.Summary(AutoAllocateRun.Status.PATH_END, 2, 1), run.summary());
    }

    private static Function<TemplateStep, StepVerdict> needsParent(Set<String> allocated, List<String> attempted,
                                                              String child, String parent) {
        return step -> {
            attempted.add(step.nodeId());
            if (step.nodeId().equals(child) && !allocated.contains(parent)) {
                return StepVerdict.NOT_ALLOCATABLE;
            }
            allocated.add(step.nodeId());
            return StepVerdict.ALLOCATE;
        };
    }

    @Test
    void aNodeWhoseParentComesLaterInThePathIsAllocatedOnARetryPass() {
        AutoAllocateRun run = new AutoAllocateRun(steps(3), 3);
        List<String> attempted = new ArrayList<>();
        Function<TemplateStep, StepVerdict> attempt = needsParent(new HashSet<>(), attempted, "node_0", "node_2");

        for (int frame = 0; frame < 100 && !run.isFinished(); frame++) {
            run.advance(0.5f, attempt, () -> true);
        }

        assertEquals(List.of("node_0", "node_1", "node_2", "node_0"), attempted);
        assertEquals(new AutoAllocateRun.Summary(AutoAllocateRun.Status.PATH_END, 3, 0), run.summary());
    }

    @Test
    void retryingStopsOnceAPassAllocatesNothingAndCountsWhatItCouldNotAllocate() {
        AutoAllocateRun run = new AutoAllocateRun(steps(2), 2);
        List<String> attempted = new ArrayList<>();
        Function<TemplateStep, StepVerdict> attempt = needsParent(new HashSet<>(), attempted, "node_0", "missing");

        for (int frame = 0; frame < 100 && !run.isFinished(); frame++) {
            run.advance(0.5f, attempt, () -> true);
        }

        assertEquals(List.of("node_0", "node_1", "node_0"), attempted);
        assertEquals(new AutoAllocateRun.Summary(AutoAllocateRun.Status.PATH_END, 1, 1), run.summary());
    }

    @Test
    void cancellingDuringARetryPassCountsEveryStepStillWaitingAsSkipped() {
        AutoAllocateRun run = new AutoAllocateRun(steps(3), 3);
        Set<String> allocated = new HashSet<>();
        List<String> attempted = new ArrayList<>();
        Function<TemplateStep, StepVerdict> attempt = step -> {
            attempted.add(step.nodeId());
            if (!step.nodeId().equals("node_2") && !allocated.contains("node_2")) {
                return StepVerdict.NOT_ALLOCATABLE;
            }
            allocated.add(step.nodeId());
            return StepVerdict.ALLOCATE;
        };

        for (int frame = 0; frame < 100 && attempted.size() < 4; frame++) {
            run.advance(0.01f, attempt, () -> true);
        }
        run.cancel();

        assertEquals(List.of("node_0", "node_1", "node_2", "node_0"), attempted);
        assertEquals(new AutoAllocateRun.Summary(AutoAllocateRun.Status.CANCELLED, 2, 1), run.summary());
    }

    @Test
    void aCancelledRunStopsAttempting() {
        AutoAllocateRun run = new AutoAllocateRun(steps(10), 10);
        run.cancel();
        run.advance(5f, step -> {
            throw new AssertionError("should not attempt after cancel");
        }, () -> true);

        assertEquals(AutoAllocateRun.Status.CANCELLED, run.summary().status());
    }
}
