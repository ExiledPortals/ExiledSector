package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.socketables.NpcSocketables;
import exiledsector.socketables.RolledEffect;
import exiledsector.socketables.SocketableDefinitions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NpcTreeTagTest {

    @BeforeEach
    void setUp() {
        RealSkillData.clear();
        register("root", type("root_type", SkillTier.ROOT));
        register("a", type("a_type", SkillTier.SMALL), "root");
        registerType(type("hull_option", SkillTier.SMALL));
        registerType(type("flux_option", SkillTier.SMALL));
        register("optional_1", new SkillType.Builder("optional", "optional", "a.png", SkillTier.SMALL)
                .optionalOptionIds(List.of("flux_option", "hull_option")).build(), "a");
        register("b", type("b_type", SkillTier.NOTABLE), "optional_1");
    }

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
    }

    private static SkillType type(String id, SkillTier tier) {
        return new SkillType.Builder(id, id, "a.png", tier).build();
    }

    private static SkillType registerType(SkillType type) {
        SkillTree.registerType(type);
        return type;
    }

    private static void register(String id, SkillType type, String... connectedTo) {
        registerType(type);
        SkillTree.register(new SkillNode(id, type, List.of(connectedTo), 0f, 0f));
    }

    private static ShipSkillData build(int nodeCount) {
        ShipSkillData data = NpcSkillTreeBuilder.rootedTree(SkillTree.get("root"), nodeCount);
        List<String> order = List.of("a", "optional_1", "b");
        for (int i = 0; i < Math.min(nodeCount, order.size()); i++) {
            SkillNode node = SkillTree.get(order.get(i));
            if (node.getType().isOptional()) {
                data.selectOption(node, SkillTree.getType("hull_option"), 1);
            } else {
                data.allocate(node, 1);
            }
        }
        return data;
    }

    private static ShipVariantAPI variantWithTags(String... tags) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getTags()).thenReturn(List.of(tags));
        return variant;
    }

    private static ShipSkillData withSocket() {
        register("socket_1", type("socket", SkillTier.SOCKET), "a");
        ShipSkillData data = NpcSkillTreeBuilder.rootedTree(SkillTree.get("root"), 2);
        data.allocate(SkillTree.get("a"), 1);
        data.allocate(SkillTree.get("socket_1"), 1);
        return data;
    }

    @Test
    void anNpcSocketableSurvivesEncodingAndDecodingInItsSocket() {
        ShipSkillData data = withSocket();
        data.socketItem("socket_1", NpcSocketables.id("domain_subroutine_military", -42L));

        String tag = NpcTreeTag.encode(data);
        ShipSkillData decoded = NpcTreeTag.decode(tag);

        assertEquals("exiledSector_npcTree|generated|2|root,a,socket_1|sockets:socket_1=npc:domain_subroutine_military/-42", tag);
        assertEquals("npc:domain_subroutine_military/-42", decoded.getSocketedItem("socket_1"));
    }

    @Test
    void aFlagshipsUnlockedSocketsAndTheirItemsSurviveTheTagAlongsideTreeSockets() {
        ShipSkillData data = withSocket();
        data.socketItem("socket_1", NpcSocketables.id("domain_subroutine_military", -42L));
        data.grantUnlockedSocketType("bridge");
        data.grantUnlockedSocketType("reactor");
        data.grantUnlockedSocketType("flight_deck");
        data.socketFrameworkItem("flight_deck", NpcSocketables.id("deck_basic", 3L));

        String tag = NpcTreeTag.encode(data);
        ShipSkillData decoded = NpcTreeTag.decode(tag);

        assertEquals("exiledSector_npcTree|generated|2|root,a,socket_1|sockets:socket_1=npc:domain_subroutine_military/-42"
                + "|frameworkTypes:bridge+reactor+flight_deck|frameworkSockets:flight_deck=npc:deck_basic/3", tag);
        assertEquals("npc:domain_subroutine_military/-42", decoded.getSocketedItem("socket_1"));
        assertEquals(List.of("bridge", "reactor", "flight_deck"), decoded.getUnlockedSocketTypeIds());
        assertEquals(3, decoded.getFrameworkPoints());
        assertEquals(Map.of("flight_deck", "npc:deck_basic/3"), decoded.getFrameworkSocketedItems());
    }

    @Test
    void aSavedTreeSocketHoldingAFrameworkItemIsDroppedOnRestore() throws Exception {
        withSocket();
        SocketableDefinitions.register(new JSONArray().put(new JSONObject().put("id", "gunners").put("kind", "weapon_mount")
                .put("name", "Gunners").put("prefixes", "HULL_MULT:4:6")));
        try {
            ShipSkillData decoded = NpcTreeTag.decode("exiledSector_npcTree|generated|2|root,a,socket_1|sockets:socket_1=npc:gunners/5");

            assertNull(decoded.getSocketedItem("socket_1"));
        } finally {
            SocketableDefinitions.clear();
        }
    }

    @Test
    void socketsWithoutItemsRoundTripAndTagsFromEarlierFrameworkBuildsAreIgnored() {
        ShipSkillData data = withSocket();
        data.grantUnlockedSocketType("bridge");

        assertEquals(List.of("bridge"), NpcTreeTag.decode(NpcTreeTag.encode(data)).getUnlockedSocketTypeIds());
        ShipSkillData legacy = NpcTreeTag.decode("exiledSector_npcTree|generated|2|root,a|framework:npcfw:CRUISER/RARE/bridge/1"
                + "|frameworkSockets:0=npc:x/1");
        assertEquals(List.of(), legacy.getUnlockedSocketTypeIds());
        assertEquals(Map.of(), legacy.getFrameworkSocketedItems());
    }

    @Test
    void socketTagsAlreadyWrittenIntoSavedFleetsStillDecodeWithTheirRolls() {
        withSocket();
        String rolled = "npc:domain_subroutine_military/-42/HULL_MULT:5;ARMOR_PERCENT:-0.25";
        String saved = "exiledSector_npcTree|generated|2|root,a,socket_1|sockets:socket_1=" + rolled;

        ShipSkillData decoded = NpcTreeTag.decode(saved);
        ShipSkillData legacy = NpcTreeTag.decode(NpcTreeTag.LEGACY_PREFIX + saved.substring(NpcTreeTag.PREFIX.length()));

        assertEquals(rolled, decoded.getSocketedItem("socket_1"));
        assertEquals(rolled, legacy.getSocketedItem("socket_1"));
        assertEquals(saved, NpcTreeTag.encode(decoded));
        assertEquals(List.of(new RolledEffect("HULL_MULT", 5f), new RolledEffect("ARMOR_PERCENT", -0.25f)),
                NpcSocketables.item(decoded.getSocketedItem("socket_1")).effects());
    }

    @Test
    void aTreeWithoutSocketablesKeepsTheThreeFieldFormat() {
        assertEquals("exiledSector_npcTree|generated|2|root,a,socket_1", NpcTreeTag.encode(withSocket()));
    }

    @Test
    void socketEntriesThatAreNotNpcItemsOrNotSocketsAreIgnored() {
        withSocket();

        ShipSkillData decoded = NpcTreeTag.decode(
                "exiledSector_npcTree|generated|2|root,a,socket_1|sockets:socket_1=socketable_7,a=npc:x/1,missing=npc:x/1,socket_1");

        assertTrue(decoded.getSocketedItems().isEmpty());
        assertTrue(decoded.isAllocated("socket_1"));
    }

    @Test
    void encodesTheLevelAndEveryAllocatedNodeWithItsOptionInOrder() {
        String tag = NpcTreeTag.encode(build(3));

        assertEquals("exiledSector_npcTree|generated|3|root,a,optional_1=hull_option,b", tag);
    }

    @Test
    void decodingRestoresTheExactTreeTheBuilderMade() {
        ShipSkillData built = build(5);

        ShipSkillData restored = NpcTreeTag.decode(NpcTreeTag.encode(built));

        assertEquals(List.copyOf(built.getAllocatedNodeIds()), List.copyOf(restored.getAllocatedNodeIds()));
        assertEquals("hull_option", restored.getOptionalSelection("optional_1"));
        assertEquals(built.getLevel(), restored.getLevel());
        assertEquals(built.getBankedFreeAllocations(), restored.getBankedFreeAllocations());
        assertEquals(2, restored.getBankedFreeAllocations());
        assertTrue(restored.isFreeNode("a"));
        assertTrue(restored.isFreeNode("b"));
        assertFalse(restored.isFreeNode("root"));
        assertEquals(0, restored.getSpentOp(1));
        assertTrue(restored.isNpcBuild());
    }

    @Test
    void nodesChargedWithFreedOpStayChargedThroughTheTag() {
        ShipSkillData built = NpcSkillTreeBuilder.rootedTree(SkillTree.get("root"), 1);
        built.allocate(SkillTree.get("a"), 3);
        built.selectOption(SkillTree.get("optional_1"), SkillTree.getType("hull_option"), 3);
        built.allocate(SkillTree.get("b"), 3);

        String tag = NpcTreeTag.encode(built);
        ShipSkillData restored = NpcTreeTag.decode(tag);

        assertEquals("exiledSector_npcTree|generated|1|root,a,optional_1=hull_option,b", tag);
        assertEquals(6, restored.getSpentOp(3));
        assertEquals(0, restored.getBankedFreeAllocations());
        assertTrue(restored.isFreeNode("a"));
        assertFalse(restored.isFreeNode("b"));
        assertEquals(List.copyOf(built.getAllocatedNodeIds()), List.copyOf(restored.getAllocatedNodeIds()));
        assertEquals("hull_option", restored.getOptionalSelection("optional_1"));
    }

    @Test
    void tagsSavedWithAPerNodeCostStillDecodeToTheSameTree() {
        ShipSkillData built = NpcSkillTreeBuilder.rootedTree(SkillTree.get("root"), 1);
        built.allocate(SkillTree.get("a"), 3);
        built.allocate(SkillTree.get("b"), 3);

        ShipSkillData restored = NpcTreeTag.decode("exiledSector_npcTree|bulwark|1|root,a,b|3");

        assertEquals(List.copyOf(built.getAllocatedNodeIds()), List.copyOf(restored.getAllocatedNodeIds()));
        assertEquals(built.getSpentOp(3), restored.getSpentOp(3));
        assertTrue(restored.isFreeNode("a"));
        assertFalse(restored.isFreeNode("b"));
    }

    @Test
    void findsTheNpcTreeTagAmongAVariantsTags() {
        String tag = NpcTreeTag.encode(build(1));

        assertEquals(tag, NpcTreeTag.find(variantWithTags("exiledSector_installed_x", tag)));
        assertNull(NpcTreeTag.find(variantWithTags("exiledSector_installed_x")));
        assertNull(NpcTreeTag.find(null));
    }

    @Test
    void tagsSavedBeforeTheNpcRenameAreStillFoundAndDecoded() {
        String current = NpcTreeTag.encode(build(5));
        String legacy = NpcTreeTag.LEGACY_PREFIX + current.substring(NpcTreeTag.PREFIX.length());

        assertEquals(legacy, NpcTreeTag.find(variantWithTags(legacy)));
        assertEquals(List.copyOf(NpcTreeTag.decode(current).getAllocatedNodeIds()),
                List.copyOf(NpcTreeTag.decode(legacy).getAllocatedNodeIds()));
    }

    @Test
    void treesSavedUnderAHandMadeBuildNameStillDecode() {
        ShipSkillData restored = NpcTreeTag.decode("exiledSector_npcTree|lowtech_bulwark|3|root,a,optional_1=hull_option,b");

        assertEquals(List.of("root", "a", "optional_1", "b"), List.copyOf(restored.getAllocatedNodeIds()));
    }

    @Test
    void malformedTagsDecodeToNull() {
        assertNull(NpcTreeTag.decode(null));
        assertNull(NpcTreeTag.decode("exiledSector_installed_heavyarmor"));
        assertNull(NpcTreeTag.decode("exiledSector_npcTree|bulwark|3"));
        assertNull(NpcTreeTag.decode("exiledSector_npcTree|bulwark|three|root,a"));
        assertNull(NpcTreeTag.decode("exiledSector_npcTree||3|root,a"));
    }

    @Test
    void anUnknownRootRestoresAnEmptyNpcTree() {
        ShipSkillData restored = NpcTreeTag.decode("exiledSector_npcTree|bulwark|3|missing_root,a");

        assertTrue(restored.getAllocatedNodeIds().isEmpty());
        assertTrue(restored.isNpcBuild());
    }

    @Test
    void unknownNodesAndInvalidOptionsAreSkipped() {
        ShipSkillData restored = NpcTreeTag.decode("exiledSector_npcTree|bulwark|3|root,a,gone_node,optional_1=bogus,b");

        assertEquals(List.of("root", "a", "b"), List.copyOf(restored.getAllocatedNodeIds()));
    }
}
