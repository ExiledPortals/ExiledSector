package exiledsector.persistence;

import com.fs.starfarer.api.Global;
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
        SkillTreeTemplate template = new SkillTreeTemplate(templateIdSupplier.get(), TemplateNames.normalise(templateName), rootNodeId, hullSize, steps);
        storedTemplates(true).put(template.id(), TemplateCodec.encode(template));
        return template;
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

    // persistentData is a raw Object map; this key is only ever written as Map<String, String>
    @SuppressWarnings("unchecked")
    private static Map<String, String> storedTemplates(boolean createIfMissing) {
        Map<String, Object> persistentData = Global.getSector().getPersistentData();
        if (createIfMissing) {
            return (Map<String, String>) persistentData.computeIfAbsent(TEMPLATES_KEY, key -> new LinkedHashMap<String, String>());
        }
        return persistentData.get(TEMPLATES_KEY) instanceof Map<?, ?> storedMap ? (Map<String, String>) storedMap : null;
    }

    // persistentData is a raw Object map; this key is only ever written as Map<String, String>
    @SuppressWarnings("unchecked")
    private static Map<String, String> storedAssignments(boolean createIfMissing) {
        Map<String, Object> persistentData = Global.getSector().getPersistentData();
        if (createIfMissing) {
            return (Map<String, String>) persistentData.computeIfAbsent(ASSIGNMENTS_KEY, key -> new HashMap<String, String>());
        }
        return persistentData.get(ASSIGNMENTS_KEY) instanceof Map<?, ?> storedMap ? (Map<String, String>) storedMap : null;
    }
}
