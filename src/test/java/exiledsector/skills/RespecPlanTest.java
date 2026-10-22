package exiledsector.skills;

import exiledsector.skills.layout.SkillNodeDecoration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RespecPlanTest {

    private static SkillNode node(String id, String... prerequisiteIds) {
        SkillType type = new SkillType.Builder(id, id, "a.png", SkillTier.SMALL).effects(List.of()).build();
        return new SkillNode(id, type, List.of(prerequisiteIds), 0f, 0f);
    }

    private static SkillNode root(String id) {
        SkillType type = new SkillType.Builder(id, id, "a.png", SkillTier.ROOT).effects(List.of()).build();
        return new SkillNode(id, type, List.of(), 0f, 0f);
    }

    private static SkillNode wormhole(String id, String pairedNodeId, String... prerequisiteIds) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", SkillTier.WORMHOLE).effects(List.of()).build();
        return new SkillNode(id, type, List.of(prerequisiteIds), 0f, 0f, new SkillNodeDecoration(null, null, null, null, pairedNodeId));
    }

    private static List<String> ids(List<SkillNode> nodes) {
        return nodes.stream().map(SkillNode::getId).toList();
    }

    private static void assertEachStepIsAllowed(ShipSkillData data, SkillTreeTopology topology, List<SkillNode> plan) {
        for (SkillNode step : plan.subList(0, plan.size() - 1)) {
            if (!data.isAllocated(step.getId())) {
                continue;
            }
            assertTrue(ShipTrees.canDeallocate(data, step, topology, "root"), "removing " + step.getId() + " would strand other nodes");
            ShipTrees.deallocate(data, step);
        }
    }

    @Test
    void theSelectedNodesDependentsAreRemovedFarthestFirstAndTheSelectedNodeLast() {
        SkillNode root = root("root");
        SkillNode a = node("a", "root");
        SkillNode b = node("b", "a");
        SkillNode c = node("c", "b");
        SkillNode d = node("d", "a");
        SkillNode e = node("e", "root");
        SkillTreeTopology topology = SkillTreeTopology.of(List.of(root, a, b, c, d, e));
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        for (SkillNode allocated : List.of(a, b, c, d, e)) {
            ShipTrees.allocate(data, allocated, 1);
        }

        List<SkillNode> plan = RespecPlan.of(data, topology, "root", a);

        assertEquals(List.of("c", "b", "d", "a"), ids(plan));
        assertEachStepIsAllowed(data, topology, plan);
    }

    @Test
    void aNodeStillReachableAnotherWayIsLeftAlone() {
        SkillNode root = root("root");
        SkillNode a = node("a", "root");
        SkillNode b = node("b", "root");
        SkillNode shared = node("shared", "a", "b");
        SkillTreeTopology topology = SkillTreeTopology.of(List.of(root, a, b, shared));
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        for (SkillNode allocated : List.of(a, b, shared)) {
            ShipTrees.allocate(data, allocated, 1);
        }

        assertEquals(List.of("a"), ids(RespecPlan.of(data, topology, "root", a)));
    }

    @Test
    void aWormholePairIsRemovedOnceAfterEverythingBeyondIt() {
        SkillNode root = root("root");
        SkillNode p = node("p", "root");
        SkillNode near = wormhole("near", "far", "p", "far");
        SkillNode far = wormhole("far", "near", "near");
        SkillNode x = node("x", "far");
        SkillNode y = node("y", "x");
        SkillTreeTopology topology = SkillTreeTopology.of(List.of(root, p, near, far, x, y));
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, p, 1);
        ShipTrees.allocate(data, near, 1);
        ShipTrees.allocate(data, x, 1);
        ShipTrees.allocate(data, y, 1);

        List<SkillNode> plan = RespecPlan.of(data, topology, "root", p);

        assertEquals(List.of("y", "x", "far", "p"), ids(plan));
        assertEachStepIsAllowed(data, topology, plan);
    }

    @Test
    void selectingTheStartingRootPlansEveryOtherNodeThenTheRoot() {
        SkillNode root = root("root");
        SkillNode a = node("a", "root");
        SkillNode b = node("b", "a");
        SkillNode e = node("e", "root");
        SkillTreeTopology topology = SkillTreeTopology.of(List.of(root, a, b, e));
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        for (SkillNode allocated : List.of(a, b, e)) {
            ShipTrees.allocate(data, allocated, 1);
        }

        List<SkillNode> plan = RespecPlan.of(data, topology, "root", root);

        assertEquals(List.of("b", "a", "e", "root"), ids(plan));
        assertEachStepIsAllowed(data, topology, plan);
        assertTrue(data.canUnchooseStartingRoot());
    }

    @Test
    void aNodeThatIsNotAllocatedHasNoPlan() {
        SkillNode root = root("root");
        SkillNode a = node("a", "root");
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);

        assertEquals(List.of(), RespecPlan.of(data, SkillTreeTopology.of(List.of(root, a)), "root", a));
    }
}
