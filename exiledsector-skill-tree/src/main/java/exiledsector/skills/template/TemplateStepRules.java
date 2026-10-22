package exiledsector.skills.template;

import exiledsector.items.DoctrineOverrides;
import exiledsector.skills.AllocationGate;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;

import java.util.List;
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
        SkillType optionType = null;
        if (node.offersOptions(shipData)) {
            optionType = optionFor(step, node.getType());
            if (optionType == null) {
                return StepVerdict.NO_OPTION;
            }
        }
        if (NodeEligibility.itemCost(DoctrineOverrides.asTransformed(shipData, node), optionType) != null) {
            return StepVerdict.ITEM_COST;
        }
        AllocationGate.Verdict verdict = allocationVerdict.apply(node, optionType);
        if (verdict.allowed()) {
            return StepVerdict.ALLOCATE;
        }
        return verdict.refusal() == AllocationGate.Refusal.INELIGIBLE ? StepVerdict.BLOCKED : StepVerdict.NOT_ALLOCATABLE;
    }

    public static TemplateStep firstAllocatable(List<TemplateStep> steps, ShipSkillData shipData, String rootId, AllocationGate gate) {
        for (TemplateStep step : steps) {
            if (verdict(step, shipData, rootId, gate::allocation) == StepVerdict.ALLOCATE) {
                return step;
            }
        }
        return null;
    }

    public static SkillType optionFor(TemplateStep step, SkillNode node, ShipSkillData shipData) {
        return node.offersOptions(shipData) ? optionFor(step, node.getType()) : null;
    }

    public static SkillType optionFor(TemplateStep step, SkillType nodeType) {
        return NodeEligibility.validOption(nodeType, step.optionTypeId());
    }
}
