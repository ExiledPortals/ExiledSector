package exiledsector.ui.node;

import exiledsector.skills.SkillNode;

import java.util.Collection;

interface TreeFeedback {

    TreeFeedback NONE = new TreeFeedback() {
    };

    default void allocated(String originId, Collection<String> nodeIds) {
    }

    default void deallocated(String originId, Collection<String> nodeIds) {
    }

    default void pulse(SkillNode node) {
    }

    default void rootChanged(SkillNode chosenRoot) {
    }
}
