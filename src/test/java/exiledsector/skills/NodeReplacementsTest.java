package exiledsector.skills;

import exiledsector.skills.npc.NpcTreeTag;
import exiledsector.skills.npc.RealSkillData;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeReplacementsTest {

    private SkillNode root;
    private SkillNode socket;
    private SkillNode kept;

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearTypes();
        SkillTree.clearNodes();
        root = node("root", SkillTier.ROOT);
        socket = node("socket", SkillTier.SOCKET);
        kept = node("kept", SkillTier.SMALL);
        NodeReplacements.register(new JSONArray().put(new JSONObject().put("old", "removed").put("new", "socket")));
    }

    @AfterEach
    void tearDown() {
        NodeReplacements.clear();
        SkillTree.clearTypes();
        SkillTree.clearNodes();
    }

    private static SkillNode node(String id, SkillTier tier) {
        SkillType type = new SkillType.Builder(id + "_type", id, "", tier).build();
        SkillTree.registerType(type);
        SkillNode node = new SkillNode(id, type, List.of(), 0f, 0f);
        SkillTree.register(node);
        return node;
    }

    @Test
    void aRemovedNodeIsSwappedForItsReplacementKeepingItsPlaceAndFreeStatus() {
        ShipSkillData data = new ShipSkillData();
        data.chooseStartingRoot(root);
        data.addFreeAllocationCredit();
        data.allocate(new SkillNode("removed", kept.getType(), List.of(), 0f, 0f), 3);
        data.allocate(kept, 3);

        assertTrue(data.replaceNode("removed", "socket"));

        assertEquals(List.of("root", "socket", "kept"), List.copyOf(data.getAllocatedNodeIds()));
        assertTrue(data.isFreeNode("socket"));
        assertFalse(data.isFreeNode("removed"));
    }

    @Test
    void nothingIsSwappedWhenTheReplacementIsAlreadyAllocated() {
        ShipSkillData data = new ShipSkillData();
        data.allocate(new SkillNode("removed", kept.getType(), List.of(), 0f, 0f), 3);
        data.allocate(socket, 3);

        assertFalse(data.replaceNode("removed", "socket"));
        assertEquals(Set.of("removed", "socket"), data.getAllocatedNodeIds());
    }

    @Test
    void anIdIsOnlyResolvedOnceItsNodeHasLeftTheTree() {
        assertEquals("socket", NodeReplacements.resolve("removed"));
        assertEquals("kept", NodeReplacements.resolve("kept"));

        node("removed", SkillTier.SMALL);

        assertEquals("removed", NodeReplacements.resolve("removed"));
    }

    @Test
    void anNpcTreeThatHadTheRemovedOptionalNodeNowHasTheSocket() {
        ShipSkillData data = NpcTreeTag.decode(NpcTreeTag.PREFIX + "generated|5|root,removed=flux_capacity,kept");

        assertNotNull(data);
        assertTrue(data.isAllocated("socket"));
        assertTrue(data.isAllocated("kept"));
        assertNull(data.getOptionalSelection("socket"));
    }

    @Test
    void everyShippedReplacementPointsFromAGoneNodeToOneThatExists() throws Exception {
        SkillTree.clearTypes();
        SkillTree.clearNodes();
        RealSkillData.load();
        try {
            String csv = Files.readString(RealSkillData.projectRoot().resolve(NodeReplacements.DATA_PATH), StandardCharsets.UTF_8);
            NodeReplacements.register(CDL.toJSONArray(csv.replace("\r\n", "\n")));
            assertFalse(NodeReplacements.all().isEmpty());
            for (Map.Entry<String, String> replacement : NodeReplacements.all().entrySet()) {
                assertNull(SkillTree.get(replacement.getKey()), replacement.getKey() + " is still in the tree");
                assertNotNull(SkillTree.get(replacement.getValue()), replacement.getValue() + " is not in the tree");
            }
        } finally {
            RealSkillData.clear();
        }
    }
}
