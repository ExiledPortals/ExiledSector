package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.skills.AnchorLinks;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTreeLinks;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.items.HullUpgradePoints;
import exiledsector.items.NpcItems;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.items.WormholeAnchorLinks;
import exiledsector.items.ItemDefinitions;
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

    private static final String HEGEMONY_ANCHOR = "npc:wormhole_anchor_hegemony/0";

    @BeforeEach
    void setUp() throws Exception {
        RealSkillData.clear();
        register("root", type("root_type", SkillTier.ROOT));
        register("a", type("a_type", SkillTier.SMALL), "root");
        registerType(type("hull_option", SkillTier.SMALL));
        registerType(type("flux_option", SkillTier.SMALL));
        register("optional_1", new SkillType.Builder("optional", "optional", "a.png", SkillTier.SMALL)
                .optionalOptionIds(List.of("flux_option", "hull_option")).build(), "a");
        register("b", type("b_type", SkillTier.NOTABLE), "optional_1");
        registerVolumeSubroutine();
        registerHegemonyAnchor();
    }

    @AfterEach
    void tearDown() {
        AnchorLinks.install(null);
        WormholeAnchorDefinitions.clear();
        NpcItems.clearCache();
        ItemDefinitions.clear();
        RealSkillData.clear();
    }

    private static SkillType type(String id, SkillTier tier) {
        return new SkillType.Builder(id, id, "a.png", tier).build();
    }

    private static void registerVolumeSubroutine() throws Exception {
        ItemDefinitions.register(new JSONArray().put(new JSONObject().put("id", "volume_subroutine").put("kind", "MODSPEC_CACHE")));
    }

    private static void registerHegemonyAnchor() throws Exception {
        SkillTree.register(new SkillNode("hegemony_2", registerType(type("wormhole_exit", SkillTier.WORMHOLE_EXIT)), List.of(), 0f, 0f,
                SkillNodeDecoration.NONE, List.of("hegemony")));
        SkillTree.register(new SkillNode("pride", registerType(type("pride_type", SkillTier.NOTABLE)), List.of("hegemony_2"), 0f, 0f,
                SkillNodeDecoration.NONE, List.of("hegemony")));
        WormholeAnchorDefinitions.register(new JSONArray().put(new JSONObject().put("id", "wormhole_anchor_hegemony").put("volume", "hegemony")
                .put("exit_node", "hegemony_2").put("name", "Hegemony Anchor").put("icon", "graphics/icons/nav_relay.png").put("rarity", "10")
                .put("description", "Opens a wormhole.")));
        WormholeAnchorLinks.install();
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
                ShipTrees.selectOption(data, node, SkillTree.getType("hull_option"), 1);
            } else {
                ShipTrees.allocate(data, node, 1);
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
        ShipTrees.allocate(data, SkillTree.get("a"), 1);
        ShipTrees.allocate(data, SkillTree.get("socket_1"), 1);
        return data;
    }

    private static ShipSkillData withOpenHegemonyVolume() {
        ShipSkillData data = withSocket();
        data.socketItem("socket_1", HEGEMONY_ANCHOR);
        ShipTrees.openAnchorWormholes(data, 100);
        ShipTrees.allocate(data, SkillTree.get("pride"), 1);
        return data;
    }

    @Test
    void anNpcAnchorSurvivesEncodingAndDecodingAndReopensItsExit() {
        ShipSkillData data = withOpenHegemonyVolume();

        String tag = NpcTreeTag.encode(data);
        ShipSkillData decoded = NpcTreeTag.decode(tag);
        ShipSkillData legacy = NpcTreeTag.decode(NpcTreeTag.LEGACY_PREFIX + tag.substring(NpcTreeTag.PREFIX.length()));

        assertEquals("exiledSector_npcTree|generated|2|root,a,socket_1,hegemony_2,pride|sockets:socket_1=" + HEGEMONY_ANCHOR, tag);
        for (ShipSkillData restored : List.of(decoded, legacy)) {
            assertEquals(Map.of("socket_1", HEGEMONY_ANCHOR), restored.getSocketedItems());
            assertEquals(List.copyOf(data.getAllocatedNodeIds()), List.copyOf(restored.getAllocatedNodeIds()));
            assertTrue(restored.isFreeNode("hegemony_2"));
            assertEquals("socket_1", ShipTreeLinks.wormholePartner(restored, SkillTree.get("hegemony_2")));
        }
    }

    @Test
    void anOldImplicitAnchorDecodesAsTheVolumesRealAnchor() {
        withSocket();

        ShipSkillData decoded = NpcTreeTag.decode("exiledSector_npcTree|generated|2|root,a,socket_1,hegemony_2,pride|anchors:socket_1=hegemony_2");

        assertEquals(Map.of("socket_1", HEGEMONY_ANCHOR), decoded.getSocketedItems());
        assertTrue(decoded.isFreeNode("hegemony_2"));
        assertEquals("exiledSector_npcTree|generated|2|root,a,socket_1,hegemony_2,pride|sockets:socket_1=" + HEGEMONY_ANCHOR,
                NpcTreeTag.encode(decoded));
    }

    @Test
    void everySavedNpcItemThatIsNotAnAnchorIsDropped() {
        withSocket();

        for (String itemId : List.of("npc:volume_subroutine/-42", "npc:volume_subroutine/-42/HULL_MULT:5;ARMOR_PERCENT:-0.25",
                "npc:domain_subroutine_military/5", "npc:not_loaded_subroutine/5")) {
            ShipSkillData decoded = NpcTreeTag.decode("exiledSector_npcTree|generated|2|root,a,socket_1|sockets:socket_1=" + itemId);

            assertNull(decoded.getSocketedItem("socket_1"), itemId);
            assertTrue(decoded.isAllocated("socket_1"));
        }
    }

    @Test
    void sectionsAndTheirItemsFromSavedTagsAreDropped() {
        withSocket();

        ShipSkillData sections = NpcTreeTag.decode("exiledSector_npcTree|generated|2|root,a,socket_1|frameworkTypes:bridge+reactor+flight_deck"
                + "|frameworkSockets:flight_deck=npc:deck_basic/3");
        ShipSkillData legacy = NpcTreeTag.decode("exiledSector_npcTree|generated|2|root,a|framework:npcfw:CRUISER/RARE/bridge/1"
                + "|frameworkSockets:0=npc:x/1");

        for (ShipSkillData decoded : List.of(sections, legacy)) {
            assertEquals(List.of(), decoded.getUnlockedSocketTypeIds());
            assertEquals(Map.of(), decoded.getSectionItems());
        }
        assertEquals("exiledSector_npcTree|generated|2|root,a,socket_1", NpcTreeTag.encode(sections));
    }

    @Test
    void aSavedTreeSocketHoldingASectionItemIsDroppedOnRestore() throws Exception {
        withSocket();
        ItemDefinitions.register(new JSONArray().put(new JSONObject().put("id", "gunners").put("section", "weapon_mount")
                .put("name", "Gunners").put("prefixes", "HULL_MULT:4:6")));
        try {
            ShipSkillData decoded = NpcTreeTag.decode("exiledSector_npcTree|generated|2|root,a,socket_1|sockets:socket_1=npc:gunners/5");

            assertNull(decoded.getSocketedItem("socket_1"));
        } finally {
            ItemDefinitions.clear();
        }
    }

    @Test
    void aTreeWithoutAnAnchorKeepsTheThreeFieldFormat() {
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
        assertEquals(0, ShipTrees.getSpentOp(restored, 1));
        assertTrue(restored.isNpcBuild());
    }

    @Test
    void anNpcBuildHasNoHullUpgradesBeforeAndAfterTheTagAndACapturedShipStartsAtThePlayersPoints() {
        ShipSkillData built = build(5);
        ShipSkillData restored = NpcTreeTag.decode(NpcTreeTag.encode(built));

        for (ShipSkillData npcShip : List.of(built, restored)) {
            assertEquals(0, npcShip.getStoredHullUpgradePoints());
            assertEquals(List.of(), npcShip.getUnlockedSocketTypeIds());
            assertEquals(Map.of(), npcShip.getSectionItems());
            assertEquals(0, HullUpgradePoints.of(npcShip));
        }
        restored.clearNpcBuild();
        assertEquals(HullUpgradePoints.STARTING_POINTS, HullUpgradePoints.of(restored));
        assertEquals(HullUpgradePoints.STARTING_POINTS, HullUpgradePoints.unspent(restored));
    }

    @Test
    void nodesChargedWithFreedOpStayChargedThroughTheTag() {
        ShipSkillData built = NpcSkillTreeBuilder.rootedTree(SkillTree.get("root"), 1);
        ShipTrees.allocate(built, SkillTree.get("a"), 3);
        ShipTrees.selectOption(built, SkillTree.get("optional_1"), SkillTree.getType("hull_option"), 3);
        ShipTrees.allocate(built, SkillTree.get("b"), 3);

        String tag = NpcTreeTag.encode(built);
        ShipSkillData restored = NpcTreeTag.decode(tag);

        assertEquals("exiledSector_npcTree|generated|1|root,a,optional_1=hull_option,b", tag);
        assertEquals(6, ShipTrees.getSpentOp(restored, 3));
        assertEquals(0, restored.getBankedFreeAllocations());
        assertTrue(restored.isFreeNode("a"));
        assertFalse(restored.isFreeNode("b"));
        assertEquals(List.copyOf(built.getAllocatedNodeIds()), List.copyOf(restored.getAllocatedNodeIds()));
        assertEquals("hull_option", restored.getOptionalSelection("optional_1"));
    }

    @Test
    void tagsSavedWithAPerNodeCostStillDecodeToTheSameTree() {
        ShipSkillData built = NpcSkillTreeBuilder.rootedTree(SkillTree.get("root"), 1);
        ShipTrees.allocate(built, SkillTree.get("a"), 3);
        ShipTrees.allocate(built, SkillTree.get("b"), 3);

        ShipSkillData restored = NpcTreeTag.decode("exiledSector_npcTree|bulwark|1|root,a,b|3");

        assertEquals(List.copyOf(built.getAllocatedNodeIds()), List.copyOf(restored.getAllocatedNodeIds()));
        assertEquals(ShipTrees.getSpentOp(built, 3), ShipTrees.getSpentOp(restored, 3));
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
