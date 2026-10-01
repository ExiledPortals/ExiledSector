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
    private List<TemplateStep> pass;
    private List<TemplateStep> deferred = new ArrayList<>();
    private boolean retrying;
    private boolean progressed;
    private float budget;
    private int index;
    private int allocated;
    private int skipped;
    private Status status = Status.RUNNING;

    public AutoAllocateRun(List<TemplateStep> steps, int pendingCount) {
        this.pass = List.copyOf(steps);
        this.stepSeconds = Math.min(BASE_STEP_SECONDS, TARGET_TOTAL_SECONDS / Math.max(1, pendingCount));
        this.budget = stepSeconds;
    }

    public void advance(float amount, Function<TemplateStep, StepVerdict> attempt, BooleanSupplier pointsLeft) {
        if (status != Status.RUNNING) {
            return;
        }
        budget += amount;
        int blockedChecks = 0;
        while (status == Status.RUNNING) {
            if (!pointsLeft.getAsBoolean()) {
                status = Status.OUT_OF_POINTS;
            } else if (index >= pass.size()) {
                endPass();
            } else if (budget < stepSeconds || blockedChecks >= MAX_BLOCKED_CHECKS_PER_FRAME) {
                return;
            } else {
                TemplateStep step = pass.get(index++);
                if (tally(step, attempt.apply(step))) {
                    blockedChecks++;
                }
            }
        }
    }

    private boolean tally(TemplateStep step, StepVerdict verdict) {
        if (verdict == StepVerdict.ALLOCATE) {
            allocated++;
            progressed = true;
            budget -= stepSeconds;
        } else if (verdict == StepVerdict.NOT_ALLOCATABLE) {
            deferred.add(step);
        } else if (verdict != StepVerdict.ALREADY_ALLOCATED) {
            skipped++;
        }
        return verdict == StepVerdict.BLOCKED;
    }

    private void endPass() {
        if (!progressed || deferred.isEmpty()) {
            status = Status.PATH_END;
            return;
        }
        pass = deferred;
        deferred = new ArrayList<>();
        index = 0;
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
        int notRetried = retrying ? pass.size() - index : 0;
        return new Summary(status, allocated, skipped + deferred.size() + notRetried);
    }

    float stepSeconds() {
        return stepSeconds;
    }
}
