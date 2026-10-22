package exiledsector.persistence;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.TemplateCodec;
import exiledsector.skills.template.TemplateNames;
import exiledsector.skills.template.TemplateStep;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class SkillTreeTemplateStore {

    private static final String TEMPLATES_KEY = "exiledSector_skillTreeTemplates";
    private static final String ASSIGNMENTS_KEY = "exiledSector_skillTreeTemplateAssignments";
    private static final Set<String> REPORTED_MALFORMED = new HashSet<>();

    private SkillTreeTemplateStore() {
    }

    public static List<SkillTreeTemplate> all() {
        Map<String, String> encodedTemplatesById = storedTemplates(false);
        if (encodedTemplatesById == null) {
            return List.of();
        }
        List<SkillTreeTemplate> templates = new ArrayList<>();
        for (Map.Entry<String, String> entry : encodedTemplatesById.entrySet()) {
            SkillTreeTemplate template = TemplateCodec.decode(entry.getValue());
            if (template != null) {
                templates.add(template);
            } else {
                reportMalformed(entry.getKey());
            }
        }
        return templates;
    }

    public static SkillTreeTemplate find(String templateId) {
        Map<String, String> encodedTemplatesById = storedTemplates(false);
        return encodedTemplatesById == null || templateId == null ? null : TemplateCodec.decode(encodedTemplatesById.get(templateId));
    }

    public static SkillTreeTemplate save(String templateName, String rootNodeId, HullSize hullSize, List<TemplateStep> steps) {
        return save(templateName, rootNodeId, hullSize, steps, () -> UUID.randomUUID().toString());
    }

    static SkillTreeTemplate save(String templateName, String rootNodeId, HullSize hullSize, List<TemplateStep> steps, Supplier<String> templateIdSupplier) {
        return store(new SkillTreeTemplate(templateIdSupplier.get(), TemplateNames.normalise(templateName), rootNodeId, hullSize, steps));
    }

    public static SkillTreeTemplate replaceSteps(String templateId, List<TemplateStep> steps) {
        SkillTreeTemplate template = find(templateId);
        return template == null ? null : store(new SkillTreeTemplate(template.id(), template.name(), template.rootNodeId(), template.hullSize(), steps));
    }

    public static SkillTreeTemplate rename(String templateId, String templateName) {
        SkillTreeTemplate template = find(templateId);
        return template == null ? null
                : store(new SkillTreeTemplate(template.id(), TemplateNames.normalise(templateName), template.rootNodeId(), template.hullSize(), template.steps()));
    }

    private static SkillTreeTemplate store(SkillTreeTemplate template) {
        storedTemplates(true).put(template.id(), TemplateCodec.encode(template));
        return template;
    }

    public static int assignmentCount(String templateId) {
        return assignedShipIds(templateId).size();
    }

    public static List<String> assignedShipIds(String templateId) {
        Map<String, String> assignments = storedAssignments(false);
        if (assignments == null || templateId == null) {
            return List.of();
        }
        List<String> shipIds = new ArrayList<>();
        for (Map.Entry<String, String> assignment : assignments.entrySet()) {
            if (templateId.equals(assignment.getValue())) {
                shipIds.add(assignment.getKey());
            }
        }
        return shipIds;
    }

    public static boolean delete(String templateId) {
        Map<String, String> encodedTemplatesById = storedTemplates(false);
        if (encodedTemplatesById == null || encodedTemplatesById.remove(templateId) == null) {
            return false;
        }
        Map<String, String> assignments = storedAssignments(false);
        if (assignments != null) {
            assignments.values().removeIf(templateId::equals);
        }
        return true;
    }

    public static void assign(String memberId, String templateId) {
        storedAssignments(true).put(memberId, templateId);
    }

    public static void clearAssignment(String memberId) {
        Map<String, String> assignments = storedAssignments(false);
        if (assignments != null) {
            assignments.remove(memberId);
        }
    }

    public static SkillTreeTemplate assignedTo(String memberId, String rootNodeId) {
        Map<String, String> assignments = storedAssignments(false);
        if (assignments == null || rootNodeId == null) {
            return null;
        }
        SkillTreeTemplate template = find(assignments.get(memberId));
        return template != null && template.rootNodeId().equals(rootNodeId) ? template : null;
    }

    public static void pruneAssignments(Predicate<String> shipStillNeeded) {
        Map<String, String> assignments = storedAssignments(false);
        if (assignments == null) {
            return;
        }
        Map<String, String> encodedTemplatesById = storedTemplates(false);
        assignments.entrySet().removeIf(entry -> encodedTemplatesById == null || !encodedTemplatesById.containsKey(entry.getValue())
                || !shipStillNeeded.test(entry.getKey()));
    }

    private static void reportMalformed(String templateId) {
        if (REPORTED_MALFORMED.add(templateId)) {
            Logger.getLogger(SkillTreeTemplateStore.class).warn("[ExiledSector] Skill tree template " + templateId + " could not be read and is ignored.");
        }
    }

    private static Map<String, String> storedTemplates(boolean createIfMissing) {
        return createIfMissing ? SaveData.getOrCreate(TEMPLATES_KEY, LinkedHashMap::new) : SaveData.findOrNull(TEMPLATES_KEY, Map.class);
    }

    private static Map<String, String> storedAssignments(boolean createIfMissing) {
        return createIfMissing ? SaveData.getOrCreate(ASSIGNMENTS_KEY, HashMap::new) : SaveData.findOrNull(ASSIGNMENTS_KEY, Map.class);
    }
}
