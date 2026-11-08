package exiledsector.persistence;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.MockSector;
import exiledsector.skills.NodeReplacements;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.TemplateStep;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillTreeTemplateStoreTest {

    private static final List<TemplateStep> STEPS = List.of(new TemplateStep("armor_1", null), new TemplateStep("slot_2", "low_tech_affinity"));

    private MockSector mockSector;
    private Map<String, Object> persistentData;
    private InMemoryTemplateFile file;
    private Iterator<String> ids;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        persistentData = mockSector.persistentData;
        file = InMemoryTemplateFile.install();
        ids = List.of("t1", "t2", "t3").iterator();
    }

    @AfterEach
    void tearDown() {
        SkillTreeTemplateStore.useCommonFolder();
        NodeReplacements.clear();
        SkillTree.clearNodes();
        mockSector.close();
    }

    private SkillTreeTemplate save(String name, String root) {
        return SkillTreeTemplateStore.save(name, root, HullSize.CRUISER, STEPS, Map.of("slot_2", "anchor_old"), ids::next);
    }

    private JSONArray fileTemplates() throws Exception {
        return new JSONObject(file.templates()).getJSONArray("templates");
    }

    private static String template(String id, String name, String root, String... steps) throws Exception {
        return new JSONObject().put("v", 1).put("id", id).put("name", name).put("root", root).put("steps", new JSONArray(List.of(steps))).toString();
    }

    @Test
    void readingAnEmptyStoreCreatesNothingInTheSaveOrTheFile() {
        SkillTreeTemplateStore.reload();
        SkillTreeTemplateStore.moveSavedTemplatesToFile();

        assertTrue(SkillTreeTemplateStore.all().isEmpty());
        assertNull(SkillTreeTemplateStore.find("t1"));
        assertNull(SkillTreeTemplateStore.assignedTo("ship-a", "root_low_tech_1"));
        SkillTreeTemplateStore.clearAssignment("ship-a");
        SkillTreeTemplateStore.pruneAssignments(id -> true);

        assertTrue(persistentData.isEmpty());
        assertTrue(file.contentsByFileName.isEmpty());
    }

    @Test
    void savedTemplatesGoToTheSharedFileNotTheSave() throws Exception {
        SkillTreeTemplate first = save("  Brawler ", "root_low_tech_1");
        save("Kite", "root_low_tech_1");

        assertFalse(persistentData.containsKey("exiledSector_skillTreeTemplates"));
        assertEquals(1, new JSONObject(file.templates()).getInt("v"));
        assertEquals(2, fileTemplates().length());
        assertEquals("Brawler", first.name());

        SkillTreeTemplateStore.useFile(file);
        SkillTreeTemplateStore.reload();
        assertEquals(List.of("t1", "t2"), SkillTreeTemplateStore.all().stream().map(SkillTreeTemplate::id).toList());
        assertEquals(first, SkillTreeTemplateStore.find("t1"));
    }

    @Test
    void anotherToolsEditsAreSeenOnReloadAndKeptWhenTheGameNextWrites() throws Exception {
        save("Brawler", "root_low_tech_1");
        file.setTemplates(new JSONObject().put("templates", new JSONArray()
                .put(new JSONObject(template("t1", "Brawler v2", "root_low_tech_1", "armor_1")))
                .put(new JSONObject(template("planner-1", "Planned", "root_high_tech_1", "flux_1", "slot_2=high_tech_affinity")))).toString());

        SkillTreeTemplateStore.reload();
        assertEquals("Brawler v2", SkillTreeTemplateStore.find("t1").name());
        assertEquals(List.of(new TemplateStep("flux_1", null), new TemplateStep("slot_2", "high_tech_affinity")),
                SkillTreeTemplateStore.find("planner-1").steps());

        file.setTemplates(new JSONObject().put("templates", new JSONArray()
                .put(new JSONObject(template("planner-1", "Planned", "root_high_tech_1", "flux_1")))).toString());
        save("Kite", "root_low_tech_1");

        assertEquals(List.of("planner-1", "t2"), SkillTreeTemplateStore.all().stream().map(SkillTreeTemplate::id).toList());
        assertEquals(2, fileTemplates().length());
    }

    @Test
    void aBareArrayOfTemplatesIsAccepted() throws Exception {
        file.setTemplates("[" + template("p1", "Planned", "root_low_tech_1", "armor_1") + "]");

        SkillTreeTemplateStore.reload();

        assertEquals("Planned", SkillTreeTemplateStore.find("p1").name());
    }

    @Test
    void anUnreadableEntryIsSkippedButWrittenBackUnchanged() throws Exception {
        file.setTemplates(new JSONObject().put("templates", new JSONArray()
                .put(new JSONObject(template("p1", "Planned", "root_low_tech_1", "armor_1")))
                .put(new JSONObject().put("name", "no id"))).toString());

        SkillTreeTemplateStore.reload();
        assertEquals(List.of("p1"), SkillTreeTemplateStore.all().stream().map(SkillTreeTemplate::id).toList());

        save("Kite", "root_low_tech_1");
        JSONArray written = fileTemplates();
        assertEquals(3, written.length());
        assertEquals("no id", written.getJSONObject(2).getString("name"));
    }

    @Test
    void aFileThatIsNotJsonKeepsTheLoadedTemplatesAndIsBackedUpBeforeBeingRewritten() throws Exception {
        save("Brawler", "root_low_tech_1");
        file.setTemplates("{\"templates\": [ half written");

        SkillTreeTemplateStore.reload();
        assertEquals("Brawler", SkillTreeTemplateStore.find("t1").name());

        save("Kite", "root_low_tech_1");
        assertEquals("{\"templates\": [ half written", file.contentsByFileName.get(SkillTreeTemplateStore.UNREADABLE_BACKUP_FILE));
        assertEquals(2, fileTemplates().length());
    }

    @Test
    void deletingTheFileRemovesEveryTemplate() {
        save("Brawler", "root_low_tech_1");
        file.contentsByFileName.remove(SkillTreeTemplateStore.COMMON_FILE);

        SkillTreeTemplateStore.reload();

        assertTrue(SkillTreeTemplateStore.all().isEmpty());
    }

    @Test
    void templatesStillInASaveMoveIntoTheFileOnce() throws Exception {
        Map<String, String> savedTemplates = new HashMap<>();
        savedTemplates.put("old", template("old", "From save", "root_low_tech_1", "armor_1"));
        savedTemplates.put("broken", "{not json");
        persistentData.put("exiledSector_skillTreeTemplates", savedTemplates);
        file.setTemplates("[" + template("p1", "Planned", "root_low_tech_1", "armor_1") + "]");

        SkillTreeTemplateStore.moveSavedTemplatesToFile();

        assertFalse(persistentData.containsKey("exiledSector_skillTreeTemplates"));
        assertEquals(Set.of("p1", "old"), Set.copyOf(SkillTreeTemplateStore.all().stream().map(SkillTreeTemplate::id).toList()));
        assertEquals(2, fileTemplates().length());
    }

    @Test
    void renamedNodesLoadUnderTheirNewIdsAndTheFileTakesThemOnTheNextWrite() throws Exception {
        SkillType type = new SkillType.Builder("armor_type", "armor_type", "a.png", SkillTier.SMALL).effects(List.of()).build();
        SkillTree.register(new SkillNode("armor_new", type, List.of(), 0f, 0f));
        NodeReplacements.registerRenames(new JSONArray().put(new JSONObject().put("old", "armor_old").put("new", "armor_new")));
        file.setTemplates("[" + template("p1", "Planned", "root_low_tech_1", "armor_old", "retired_node") + "]");

        SkillTreeTemplateStore.reload();
        assertEquals(List.of(new TemplateStep("armor_new", null), new TemplateStep("retired_node", null)), SkillTreeTemplateStore.find("p1").steps());

        save("Kite", "root_low_tech_1");
        JSONArray writtenSteps = fileTemplates().getJSONObject(0).getJSONArray("steps");
        assertEquals(List.of("armor_new", "retired_node"), List.of(writtenSteps.getString(0), writtenSteps.getString(1)));
    }

    @Test
    void aShipsAssignmentOnlyCountsWhileTheTemplateExistsAndMatchesItsRoot() {
        save("Brawler", "root_low_tech_1");
        SkillTreeTemplateStore.assign("ship-a", "t1");

        assertEquals("t1", SkillTreeTemplateStore.assignedTo("ship-a", "root_low_tech_1").id());
        assertNull(SkillTreeTemplateStore.assignedTo("ship-a", "root_high_tech_1"));
        assertNull(SkillTreeTemplateStore.assignedTo("ship-b", "root_low_tech_1"));

        SkillTreeTemplateStore.clearAssignment("ship-a");
        assertNull(SkillTreeTemplateStore.assignedTo("ship-a", "root_low_tech_1"));
    }

    @Test
    void deletingATemplateRemovesItFromTheFileAndEveryAssignmentToIt() throws Exception {
        save("Brawler", "root_low_tech_1");
        save("Kite", "root_low_tech_1");
        SkillTreeTemplateStore.assign("ship-a", "t1");
        SkillTreeTemplateStore.assign("ship-b", "t1");
        SkillTreeTemplateStore.assign("ship-c", "t2");

        assertTrue(SkillTreeTemplateStore.delete("t1"));
        assertFalse(SkillTreeTemplateStore.delete("t1"));

        assertNull(SkillTreeTemplateStore.find("t1"));
        assertEquals(1, fileTemplates().length());
        assertEquals(Map.of("ship-c", "t2"), persistentData.get("exiledSector_skillTreeTemplateAssignments"));
    }

    @Test
    void pruningDropsAssignmentsToMissingTemplatesAndShipsNoLongerNeeded() {
        save("Brawler", "root_low_tech_1");
        SkillTreeTemplateStore.assign("kept", "t1");
        SkillTreeTemplateStore.assign("gone-ship", "t1");
        SkillTreeTemplateStore.assign("dangling", "deleted-template");

        SkillTreeTemplateStore.pruneAssignments(Set.of("kept", "dangling")::contains);

        assertEquals(Map.of("kept", "t1"), persistentData.get("exiledSector_skillTreeTemplateAssignments"));
    }

    @Test
    void editingATemplateInPlaceKeepsItsNameAndRenamingKeepsItsSteps() {
        save("Brawler", "root_low_tech_1");
        List<TemplateStep> newSteps = List.of(new TemplateStep("armor_1", null));

        SkillTreeTemplate edited = SkillTreeTemplateStore.replaceSteps("t1", newSteps, Map.of());
        SkillTreeTemplate renamed = SkillTreeTemplateStore.rename("t1", "  Kite ");

        assertEquals("Brawler", edited.name());
        assertEquals(newSteps, SkillTreeTemplateStore.find("t1").steps());
        assertEquals("Kite", renamed.name());
        assertEquals("Kite", SkillTreeTemplateStore.find("t1").name());
        assertNull(SkillTreeTemplateStore.replaceSteps("missing", newSteps, Map.of()));
        assertNull(SkillTreeTemplateStore.replaceSteps(null, newSteps, Map.of()));
    }

    @Test
    void editingATemplateKeepsItsAnchorsAndTakesTheShipsSocketedOnes() {
        save("Brawler", "root_low_tech_1");
        List<TemplateStep> newSteps = List.of(new TemplateStep("slot_2", null), new TemplateStep("slot_5", null));

        SkillTreeTemplateStore.replaceSteps("t1", newSteps, Map.of("slot_5", "anchor_new", "slot_9", "anchor_elsewhere"));
        SkillTreeTemplateStore.rename("t1", "Kite");

        assertEquals(Map.of("slot_2", "anchor_old", "slot_5", "anchor_new"), SkillTreeTemplateStore.find("t1").anchorsBySocket());
        SkillTreeTemplateStore.replaceSteps("t1", List.of(new TemplateStep("slot_5", null)), Map.of());
        assertEquals(Map.of("slot_5", "anchor_new"), SkillTreeTemplateStore.find("t1").anchorsBySocket());
    }

    @Test
    void assignmentCountCountsEveryShipUsingATemplate() {
        save("Brawler", "root_low_tech_1");
        assertEquals(0, SkillTreeTemplateStore.assignmentCount("t1"));
        SkillTreeTemplateStore.assign("ship-a", "t1");
        SkillTreeTemplateStore.assign("ship-b", "t1");

        assertEquals(2, SkillTreeTemplateStore.assignmentCount("t1"));
        assertEquals(0, SkillTreeTemplateStore.assignmentCount(null));
    }
}
