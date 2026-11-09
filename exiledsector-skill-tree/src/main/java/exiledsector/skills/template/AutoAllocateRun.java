package exiledsector.skills.template;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public final class AutoAllocateRun {

    public enum Status {
        PATH_END, OUT_OF_POINTS
    }

    public record Summary(Status status, int allocated, int skipped) {
    }

    @FunctionalInterface
    public interface Bridge {
        Bridge NONE = unreachedSteps -> false;

        boolean allocateToward(List<TemplateStep> unreachedSteps);
    }

    private AutoAllocateRun() {
    }

    public static Summary run(List<TemplateStep> steps, Function<TemplateStep, StepVerdict> attempt, BooleanSupplier pointsLeft) {
        return run(steps, attempt, Bridge.NONE, pointsLeft);
    }

    public static Summary run(List<TemplateStep> steps, Function<TemplateStep, StepVerdict> attempt, Bridge bridge, BooleanSupplier pointsLeft) {
        List<TemplateStep> passSteps = List.copyOf(steps);
        int allocatedCount = 0;
        int skippedCount = 0;
        boolean retrying = false;
        while (true) {
            List<TemplateStep> deferredSteps = new ArrayList<>();
            boolean progressed = false;
            for (int stepIndex = 0; stepIndex < passSteps.size(); stepIndex++) {
                if (!pointsLeft.getAsBoolean()) {
                    int notRetried = retrying ? passSteps.size() - stepIndex : 0;
                    return new Summary(Status.OUT_OF_POINTS, allocatedCount, skippedCount + deferredSteps.size() + notRetried);
                }
                TemplateStep step = passSteps.get(stepIndex);
                StepVerdict verdict = attempt.apply(step);
                if (verdict == StepVerdict.ALLOCATE) {
                    allocatedCount++;
                    progressed = true;
                } else if (verdict == StepVerdict.NOT_ALLOCATABLE) {
                    deferredSteps.add(step);
                } else if (verdict != StepVerdict.ALREADY_ALLOCATED) {
                    skippedCount++;
                }
            }
            if (!pointsLeft.getAsBoolean()) {
                return new Summary(Status.OUT_OF_POINTS, allocatedCount, skippedCount + deferredSteps.size());
            }
            boolean bridged = !progressed && !deferredSteps.isEmpty() && bridge.allocateToward(deferredSteps);
            if (deferredSteps.isEmpty() || !progressed && !bridged) {
                return new Summary(Status.PATH_END, allocatedCount, skippedCount + deferredSteps.size());
            }
            if (bridged) {
                allocatedCount++;
            }
            passSteps = deferredSteps;
            retrying = true;
        }
    }
}
