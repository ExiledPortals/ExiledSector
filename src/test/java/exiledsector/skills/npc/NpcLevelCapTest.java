package exiledsector.skills.npc;

import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcLevelCapTest {

    private static final int MAX_LEVEL = 50;
    private static final int OVER_CAP_NODES = MAX_LEVEL + 9;

    @BeforeEach
    void setUp() {
        RealSkillData.clear();
        register("root", SkillTier.ROOT);
        String previous = "root";
        for (int index = 0; index < OVER_CAP_NODES; index++) {
            String id = "chain_" + index;
            register(id, SkillTier.SMALL, previous);
            previous = id;
        }
    }

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
    }

    private static void register(String id, SkillTier tier, String... connectedTo) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", tier).build();
        SkillTree.registerType(type);
        SkillTree.register(new SkillNode(id, type, List.of(connectedTo), 0f, 0f));
    }

    private static ShipSkillData overCapTree() {
        ShipSkillData data = NpcSkillTreeBuilder.rootedTree(SkillTree.get("root"), OVER_CAP_NODES);
        for (int index = 0; index < OVER_CAP_NODES; index++) {
            ShipTrees.allocate(data, SkillTree.get("chain_" + index), 1);
        }
        return data;
    }

    @Test
    void cappingTheLevelLowersOnlyALevelAboveTheCap() {
        ShipSkillData overCap = overCapTree();
        ShipSkillData underCap = NpcSkillTreeBuilder.rootedTree(SkillTree.get("root"), 3);

        overCap.capLevel(MAX_LEVEL);
        underCap.capLevel(MAX_LEVEL);

        assertEquals(MAX_LEVEL, overCap.getLevel());
        assertEquals(3, underCap.getLevel());
    }

    @Test
    void aCapturedTreeBiggerThanItsLevelLosesItsNewestFreeNodes() {
        ShipSkillData data = overCapTree();
        data.capLevel(MAX_LEVEL);

        List<String> removed = ShipTrees.trimFreeAllocationsToLevel(data, SkillTree.getAllNodes(), SkillTree.topology());

        assertEquals(9, removed.size());
        assertEquals(MAX_LEVEL + 1, data.getAllocatedNodeIds().size());
        assertEquals(0, data.getBankedFreeAllocations());
        assertTrue(data.isAllocated("chain_0"));
        assertFalse(data.isAllocated("chain_" + (OVER_CAP_NODES - 1)));
    }

    @Test
    void unspentFreeAllocationsBeyondTheLevelAreDroppedOnCapture() {
        ShipSkillData data = NpcSkillTreeBuilder.rootedTree(SkillTree.get("root"), OVER_CAP_NODES);
        ShipTrees.allocate(data, SkillTree.get("chain_0"), 1);
        data.capLevel(MAX_LEVEL);

        List<String> removed = ShipTrees.trimFreeAllocationsToLevel(data, SkillTree.getAllNodes(), SkillTree.topology());

        assertTrue(removed.isEmpty());
        assertEquals(MAX_LEVEL - 1, data.getBankedFreeAllocations());
    }

    @Test
    void aTreeWithinItsLevelIsLeftAlone() {
        ShipSkillData data = NpcSkillTreeBuilder.rootedTree(SkillTree.get("root"), 3);
        ShipTrees.allocate(data, SkillTree.get("chain_0"), 1);
        ShipTrees.allocate(data, SkillTree.get("chain_1"), 1);

        List<String> removed = ShipTrees.trimFreeAllocationsToLevel(data, SkillTree.getAllNodes(), SkillTree.topology());

        assertTrue(removed.isEmpty());
        assertEquals(1, data.getBankedFreeAllocations());
        assertEquals(3, data.getAllocatedNodeIds().size());
    }
}
