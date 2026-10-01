package exiledsector.skills;

import exiledsector.skills.layout.SkillNodeDecoration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipSkillDataTest {

    private static final int AMPLE_BUDGET = 100;
    private static final int AMPLE_NODE_CAP = 1000;

    @BeforeEach
    void setUp() {
        SkillTree.clearTypes();
        SkillTree.clearNodes();
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearTypes();
        SkillTree.clearNodes();
    }

    private static SkillNode node(String id, List<String> prerequisiteIds) {
        SkillType type = new SkillType.Builder(id, id, "graphics/hullmods/heavy_armor.png", SkillTier.SMALL)
                .effects(List.of())
                .build();
        return new SkillNode(id, type, prerequisiteIds, 0f, 0f);
    }

    private static SkillNode rootNode(String id, List<String> prerequisiteIds) {
        SkillType type = new SkillType.Builder(id, id, "a.png", SkillTier.ROOT)
                .effects(List.of())
                .build();
        return new SkillNode(id, type, prerequisiteIds, 0f, 0f);
    }

    private static SkillNode wormholeNode(String id, List<String> prerequisiteIds, String pairedNodeId) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", SkillTier.WORMHOLE)
                .effects(List.of())
                .build();
        return new SkillNode(id, type, prerequisiteIds, 0f, 0f, new SkillNodeDecoration(null, null, null, null, pairedNodeId));
    }

    @Test
    void startsWithNoProgress() {
        ShipSkillData data = new ShipSkillData();

        assertEquals(0, data.getSpentOp(3));
        assertTrue(data.getAllocatedNodeIds().isEmpty());
    }

    @Test
    void allocateMarksNodeAndSpendsTheGivenOpCost() {
        ShipSkillData data = new ShipSkillData();
        SkillNode node = node("armor_1", List.of());

        data.allocate(node, 3);

        assertTrue(data.isAllocated("armor_1"));
        assertEquals(3, data.getSpentOp(3));
    }

    @Test
    void aRootAllocatedBeforeStartingRootsWereRecordedStillCostsNothing() {
        ShipSkillData data = new ShipSkillData();
        SkillNode root = rootNode("root_low_tech_1", List.of());
        SkillTree.register(root);

        data.allocate(root, 0);
        data.allocate(node("armor_1", List.of("root_low_tech_1")), 3);

        assertTrue(data.isAllocated("root_low_tech_1"));
        assertEquals(3, data.getSpentOp(3));
    }

    @Test
    void spentOpPricesEveryPaidNodeAtTheCurrentCostSoRefundsNeverDrift() {
        ShipSkillData data = new ShipSkillData();
        data.chooseStartingRoot(rootNode("root_low_tech_1", List.of()));
        SkillNode a = node("a", List.of());
        SkillNode b = node("b", List.of());
        data.allocate(a, 1);
        data.allocate(b, 1);
        data.addFreeAllocationCredit();
        data.allocate(node("c", List.of()), 1);

        assertEquals(2, data.getSpentOp(1));
        assertEquals(6, data.getSpentOp(3));

        data.deallocate(a);
        assertEquals(3, data.getSpentOp(3));
        data.deallocate(b);
        assertEquals(0, data.getSpentOp(3));
        assertEquals(0, data.getSpentOp(1));
    }

    @Test
    void deallocateRefundsTheOpCost() {
        ShipSkillData data = new ShipSkillData();
        SkillNode node = node("armor_1", List.of());
        data.allocate(node, 3);

        data.deallocate(node);

        assertFalse(data.isAllocated("armor_1"));
        assertEquals(0, data.getSpentOp(3));
    }

    @Test
    void isAllocatedIsFalseForANodeThatWasNeverAllocated() {
        ShipSkillData data = new ShipSkillData();

        assertFalse(data.isAllocated("nonexistent"));
    }

    @Test
    void canAllocateIsTrueWhenThereAreNoPrerequisites() {
        ShipSkillData data = new ShipSkillData();

        assertTrue(data.canAllocate(node("root", List.of()), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canAllocateIsFalseWhenAPrerequisiteIsNotAllocated() {
        ShipSkillData data = new ShipSkillData();

        assertFalse(data.canAllocate(node("child", List.of("parent")), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canAllocateIsTrueOnceEveryPrerequisiteIsAllocated() {
        ShipSkillData data = new ShipSkillData();
        SkillNode parent = node("parent", List.of());
        data.allocate(parent, 1);

        assertTrue(data.canAllocate(node("child", List.of("parent")), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canAllocateIsFalseWithMultiplePrerequisitesWhenNoneAreAllocated() {
        ShipSkillData data = new ShipSkillData();

        assertFalse(data.canAllocate(node("child", List.of("b", "c")), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canAllocateIsTrueWithMultiplePrerequisitesWhenOnlyOneIsAllocated() {
        ShipSkillData data = new ShipSkillData();
        SkillNode b = node("b", List.of());
        data.allocate(b, 1);

        assertTrue(data.canAllocate(node("a", List.of("b", "c")), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canAllocateIsTrueWithMultiplePrerequisitesWhenAllAreAllocated() {
        ShipSkillData data = new ShipSkillData();
        SkillNode b = node("b", List.of());
        SkillNode c = node("c", List.of());
        data.allocate(b, 1);
        data.allocate(c, 1);

        assertTrue(data.canAllocate(node("a", List.of("b", "c")), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canAllocateIsTrueWhenSpendingTheLastRemainingOpBudget() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of());
        data.allocate(a, 1);

        assertTrue(data.canAllocate(node("b", List.of()), null, 2, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canAllocateIsFalseWhenTheBudgetIsExhausted() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of());
        data.allocate(a, 1);

        assertFalse(data.canAllocate(node("b", List.of()), null, 1, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canAllocateAllowsAZeroCostNodeEvenWhenTheBudgetIsFullySpent() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of());
        data.allocate(a, 1);

        assertTrue(data.canAllocate(rootNode("root_low_tech_1", List.of()), null, 1, 0, AMPLE_NODE_CAP));
    }

    @Test
    void canDeallocateIsTrueWhenNoAllocatedNodeDependsOnIt() {
        ShipSkillData data = new ShipSkillData();
        SkillNode parent = node("parent", List.of());
        data.allocate(parent, 1);

        assertTrue(data.canDeallocate(parent, List.of(parent), null));
    }

    @Test
    void canDeallocateIsFalseWhenAnAllocatedChildDependsOnIt() {
        ShipSkillData data = new ShipSkillData();
        SkillNode parent = node("parent", List.of());
        SkillNode child = node("child", List.of("parent"));
        data.allocate(parent, 1);
        data.allocate(child, 1);

        assertFalse(data.canDeallocate(parent, List.of(parent, child), null));
    }

    @Test
    void canDeallocateIsTrueWhenAnAllocatedChildHasAnotherAllocatedPrerequisite() {
        ShipSkillData data = new ShipSkillData();
        SkillNode b = node("b", List.of());
        SkillNode c = node("c", List.of());
        SkillNode a = node("a", List.of("b", "c"));
        data.allocate(b, 1);
        data.allocate(c, 1);
        data.allocate(a, 1);

        assertTrue(data.canDeallocate(b, List.of(a, b, c), null));
    }

    @Test
    void canDeallocateIsFalseWhenItIsTheOnlyAllocatedPrerequisiteOfAnAllocatedChild() {
        ShipSkillData data = new ShipSkillData();
        SkillNode b = node("b", List.of());
        SkillNode c = node("c", List.of());
        SkillNode a = node("a", List.of("b", "c"));
        data.allocate(b, 1);
        data.allocate(a, 1);

        assertFalse(data.canDeallocate(b, List.of(a, b, c), null));
    }

    @Test
    void toggleAllocatesAnUnallocatedNodeWhosePrerequisitesAreMet() {
        ShipSkillData data = new ShipSkillData();
        SkillNode root = node("root", List.of());

        data.toggle(root, List.of(root), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP);

        assertTrue(data.isAllocated("root"));
    }

    @Test
    void toggleDoesNothingForAnUnallocatedNodeWithAnUnmetPrerequisite() {
        ShipSkillData data = new ShipSkillData();
        SkillNode parent = node("parent", List.of());
        SkillNode child = node("child", List.of("parent"));

        data.toggle(child, List.of(parent, child), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP);

        assertFalse(data.isAllocated("child"));
    }

    @Test
    void toggleDoesNothingForAnUnallocatedNodeWhenTheBudgetIsExhausted() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of());
        SkillNode b = node("b", List.of());
        data.allocate(a, 1);

        data.toggle(b, List.of(a, b), null, 1, 1, AMPLE_NODE_CAP);

        assertFalse(data.isAllocated("b"));
    }

    @Test
    void toggleDeallocatesAnAllocatedNodeWithNoAllocatedChildren() {
        ShipSkillData data = new ShipSkillData();
        SkillNode root = node("root", List.of());
        data.allocate(root, 1);

        data.toggle(root, List.of(root), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP);

        assertFalse(data.isAllocated("root"));
    }

    @Test
    void toggleDoesNothingForAnAllocatedNodeWithAnAllocatedChild() {
        ShipSkillData data = new ShipSkillData();
        SkillNode parent = node("parent", List.of());
        SkillNode child = node("child", List.of("parent"));
        data.allocate(parent, 1);
        data.allocate(child, 1);

        data.toggle(parent, List.of(parent, child), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP);

        assertTrue(data.isAllocated("parent"));
    }

    @Test
    void isSatisfiedIsTrueForAnAllocatedNode() {
        ShipSkillData data = new ShipSkillData();
        data.allocate(node("hull_1", List.of()), 1);

        assertTrue(data.isSatisfied("hull_1", null));
    }

    @Test
    void isSatisfiedIsTrueForTheSatisfiedRootEvenIfNeverAllocated() {
        ShipSkillData data = new ShipSkillData();

        assertTrue(data.isSatisfied("root_low_tech_1", "root_low_tech_1"));
    }

    @Test
    void isSatisfiedIsFalseForAnUnsatisfiedRoot() {
        ShipSkillData data = new ShipSkillData();

        assertFalse(data.isSatisfied("root_high_tech_1", "root_low_tech_1"));
    }

    @Test
    void canAllocateIsTrueWhenPrerequisiteIsTheSatisfiedRootEvenIfNotAllocated() {
        ShipSkillData data = new ShipSkillData();
        SkillNode child = node("hull_1", List.of("root_low_tech_1"));

        assertTrue(data.canAllocate(child, "root_low_tech_1", AMPLE_BUDGET, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canAllocateIsFalseWhenPrerequisiteIsAnUnsatisfiedRoot() {
        ShipSkillData data = new ShipSkillData();
        SkillNode child = node("hull_1", List.of("root_midline_1"));

        assertFalse(data.canAllocate(child, "root_low_tech_1", AMPLE_BUDGET, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canDeallocateTreatsTheSatisfiedRootAsAnotherAllocatedPrerequisite() {
        ShipSkillData data = new ShipSkillData();
        SkillNode b = node("b", List.of());
        SkillNode a = node("a", List.of("b", "root_low_tech_1"));
        data.allocate(b, 1);
        data.allocate(a, 1);

        assertTrue(data.canDeallocate(b, List.of(a, b), "root_low_tech_1"));
    }

    @Test
    void canDeallocateIsFalseWhenTheOnlyAlternatePathIsItselfNotConnectedToTheRoot() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of());
        SkillNode b = node("b", List.of("a", "x"));
        SkillNode x = node("x", List.of("b"));
        data.allocate(a, 1);
        data.allocate(b, 1);
        data.allocate(x, 1);

        assertFalse(data.canDeallocate(a, List.of(a, b, x), null));
    }

    @Test
    void canDeallocateIsTrueWhenTheAlternatePathTracesBackToTheRoot() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a1 = node("a1", List.of());
        SkillNode a2 = node("a2", List.of());
        SkillNode b = node("b", List.of("a1", "x"));
        SkillNode x = node("x", List.of("b", "a2"));
        data.allocate(a1, 1);
        data.allocate(a2, 1);
        data.allocate(b, 1);
        data.allocate(x, 1);

        assertTrue(data.canDeallocate(a1, List.of(a1, a2, b, x), null));
    }

    @Test
    void canDeallocateIsTrueForAMemberOfASymmetricCircularLoopWhenTheRestStillReachTheRoot() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of("root_low_tech_1", "b", "c"));
        SkillNode b = node("b", List.of("a", "c"));
        SkillNode c = node("c", List.of("a", "b"));
        data.allocate(a, 1);
        data.allocate(b, 1);
        data.allocate(c, 1);

        assertTrue(data.canDeallocate(b, List.of(a, b, c), "root_low_tech_1"));
    }

    @Test
    void canDeallocateIsFalseForTheSoleBridgeOfASymmetricCircularLoop() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of("root_low_tech_1", "b", "c"));
        SkillNode b = node("b", List.of("a", "c"));
        SkillNode c = node("c", List.of("a", "b"));
        data.allocate(a, 1);
        data.allocate(b, 1);
        data.allocate(c, 1);

        assertFalse(data.canDeallocate(a, List.of(a, b, c), "root_low_tech_1"));
    }

    @Test
    void canDeallocateIsUnaffectedByAnUnrelatedNodeThatWasAlreadyStrandedBeforeThisRemoval() {
        ShipSkillData data = new ShipSkillData();
        SkillNode leaf = node("leaf", List.of("root_low_tech_1"));
        SkillNode orphan = node("orphan", List.of("nothingThatExists"));
        data.allocate(leaf, 1);
        data.allocate(orphan, 1);

        assertTrue(data.canDeallocate(leaf, List.of(leaf, orphan), "root_low_tech_1"));
    }

    @Test
    void canDeallocateIsTrueForEveryNonBridgeMemberOfATenNodeSymmetricLoop() {
        int ringSize = 10;
        List<SkillNode> ring = new ArrayList<>();
        for (int i = 0; i < ringSize; i++) {
            String id = "ring" + i;
            List<String> neighbors = new ArrayList<>();
            neighbors.add("ring" + ((i - 1 + ringSize) % ringSize));
            neighbors.add("ring" + ((i + 1) % ringSize));
            if (i == 0) neighbors.add("root_low_tech_1");
            ring.add(node(id, neighbors));
        }

        ShipSkillData data = new ShipSkillData();
        for (SkillNode n : ring) {
            data.allocate(n, 1);
        }

        for (int i = 1; i < ringSize; i++) {
            assertTrue(data.canDeallocate(ring.get(i), ring, "root_low_tech_1"),
                    "expected ring" + i + " to be deallocatable");
        }
        assertFalse(data.canDeallocate(ring.get(0), ring, "root_low_tech_1"),
                "ring0 is the sole bridge to root and must not be deallocatable while the rest of the ring is allocated");
    }

    @Test
    void canDeallocateIsFalseWhenASecondAllocatedRootOnlyReachesTheTrueRootThroughTheRemovedNode() {
        SkillType rootType = new SkillType.Builder("secondRootType", "Second Root", "a.png", SkillTier.ROOT)
                .effects(List.of())
                .build();
        SkillNode secondRoot = new SkillNode("secondRoot", rootType, List.of("bridge", "descendant"), 0f, 0f);
        SkillNode bridge = node("bridge", List.of("root_low_tech_1", "secondRoot"));
        SkillNode descendant = node("descendant", List.of("secondRoot"));

        ShipSkillData data = new ShipSkillData();
        data.allocate(bridge, 1);
        data.allocate(secondRoot, 1);
        data.allocate(descendant, 1);

        assertFalse(data.canDeallocate(bridge, List.of(bridge, secondRoot, descendant), "root_low_tech_1"));
    }

    @Test
    void canDeallocateIsTrueWhenASecondAllocatedRootStillTracesBackToTheTrueRootAfterRemoval() {
        SkillType rootType = new SkillType.Builder("secondRootType", "Second Root", "a.png", SkillTier.ROOT)
                .effects(List.of())
                .build();
        SkillNode secondRoot = new SkillNode("secondRoot", rootType, List.of("bridgeA", "bridgeB"), 0f, 0f);
        SkillNode bridgeA = node("bridgeA", List.of("root_low_tech_1", "secondRoot"));
        SkillNode bridgeB = node("bridgeB", List.of("root_low_tech_1", "secondRoot"));

        ShipSkillData data = new ShipSkillData();
        data.allocate(bridgeA, 1);
        data.allocate(bridgeB, 1);
        data.allocate(secondRoot, 1);

        assertTrue(data.canDeallocate(bridgeA, List.of(bridgeA, bridgeB, secondRoot), "root_low_tech_1"));
    }

    @Test
    void reproduceReportedLoopLockup() {
        SkillNode rootLowTech = new SkillNode("root_low_tech_1",
                new SkillType.Builder("root_low_tech", "Root Low Tech", "a.png", SkillTier.ROOT)
                        .effects(List.of())
                        .build(),
                List.of("small_logistics_optional_11", "small_logistics_optional_31", "small_logistics_optional_14", "small_flux_optional_34"),
                0f, 0f);
        SkillNode n3 = node("small_logistics_optional_3", List.of("root_high_tech_1", "small_logistics_optional_6", "small_logistics_optional_24"));
        SkillNode n6 = node("small_logistics_optional_6", List.of("small_logistics_optional_8", "small_logistics_optional_3"));
        SkillNode n8 = node("small_logistics_optional_8", List.of("small_logistics_optional_6", "small_logistics_optional_12"));
        SkillNode n12 = node("small_logistics_optional_12", List.of("small_logistics_optional_8", "small_logistics_optional_11"));
        SkillNode n11 = node("small_logistics_optional_11", List.of("root_low_tech_1", "small_logistics_optional_26", "small_logistics_optional_12"));
        SkillNode n5 = node("small_logistics_optional_5", List.of("small_logistics_optional_7", "small_logistics_optional_25"));
        SkillNode n7 = node("small_logistics_optional_7", List.of("small_logistics_optional_5", "small_logistics_optional_10", "survey_generic_cost_reduction_1"));
        SkillNode n9 = node("small_logistics_optional_9", List.of("small_logistics_optional_13", "small_logistics_optional_25"));
        SkillNode n10 = node("small_logistics_optional_10", List.of("small_logistics_optional_7", "root_high_tech_1", "increased_sensor_strength_2", "small_logistics_optional_23"));
        SkillNode n13 = node("small_logistics_optional_13", List.of("small_logistics_optional_9", "small_logistics_optional_14", "cargo_capacity_flat_2", "fuel_flat_2"));
        SkillNode n14 = node("small_logistics_optional_14", List.of("small_logistics_optional_13", "root_low_tech_1", "small_logistics_optional_22"));
        SkillNode n25 = node("small_logistics_optional_25", List.of("small_logistics_optional_4", "small_logistics_optional_9", "small_logistics_optional_5", "operations_center_1", "efficiency_overhaul_1", "converted_fighterbay_1"));

        List<SkillNode> allNodes = List.of(rootLowTech, n3, n6, n8, n12, n11, n5, n7, n9, n10, n13, n14, n25);

        ShipSkillData data = new ShipSkillData();
        String satisfiedRootId = "root_high_tech_1";
        for (SkillNode n : List.of(n10, n7, n5, n25, n9, n13, n14, rootLowTech, n11, n12, n8, n6, n3)) {
            assertTrue(data.canAllocate(n, satisfiedRootId, AMPLE_BUDGET, 1, AMPLE_NODE_CAP), "expected to be able to allocate " + n.getId());
            data.allocate(n, 1);
        }

        for (SkillNode n : allNodes) {
            assertTrue(data.canDeallocate(n, allNodes, satisfiedRootId),
                    "expected " + n.getId() + " to be deallocatable (ring has two independent paths to root_high_tech_1)");
        }
    }

    @Test
    void reproduceReportedAcceleratedShieldsWheelLockup() {
        SkillNode hub = node("advancedshieldemitter_1", List.of("shield_raise_rate_1", "shield_turn_rate_1"));
        SkillNode raise1 = node("shield_raise_rate_1", List.of("shield_raise_rate_2", "advancedshieldemitter_1"));
        SkillNode raise2 = node("shield_raise_rate_2", List.of("shield_raise_rate_1", "shield_turn_raise_rate_optional_1"));
        SkillNode bridge = node("shield_turn_raise_rate_optional_1", List.of("small_flux_optional_25", "shield_raise_rate_2", "shield_turn_rate_2"));
        SkillNode turn2 = node("shield_turn_rate_2", List.of("shield_turn_rate_1", "shield_turn_raise_rate_optional_1"));
        SkillNode turn1 = node("shield_turn_rate_1", List.of("shield_turn_rate_2", "advancedshieldemitter_1"));

        List<SkillNode> ring = List.of(hub, raise1, raise2, bridge, turn2, turn1);

        ShipSkillData data = new ShipSkillData();
        for (SkillNode n : ring) {
            data.allocate(n, 1);
        }

        for (SkillNode n : ring) {
            if (n == bridge) continue;
            assertTrue(data.canDeallocate(n, ring, "small_flux_optional_25"),
                    "expected " + n.getId() + " to be deallocatable");
        }
        assertFalse(data.canDeallocate(bridge, ring, "small_flux_optional_25"),
                "shield_turn_raise_rate_optional_1 is the ring's sole bridge and must stay blocked while the rest of the ring is allocated");
    }

    @Test
    void getOptionalSelectionIsNullWhenNothingHasBeenSelected() {
        ShipSkillData data = new ShipSkillData();

        assertNull(data.getOptionalSelection("slot_1"));
    }

    @Test
    void selectOptionAllocatesTheSlotNodeAndSpendsTheGivenOpCost() {
        ShipSkillData data = new ShipSkillData();
        SkillNode slot = node("slot_1", List.of());
        SkillType chosenOption = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL)
                .effects(List.of())
                .build();

        data.selectOption(slot, chosenOption, 1);

        assertTrue(data.isAllocated("slot_1"));
        assertEquals("hull", data.getOptionalSelection("slot_1"));
        assertEquals(1, data.getSpentOp(1));
    }

    @Test
    void selectOptionConsumesABankedFreeAllocationInsteadOfSpendingOp() {
        ShipSkillData data = new ShipSkillData();
        data.addFreeAllocationCredit();
        SkillNode slot = node("slot_1", List.of());
        SkillType chosenOption = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL)
                .effects(List.of())
                .build();

        data.selectOption(slot, chosenOption, 1);

        assertTrue(data.isAllocated("slot_1"));
        assertTrue(data.isFreeNode("slot_1"));
        assertEquals(0, data.getBankedFreeAllocations());
        assertEquals(0, data.getSpentOp(1));
    }

    @Test
    void deallocatingAFreelySelectedOptionalNodeRefundsTheBankedCreditInsteadOfOp() {
        ShipSkillData data = new ShipSkillData();
        data.addFreeAllocationCredit();
        SkillNode slot = node("slot_1", List.of());
        SkillType chosenOption = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL)
                .effects(List.of())
                .build();
        SkillTree.registerType(chosenOption);
        data.selectOption(slot, chosenOption, 1);

        data.deallocate(slot);

        assertFalse(data.isAllocated("slot_1"));
        assertEquals(1, data.getBankedFreeAllocations());
        assertEquals(0, data.getSpentOp(1));
    }

    @Test
    void deallocateRefundsTheSlotsOpCostRegardlessOfSelectedOption() {
        ShipSkillData data = new ShipSkillData();
        SkillNode slot = node("slot_1", List.of());
        SkillType chosenOption = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL)
                .effects(List.of())
                .build();
        SkillTree.registerType(chosenOption);
        data.selectOption(slot, chosenOption, 1);

        data.deallocate(slot);

        assertFalse(data.isAllocated("slot_1"));
        assertNull(data.getOptionalSelection("slot_1"));
        assertEquals(0, data.getSpentOp(1));
    }

    @Test
    void togglingASelectedOptionalNodeOffClearsTheSelection() {
        ShipSkillData data = new ShipSkillData();
        SkillNode slot = node("slot_1", List.of());
        SkillType chosenOption = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL)
                .effects(List.of())
                .build();
        SkillTree.registerType(chosenOption);
        data.selectOption(slot, chosenOption, 1);

        data.toggle(slot, List.of(slot), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP);

        assertFalse(data.isAllocated("slot_1"));
        assertNull(data.getOptionalSelection("slot_1"));
    }

    @Test
    void selectingAnotherOptionOnAnAlreadyAllocatedNodeDoesNotChargeAnotherOpCost() {
        ShipSkillData data = new ShipSkillData();
        SkillNode slot = node("slot_1", List.of());
        SkillType hullOption = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL)
                .effects(List.of())
                .build();
        SkillType armorOption = new SkillType.Builder("armor", "Armor", "a.png", SkillTier.SMALL)
                .effects(List.of())
                .build();
        SkillTree.registerType(hullOption);
        SkillTree.registerType(armorOption);
        data.selectOption(slot, hullOption, 1);

        data.selectOption(slot, armorOption, 1);

        assertTrue(data.isAllocated("slot_1"));
        assertEquals("armor", data.getOptionalSelection("slot_1"));
        assertEquals(1, data.getSpentOp(1));
    }

    @Test
    void reselectingTheSameOptionOnAnAlreadyAllocatedNodeDoesNotDoubleChargeIt() {
        ShipSkillData data = new ShipSkillData();
        SkillNode slot = node("slot_1", List.of());
        SkillType hullOption = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL)
                .effects(List.of())
                .build();
        SkillTree.registerType(hullOption);
        data.selectOption(slot, hullOption, 1);

        data.selectOption(slot, hullOption, 1);

        assertEquals(1, data.getSpentOp(1));
    }

    @Test
    void startsAtLevelZeroWithNoXpOrBankedAllocations() {
        ShipSkillData data = new ShipSkillData();

        assertEquals(0, data.getLevel());
        assertEquals(0f, data.getXp());
        assertEquals(0, data.getBankedFreeAllocations());
    }

    @Test
    void convertMostRecentAllocationToFreeMarksTheLastAllocatedNodeFreeAndRefundsItsOp() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of());
        SkillNode b = node("b", List.of());
        data.allocate(a, 3);
        data.allocate(b, 3);

        boolean converted = data.convertMostRecentAllocationToFree(List.of(a, b));

        assertTrue(converted);
        assertTrue(data.isFreeNode("b"));
        assertFalse(data.isFreeNode("a"));
        assertEquals(3, data.getSpentOp(3));
    }

    @Test
    void convertMostRecentAllocationToFreeSkipsTheStartingRoot() {
        ShipSkillData data = new ShipSkillData();
        SkillNode root = rootNode("root_low_tech_1", List.of());
        SkillNode a = node("a", List.of("root_low_tech_1"));
        data.allocate(a, 3);
        data.chooseStartingRoot(root);

        boolean converted = data.convertMostRecentAllocationToFree(List.of(a, root));

        assertTrue(converted);
        assertTrue(data.isFreeNode("a"));
        assertFalse(data.isFreeNode("root_low_tech_1"));
        assertEquals(0, data.getSpentOp(3));
    }

    @Test
    void convertMostRecentAllocationToFreeRefundsAPaidForNonStartingRoot() {
        ShipSkillData data = new ShipSkillData();
        SkillNode startingRoot = rootNode("root_low_tech_1", List.of());
        SkillNode otherRoot = rootNode("root_high_tech_1", List.of());
        data.chooseStartingRoot(startingRoot);
        data.allocate(otherRoot, 3);

        boolean converted = data.convertMostRecentAllocationToFree(List.of(startingRoot, otherRoot));

        assertTrue(converted);
        assertTrue(data.isFreeNode("root_high_tech_1"));
        assertEquals(0, data.getSpentOp(3));
    }

    @Test
    void convertMostRecentAllocationToFreeSkipsNodesAlreadyFree() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of());
        SkillNode b = node("b", List.of());
        data.allocate(a, 3);
        data.allocate(b, 3);
        data.convertMostRecentAllocationToFree(List.of(a, b));

        boolean convertedAgain = data.convertMostRecentAllocationToFree(List.of(a, b));

        assertTrue(convertedAgain);
        assertTrue(data.isFreeNode("a"));
        assertEquals(0, data.getSpentOp(3));
    }

    @Test
    void convertMostRecentAllocationToFreeSkipsTheAutoGrantedWormholeCompanion() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");
        SkillNode b = wormholeNode("wormhole_b", List.of(), "wormhole_a");
        data.allocate(a, 3);

        boolean converted = data.convertMostRecentAllocationToFree(List.of(a, b));

        assertTrue(converted);
        assertTrue(data.isFreeNode("wormhole_a"));
        assertEquals(0, data.getSpentOp(3));

        data.deallocate(a);

        assertEquals(0, data.getSpentOp(3));
    }

    @Test
    void convertMostRecentAllocationToFreeReturnsFalseWhenNothingIsEligible() {
        ShipSkillData data = new ShipSkillData();
        SkillNode root = rootNode("root_low_tech_1", List.of());
        data.allocate(root, 0);

        assertFalse(data.convertMostRecentAllocationToFree(List.of(root)));
    }

    @Test
    void allocateConsumesABankedFreeAllocationInsteadOfSpendingOp() {
        ShipSkillData data = new ShipSkillData();
        data.addFreeAllocationCredit();
        SkillNode a = node("a", List.of());

        data.allocate(a, 3);

        assertTrue(data.isFreeNode("a"));
        assertEquals(0, data.getBankedFreeAllocations());
        assertEquals(0, data.getSpentOp(3));
    }

    @Test
    void allocateDoesNotConsumeABankedCreditForAZeroCostNode() {
        ShipSkillData data = new ShipSkillData();
        data.addFreeAllocationCredit();
        SkillNode root = rootNode("root_low_tech_1", List.of());

        data.allocate(root, 0);

        assertFalse(data.isFreeNode("root_low_tech_1"));
        assertEquals(1, data.getBankedFreeAllocations());
    }

    @Test
    void deallocatingAFreeNodeRefundsTheBankedCreditInsteadOfOp() {
        ShipSkillData data = new ShipSkillData();
        data.addFreeAllocationCredit();
        SkillNode a = node("a", List.of());
        data.allocate(a, 3);

        data.deallocate(a);

        assertFalse(data.isAllocated("a"));
        assertEquals(1, data.getBankedFreeAllocations());
        assertEquals(0, data.getSpentOp(3));
    }

    @Test
    void canAllocateIsFalseOnceTheNodeCapIsReached() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of());
        data.allocate(a, 1);

        assertFalse(data.canAllocate(node("b", List.of()), null, AMPLE_BUDGET, 1, 1));
    }

    @Test
    void canAllocateIsTrueBelowTheNodeCap() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of());
        data.allocate(a, 1);

        assertTrue(data.canAllocate(node("b", List.of()), null, AMPLE_BUDGET, 1, 2));
    }

    @Test
    void allocatingAWormholeAlsoAllocatesItsPairForFree() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");

        data.allocate(a, 3);

        assertTrue(data.isAllocated("wormhole_a"));
        assertTrue(data.isAllocated("wormhole_b"));
        assertTrue(data.isFreeNode("wormhole_b"));
        assertFalse(data.isFreeNode("wormhole_a"));
        assertEquals(3, data.getSpentOp(3));
    }

    @Test
    void allocatingAWormholeDoesNotDoubleAllocateAnAlreadyAllocatedPair() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");
        SkillNode b = wormholeNode("wormhole_b", List.of(), "wormhole_a");
        data.allocate(b, 3);

        data.allocate(a, 3);

        assertEquals(2, data.getAllocatedNodeIds().size());
        assertEquals(3, data.getSpentOp(3));
    }

    @Test
    void deallocatingAWormholeAlsoDeallocatesItsPair() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");
        data.allocate(a, 3);

        data.deallocate(a);

        assertFalse(data.isAllocated("wormhole_a"));
        assertFalse(data.isAllocated("wormhole_b"));
        assertEquals(0, data.getSpentOp(3));
        assertEquals(0, data.getBankedFreeAllocations());
    }

    @Test
    void deallocatingEitherEndOfAWormholePairDeallocatesBoth() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");
        SkillNode b = wormholeNode("wormhole_b", List.of(), "wormhole_a");
        data.allocate(a, 3);

        data.deallocate(b);

        assertFalse(data.isAllocated("wormhole_a"));
        assertFalse(data.isAllocated("wormhole_b"));
        assertEquals(0, data.getSpentOp(3));
        assertEquals(0, data.getBankedFreeAllocations());
    }

    @Test
    void repeatedlyAllocatingAndDeallocatingAWormholePairNeverMintsFreeAllocationCredits() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");

        for (int i = 0; i < 3; i++) {
            data.allocate(a, 3);
            data.deallocate(a);

            assertEquals(0, data.getSpentOp(3));
            assertEquals(0, data.getBankedFreeAllocations());
        }
    }

    @Test
    void nonWormholeAllocationIsUnaffectedByPairing() {
        ShipSkillData data = new ShipSkillData();
        SkillNode plain = node("armor_1", List.of());

        data.allocate(plain, 3);

        assertEquals(1, data.getAllocatedNodeIds().size());
        assertEquals(3, data.getSpentOp(3));
    }

    @Test
    void canAllocateRequiresRoomForBothEndsOfAnUnallocatedPair() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");

        assertFalse(data.canAllocate(a, null, AMPLE_BUDGET, 1, 1));
        assertTrue(data.canAllocate(a, null, AMPLE_BUDGET, 1, 2));
    }

    @Test
    void canAllocateOnlyNeedsOneSlotWhenThePairIsAlreadyIndependentlyAllocated() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");
        data.getAllocatedNodeIds().add("wormhole_b");

        assertFalse(data.canAllocate(a, null, AMPLE_BUDGET, 1, 1));
        assertTrue(data.canAllocate(a, null, AMPLE_BUDGET, 1, 2));
    }

    @Test
    void canDeallocateIsFalseWhenAChildOnlyReachesTheRootThroughTheOtherEndOfTheWormholePair() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");
        SkillNode b = wormholeNode("wormhole_b", List.of(), "wormhole_a");
        SkillNode child = node("child", List.of("wormhole_b"));
        data.allocate(a, 1);
        data.allocate(child, 1);

        assertFalse(data.canDeallocate(a, List.of(a, b, child), null));
        assertFalse(data.canDeallocate(b, List.of(a, b, child), null));
    }

    @Test
    void canDeallocateIsTrueWhenNothingDependsOnEitherEndOfTheWormholePair() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");
        SkillNode b = wormholeNode("wormhole_b", List.of(), "wormhole_a");
        data.allocate(a, 1);

        assertTrue(data.canDeallocate(a, List.of(a, b), null));
    }

    @Test
    void choosingAStartingRootAllocatesItForFree() {
        SkillNode root = rootNode("root_a", List.of());
        ShipSkillData data = new ShipSkillData();

        assertTrue(data.chooseStartingRoot(root));

        assertTrue(data.isAllocated("root_a"));
        assertEquals(0, data.getSpentOp(3));
        assertEquals("root_a", data.resolveStartingRootId(List.of(root)));
    }

    @Test
    void theStartingRootCannotBeChosenTwice() {
        SkillNode first = rootNode("root_a", List.of());
        SkillNode second = rootNode("root_b", List.of());
        ShipSkillData data = new ShipSkillData();
        data.chooseStartingRoot(first);

        assertFalse(data.chooseStartingRoot(second));

        assertFalse(data.isAllocated("root_b"));
        assertEquals("root_a", data.resolveStartingRootId(List.of(first, second)));
    }

    @Test
    void theStartingRootCanBeUnchosenWhileItIsTheOnlyAllocatedNodeAndThenChosenAgain() {
        SkillNode first = rootNode("root_a", List.of());
        SkillNode second = rootNode("root_b", List.of());
        ShipSkillData data = new ShipSkillData();
        data.chooseStartingRoot(first);

        assertTrue(data.canUnchooseStartingRoot());
        assertTrue(data.unchooseStartingRoot());

        assertFalse(data.isAllocated("root_a"));
        assertNull(data.resolveStartingRootId(List.of(first, second)));
        assertTrue(data.chooseStartingRoot(second));
        assertEquals("root_b", data.resolveStartingRootId(List.of(first, second)));
    }

    @Test
    void theStartingRootIsLockedInOnceAnyOtherNodeIsAllocated() {
        SkillNode root = rootNode("root_a", List.of());
        SkillNode next = node("small", List.of("root_a"));
        ShipSkillData data = new ShipSkillData();
        data.chooseStartingRoot(root);
        data.allocate(next, 3);

        assertFalse(data.canUnchooseStartingRoot());
        assertFalse(data.unchooseStartingRoot());

        assertTrue(data.isAllocated("root_a"));
        assertEquals("root_a", data.resolveStartingRootId(List.of(root, next)));
    }

    @Test
    void thereIsNothingToUnchooseBeforeARootIsChosen() {
        ShipSkillData data = new ShipSkillData();

        assertFalse(data.canUnchooseStartingRoot());
        assertFalse(data.unchooseStartingRoot());
    }

    @Test
    void onlyARootNodeCanBeTheStartingRoot() {
        ShipSkillData data = new ShipSkillData();

        assertFalse(data.chooseStartingRoot(node("small", List.of())));
        assertNull(data.resolveStartingRootId(List.of()));
    }

    @Test
    void aShipThatNeverOpenedItsTreeHasNoStartingRoot() {
        SkillNode root = rootNode("root_a", List.of());

        assertNull(new ShipSkillData().resolveStartingRootId(List.of(root)));
    }

    @Test
    void aLegacySaveAdoptsItsFirstAllocatedRootAsTheStartingRoot() {
        SkillNode autoAllocated = rootNode("root_a", List.of());
        SkillNode reachedLater = rootNode("root_b", List.of("small"));
        SkillNode small = node("small", List.of("root_a"));
        ShipSkillData data = new ShipSkillData();
        data.allocate(autoAllocated, 0);
        data.allocate(small, 1);
        data.allocate(reachedLater, 0);

        assertEquals("root_a", data.resolveStartingRootId(List.of(reachedLater, small, autoAllocated)));
        assertFalse(data.chooseStartingRoot(reachedLater));
    }

    @Test
    void aShipIsOnlyAnNpcBuildOnceMarked() {
        ShipSkillData data = new ShipSkillData();
        assertFalse(data.isNpcBuild());

        data.markNpcBuild();

        assertTrue(data.isNpcBuild());
    }

    @Test
    void clearingTheNpcBuildMarkerMakesItAnOrdinaryTree() {
        ShipSkillData data = new ShipSkillData();
        data.markNpcBuild();

        data.clearNpcBuild();

        assertFalse(data.isNpcBuild());
    }

    @Test
    void aTreeIsBlankUntilAnythingIsRecordedOnIt() {
        assertTrue(new ShipSkillData().isBlank());

        ShipSkillData allocated = new ShipSkillData();
        allocated.allocate(node("a", List.of()), 0);
        ShipSkillData levelled = new ShipSkillData();
        levelled.incrementLevel();
        ShipSkillData experienced = new ShipSkillData();
        experienced.addXp(1f);
        ShipSkillData credited = new ShipSkillData();
        credited.addFreeAllocationCredit();

        assertFalse(allocated.isBlank());
        assertFalse(levelled.isBlank());
        assertFalse(experienced.isBlank());
        assertFalse(credited.isBlank());
    }

    @Test
    void forgettingNodesTheTreeNoLongerHasRefundsTheirFreeCreditsAndOrdnancePoints() {
        SkillNode root = rootNode("root", List.of());
        SkillNode kept = node("kept", List.of("root"));
        ShipSkillData data = new ShipSkillData();
        data.chooseStartingRoot(root);
        data.allocate(kept, 3);
        data.allocate(node("gone", List.of("root")), 3);
        data.addFreeAllocationCredit();
        data.allocate(node("free_gone", List.of("root")), 3);
        Map<String, SkillNode> tree = Map.of("root", root, "kept", kept);

        assertEquals(List.of("gone", "free_gone"), data.forgetUnknownNodes(tree, Map.of()));
        assertEquals(List.of("root", "kept"), List.copyOf(data.getAllocatedNodeIds()));
        assertEquals(1, data.getBankedFreeAllocations());
        assertEquals(3, data.getSpentOp(3));
        assertFalse(data.hasLostStartingRoot(tree));
        assertEquals(List.of(), data.forgetUnknownNodes(tree, Map.of()));
    }

    @Test
    void aShipWhoseStartingRootIsGoneOrNoLongerARootIsResetButKeepsItsProgress() {
        SkillNode root = rootNode("root", List.of());
        SkillNode child = node("child", List.of("root"));
        ShipSkillData data = new ShipSkillData();
        data.chooseStartingRoot(root);
        data.addFreeAllocationCredit();
        data.allocate(child, 3);
        data.incrementLevel();

        assertTrue(data.hasLostStartingRoot(Map.of("child", child)));
        assertTrue(data.hasLostStartingRoot(Map.of("root", node("root", List.of()), "child", child)));
        assertEquals(List.of("root", "child"), data.resetAllocations());

        assertTrue(data.getAllocatedNodeIds().isEmpty());
        assertEquals(1, data.getBankedFreeAllocations());
        assertEquals(1, data.getLevel());
        assertTrue(data.chooseStartingRoot(root));
    }

    @Test
    void anOlderTreeWithNoRecordedStartingRootIsLostOnlyOnceNoAllocatedRootRemains() {
        SkillNode root = rootNode("root", List.of());
        SkillNode child = node("child", List.of("root"));
        ShipSkillData legacy = new ShipSkillData();
        legacy.allocate(root, 0);
        legacy.allocate(child, 3);

        assertFalse(legacy.hasLostStartingRoot(Map.of("root", root, "child", child)));
        legacy.forgetUnknownNodes(Map.of("child", child), Map.of());
        assertTrue(legacy.hasLostStartingRoot(Map.of("child", child)));
        assertFalse(new ShipSkillData().hasLostStartingRoot(Map.of()));
    }

    private static SkillType plainType(String id) {
        return new SkillType.Builder(id, id, "a.png", SkillTier.SMALL).effects(List.of()).build();
    }

    private static SkillType optionalType(String id, String... options) {
        return new SkillType.Builder(id, id, "a.png", SkillTier.SMALL).effects(List.of()).optionalOptionIds(List.of(options)).build();
    }

    @Test
    void forgettingRefundsOptionalNodesWhoseChosenOptionIsNoLongerOfferedOrNoLongerExists() {
        SkillType hull = plainType("hull");
        SkillType armor = plainType("armor");
        SkillType removed = plainType("removed");
        SkillNode root = rootNode("root", List.of());
        SkillNode kept = new SkillNode("kept", optionalType("slot", "hull"), List.of("root"), 0f, 0f);
        SkillNode unlisted = new SkillNode("unlisted", optionalType("slot", "hull"), List.of("root"), 0f, 0f);
        SkillNode missingType = new SkillNode("missing_type", optionalType("other_slot", "removed"), List.of("root"), 0f, 0f);
        SkillNode plain = node("plain", List.of("root"));
        SkillNode unchosen = new SkillNode("unchosen", optionalType("slot", "hull"), List.of("root"), 0f, 0f);
        ShipSkillData data = new ShipSkillData();
        data.chooseStartingRoot(root);
        data.selectOption(kept, hull, 3);
        data.allocate(unchosen, 3);
        data.selectOption(unlisted, armor, 3);
        data.addFreeAllocationCredit();
        data.selectOption(missingType, removed, 3);
        data.allocate(plain, 3);
        data.selectOption(plain, hull, 3);
        Map<String, SkillNode> tree = Map.of("root", root, "kept", kept, "unlisted", unlisted, "missing_type", missingType,
                "plain", plain, "unchosen", unchosen);

        assertEquals(List.of("unchosen", "unlisted", "missing_type"), data.forgetUnknownNodes(tree, Map.of("hull", hull, "armor", armor)));
        assertEquals(List.of("root", "kept", "plain"), List.copyOf(data.getAllocatedNodeIds()));
        assertEquals(1, data.getBankedFreeAllocations());
        assertEquals(6, data.getSpentOp(3));
        assertEquals("hull", data.getOptionalSelection("kept"));
        assertNull(data.getOptionalSelection("plain"));
    }
}
