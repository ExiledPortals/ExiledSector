package exiledsector.ui.node;

import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.TemplateCapture;
import exiledsector.skills.template.TemplateStep;
import exiledsector.skills.template.TemplateStepRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

final class TemplatePlanner {

    private final NodeAllocator allocator;
    private final Supplier<NodeAllocator.Snapshot> liveSnapshotSupplier;

    private ShipSkillData planData;
    private String editedTemplateId;
    private List<TemplateStep> templateSteps = List.of();
    private List<TemplateStep> carriedSteps = List.of();
    private NodeAllocator.Snapshot planSnapshot;
    private NodeAllocator.Snapshot planSnapshotLive;
    private Set<String> planNodeIds = Set.of();
    private Set<String> plannedOnlyNodeIds = Set.of();
    private int nodeIdsRevision = -1;
    private NodeAllocator.Snapshot nodeIdsLive;

    TemplatePlanner(NodeAllocator allocator, Supplier<NodeAllocator.Snapshot> liveSnapshotSupplier) {
        this.allocator = allocator;
        this.liveSnapshotSupplier = liveSnapshotSupplier;
    }

    boolean isActive() {
        return planData != null;
    }

    void start(SkillTreeTemplate template) {
        planData = liveSnapshotSupplier.get().skillData().planningCopy();
        editedTemplateId = template == null ? null : template.id();
        templateSteps = template == null ? List.of() : template.steps();
        planSnapshot = null;
        nodeIdsRevision = -1;
        planSteps(templateSteps);
        List<TemplateStep> unplacedSteps = new ArrayList<>();
        for (TemplateStep step : templateSteps) {
            if (!planData.isAllocated(step.nodeId())) {
                unplacedSteps.add(step);
            }
        }
        carriedSteps = List.copyOf(unplacedSteps);
    }

    void stop() {
        planData = null;
        editedTemplateId = null;
        templateSteps = List.of();
        carriedSteps = List.of();
        planSnapshot = null;
        planSnapshotLive = null;
        nodeIdsLive = null;
        planNodeIds = Set.of();
        plannedOnlyNodeIds = Set.of();
    }

    String editedTemplateId() {
        return editedTemplateId;
    }

    void templateSaved(SkillTreeTemplate template) {
        editedTemplateId = template == null ? null : template.id();
        templateSteps = template == null ? List.of() : template.steps();
    }

    NodeAllocator.Snapshot snapshot() {
        NodeAllocator.Snapshot liveSnapshot = liveSnapshotSupplier.get();
        if (planSnapshot == null || planSnapshotLive != liveSnapshot || !planSnapshot.isCurrent(planData)) {
            if (planSnapshotLive != liveSnapshot) {
                planData.mirrorSocketedItems(liveSnapshot.skillData());
            }
            planSnapshot = allocator.planningSnapshot(planData, liveSnapshot);
            planSnapshotLive = liveSnapshot;
        }
        return planSnapshot;
    }

    Set<String> nodeIds() {
        refreshNodeIds();
        return planNodeIds;
    }

    boolean isPlannedOnly(SkillNode node) {
        if (planData == null) {
            return false;
        }
        refreshNodeIds();
        return plannedOnlyNodeIds.contains(node.getId());
    }

    Set<String> plannedOnlyNodeIds() {
        if (planData == null) {
            return Set.of();
        }
        refreshNodeIds();
        return plannedOnlyNodeIds;
    }

    private void refreshNodeIds() {
        NodeAllocator.Snapshot liveSnapshot = liveSnapshotSupplier.get();
        if (nodeIdsRevision == planData.revision() && nodeIdsLive == liveSnapshot) {
            return;
        }
        nodeIdsRevision = planData.revision();
        nodeIdsLive = liveSnapshot;
        planNodeIds = Set.copyOf(planData.getAllocatedNodeIds());
        Set<String> plannedOnly = new HashSet<>(planNodeIds);
        plannedOnly.removeAll(liveSnapshot.skillData().getAllocatedNodeIds());
        plannedOnlyNodeIds = Set.copyOf(plannedOnly);
    }

    boolean add(SkillNode node, SkillType option) {
        NodeAllocator.Snapshot currentPlan = snapshot();
        if (currentPlan.isHidden(node) || !currentPlan.gate().allocation(node, option).allowed()) {
            return false;
        }
        if (option != null) {
            planData.selectOption(node, option, 0);
        } else {
            planData.allocate(node, 0);
        }
        return true;
    }

    boolean remove(SkillNode node) {
        if (!isPlannedOnly(node) || !snapshot().gate().deallocation(node).allowed()) {
            return false;
        }
        planData.deallocate(node);
        return true;
    }

    boolean switchOption(SkillNode node, SkillType option) {
        if (!isPlannedOnly(node) || !snapshot().gate().optionSwitch(node, option).allowed()) {
            return false;
        }
        planData.selectOption(node, option, 0);
        return true;
    }

    List<TemplateStep> captureSteps(String startingRootId) {
        Map<String, String> templateOptionsByNodeId = new HashMap<>();
        Set<String> orderedNodeIds = new LinkedHashSet<>();
        for (TemplateStep step : templateSteps) {
            templateOptionsByNodeId.putIfAbsent(step.nodeId(), step.optionTypeId());
            if (planData.isAllocated(step.nodeId())) {
                orderedNodeIds.add(step.nodeId());
            }
        }
        orderedNodeIds.addAll(planData.getAllocatedNodeIds());
        ShipSkillData liveData = liveSnapshotSupplier.get().skillData();
        List<TemplateStep> steps = new ArrayList<>(TemplateCapture.capture(orderedNodeIds, nodeId -> {
            String templateOption = templateOptionsByNodeId.get(nodeId);
            return templateOption != null && liveData.isAllocated(nodeId) ? templateOption : planData.getOptionalSelection(nodeId);
        }, planData, startingRootId, SkillTree.getAllNodes()));
        for (TemplateStep carriedStep : carriedSteps) {
            if (!planData.isAllocated(carriedStep.nodeId())) {
                steps.add(carriedStep);
            }
        }
        return steps;
    }

    private void planSteps(List<TemplateStep> steps) {
        List<TemplateStep> pendingSteps = new ArrayList<>(steps);
        boolean progressed = true;
        while (progressed) {
            progressed = false;
            for (Iterator<TemplateStep> stepIterator = pendingSteps.iterator(); stepIterator.hasNext(); ) {
                TemplateStep step = stepIterator.next();
                SkillNode node = SkillTree.get(step.nodeId());
                SkillType option = node == null ? null : TemplateStepRules.optionFor(step, node.getType());
                if (node == null || planData.isAllocated(node.getId()) || node.getType().isOptional() && option == null) {
                    stepIterator.remove();
                } else if (add(node, option)) {
                    stepIterator.remove();
                    progressed = true;
                }
            }
        }
    }
}
