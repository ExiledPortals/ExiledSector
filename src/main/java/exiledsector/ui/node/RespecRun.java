package exiledsector.ui.node;

import exiledsector.skills.SkillNode;
import exiledsector.skills.template.AutoAllocateRun;

import java.util.List;
import java.util.function.Predicate;

final class RespecRun {

    private final List<SkillNode> steps;
    private final float stepSeconds;
    private float budget;
    private int index;
    private boolean stopped;

    RespecRun(List<SkillNode> steps) {
        this.steps = List.copyOf(steps);
        this.stepSeconds = AutoAllocateRun.stepSecondsFor(this.steps.size());
        this.budget = stepSeconds;
    }

    void advance(float amount, Predicate<SkillNode> removeStep) {
        if (isFinished()) {
            return;
        }
        budget += amount;
        while (!isFinished() && budget >= stepSeconds) {
            budget -= stepSeconds;
            if (!removeStep.test(steps.get(index++))) {
                stopped = true;
            }
        }
    }

    void cancel() {
        stopped = true;
    }

    boolean isFinished() {
        return stopped || index >= steps.size();
    }

    float stepSeconds() {
        return stepSeconds;
    }
}
