package exiledsector.skills.template;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public final class AutoAllocateRun {

    static final float BASE_STEP_SECONDS = 0.15f;
    static final float TARGET_TOTAL_SECONDS = 2.5f;
    static final int MAX_BLOCKED_CHECKS_PER_FRAME = 8;

    public enum Status {
        RUNNING, PATH_END, OUT_OF_POINTS, CANCELLED
    }

    public record Summary(Status status, int allocated, int skipped) {
    }

    private final float stepSeconds;
    private List<TemplateStep> passSteps;
    private List<TemplateStep> deferredSteps = new ArrayList<>();
    private boolean retrying;
    private boolean progressed;
    private float timeBudgetSeconds;
    private int passStepIndex;
    private int allocatedCount;
    private int skippedCount;
    private Status status = Status.RUNNING;

    public AutoAllocateRun(List<TemplateStep> steps, int pendingCount) {
        this.passSteps = List.copyOf(steps);
        this.stepSeconds = stepSecondsFor(pendingCount);
        this.timeBudgetSeconds = stepSeconds;
    }

    public static float stepSecondsFor(int pendingCount) {
        return Math.min(BASE_STEP_SECONDS, TARGET_TOTAL_SECONDS / Math.max(1, pendingCount));
    }

    public void advance(float amount, Function<TemplateStep, StepVerdict> attempt, BooleanSupplier pointsLeft) {
        if (status != Status.RUNNING) {
            return;
        }
        timeBudgetSeconds += amount;
        int blockedChecks = 0;
        while (status == Status.RUNNING) {
            if (!pointsLeft.getAsBoolean()) {
                status = Status.OUT_OF_POINTS;
            } else if (passStepIndex >= passSteps.size()) {
                endPass();
            } else if (timeBudgetSeconds < stepSeconds || blockedChecks >= MAX_BLOCKED_CHECKS_PER_FRAME) {
                return;
            } else {
                TemplateStep step = passSteps.get(passStepIndex++);
                if (tally(step, attempt.apply(step))) {
                    blockedChecks++;
                }
            }
        }
    }

    private boolean tally(TemplateStep step, StepVerdict verdict) {
        if (verdict == StepVerdict.ALLOCATE) {
            allocatedCount++;
            progressed = true;
            timeBudgetSeconds -= stepSeconds;
        } else if (verdict == StepVerdict.NOT_ALLOCATABLE) {
            deferredSteps.add(step);
        } else if (verdict != StepVerdict.ALREADY_ALLOCATED) {
            skippedCount++;
        }
        return verdict == StepVerdict.BLOCKED;
    }

    private void endPass() {
        if (!progressed || deferredSteps.isEmpty()) {
            status = Status.PATH_END;
            return;
        }
        passSteps = deferredSteps;
        deferredSteps = new ArrayList<>();
        passStepIndex = 0;
        retrying = true;
        progressed = false;
    }

    public void cancel() {
        if (status == Status.RUNNING) {
            status = Status.CANCELLED;
        }
    }

    public boolean isFinished() {
        return status != Status.RUNNING;
    }

    public Summary summary() {
        int notRetried = retrying ? passSteps.size() - passStepIndex : 0;
        return new Summary(status, allocatedCount, skippedCount + deferredSteps.size() + notRetried);
    }

    float stepSeconds() {
        return stepSeconds;
    }
}
