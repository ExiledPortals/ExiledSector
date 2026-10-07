package exiledsector.skills.template;

import exiledsector.skills.NodeEligibility;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class TemplateStepRules {

    private TemplateStepRules() {
    }

    public static StepVerdict verdict(TemplateStep step, ShipSkillData shipData, String rootId,
                                      Predicate<SkillNode> canAllocate, BiFunction<SkillNode, SkillType, String> blockReason) {
        SkillNode node = SkillTree.get(step.nodeId());
        if (node == null) {
            return StepVerdict.UNKNOWN_NODE;
        }
        if (node.getId().equals(rootId) || shipData.isAllocated(node.getId())) {
            return StepVerdict.ALREADY_ALLOCATED;
        }
        SkillType nodeType = node.getType();
        if (nodeType.getItemCost() != null) {
            return StepVerdict.ITEM_COST;
        }
        SkillType optionType = null;
        if (nodeType.isOptional()) {
            optionType = optionFor(step, nodeType);
            if (optionType == null) {
                return StepVerdict.NO_OPTION;
            }
            if (optionType.getItemCost() != null) {
                return StepVerdict.ITEM_COST;
            }
        }
        if (!canAllocate.test(node)) {
            return StepVerdict.NOT_ALLOCATABLE;
        }
        return blockReason.apply(node, optionType) == null ? StepVerdict.ALLOCATE : StepVerdict.BLOCKED;
    }

    public static SkillType optionFor(TemplateStep step, SkillType nodeType) {
        return NodeEligibility.validOption(nodeType, step.optionTypeId());
    }
}
