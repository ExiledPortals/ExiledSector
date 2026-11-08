package exiledsector.persistence;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.TemplateCodec;
import exiledsector.skills.template.TemplateNames;
import exiledsector.skills.template.TemplateStep;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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

    public static final String COMMON_FILE = "exiledSector_skillTreeTemplates.json";
    static final String UNREADABLE_BACKUP_FILE = "exiledSector_skillTreeTemplates.unreadable.json";
    private static final String TEMPLATES_FIELD = "templates";
    private static final String VERSION_FIELD = "v";
    private static final int FILE_VERSION = 1;
    private static final String SAVED_TEMPLATES_KEY = "exiledSector_skillTreeTemplates";
    private static final String ASSIGNMENTS_KEY = "exiledSector_skillTreeTemplateAssignments";
    private static final Logger LOG = Logger.getLogger(SkillTreeTemplateStore.class);
    private static final Map<String, SkillTreeTemplate> TEMPLATES_BY_ID = new LinkedHashMap<>();
    private static final List<Object> UNREADABLE_ENTRIES = new ArrayList<>();
    private static final Set<String> REPORTED_UNREADABLE = new HashSet<>();
    private static TemplateFile file = new CommonFolderFile();
    private static String unreadableFileContents;

    public interface TemplateFile {
        String read() throws IOException;

        void write(String fileName, String contents) throws IOException;
    }

    private SkillTreeTemplateStore() {
    }

    public static void reload() {
        String contents;
        try {
            contents = file.read();
        } catch (IOException e) {
            LOG.warn("[ExiledSector] Could not read skill tree templates from " + COMMON_FILE + "; keeping the ones already loaded.", e);
            return;
        }
        if (contents == null || contents.isBlank()) {
            TEMPLATES_BY_ID.clear();
            UNREADABLE_ENTRIES.clear();
            unreadableFileContents = null;
            return;
        }
        JSONArray templateEntries = templateEntries(contents.strip());
        if (templateEntries == null) {
            if (REPORTED_UNREADABLE.add(contents)) {
                LOG.warn("[ExiledSector] " + COMMON_FILE + " is not valid JSON; keeping the templates already loaded. The next template change"
                        + " backs it up to " + UNREADABLE_BACKUP_FILE + " and rewrites it.");
            }
            unreadableFileContents = contents;
            return;
        }
        unreadableFileContents = null;
        TEMPLATES_BY_ID.clear();
        UNREADABLE_ENTRIES.clear();
        for (int i = 0; i < templateEntries.length(); i++) {
            Object entry = templateEntries.opt(i);
            SkillTreeTemplate template = entry instanceof JSONObject templateJson ? TemplateCodec.fromJson(templateJson) : null;
            if (template != null) {
                TEMPLATES_BY_ID.put(template.id(), template);
            } else {
                UNREADABLE_ENTRIES.add(entry);
                if (REPORTED_UNREADABLE.add(String.valueOf(entry))) {
                    LOG.warn("[ExiledSector] Skill tree template " + (i + 1) + " in " + COMMON_FILE + " could not be read and is ignored.");
                }
            }
        }
    }

    public static void moveSavedTemplatesToFile() {
        Map<String, String> savedTemplatesById = SaveData.findOrNull(SAVED_TEMPLATES_KEY, Map.class);
        if (savedTemplatesById == null) {
            return;
        }
        reload();
        for (String encodedTemplate : savedTemplatesById.values()) {
            SkillTreeTemplate template = TemplateCodec.decode(encodedTemplate);
            if (template != null) {
                TEMPLATES_BY_ID.putIfAbsent(template.id(), template);
            }
        }
        if (writeFile()) {
            SaveData.remove(SAVED_TEMPLATES_KEY);
        }
    }

    public static List<SkillTreeTemplate> all() {
        return List.copyOf(TEMPLATES_BY_ID.values());
    }

    public static SkillTreeTemplate find(String templateId) {
        return templateId == null ? null : TEMPLATES_BY_ID.get(templateId);
    }

    public static SkillTreeTemplate save(String templateName, String rootNodeId, HullSize hullSize, List<TemplateStep> steps) {
        return save(templateName, rootNodeId, hullSize, steps, Map.of());
    }

    public static SkillTreeTemplate save(String templateName, String rootNodeId, HullSize hullSize, List<TemplateStep> steps,
                                         Map<String, String> anchorsBySocket) {
        return save(templateName, rootNodeId, hullSize, steps, anchorsBySocket, () -> UUID.randomUUID().toString());
    }

    static SkillTreeTemplate save(String templateName, String rootNodeId, HullSize hullSize, List<TemplateStep> steps,
                                  Map<String, String> anchorsBySocket, Supplier<String> templateIdSupplier) {
        reload();
        return store(new SkillTreeTemplate(templateIdSupplier.get(), TemplateNames.normalise(templateName), rootNodeId, hullSize, steps, anchorsBySocket));
    }

    public static SkillTreeTemplate replaceSteps(String templateId, List<TemplateStep> steps, Map<String, String> socketedAnchors) {
        reload();
        SkillTreeTemplate template = find(templateId);
        if (template == null) {
            return null;
        }
        Map<String, String> anchorsBySocket = new HashMap<>(template.anchorsBySocket());
        anchorsBySocket.putAll(socketedAnchors);
        return store(new SkillTreeTemplate(template.id(), template.name(), template.rootNodeId(), template.hullSize(), steps, anchorsBySocket));
    }

    public static SkillTreeTemplate rename(String templateId, String templateName) {
        reload();
        SkillTreeTemplate template = find(templateId);
        return template == null ? null
                : store(new SkillTreeTemplate(template.id(), TemplateNames.normalise(templateName), template.rootNodeId(), template.hullSize(), template.steps(),
                        template.anchorsBySocket()));
    }

    private static SkillTreeTemplate store(SkillTreeTemplate template) {
        TEMPLATES_BY_ID.put(template.id(), template);
        writeFile();
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
        reload();
        if (templateId == null || TEMPLATES_BY_ID.remove(templateId) == null) {
            return false;
        }
        writeFile();
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
        if (assignments != null) {
            assignments.entrySet().removeIf(entry -> !TEMPLATES_BY_ID.containsKey(entry.getValue()) || !shipStillNeeded.test(entry.getKey()));
        }
    }

    public static void useFile(TemplateFile replacement) {
        file = replacement;
        TEMPLATES_BY_ID.clear();
        UNREADABLE_ENTRIES.clear();
        unreadableFileContents = null;
    }

    public static void useCommonFolder() {
        useFile(new CommonFolderFile());
    }

    private static JSONArray templateEntries(String contents) {
        try {
            if (contents.startsWith("[")) {
                return new JSONArray(contents);
            }
            return new JSONObject(contents).getJSONArray(TEMPLATES_FIELD);
        } catch (JSONException e) {
            return null;
        }
    }

    private static boolean writeFile() {
        try {
            if (unreadableFileContents != null) {
                file.write(UNREADABLE_BACKUP_FILE, unreadableFileContents);
                unreadableFileContents = null;
            }
            JSONArray templateEntries = new JSONArray();
            for (SkillTreeTemplate template : TEMPLATES_BY_ID.values()) {
                templateEntries.put(TemplateCodec.toJson(template));
            }
            UNREADABLE_ENTRIES.forEach(templateEntries::put);
            JSONObject root = new JSONObject();
            root.put(VERSION_FIELD, FILE_VERSION);
            root.put(TEMPLATES_FIELD, templateEntries);
            file.write(COMMON_FILE, root.toString(2));
            return true;
        } catch (IOException | JSONException e) {
            LOG.warn("[ExiledSector] Could not write skill tree templates to " + COMMON_FILE, e);
            return false;
        }
    }

    private static Map<String, String> storedAssignments(boolean createIfMissing) {
        return createIfMissing ? SaveData.getOrCreate(ASSIGNMENTS_KEY, HashMap::new) : SaveData.findOrNull(ASSIGNMENTS_KEY, Map.class);
    }

    private static final class CommonFolderFile implements TemplateFile {

        @Override
        public String read() throws IOException {
            return Global.getSettings().fileExistsInCommon(COMMON_FILE) ? Global.getSettings().readTextFileFromCommon(COMMON_FILE) : null;
        }

        @Override
        public void write(String fileName, String contents) throws IOException {
            Global.getSettings().writeTextFileToCommon(fileName, contents);
        }
    }
}
