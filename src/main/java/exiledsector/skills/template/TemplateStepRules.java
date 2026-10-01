package exiledsector.skills.template;

import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class TemplateStepRules {

    private TemplateStepRules() {
    }

    public static StepVerdict verdict(TemplateStep step, ShipSkillData data, String rootId,
                                      Predicate<SkillNode> canAllocate, BiFunction<SkillNode, SkillType, String> blockReason) {
        SkillNode node = SkillTree.get(step.nodeId());
        if (node == null) {
            return StepVerdict.UNKNOWN_NODE;
        }
        if (node.getId().equals(rootId) || data.isAllocated(node.getId())) {
            return StepVerdict.ALREADY_ALLOCATED;
        }
        SkillType type = node.getType();
        if (type.getItemCost() != null) {
            return StepVerdict.ITEM_COST;
        }
        SkillType option = null;
        if (type.isOptional()) {
            option = optionFor(step, type);
            if (option == null) {
                return StepVerdict.NO_OPTION;
            }
            if (option.getItemCost() != null) {
                return StepVerdict.ITEM_COST;
            }
        }
        if (!canAllocate.test(node)) {
            return StepVerdict.NOT_ALLOCATABLE;
        }
        return blockReason.apply(node, option) == null ? StepVerdict.ALLOCATE : StepVerdict.BLOCKED;
    }

    public static SkillType optionFor(TemplateStep step, SkillType type) {
        String optionId = step.optionTypeId();
        if (optionId == null || !type.getOptionalOptionIds().contains(optionId)) {
            return null;
        }
        return SkillTree.getType(optionId);
    }
}
