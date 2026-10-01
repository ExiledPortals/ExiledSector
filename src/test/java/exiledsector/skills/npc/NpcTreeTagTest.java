package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.tags.ShipProfile;
import exiledsector.skills.tags.WeaponKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NpcTreeTagTest {

    private static final ShipProfile FRIGATE =
            new ShipProfile(HullSize.FRIGATE, ShieldType.FRONT, 0, Set.of(WeaponKind.BALLISTIC), false);

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

    private static NpcTreeBuild build(int nodeCount) {
        NpcLayout layout = new NpcLayout("bulwark", "Bulwark", "root", List.of(), "",
                List.of(new NpcLayoutEntry("a", null), new NpcLayoutEntry("optional_1", "hull_option"),
                        new NpcLayoutEntry("b", null)));
        return NpcSkillTreeBuilder.build(layout, nodeCount, FRIGATE, NpcHullMods.NONE);
    }

    private static ShipVariantAPI variantWithTags(String... tags) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getTags()).thenReturn(List.of(tags));
        return variant;
    }

    @Test
    void encodesTheLayoutLevelAndEveryAllocatedNodeWithItsOptionInOrder() {
        String tag = NpcTreeTag.encode("bulwark", build(3).data());

        assertEquals("exiledSector_npcTree|bulwark|3|root,a,optional_1=hull_option,b", tag);
    }

    @Test
    void decodingRestoresTheExactTreeTheBuilderMade() {
        ShipSkillData built = build(5).data();

        ShipSkillData restored = NpcTreeTag.decode(NpcTreeTag.encode("bulwark", built));

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

        String tag = NpcTreeTag.encode("bulwark", built);
        ShipSkillData restored = NpcTreeTag.decode(tag);

        assertEquals("exiledSector_npcTree|bulwark|1|root,a,optional_1=hull_option,b", tag);
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
        String tag = NpcTreeTag.encode("bulwark", build(1).data());

        assertEquals(tag, NpcTreeTag.find(variantWithTags("exiledSector_installed_x", tag)));
        assertNull(NpcTreeTag.find(variantWithTags("exiledSector_installed_x")));
        assertNull(NpcTreeTag.find(null));
    }

    @Test
    void tagsSavedBeforeTheNpcRenameAreStillFoundAndDecoded() {
        String current = NpcTreeTag.encode("bulwark", build(5).data());
        String legacy = NpcTreeTag.LEGACY_PREFIX + current.substring(NpcTreeTag.PREFIX.length());

        assertEquals(legacy, NpcTreeTag.find(variantWithTags(legacy)));
        assertEquals("bulwark", NpcTreeTag.layoutId(legacy));
        assertEquals(List.copyOf(NpcTreeTag.decode(current).getAllocatedNodeIds()),
                List.copyOf(NpcTreeTag.decode(legacy).getAllocatedNodeIds()));
    }

    @Test
    void readsTheLayoutIdBackFromATag() {
        assertEquals("bulwark", NpcTreeTag.layoutId(NpcTreeTag.encode("bulwark", build(1).data())));
        assertNull(NpcTreeTag.layoutId("something_else"));
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
