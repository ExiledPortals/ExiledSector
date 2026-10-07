package exiledsector.ui.node;

import exiledsector.skills.SkillNode;
import exiledsector.skills.template.AutoAllocateRun;

import java.util.List;
import java.util.function.Predicate;

final class RespecRun {

    private final List<SkillNode> steps;
    private final float stepSeconds;
    private float accumulatedSeconds;
    private int nextStepIndex;
    private boolean stopped;

    RespecRun(List<SkillNode> steps) {
        this.steps = List.copyOf(steps);
        this.stepSeconds = AutoAllocateRun.stepSecondsFor(this.steps.size());
        this.accumulatedSeconds = stepSeconds;
    }

    void advance(float amount, Predicate<SkillNode> removeStep) {
        if (isFinished()) {
            return;
        }
        accumulatedSeconds += amount;
        while (!isFinished() && accumulatedSeconds >= stepSeconds) {
            accumulatedSeconds -= stepSeconds;
            if (!removeStep.test(steps.get(nextStepIndex++))) {
                stopped = true;
            }
        }
    }

    void cancel() {
        stopped = true;
    }

    boolean isFinished() {
        return stopped || nextStepIndex >= steps.size();
    }

    float stepSeconds() {
        return stepSeconds;
    }
}
