package exiledsector.ui.node;

import exiledsector.skills.SkillNode;

import java.util.Collection;

interface TreeFeedback {

    TreeFeedback NONE = new TreeFeedback() {
    };

    default void allocated(SkillNode node) {
    }

    default void deallocated(SkillNode node) {
    }

    default void released(Collection<String> releasedNodeIds) {
    }

    default void pulse(SkillNode node) {
    }

    default void rootChanged(SkillNode chosenRoot) {
    }
}
