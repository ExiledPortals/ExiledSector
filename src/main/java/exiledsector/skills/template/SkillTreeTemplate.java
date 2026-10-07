package exiledsector.skills.template;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.SkillTree;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record SkillTreeTemplate(String id, String name, String rootNodeId, HullSize hullSize, List<TemplateStep> steps) {

    public SkillTreeTemplate {
        steps = List.copyOf(steps);
    }

    public Set<String> nodeIds() {
        Set<String> templateNodeIds = new LinkedHashSet<>();
        templateNodeIds.add(rootNodeId);
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
