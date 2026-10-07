package exiledsector.ui.node;

import exiledsector.skills.SkillNode;

interface TreeFeedback {

    TreeFeedback NONE = new TreeFeedback() {
    };

    default void allocated(SkillNode node) {
    }

    default void deallocated(SkillNode node) {
    }

    default void pulse(SkillNode node) {
    }

    default void rootChanged(SkillNode chosenRoot) {
    }
}
