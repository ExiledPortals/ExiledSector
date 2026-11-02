package exiledsector.skills;

import exiledsector.ProjectFiles;
import exiledsector.skills.npc.RealSkillData;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NodeIdRenamesDataTest {

    private static final Path LEDGER = Path.of("exiledsector-skill-tree/src/test/resources/released_skill_nodes.json");

    @BeforeEach
    void setUp() throws Exception {
        RealSkillData.load();
        NodeReplacements.register(rows(NodeReplacements.DATA_PATH));
        NodeReplacements.registerRenames(rows(NodeReplacements.NODE_RENAMES_PATH));
        NodeReplacements.registerTypeRenames(rows(NodeReplacements.TYPE_RENAMES_PATH));
    }

    @AfterEach
    void tearDown() {
        NodeReplacements.clear();
        RealSkillData.clear();
    }

    private static JSONArray rows(String path) throws Exception {
        String csv = Files.readString(ProjectFiles.projectRoot().resolve(path), StandardCharsets.UTF_8);
        return CDL.toJSONArray(csv.replace("\r\n", "\n"));
    }

    private static Map<String, String> pairs(String path) throws Exception {
        JSONArray rows = rows(path);
        Map<String, String> pairs = new LinkedHashMap<>();
        for (int i = 0; i < rows.length(); i++) {
            pairs.put(rows.getJSONObject(i).getString("old"), rows.getJSONObject(i).getString("new"));
        }
        return pairs;
    }

    private static List<String> strings(JSONArray array) throws Exception {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            ids.add(array.getString(i));
        }
        return ids;
    }

    @Test
    void everyRenamedNodeIsGoneFromTheTreeAndSavesFindItUnderItsNewId() throws Exception {
        Map<String, String> renames = pairs(NodeReplacements.NODE_RENAMES_PATH);
        List<String> wrong = new ArrayList<>();
        renames.forEach((oldId, newId) -> {
            if (SkillTree.get(oldId) != null || SkillTree.get(newId) == null || !newId.equals(NodeReplacements.current(oldId))) {
                wrong.add(oldId + " -> " + newId);
            }
        });

        assertFalse(renames.isEmpty());
        assertEquals(List.of(), wrong);
    }

    @Test
    void everyRenamedNodeKeepsItsOldIdRetiredAndTakesNoReleasedOrRetiredId() throws Exception {
        JSONObject ledger = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(LEDGER));
        List<String> released = strings(ledger.getJSONArray("nodes"));
        List<String> retired = strings(ledger.getJSONArray("retired"));
        List<String> wrong = new ArrayList<>();
        pairs(NodeReplacements.NODE_RENAMES_PATH).forEach((oldId, newId) -> {
            if (!retired.contains(oldId) || retired.contains(newId) || !released.contains(newId)) {
                wrong.add(oldId + " -> " + newId);
            }
        });

        assertEquals(List.of(), wrong);
    }

    @Test
    void everyRenamedTypeIsGoneAndSavedOptionsFindItUnderItsNewId() throws Exception {
        Map<String, String> renames = pairs(NodeReplacements.TYPE_RENAMES_PATH);
        List<String> wrong = new ArrayList<>();
        renames.forEach((oldId, newId) -> {
            if (SkillTree.getType(oldId) != null || SkillTree.getType(newId) == null || !newId.equals(NodeReplacements.currentTypeId(oldId))) {
                wrong.add(oldId + " -> " + newId);
            }
        });

        assertFalse(renames.isEmpty());
        assertEquals(List.of(), wrong);
    }

    @Test
    void oldAffinityOptionsReadBackAsTheRenamedAffinities() {
        assertEquals("low_tech_affinity", AffinityOptions.currentOptionId("affinity_low_tech"));
        assertEquals("low_tech_affinity", AffinityOptions.currentOptionId("hull"));
        assertEquals("high_tech_affinity", AffinityOptions.currentOptionId("affinity_high_tech"));
        assertEquals("midline_affinity", AffinityOptions.currentOptionId("affinity_midline"));
        assertEquals("ballistic_weapon_damage", AffinityOptions.currentOptionId("ballistic_damage"));
        assertEquals("non_beam_energy_weapon_damage", AffinityOptions.currentOptionId("non_beam_energy_damage"));
    }
}
