package exiledsector.skills.template;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.SkillTree;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record SkillTreeTemplate(String id, String name, String rootNodeId, HullSize hullSize, List<TemplateStep> steps,
                                Map<String, String> anchorsBySocket, Set<String> integrationMarks) {

    public SkillTreeTemplate {
        steps = List.copyOf(steps);
        anchorsBySocket = anchorsOnSteps(anchorsBySocket, steps);
        integrationMarks = marksOnSteps(integrationMarks, steps);
    }

    public SkillTreeTemplate(String id, String name, String rootNodeId, HullSize hullSize, List<TemplateStep> steps) {
        this(id, name, rootNodeId, hullSize, steps, Map.of());
    }

    public SkillTreeTemplate(String id, String name, String rootNodeId, HullSize hullSize, List<TemplateStep> steps,
                             Map<String, String> anchorsBySocket) {
        this(id, name, rootNodeId, hullSize, steps, anchorsBySocket, Set.of());
    }

    public boolean marksForIntegration(String nodeId) {
        return integrationMarks.contains(nodeId);
    }

    private static Set<String> marksOnSteps(Set<String> integrationMarks, List<TemplateStep> steps) {
        if (integrationMarks == null || integrationMarks.isEmpty()) {
            return Set.of();
        }
        Set<String> kept = new LinkedHashSet<>();
        for (TemplateStep step : steps) {
            if (integrationMarks.contains(step.nodeId())) {
                kept.add(step.nodeId());
            }
        }
        return Collections.unmodifiableSet(kept);
    }

    private static Map<String, String> anchorsOnSteps(Map<String, String> anchorsBySocket, List<TemplateStep> steps) {
        if (anchorsBySocket == null || anchorsBySocket.isEmpty()) {
            return Map.of();
        }
        Map<String, String> kept = new HashMap<>();
        for (TemplateStep step : steps) {
            String anchorDefinitionId = anchorsBySocket.get(step.nodeId());
            if (anchorDefinitionId != null) {
                kept.put(step.nodeId(), anchorDefinitionId);
            }
        }
        return Map.copyOf(kept);
    }

    public Set<String> nodeIds(String startingRootId) {
        Set<String> templateNodeIds = new LinkedHashSet<>();
        templateNodeIds.add(startingRootId == null ? rootNodeId : startingRootId);
        for (TemplateStep step : steps) {
            templateNodeIds.add(step.nodeId());
        }
        return Set.copyOf(templateNodeIds);
    }

    public int knownStepCount() {
        int knownCount = 0;
        for (TemplateStep step : steps) {
            if (SkillTree.get(step.nodeId()) != null) {
                knownCount++;
            }
        }
        return knownCount;
    }
}
