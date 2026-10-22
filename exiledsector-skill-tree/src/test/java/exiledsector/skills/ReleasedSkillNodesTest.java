package exiledsector.skills;

import exiledsector.ProjectFiles;
import exiledsector.skills.npc.RealSkillData;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReleasedSkillNodesTest {

    private static final Path LEDGER = Path.of("exiledsector-skill-tree/src/test/resources/released_skill_nodes.json");

    @BeforeEach
    void setUp() throws Exception {
        RealSkillData.load();
    }

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
    }

    private static List<String> released(String key) throws Exception {
        JSONArray array = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(LEDGER)).getJSONArray(key);
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            ids.add(array.getString(i));
        }
        return ids;
    }

    @Test
    void everyNodeThatShippedInAReleaseIsStillInTheTree() throws Exception {
        List<String> released = released("nodes");
        List<String> missing = released.stream().filter(id -> SkillTree.get(id) == null).toList();

        assertFalse(released.isEmpty());
        assertEquals(List.of(), missing, "These node ids shipped in a release but are no longer in " + RealSkillData.TREE_FILE
                + ". Loading a save drops them from every ship, refunding free allocations and OP but not item costs."
                + " If that is intended, remove them from " + LEDGER + ".");
    }

    @Test
    void everyStartingRootThatShippedInAReleaseIsStillARoot() throws Exception {
        List<String> lost = released("roots").stream()
                .filter(id -> SkillTree.get(id) == null || SkillTree.get(id).getType().getTier() != SkillTier.ROOT)
                .toList();

        assertEquals(List.of(), lost, "These starting roots shipped in a release but are gone or no longer ROOT tier."
                + " Loading a save resets the whole tree of every ship that started on them."
                + " If that is intended, remove them from the roots list in " + LEDGER + ".");
    }
}
