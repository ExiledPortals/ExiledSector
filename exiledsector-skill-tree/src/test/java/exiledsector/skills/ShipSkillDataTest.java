package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.layout.SkillNodeDecoration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static AllocationGate gate(ShipSkillData data, Collection<SkillNode> allNodes, String rootId, int totalOp, int opCost,
                                       int maxAllocatedNodes) {
        NodeEligibility.Context eligibility = NodeEligibility.Context.of(data,
                new ShipFacts(HullSize.CRUISER, ShieldType.FRONT, false, 0f, hullModId -> false), null);
        return new AllocationGate(data, SkillTreeTopology.of(allNodes), rootId, new AllocationGate.Budget(totalOp, opCost, maxAllocatedNodes),
                eligibility);
    }

    private static boolean canAllocate(ShipSkillData data, SkillNode node, String rootId, int totalOp, int opCost, int maxAllocatedNodes) {
        return gate(data, List.of(node), rootId, totalOp, opCost, maxAllocatedNodes).allocation(node).allowed();
    }

    private static void toggle(ShipSkillData data, SkillNode node, Collection<SkillNode> allNodes, String rootId, int totalOp, int opCost,
                               int maxAllocatedNodes) {
        AllocationGate gate = gate(data, allNodes, rootId, totalOp, opCost, maxAllocatedNodes);
        if (data.isAllocated(node.getId())) {
            if (gate.deallocation(node).allowed()) {
                ShipTrees.deallocate(data, node);
            }
        } else if (gate.allocation(node).allowed()) {
            ShipTrees.allocate(data, node, gate.opCostFor(node));
        }
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

        assertEquals(0, ShipTrees.getSpentOp(data, 3));
        assertTrue(data.getAllocatedNodeIds().isEmpty());
    }

    @Test
    void allocateMarksNodeAndSpendsTheGivenOpCost() {
        ShipSkillData data = new ShipSkillData();
        SkillNode node = node("armor_1", List.of());

        ShipTrees.allocate(data, node, 3);

        assertTrue(data.isAllocated("armor_1"));
        assertEquals(3, ShipTrees.getSpentOp(data, 3));
    }

    @Test
    void aRootAllocatedBeforeStartingRootsWereRecordedStillCostsNothing() {
        ShipSkillData data = new ShipSkillData();
        SkillNode root = rootNode("root_low_tech_1", List.of());
        SkillTree.register(root);

        ShipTrees.allocate(data, root, 0);
        ShipTrees.allocate(data, node("armor_1", List.of("root_low_tech_1")), 3);

        assertTrue(data.isAllocated("root_low_tech_1"));
        assertEquals(3, ShipTrees.getSpentOp(data, 3));
    }

    @Test
    void spentOpPricesEveryPaidNodeAtTheCurrentCostSoRefundsNeverDrift() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, rootNode("root_low_tech_1", List.of()));
        SkillNode a = node("a", List.of());
        SkillNode b = node("b", List.of());
        ShipTrees.allocate(data, a, 1);
        ShipTrees.allocate(data, b, 1);
        data.addFreeAllocationCredit();
        ShipTrees.allocate(data, node("c", List.of()), 1);

        assertEquals(2, ShipTrees.getSpentOp(data, 1));
        assertEquals(6, ShipTrees.getSpentOp(data, 3));

        ShipTrees.deallocate(data, a);
        assertEquals(3, ShipTrees.getSpentOp(data, 3));
        ShipTrees.deallocate(data, b);
        assertEquals(0, ShipTrees.getSpentOp(data, 3));
        assertEquals(0, ShipTrees.getSpentOp(data, 1));
    }

    @Test
    void deallocateRefundsTheOpCost() {
        ShipSkillData data = new ShipSkillData();
        SkillNode node = node("armor_1", List.of());
        ShipTrees.allocate(data, node, 3);

        ShipTrees.deallocate(data, node);

        assertFalse(data.isAllocated("armor_1"));
        assertEquals(0, ShipTrees.getSpentOp(data, 3));
    }

    @Test
    void isAllocatedIsFalseForANodeThatWasNeverAllocated() {
        ShipSkillData data = new ShipSkillData();

        assertFalse(data.isAllocated("nonexistent"));
    }

    @Test
    void canAllocateIsTrueWhenThereAreNoPrerequisites() {
        ShipSkillData data = new ShipSkillData();

        assertTrue(canAllocate(data, node("root", List.of()), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canAllocateIsFalseWhenAPrerequisiteIsNotAllocated() {
        ShipSkillData data = new ShipSkillData();

        assertFalse(canAllocate(data, node("child", List.of("parent")), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canAllocateIsTrueOnceEveryPrerequisiteIsAllocated() {
        ShipSkillData data = new ShipSkillData();
        SkillNode parent = node("parent", List.of());
        ShipTrees.allocate(data, parent, 1);

        assertTrue(canAllocate(data, node("child", List.of("parent")), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canAllocateIsFalseWithMultiplePrerequisitesWhenNoneAreAllocated() {
        ShipSkillData data = new ShipSkillData();

        assertFalse(canAllocate(data, node("child", List.of("b", "c")), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canAllocateIsTrueWithMultiplePrerequisitesWhenOnlyOneIsAllocated() {
        ShipSkillData data = new ShipSkillData();
        SkillNode b = node("b", List.of());
        ShipTrees.allocate(data, b, 1);

        assertTrue(canAllocate(data, node("a", List.of("b", "c")), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canAllocateIsTrueWithMultiplePrerequisitesWhenAllAreAllocated() {
        ShipSkillData data = new ShipSkillData();
        SkillNode b = node("b", List.of());
        SkillNode c = node("c", List.of());
        ShipTrees.allocate(data, b, 1);
        ShipTrees.allocate(data, c, 1);

        assertTrue(canAllocate(data, node("a", List.of("b", "c")), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canAllocateIsTrueWhenSpendingTheLastRemainingOpBudget() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of());
        ShipTrees.allocate(data, a, 1);

        assertTrue(canAllocate(data, node("b", List.of()), null, 2, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canAllocateIsFalseWhenTheBudgetIsExhausted() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of());
        ShipTrees.allocate(data, a, 1);

        assertFalse(canAllocate(data, node("b", List.of()), null, 1, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canAllocateAllowsAZeroCostNodeEvenWhenTheBudgetIsFullySpent() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of());
        ShipTrees.allocate(data, a, 1);

        assertTrue(canAllocate(data, rootNode("root_low_tech_1", List.of()), null, 1, 0, AMPLE_NODE_CAP));
    }

    @Test
    void canDeallocateIsTrueWhenNoAllocatedNodeDependsOnIt() {
        ShipSkillData data = new ShipSkillData();
        SkillNode parent = node("parent", List.of());
        ShipTrees.allocate(data, parent, 1);

        assertTrue(ShipTrees.canDeallocate(data, parent, List.of(parent), null));
    }

    @Test
    void canDeallocateIsFalseWhenAnAllocatedChildDependsOnIt() {
        ShipSkillData data = new ShipSkillData();
        SkillNode parent = node("parent", List.of());
        SkillNode child = node("child", List.of("parent"));
        ShipTrees.allocate(data, parent, 1);
        ShipTrees.allocate(data, child, 1);

        assertFalse(ShipTrees.canDeallocate(data, parent, List.of(parent, child), null));
    }

    @Test
    void canDeallocateIsTrueWhenAnAllocatedChildHasAnotherAllocatedPrerequisite() {
        ShipSkillData data = new ShipSkillData();
        SkillNode b = node("b", List.of());
        SkillNode c = node("c", List.of());
        SkillNode a = node("a", List.of("b", "c"));
        ShipTrees.allocate(data, b, 1);
        ShipTrees.allocate(data, c, 1);
        ShipTrees.allocate(data, a, 1);

        assertTrue(ShipTrees.canDeallocate(data, b, List.of(a, b, c), null));
    }

    @Test
    void canDeallocateIsFalseWhenItIsTheOnlyAllocatedPrerequisiteOfAnAllocatedChild() {
        ShipSkillData data = new ShipSkillData();
        SkillNode b = node("b", List.of());
        SkillNode c = node("c", List.of());
        SkillNode a = node("a", List.of("b", "c"));
        ShipTrees.allocate(data, b, 1);
        ShipTrees.allocate(data, a, 1);

        assertFalse(ShipTrees.canDeallocate(data, b, List.of(a, b, c), null));
    }

    @Test
    void toggleAllocatesAnUnallocatedNodeWhosePrerequisitesAreMet() {
        ShipSkillData data = new ShipSkillData();
        SkillNode root = node("root", List.of());

        toggle(data, root, List.of(root), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP);

        assertTrue(data.isAllocated("root"));
    }

    @Test
    void toggleDoesNothingForAnUnallocatedNodeWithAnUnmetPrerequisite() {
        ShipSkillData data = new ShipSkillData();
        SkillNode parent = node("parent", List.of());
        SkillNode child = node("child", List.of("parent"));

        toggle(data, child, List.of(parent, child), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP);

        assertFalse(data.isAllocated("child"));
    }

    @Test
    void toggleDoesNothingForAnUnallocatedNodeWhenTheBudgetIsExhausted() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of());
        SkillNode b = node("b", List.of());
        ShipTrees.allocate(data, a, 1);

        toggle(data, b, List.of(a, b), null, 1, 1, AMPLE_NODE_CAP);

        assertFalse(data.isAllocated("b"));
    }

    @Test
    void toggleDeallocatesAnAllocatedNodeWithNoAllocatedChildren() {
        ShipSkillData data = new ShipSkillData();
        SkillNode root = node("root", List.of());
        ShipTrees.allocate(data, root, 1);

        toggle(data, root, List.of(root), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP);

        assertFalse(data.isAllocated("root"));
    }

    @Test
    void toggleDoesNothingForAnAllocatedNodeWithAnAllocatedChild() {
        ShipSkillData data = new ShipSkillData();
        SkillNode parent = node("parent", List.of());
        SkillNode child = node("child", List.of("parent"));
        ShipTrees.allocate(data, parent, 1);
        ShipTrees.allocate(data, child, 1);

        toggle(data, parent, List.of(parent, child), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP);

        assertTrue(data.isAllocated("parent"));
    }

    @Test
    void isSatisfiedIsTrueForAnAllocatedNode() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.allocate(data, node("hull_1", List.of()), 1);

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

        assertTrue(canAllocate(data, child, "root_low_tech_1", AMPLE_BUDGET, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canAllocateIsFalseWhenPrerequisiteIsAnUnsatisfiedRoot() {
        ShipSkillData data = new ShipSkillData();
        SkillNode child = node("hull_1", List.of("root_midline_1"));

        assertFalse(canAllocate(data, child, "root_low_tech_1", AMPLE_BUDGET, 1, AMPLE_NODE_CAP));
    }

    @Test
    void canDeallocateTreatsTheSatisfiedRootAsAnotherAllocatedPrerequisite() {
        ShipSkillData data = new ShipSkillData();
        SkillNode b = node("b", List.of());
        SkillNode a = node("a", List.of("b", "root_low_tech_1"));
        ShipTrees.allocate(data, b, 1);
        ShipTrees.allocate(data, a, 1);

        assertTrue(ShipTrees.canDeallocate(data, b, List.of(a, b), "root_low_tech_1"));
    }

    @Test
    void canDeallocateIsFalseWhenTheOnlyAlternatePathIsItselfNotConnectedToTheRoot() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of());
        SkillNode b = node("b", List.of("a", "x"));
        SkillNode x = node("x", List.of("b"));
        ShipTrees.allocate(data, a, 1);
        ShipTrees.allocate(data, b, 1);
        ShipTrees.allocate(data, x, 1);

        assertFalse(ShipTrees.canDeallocate(data, a, List.of(a, b, x), null));
    }

    @Test
    void canDeallocateIsTrueWhenTheAlternatePathTracesBackToTheRoot() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a1 = node("a1", List.of());
        SkillNode a2 = node("a2", List.of());
        SkillNode b = node("b", List.of("a1", "x"));
        SkillNode x = node("x", List.of("b", "a2"));
        ShipTrees.allocate(data, a1, 1);
        ShipTrees.allocate(data, a2, 1);
        ShipTrees.allocate(data, b, 1);
        ShipTrees.allocate(data, x, 1);

        assertTrue(ShipTrees.canDeallocate(data, a1, List.of(a1, a2, b, x), null));
    }

    @Test
    void canDeallocateIsTrueForAMemberOfASymmetricCircularLoopWhenTheRestStillReachTheRoot() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of("root_low_tech_1", "b", "c"));
        SkillNode b = node("b", List.of("a", "c"));
        SkillNode c = node("c", List.of("a", "b"));
        ShipTrees.allocate(data, a, 1);
        ShipTrees.allocate(data, b, 1);
        ShipTrees.allocate(data, c, 1);

        assertTrue(ShipTrees.canDeallocate(data, b, List.of(a, b, c), "root_low_tech_1"));
    }

    @Test
    void canDeallocateIsFalseForTheSoleBridgeOfASymmetricCircularLoop() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of("root_low_tech_1", "b", "c"));
        SkillNode b = node("b", List.of("a", "c"));
        SkillNode c = node("c", List.of("a", "b"));
        ShipTrees.allocate(data, a, 1);
        ShipTrees.allocate(data, b, 1);
        ShipTrees.allocate(data, c, 1);

        assertFalse(ShipTrees.canDeallocate(data, a, List.of(a, b, c), "root_low_tech_1"));
    }

    @Test
    void canDeallocateIsUnaffectedByAnUnrelatedNodeThatWasAlreadyStrandedBeforeThisRemoval() {
        ShipSkillData data = new ShipSkillData();
        SkillNode leaf = node("leaf", List.of("root_low_tech_1"));
        SkillNode orphan = node("orphan", List.of("nothingThatExists"));
        ShipTrees.allocate(data, leaf, 1);
        ShipTrees.allocate(data, orphan, 1);

        assertTrue(ShipTrees.canDeallocate(data, leaf, List.of(leaf, orphan), "root_low_tech_1"));
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
            ShipTrees.allocate(data, n, 1);
        }

        for (int i = 1; i < ringSize; i++) {
            assertTrue(ShipTrees.canDeallocate(data, ring.get(i), ring, "root_low_tech_1"),
                    "expected ring" + i + " to be deallocatable");
        }
        assertFalse(ShipTrees.canDeallocate(data, ring.get(0), ring, "root_low_tech_1"),
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
        ShipTrees.allocate(data, bridge, 1);
        ShipTrees.allocate(data, secondRoot, 1);
        ShipTrees.allocate(data, descendant, 1);

        assertFalse(ShipTrees.canDeallocate(data, bridge, List.of(bridge, secondRoot, descendant), "root_low_tech_1"));
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
        ShipTrees.allocate(data, bridgeA, 1);
        ShipTrees.allocate(data, bridgeB, 1);
        ShipTrees.allocate(data, secondRoot, 1);

        assertTrue(ShipTrees.canDeallocate(data, bridgeA, List.of(bridgeA, bridgeB, secondRoot), "root_low_tech_1"));
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
            assertTrue(canAllocate(data, n, satisfiedRootId, AMPLE_BUDGET, 1, AMPLE_NODE_CAP), "expected to be able to allocate " + n.getId());
            ShipTrees.allocate(data, n, 1);
        }

        for (SkillNode n : allNodes) {
            assertTrue(ShipTrees.canDeallocate(data, n, allNodes, satisfiedRootId),
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
            ShipTrees.allocate(data, n, 1);
        }

        for (SkillNode n : ring) {
            if (n == bridge) continue;
            assertTrue(ShipTrees.canDeallocate(data, n, ring, "small_flux_optional_25"),
                    "expected " + n.getId() + " to be deallocatable");
        }
        assertFalse(ShipTrees.canDeallocate(data, bridge, ring, "small_flux_optional_25"),
                "shield_turn_raise_rate_optional_1 is the ring's sole bridge and must stay blocked while the rest of the ring is allocated");
    }

    @Test
    void getOptionalSelectionIsNullWhenNothingHasBeenSelected() {
        ShipSkillData data = new ShipSkillData();

        assertNull(data.getOptionalSelection("slot_1"));
    }

    @Test
    void aPlanningCopyStartsFromTheAllocationsButChangesNeverReachTheShip() {
        ShipSkillData data = new ShipSkillData();
        SkillNode root = rootNode("root", List.of());
        SkillNode slot = node("slot_1", List.of("root"));
        ShipTrees.chooseStartingRoot(data, root);
        data.addFreeAllocationCredit();
        ShipTrees.selectOption(data, slot, new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL).effects(List.of()).build(), 1);
        data.addFreeAllocationCredit();

        ShipSkillData plan = ShipTrees.planningCopy(data);
        ShipTrees.allocate(plan, node("armor_1", List.of("slot_1")), 0);

        assertEquals("root", ShipTrees.resolveStartingRootId(plan));
        assertEquals("hull", plan.getOptionalSelection("slot_1"));
        assertEquals(Set.of("root", "slot_1", "armor_1"), plan.getAllocatedNodeIds());
        assertEquals(0, plan.getBankedFreeAllocations());
        assertFalse(data.isAllocated("armor_1"));
        assertEquals(1, data.getBankedFreeAllocations());
    }

    @Test
    void selectOptionAllocatesTheSlotNodeAndSpendsTheGivenOpCost() {
        ShipSkillData data = new ShipSkillData();
        SkillNode slot = node("slot_1", List.of());
        SkillType chosenOption = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL)
                .effects(List.of())
                .build();

        ShipTrees.selectOption(data, slot, chosenOption, 1);

        assertTrue(data.isAllocated("slot_1"));
        assertEquals("hull", data.getOptionalSelection("slot_1"));
        assertEquals(1, ShipTrees.getSpentOp(data, 1));
    }

    @Test
    void selectOptionConsumesABankedFreeAllocationInsteadOfSpendingOp() {
        ShipSkillData data = new ShipSkillData();
        data.addFreeAllocationCredit();
        SkillNode slot = node("slot_1", List.of());
        SkillType chosenOption = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL)
                .effects(List.of())
                .build();

        ShipTrees.selectOption(data, slot, chosenOption, 1);

        assertTrue(data.isAllocated("slot_1"));
        assertTrue(data.isFreeNode("slot_1"));
        assertEquals(0, data.getBankedFreeAllocations());
        assertEquals(0, ShipTrees.getSpentOp(data, 1));
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
        ShipTrees.selectOption(data, slot, chosenOption, 1);

        ShipTrees.deallocate(data, slot);

        assertFalse(data.isAllocated("slot_1"));
        assertEquals(1, data.getBankedFreeAllocations());
        assertEquals(0, ShipTrees.getSpentOp(data, 1));
    }

    @Test
    void deallocateRefundsTheSlotsOpCostRegardlessOfSelectedOption() {
        ShipSkillData data = new ShipSkillData();
        SkillNode slot = node("slot_1", List.of());
        SkillType chosenOption = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL)
                .effects(List.of())
                .build();
        SkillTree.registerType(chosenOption);
        ShipTrees.selectOption(data, slot, chosenOption, 1);

        ShipTrees.deallocate(data, slot);

        assertFalse(data.isAllocated("slot_1"));
        assertNull(data.getOptionalSelection("slot_1"));
        assertEquals(0, ShipTrees.getSpentOp(data, 1));
    }

    @Test
    void togglingASelectedOptionalNodeOffClearsTheSelection() {
        ShipSkillData data = new ShipSkillData();
        SkillNode slot = node("slot_1", List.of());
        SkillType chosenOption = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL)
                .effects(List.of())
                .build();
        SkillTree.registerType(chosenOption);
        ShipTrees.selectOption(data, slot, chosenOption, 1);

        toggle(data, slot, List.of(slot), null, AMPLE_BUDGET, 1, AMPLE_NODE_CAP);

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
        ShipTrees.selectOption(data, slot, hullOption, 1);

        ShipTrees.selectOption(data, slot, armorOption, 1);

        assertTrue(data.isAllocated("slot_1"));
        assertEquals("armor", data.getOptionalSelection("slot_1"));
        assertEquals(1, ShipTrees.getSpentOp(data, 1));
    }

    @Test
    void reselectingTheSameOptionOnAnAlreadyAllocatedNodeDoesNotDoubleChargeIt() {
        ShipSkillData data = new ShipSkillData();
        SkillNode slot = node("slot_1", List.of());
        SkillType hullOption = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL)
                .effects(List.of())
                .build();
        SkillTree.registerType(hullOption);
        ShipTrees.selectOption(data, slot, hullOption, 1);

        ShipTrees.selectOption(data, slot, hullOption, 1);

        assertEquals(1, ShipTrees.getSpentOp(data, 1));
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
        ShipTrees.allocate(data, a, 3);
        ShipTrees.allocate(data, b, 3);

        boolean converted = ShipTrees.convertMostRecentAllocationToFree(data, List.of(a, b));

        assertTrue(converted);
        assertTrue(data.isFreeNode("b"));
        assertFalse(data.isFreeNode("a"));
        assertEquals(3, ShipTrees.getSpentOp(data, 3));
    }

    @Test
    void convertMostRecentAllocationToFreeSkipsTheStartingRoot() {
        ShipSkillData data = new ShipSkillData();
        SkillNode root = rootNode("root_low_tech_1", List.of());
        SkillNode a = node("a", List.of("root_low_tech_1"));
        ShipTrees.allocate(data, a, 3);
        ShipTrees.chooseStartingRoot(data, root);

        boolean converted = ShipTrees.convertMostRecentAllocationToFree(data, List.of(a, root));

        assertTrue(converted);
        assertTrue(data.isFreeNode("a"));
        assertFalse(data.isFreeNode("root_low_tech_1"));
        assertEquals(0, ShipTrees.getSpentOp(data, 3));
    }

    @Test
    void convertMostRecentAllocationToFreeRefundsAPaidForNonStartingRoot() {
        ShipSkillData data = new ShipSkillData();
        SkillNode startingRoot = rootNode("root_low_tech_1", List.of());
        SkillNode otherRoot = rootNode("root_high_tech_1", List.of());
        ShipTrees.chooseStartingRoot(data, startingRoot);
        ShipTrees.allocate(data, otherRoot, 3);

        boolean converted = ShipTrees.convertMostRecentAllocationToFree(data, List.of(startingRoot, otherRoot));

        assertTrue(converted);
        assertTrue(data.isFreeNode("root_high_tech_1"));
        assertEquals(0, ShipTrees.getSpentOp(data, 3));
    }

    @Test
    void convertMostRecentAllocationToFreeSkipsNodesAlreadyFree() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of());
        SkillNode b = node("b", List.of());
        ShipTrees.allocate(data, a, 3);
        ShipTrees.allocate(data, b, 3);
        ShipTrees.convertMostRecentAllocationToFree(data, List.of(a, b));

        boolean convertedAgain = ShipTrees.convertMostRecentAllocationToFree(data, List.of(a, b));

        assertTrue(convertedAgain);
        assertTrue(data.isFreeNode("a"));
        assertEquals(0, ShipTrees.getSpentOp(data, 3));
    }

    @Test
    void convertMostRecentAllocationToFreeSkipsTheAutoGrantedWormholeCompanion() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");
        SkillNode b = wormholeNode("wormhole_b", List.of(), "wormhole_a");
        ShipTrees.allocate(data, a, 3);

        boolean converted = ShipTrees.convertMostRecentAllocationToFree(data, List.of(a, b));

        assertTrue(converted);
        assertTrue(data.isFreeNode("wormhole_a"));
        assertEquals(0, ShipTrees.getSpentOp(data, 3));

        ShipTrees.deallocate(data, a);

        assertEquals(0, ShipTrees.getSpentOp(data, 3));
    }

    @Test
    void convertMostRecentAllocationToFreeReturnsFalseWhenNothingIsEligible() {
        ShipSkillData data = new ShipSkillData();
        SkillNode root = rootNode("root_low_tech_1", List.of());
        ShipTrees.allocate(data, root, 0);

        assertFalse(ShipTrees.convertMostRecentAllocationToFree(data, List.of(root)));
    }

    @Test
    void allocateConsumesABankedFreeAllocationInsteadOfSpendingOp() {
        ShipSkillData data = new ShipSkillData();
        data.addFreeAllocationCredit();
        SkillNode a = node("a", List.of());

        ShipTrees.allocate(data, a, 3);

        assertTrue(data.isFreeNode("a"));
        assertEquals(0, data.getBankedFreeAllocations());
        assertEquals(0, ShipTrees.getSpentOp(data, 3));
    }

    @Test
    void allocateDoesNotConsumeABankedCreditForAZeroCostNode() {
        ShipSkillData data = new ShipSkillData();
        data.addFreeAllocationCredit();
        SkillNode root = rootNode("root_low_tech_1", List.of());

        ShipTrees.allocate(data, root, 0);

        assertFalse(data.isFreeNode("root_low_tech_1"));
        assertEquals(1, data.getBankedFreeAllocations());
    }

    @Test
    void deallocatingAFreeNodeRefundsTheBankedCreditInsteadOfOp() {
        ShipSkillData data = new ShipSkillData();
        data.addFreeAllocationCredit();
        SkillNode a = node("a", List.of());
        ShipTrees.allocate(data, a, 3);

        ShipTrees.deallocate(data, a);

        assertFalse(data.isAllocated("a"));
        assertEquals(1, data.getBankedFreeAllocations());
        assertEquals(0, ShipTrees.getSpentOp(data, 3));
    }

    @Test
    void canAllocateIsFalseOnceTheNodeCapIsReached() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of());
        ShipTrees.allocate(data, a, 1);

        assertFalse(canAllocate(data, node("b", List.of()), null, AMPLE_BUDGET, 1, 1));
    }

    @Test
    void canAllocateIsTrueBelowTheNodeCap() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a", List.of());
        ShipTrees.allocate(data, a, 1);

        assertTrue(canAllocate(data, node("b", List.of()), null, AMPLE_BUDGET, 1, 2));
    }

    @Test
    void allocatingAWormholeAlsoAllocatesItsPairForFree() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");

        ShipTrees.allocate(data, a, 3);

        assertTrue(data.isAllocated("wormhole_a"));
        assertTrue(data.isAllocated("wormhole_b"));
        assertTrue(data.isFreeNode("wormhole_b"));
        assertFalse(data.isFreeNode("wormhole_a"));
        assertEquals(3, ShipTrees.getSpentOp(data, 3));
    }

    @Test
    void allocatingAWormholeDoesNotDoubleAllocateAnAlreadyAllocatedPair() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");
        SkillNode b = wormholeNode("wormhole_b", List.of(), "wormhole_a");
        ShipTrees.allocate(data, b, 3);

        ShipTrees.allocate(data, a, 3);

        assertEquals(2, data.getAllocatedNodeIds().size());
        assertEquals(3, ShipTrees.getSpentOp(data, 3));
    }

    @Test
    void deallocatingAWormholeAlsoDeallocatesItsPair() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");
        ShipTrees.allocate(data, a, 3);

        ShipTrees.deallocate(data, a);

        assertFalse(data.isAllocated("wormhole_a"));
        assertFalse(data.isAllocated("wormhole_b"));
        assertEquals(0, ShipTrees.getSpentOp(data, 3));
        assertEquals(0, data.getBankedFreeAllocations());
    }

    @Test
    void deallocatingEitherEndOfAWormholePairDeallocatesBoth() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");
        SkillNode b = wormholeNode("wormhole_b", List.of(), "wormhole_a");
        ShipTrees.allocate(data, a, 3);

        ShipTrees.deallocate(data, b);

        assertFalse(data.isAllocated("wormhole_a"));
        assertFalse(data.isAllocated("wormhole_b"));
        assertEquals(0, ShipTrees.getSpentOp(data, 3));
        assertEquals(0, data.getBankedFreeAllocations());
    }

    @Test
    void repeatedlyAllocatingAndDeallocatingAWormholePairNeverMintsFreeAllocationCredits() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");

        for (int i = 0; i < 3; i++) {
            ShipTrees.allocate(data, a, 3);
            ShipTrees.deallocate(data, a);

            assertEquals(0, ShipTrees.getSpentOp(data, 3));
            assertEquals(0, data.getBankedFreeAllocations());
        }
    }

    @Test
    void nonWormholeAllocationIsUnaffectedByPairing() {
        ShipSkillData data = new ShipSkillData();
        SkillNode plain = node("armor_1", List.of());

        ShipTrees.allocate(data, plain, 3);

        assertEquals(1, data.getAllocatedNodeIds().size());
        assertEquals(3, ShipTrees.getSpentOp(data, 3));
    }

    @Test
    void canAllocateRequiresRoomForBothEndsOfAnUnallocatedPair() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");

        assertFalse(canAllocate(data, a, null, AMPLE_BUDGET, 1, 1));
        assertTrue(canAllocate(data, a, null, AMPLE_BUDGET, 1, 2));
    }

    @Test
    void canAllocateOnlyNeedsOneSlotWhenThePairIsAlreadyIndependentlyAllocated() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");
        data.getAllocatedNodeIds().add("wormhole_b");

        assertFalse(canAllocate(data, a, null, AMPLE_BUDGET, 1, 1));
        assertTrue(canAllocate(data, a, null, AMPLE_BUDGET, 1, 2));
    }

    @Test
    void canDeallocateIsFalseWhenAChildOnlyReachesTheRootThroughTheOtherEndOfTheWormholePair() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");
        SkillNode b = wormholeNode("wormhole_b", List.of(), "wormhole_a");
        SkillNode child = node("child", List.of("wormhole_b"));
        ShipTrees.allocate(data, a, 1);
        ShipTrees.allocate(data, child, 1);

        assertFalse(ShipTrees.canDeallocate(data, a, List.of(a, b, child), null));
        assertFalse(ShipTrees.canDeallocate(data, b, List.of(a, b, child), null));
    }

    @Test
    void canDeallocateIsTrueWhenNothingDependsOnEitherEndOfTheWormholePair() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = wormholeNode("wormhole_a", List.of(), "wormhole_b");
        SkillNode b = wormholeNode("wormhole_b", List.of(), "wormhole_a");
        ShipTrees.allocate(data, a, 1);

        assertTrue(ShipTrees.canDeallocate(data, a, List.of(a, b), null));
    }

    @Test
    void choosingAStartingRootAllocatesItForFree() {
        SkillNode root = rootNode("root_a", List.of());
        ShipSkillData data = new ShipSkillData();

        assertTrue(ShipTrees.chooseStartingRoot(data, root));

        assertTrue(data.isAllocated("root_a"));
        assertEquals(0, ShipTrees.getSpentOp(data, 3));
        assertEquals("root_a", ShipTrees.resolveStartingRootId(data, List.of(root)));
    }

    @Test
    void theStartingRootCannotBeChosenTwice() {
        SkillNode first = rootNode("root_a", List.of());
        SkillNode second = rootNode("root_b", List.of());
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, first);

        assertFalse(ShipTrees.chooseStartingRoot(data, second));

        assertFalse(data.isAllocated("root_b"));
        assertEquals("root_a", ShipTrees.resolveStartingRootId(data, List.of(first, second)));
    }

    @Test
    void theStartingRootCanBeUnchosenWhileItIsTheOnlyAllocatedNodeAndThenChosenAgain() {
        SkillNode first = rootNode("root_a", List.of());
        SkillNode second = rootNode("root_b", List.of());
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, first);

        assertTrue(data.canUnchooseStartingRoot());
        assertTrue(ShipTrees.unchooseStartingRoot(data));

        assertFalse(data.isAllocated("root_a"));
        assertNull(ShipTrees.resolveStartingRootId(data, List.of(first, second)));
        assertTrue(ShipTrees.chooseStartingRoot(data, second));
        assertEquals("root_b", ShipTrees.resolveStartingRootId(data, List.of(first, second)));
    }

    @Test
    void theStartingRootIsLockedInOnceAnyOtherNodeIsAllocated() {
        SkillNode root = rootNode("root_a", List.of());
        SkillNode next = node("small", List.of("root_a"));
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, next, 3);

        assertFalse(data.canUnchooseStartingRoot());
        assertFalse(ShipTrees.unchooseStartingRoot(data));

        assertTrue(data.isAllocated("root_a"));
        assertEquals("root_a", ShipTrees.resolveStartingRootId(data, List.of(root, next)));
    }

    @Test
    void thereIsNothingToUnchooseBeforeARootIsChosen() {
        ShipSkillData data = new ShipSkillData();

        assertFalse(data.canUnchooseStartingRoot());
        assertFalse(ShipTrees.unchooseStartingRoot(data));
    }

    @Test
    void onlyARootNodeCanBeTheStartingRoot() {
        ShipSkillData data = new ShipSkillData();

        assertFalse(ShipTrees.chooseStartingRoot(data, node("small", List.of())));
        assertNull(ShipTrees.resolveStartingRootId(data, List.of()));
    }

    @Test
    void aShipThatNeverOpenedItsTreeHasNoStartingRoot() {
        SkillNode root = rootNode("root_a", List.of());

        assertNull(ShipTrees.resolveStartingRootId(new ShipSkillData(), List.of(root)));
    }

    @Test
    void aLegacySaveAdoptsItsFirstAllocatedRootAsTheStartingRoot() {
        SkillNode autoAllocated = rootNode("root_a", List.of());
        SkillNode reachedLater = rootNode("root_b", List.of("small"));
        SkillNode small = node("small", List.of("root_a"));
        ShipSkillData data = new ShipSkillData();
        ShipTrees.allocate(data, autoAllocated, 0);
        ShipTrees.allocate(data, small, 1);
        ShipTrees.allocate(data, reachedLater, 0);

        assertEquals("root_a", ShipTrees.resolveStartingRootId(data, List.of(reachedLater, small, autoAllocated)));
        assertFalse(ShipTrees.chooseStartingRoot(data, reachedLater));
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
        ShipTrees.allocate(allocated, node("a", List.of()), 0);
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
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, kept, 3);
        ShipTrees.allocate(data, node("gone", List.of("root")), 3);
        data.addFreeAllocationCredit();
        ShipTrees.allocate(data, node("free_gone", List.of("root")), 3);
        Map<String, SkillNode> tree = Map.of("root", root, "kept", kept);

        assertEquals(List.of("gone", "free_gone"), ShipTrees.forgetUnknownNodes(data, tree, Map.of()));
        assertEquals(List.of("root", "kept"), List.copyOf(data.getAllocatedNodeIds()));
        assertEquals(1, data.getBankedFreeAllocations());
        assertEquals(3, ShipTrees.getSpentOp(data, 3));
        assertFalse(ShipTrees.hasLostStartingRoot(data, tree));
        assertEquals(List.of(), ShipTrees.forgetUnknownNodes(data, tree, Map.of()));
    }

    @Test
    void nodesInAHiddenAreaAreKeptDormantAndTakeNoOrdnancePointsOrSlots() {
        SkillNode root = rootNode("root", List.of());
        SkillNode bridge = node("bridge", List.of("root"));
        SkillNode hidden = node("hidden", List.of("bridge"));
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, bridge, 3);
        ShipTrees.allocate(data, hidden, 3);
        Map<String, SkillNode> declared = Map.of("root", root, "bridge", bridge, "hidden", hidden);
        Map<String, SkillNode> shown = Map.of("root", root, "bridge", bridge);

        assertEquals(List.of(), ShipTrees.forgetUnknownNodes(data, shown, declared, Map.of()));

        assertEquals(List.of("root", "bridge"), List.copyOf(data.getAllocatedNodeIds()));
        assertEquals(Set.of("hidden"), data.getDormantNodeIds());
        assertEquals(3, ShipTrees.getSpentOp(data, 3));
        assertEquals(List.of(), ShipTrees.wakeDormantNodes(data, shown, 10, dormantNode -> true));
        assertEquals(Set.of("hidden"), data.getDormantNodeIds());

        assertEquals(List.of(), ShipTrees.wakeDormantNodes(data, declared, 10, dormantNode -> true));
        assertTrue(data.isAllocated("hidden"));
        assertTrue(data.getDormantNodeIds().isEmpty());
        assertEquals(6, ShipTrees.getSpentOp(data, 3));
    }

    @Test
    void dormantNodesThatNoLongerConnectOrFitAreReleasedWithTheirFreeCredits() {
        SkillNode root = rootNode("root", List.of());
        SkillNode bridge = node("bridge", List.of("root"));
        SkillNode first = node("first", List.of("bridge"));
        SkillNode second = node("second", List.of("first"));
        SkillNode filler = node("filler", List.of("root"));
        Map<String, SkillNode> declared = Map.of("root", root, "bridge", bridge, "first", first, "second", second, "filler", filler);

        ShipSkillData stranded = new ShipSkillData();
        ShipTrees.chooseStartingRoot(stranded, root);
        ShipTrees.allocate(stranded, bridge, 3);
        stranded.addFreeAllocationCredit();
        ShipTrees.allocate(stranded, first, 3);
        ShipTrees.forgetUnknownNodes(stranded, Map.of("root", root, "bridge", bridge), declared, Map.of());
        ShipTrees.deallocate(stranded, bridge);
        assertEquals(List.of("first"), ShipTrees.wakeDormantNodes(stranded, declared, 10, dormantNode -> true));
        assertFalse(stranded.isAllocated("first"));
        assertEquals(1, stranded.getBankedFreeAllocations());

        ShipSkillData capped = new ShipSkillData();
        ShipTrees.chooseStartingRoot(capped, root);
        ShipTrees.allocate(capped, bridge, 3);
        ShipTrees.allocate(capped, first, 3);
        ShipTrees.allocate(capped, second, 3);
        ShipTrees.forgetUnknownNodes(capped, Map.of("root", root, "bridge", bridge, "filler", filler), declared, Map.of());
        ShipTrees.allocate(capped, filler, 3);
        assertEquals(List.of("second"), ShipTrees.wakeDormantNodes(capped, declared, 4, dormantNode -> true));
        assertEquals(List.of("root", "bridge", "filler", "first"), List.copyOf(capped.getAllocatedNodeIds()));
    }

    @Test
    void aDormantWormholePartnerReturnsWhileItsSlotIsStillFree() {
        SkillNode root = rootNode("root", List.of());
        SkillNode entrance = wormholeNode("entrance", List.of("root"), "exit");
        SkillNode exit = wormholeNode("exit", List.of("entrance"), "entrance");
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, entrance, 3);
        assertTrue(data.isAllocated("exit"));
        Map<String, SkillNode> declared = Map.of("root", root, "entrance", entrance, "exit", exit);
        ShipTrees.forgetUnknownNodes(data, Map.of("root", root, "entrance", entrance), declared, Map.of());

        assertEquals(List.of(), ShipTrees.wakeDormantNodes(data, declared, 3, dormantNode -> true));
        assertTrue(data.isAllocated("exit"));
        assertEquals(3, ShipTrees.getSpentOp(data, 3));
    }

    @Test
    void aWormholePairWhoseFreeEndCannotReturnIsReleasedWholeInsteadOfGoingOverTheCap() {
        SkillNode root = rootNode("root", List.of());
        SkillNode entrance = wormholeNode("entrance", List.of("root"), "exit");
        SkillNode exit = wormholeNode("exit", List.of("entrance"), "entrance");
        SkillNode filler = node("filler", List.of("root"));
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, entrance, 3);
        Map<String, SkillNode> declared = Map.of("root", root, "entrance", entrance, "exit", exit, "filler", filler);
        ShipTrees.forgetUnknownNodes(data, Map.of("root", root, "entrance", entrance, "filler", filler), declared, Map.of());
        ShipTrees.allocate(data, filler, 3);

        assertEquals(List.of("exit", "entrance"), ShipTrees.wakeDormantNodes(data, declared, 3, dormantNode -> true));
        assertEquals(List.of("root", "filler"), List.copyOf(data.getAllocatedNodeIds()));
        assertEquals(3, ShipTrees.getSpentOp(data, 3));
    }

    @Test
    void aDormantNodeTheWakeCheckRefusesIsReleasedAndItsFreeCreditReturned() {
        SkillNode root = rootNode("root", List.of());
        SkillNode bridge = node("bridge", List.of("root"));
        SkillNode refused = node("refused", List.of("bridge"));
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, bridge, 3);
        data.addFreeAllocationCredit();
        ShipTrees.allocate(data, refused, 3);
        Map<String, SkillNode> declared = Map.of("root", root, "bridge", bridge, "refused", refused);
        ShipTrees.forgetUnknownNodes(data, Map.of("root", root, "bridge", bridge), declared, Map.of());

        assertEquals(List.of("refused"), ShipTrees.wakeDormantNodes(data, declared, 10, dormantNode -> false));
        assertFalse(data.isAllocated("refused"));
        assertEquals(1, data.getBankedFreeAllocations());
    }

    @Test
    void aPaidDormantNodeWakesOnABankedFreeAllocationWhenThereIsOne() {
        SkillNode root = rootNode("root", List.of());
        SkillNode bridge = node("bridge", List.of("root"));
        SkillNode hidden = node("hidden", List.of("bridge"));
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, bridge, 3);
        ShipTrees.allocate(data, hidden, 3);
        Map<String, SkillNode> declared = Map.of("root", root, "bridge", bridge, "hidden", hidden);
        ShipTrees.forgetUnknownNodes(data, Map.of("root", root, "bridge", bridge), declared, Map.of());
        data.addFreeAllocationCredit();

        assertEquals(List.of(), ShipTrees.wakeDormantNodes(data, declared, 10, dormantNode -> true));
        assertTrue(data.isFreeNode("hidden"));
        assertEquals(0, data.getBankedFreeAllocations());
        assertEquals(3, ShipTrees.getSpentOp(data, 3));
    }

    @Test
    void resettingOrRemovingTheTreeAlsoReleasesDormantNodes() {
        SkillNode root = rootNode("root", List.of());
        SkillNode hidden = node("hidden", List.of("root"));
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        data.addFreeAllocationCredit();
        ShipTrees.allocate(data, hidden, 3);
        ShipTrees.forgetUnknownNodes(data, Map.of("root", root), Map.of("root", root, "hidden", hidden), Map.of());

        assertFalse(data.isBlank());
        assertEquals(List.of("root", "hidden"), ShipTrees.resetAllocations(data));
        assertTrue(data.getDormantNodeIds().isEmpty());
        assertEquals(1, data.getBankedFreeAllocations());

        ShipSkillData removed = new ShipSkillData();
        ShipTrees.chooseStartingRoot(removed, root);
        ShipTrees.allocate(removed, hidden, 3);
        ShipTrees.forgetUnknownNodes(removed, Map.of("root", root), Map.of("root", root, "hidden", hidden), Map.of());
        assertEquals(List.of("hidden"), ShipTrees.forgetUnknownNodes(removed, Map.of("root", root), Map.of("root", root), Map.of()));
        assertTrue(removed.getDormantNodeIds().isEmpty());
    }

    @Test
    void aReplacedNodeIsRenamedWhileDormant() {
        SkillNode root = rootNode("root", List.of());
        SkillNode hidden = node("hidden", List.of("root"));
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, hidden, 3);
        ShipTrees.forgetUnknownNodes(data, Map.of("root", root), Map.of("root", root, "hidden", hidden), Map.of());

        assertTrue(ShipTrees.replaceNode(data, "hidden", "renamed"));
        assertEquals(Set.of("renamed"), data.getDormantNodeIds());
    }

    @Test
    void aReplacedStartingRootStaysTheStartingRootSoTheTreeIsNotReset() {
        SkillNode oldRoot = rootNode("old_root", List.of());
        SkillNode newRoot = rootNode("new_root", List.of());
        SkillNode child = node("child", List.of("new_root"));
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, oldRoot);
        ShipTrees.allocate(data, child, 3);

        assertTrue(ShipTrees.replaceNode(data, "old_root", "new_root"));
        Map<String, SkillNode> tree = Map.of("new_root", newRoot, "child", child);
        assertTrue(ShipTrees.forgetUnknownNodes(data, tree, Map.of()).isEmpty());

        assertFalse(ShipTrees.hasLostStartingRoot(data, tree));
        assertEquals("new_root", ShipTrees.resolveStartingRootId(data));
        assertEquals(Set.of("new_root", "child"), data.getAllocatedNodeIds());
    }

    @Test
    void aShipWhoseStartingRootIsGoneOrNoLongerARootIsResetButKeepsItsProgress() {
        SkillNode root = rootNode("root", List.of());
        SkillNode child = node("child", List.of("root"));
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        data.addFreeAllocationCredit();
        ShipTrees.allocate(data, child, 3);
        data.incrementLevel();

        assertTrue(ShipTrees.hasLostStartingRoot(data, Map.of("child", child)));
        assertTrue(ShipTrees.hasLostStartingRoot(data, Map.of("root", node("root", List.of()), "child", child)));
        assertEquals(List.of("root", "child"), ShipTrees.resetAllocations(data));

        assertTrue(data.getAllocatedNodeIds().isEmpty());
        assertEquals(1, data.getBankedFreeAllocations());
        assertEquals(1, data.getLevel());
        assertTrue(ShipTrees.chooseStartingRoot(data, root));
    }

    @Test
    void anOlderTreeWithNoRecordedStartingRootIsLostOnlyOnceNoAllocatedRootRemains() {
        SkillNode root = rootNode("root", List.of());
        SkillNode child = node("child", List.of("root"));
        ShipSkillData legacy = new ShipSkillData();
        ShipTrees.allocate(legacy, root, 0);
        ShipTrees.allocate(legacy, child, 3);

        assertFalse(ShipTrees.hasLostStartingRoot(legacy, Map.of("root", root, "child", child)));
        ShipTrees.forgetUnknownNodes(legacy, Map.of("child", child), Map.of());
        assertTrue(ShipTrees.hasLostStartingRoot(legacy, Map.of("child", child)));
        assertFalse(ShipTrees.hasLostStartingRoot(new ShipSkillData(), Map.of()));
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
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.selectOption(data, kept, hull, 3);
        ShipTrees.allocate(data, unchosen, 3);
        ShipTrees.selectOption(data, unlisted, armor, 3);
        data.addFreeAllocationCredit();
        ShipTrees.selectOption(data, missingType, removed, 3);
        ShipTrees.allocate(data, plain, 3);
        ShipTrees.selectOption(data, plain, hull, 3);
        Map<String, SkillNode> tree = Map.of("root", root, "kept", kept, "unlisted", unlisted, "missing_type", missingType,
                "plain", plain, "unchosen", unchosen);

        assertEquals(List.of("unchosen", "unlisted", "missing_type"), ShipTrees.forgetUnknownNodes(data, tree, Map.of("hull", hull, "armor", armor)));
        assertEquals(List.of("root", "kept", "plain"), List.copyOf(data.getAllocatedNodeIds()));
        assertEquals(1, data.getBankedFreeAllocations());
        assertEquals(6, ShipTrees.getSpentOp(data, 3));
        assertEquals("hull", data.getOptionalSelection("kept"));
        assertNull(data.getOptionalSelection("plain"));
    }
}
