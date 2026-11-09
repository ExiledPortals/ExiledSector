package exiledsector.skills.template;

import exiledsector.skills.AffinityOptions;
import exiledsector.skills.AllocationCheck;
import exiledsector.skills.AllocationPath;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.SkillType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

public final class TemplateBridge {

    private final Map<String, TemplateStep> stepsByNodeId = new HashMap<>();
    private final List<SkillType> templateSelections;

    public TemplateBridge(List<TemplateStep> templateSteps) {
        for (TemplateStep step : templateSteps) {
            stepsByNodeId.putIfAbsent(step.nodeId(), step);
        }
        templateSelections = selections(templateSteps);
    }

    public static boolean crossesRoots(SkillTreeTemplate template, String startingRootId) {
        return template != null && startingRootId != null && !startingRootId.equals(template.rootNodeId());
    }

    public static List<SkillType> selections(List<TemplateStep> steps) {
        List<SkillType> selections = new ArrayList<>();
        for (TemplateStep step : steps) {
            SkillType selection = step.optionTypeId() == null ? null : SkillTree.getType(AffinityOptions.currentOptionId(step.optionTypeId()));
            if (selection != null) {
                selections.add(selection);
            }
        }
        return selections;
    }

    public AllocationPath.Step nextStep(List<TemplateStep> unreachedSteps, AllocationCheck gate, SkillTreeTopology topology, Predicate<SkillNode> hidden,
                                        RandomGenerator random) {
        List<SkillNode> targets = new ArrayList<>();
        for (TemplateStep step : unreachedSteps) {
            SkillNode target = SkillTree.get(step.nodeId());
            if (target != null && !targets.contains(target)) {
                targets.add(target);
            }
        }
        Predicate<SkillNode> closed = node -> hidden.test(node) || !open(node, gate);
        AllocationPath.Step step = AllocationPath.towardNearest(targets, gate, topology, closed, new AllocationPath.Choices(null, templateSelections), random);
        if (step == null) {
            return null;
        }
        TemplateStep templateStep = stepsByNodeId.get(step.node().getId());
        SkillType option = templateStep == null ? step.option() : TemplateStepRules.optionFor(templateStep, step.node(), gate.data());
        return gate.itemCost(step.node(), option) == null ? new AllocationPath.Step(step.node(), option) : null;
    }

    private boolean open(SkillNode node, AllocationCheck gate) {
        TemplateStep templateStep = stepsByNodeId.get(node.getId());
        if (templateStep == null) {
            return gate.itemCost(node, null) == null;
        }
        SkillType option = TemplateStepRules.optionFor(templateStep, node, gate.data());
        return (option != null || !gate.offersOptions(node)) && gate.itemCost(node, option) == null && gate.eligibility(node, option).allowed();
    }
}
