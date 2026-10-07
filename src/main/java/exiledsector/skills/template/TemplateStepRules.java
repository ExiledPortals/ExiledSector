package exiledsector.skills.template;

import exiledsector.skills.AllocationGate;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;

import java.util.function.BiFunction;

public final class TemplateStepRules {

    private TemplateStepRules() {
    }

    public static StepVerdict verdict(TemplateStep step, ShipSkillData shipData, String rootId,
                                      BiFunction<SkillNode, SkillType, AllocationGate.Verdict> allocationVerdict) {
        SkillNode node = SkillTree.get(step.nodeId());
        if (node == null) {
            return StepVerdict.UNKNOWN_NODE;
        }
        if (node.getId().equals(rootId) || shipData.isAllocated(node.getId())) {
            return StepVerdict.ALREADY_ALLOCATED;
        }
        SkillType nodeType = node.getType();
        SkillType optionType = null;
        if (nodeType.isOptional()) {
            optionType = optionFor(step, nodeType);
            if (optionType == null) {
                return StepVerdict.NO_OPTION;
            }
        }
        if (NodeEligibility.itemCost(node, optionType) != null) {
            return StepVerdict.ITEM_COST;
        }
        AllocationGate.Verdict verdict = allocationVerdict.apply(node, optionType);
        if (verdict.allowed()) {
            return StepVerdict.ALLOCATE;
        }
        return verdict.refusal() == AllocationGate.Refusal.INELIGIBLE ? StepVerdict.BLOCKED : StepVerdict.NOT_ALLOCATABLE;
    }

    public static SkillType optionFor(TemplateStep step, SkillType nodeType) {
        return NodeEligibility.validOption(nodeType, step.optionTypeId());
    }
}
