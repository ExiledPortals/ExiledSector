package exiledsector.skills.template;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    void theWholePathIsAllocatedInOneCall() {
        List<String> attempted = new ArrayList<>();

        AutoAllocateRun.Summary summary = AutoAllocateRun.run(steps(50), step -> {
            attempted.add(step.nodeId());
            return StepVerdict.ALLOCATE;
        }, () -> true);

        assertEquals(50, attempted.size());
        assertEquals(new AutoAllocateRun.Summary(AutoAllocateRun.Status.PATH_END, 50, 0), summary);
    }

    @Test
    void blockedStepsAreCountedAsSkipped() {
        assertEquals(new AutoAllocateRun.Summary(AutoAllocateRun.Status.PATH_END, 0, 50),
                AutoAllocateRun.run(steps(50), step -> StepVerdict.BLOCKED, () -> true));
    }

    @Test
    void theRunStopsBeforeAnotherAttemptOnceNoPointsAreLeft() {
        List<String> attempted = new ArrayList<>();
        int[] budget = {2};

        AutoAllocateRun.Summary summary = AutoAllocateRun.run(steps(10), step -> {
            attempted.add(step.nodeId());
            budget[0]--;
            return StepVerdict.ALLOCATE;
        }, () -> budget[0] > 0);

        assertEquals(List.of("node_0", "node_1"), attempted);
        assertEquals(new AutoAllocateRun.Summary(AutoAllocateRun.Status.OUT_OF_POINTS, 2, 0), summary);
    }

    @Test
    void theRunEndsAtTheEndOfThePathAndCountsWhatItDid() {
        AutoAllocateRun.Summary summary = AutoAllocateRun.run(steps(4),
                pattern(StepVerdict.ALLOCATE, StepVerdict.ITEM_COST, StepVerdict.ALREADY_ALLOCATED), () -> true);

        assertEquals(new AutoAllocateRun.Summary(AutoAllocateRun.Status.PATH_END, 2, 1), summary);
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
        List<String> attempted = new ArrayList<>();

        AutoAllocateRun.Summary summary = AutoAllocateRun.run(steps(3), needsParent(new HashSet<>(), attempted, "node_0", "node_2"),
                () -> true);

        assertEquals(List.of("node_0", "node_1", "node_2", "node_0"), attempted);
        assertEquals(new AutoAllocateRun.Summary(AutoAllocateRun.Status.PATH_END, 3, 0), summary);
    }

    @Test
    void retryingStopsOnceAPassAllocatesNothingAndCountsWhatItCouldNotAllocate() {
        List<String> attempted = new ArrayList<>();

        AutoAllocateRun.Summary summary = AutoAllocateRun.run(steps(2), needsParent(new HashSet<>(), attempted, "node_0", "missing"),
                () -> true);

        assertEquals(List.of("node_0", "node_1", "node_0"), attempted);
        assertEquals(new AutoAllocateRun.Summary(AutoAllocateRun.Status.PATH_END, 1, 1), summary);
    }

    @Test
    void runningOutOfPointsDuringARetryPassCountsEveryStepStillWaitingAsSkipped() {
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

        AutoAllocateRun.Summary summary = AutoAllocateRun.run(steps(3), attempt, () -> attempted.size() < 4);

        assertEquals(List.of("node_0", "node_1", "node_2", "node_0"), attempted);
        assertEquals(new AutoAllocateRun.Summary(AutoAllocateRun.Status.OUT_OF_POINTS, 2, 1), summary);
    }
}
