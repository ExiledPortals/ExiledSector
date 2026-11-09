package exiledsector.charts.tree;

import exiledsector.skills.layout.TreeTopology;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasTreeAllocationTest {

    private final TreeTopology<AtlasTreeNode> topology = AtlasTreeFixtures.data().topology();

    private AtlasTreeAllocation allocation(String... storedIds) {
        return AtlasTreeAllocation.of(topology, List.of(storedIds));
    }

    private AtlasTreeNode node(String id) {
        return topology.node(id);
    }

    @Test
    void theRootIsAllocatedForFreeAndItsNeighboursCanBeAllocated() {
        AtlasTreeAllocation allocation = allocation();

        assertTrue(allocation.isAllocated("root"));
        assertEquals(0, allocation.spentPoints());
        assertTrue(allocation.canAllocate(node("a")));
        assertFalse(allocation.canAllocate(node("b")));
        assertFalse(allocation.canAllocate(node("root")));
    }

    @Test
    void storedNodesCutOffFromTheRootOrUnknownAreDropped() {
        AtlasTreeAllocation allocation = allocation("a", "c", "missing", "root");

        assertEquals(Set.of("a"), allocation.storedNodeIds());
    }

    @Test
    void aPathRunsThroughUnallocatedNodesOnlyAndCrossesWormholes() {
        assertEquals(List.of("a", "b", "c"), allocation().pathTo(node("c")).stream().map(AtlasTreeNode::getId).toList());
        assertEquals(List.of("w1", "w2", "e"), allocation("a", "b").pathTo(node("e")).stream().map(AtlasTreeNode::getId).toList());
        assertEquals(List.of(), allocation("a").pathTo(node("a")));
    }

    @Test
    void aNodeCanBeRefundedOnlyWhenNothingLosesItsWayBackToTheRoot() {
        AtlasTreeAllocation allocation = allocation("a", "b", "c", "d");

        assertFalse(allocation.canRefund(node("a")));
        assertFalse(allocation.canRefund(node("b")));
        assertTrue(allocation.canRefund(node("c")));
        assertTrue(allocation.canRefund(node("d")));
        assertFalse(allocation.canRefund(node("root")));
        assertFalse(allocation.canRefund(node("e")));
    }

    @Test
    void addingAndRemovingNodesLeavesTheOriginalUntouched() {
        AtlasTreeAllocation original = allocation("a");

        AtlasTreeAllocation grown = original.with(List.of(node("b"), node("c")));
        AtlasTreeAllocation shrunk = grown.without(node("c"));

        assertEquals(Set.of("a"), original.storedNodeIds());
        assertEquals(Set.of("a", "b", "c"), grown.storedNodeIds());
        assertEquals(Set.of("a", "b"), shrunk.storedNodeIds());
    }
}
