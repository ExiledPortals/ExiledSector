package exiledsector.skills;

import exiledsector.skills.layout.ConnectorCurve;
import exiledsector.skills.layout.SkillNodeDecoration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillTreeTopologyTest {

    @BeforeEach
    void setUp() {
        SkillTree.clear();
    }

    @AfterEach
    void tearDown() {
        SkillTree.clear();
    }

    private static SkillNode node(String id, SkillTier tier, float x, float y, String... connectedTo) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", tier).effects(List.of()).build();
        return new SkillNode(id, type, List.of(connectedTo), x, y);
    }

    private static SkillNode wormhole(String id, String pairedWith, String... connectedTo) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", SkillTier.WORMHOLE).effects(List.of()).build();
        return new SkillNode(id, type, List.of(connectedTo), 0f, 0f, new SkillNodeDecoration(null, null, null, null, pairedWith));
    }

    private static List<String> ids(List<SkillNode> nodes) {
        return nodes.stream().map(SkillNode::getId).toList();
    }

    @Test
    void rootsAndOtherNodesKeepTreeOrderAndDependentsAreSortedById() {
        SkillTreeTopology topology = SkillTreeTopology.of(List.of(
                node("root", SkillTier.ROOT, 0f, 0f, "b"),
                node("b", SkillTier.SMALL, 1f, 0f, "root"),
                node("a", SkillTier.SMALL, 2f, 0f, "root")));

        assertEquals(List.of("root"), ids(topology.roots()));
        assertEquals(List.of("b", "a"), ids(topology.nonRoots()));
        assertEquals(Set.of("root"), topology.rootIds());
        assertEquals(List.of("a", "b", "root"), ids(topology.sortedById()));
        assertEquals(List.of("a", "b"), ids(topology.dependents("root")));
        assertEquals(List.of(), topology.dependents("nothing"));
    }

    @Test
    void eachLinkedPairIsDrawnOnceAndRootLinksAreDrawnFromTheOtherNode() {
        SkillTreeTopology topology = SkillTreeTopology.of(List.of(
                node("root", SkillTier.ROOT, 0f, 0f, "a"),
                node("a", SkillTier.SMALL, 1f, 0f, "root", "b"),
                node("b", SkillTier.SMALL, 2f, 0f, "a")));

        List<String> drawn = topology.connectors().stream().map(c -> c.from().getId() + "-" + c.to().getId()).toList();

        assertEquals(List.of("a-root", "a-b"), drawn);
        assertEquals(List.of("root", "b"), ids(topology.drawnNeighbours("a")));
        assertEquals(List.of("a"), ids(topology.drawnNeighbours("root")));
    }

    @Test
    void wormholesArePairedOnceFromTheLowerId() {
        SkillTreeTopology topology = SkillTreeTopology.of(List.of(wormhole("w2", "w1"), wormhole("w1", "w2"), wormhole("lone", null)));

        assertEquals(List.of("w2", "w1", "lone"), ids(topology.wormholes()));
        assertEquals(1, topology.wormholePairs().size());
        assertEquals("w1", topology.wormholePairs().get(0).first().getId());
        assertEquals("w2", topology.wormholePairs().get(0).second().getId());
    }

    @Test
    void aCurvedConnectorsBoundsCoverItsBendNotJustThePointItPassesThrough() {
        SkillNode from = node("a", SkillTier.SMALL, 0f, 0f);
        SkillNode to = node("b", SkillTier.SMALL, 100f, 0f);

        SkillTreeTopology.Connector curved = SkillTreeTopology.connector(from, to, new ConnectorCurve(50f, 40f));

        assertEquals(0f, curved.minX());
        assertEquals(100f, curved.maxX());
        assertEquals(0f, curved.minY());
        assertEquals(80f, curved.maxY());
    }

    @Test
    void theSharedTopologyIsRebuiltOnlyWhenNodesChangeAndTheMapsAreReadOnly() {
        SkillTree.register(node("a", SkillTier.SMALL, 0f, 0f));
        SkillTreeTopology first = SkillTree.topology();

        assertSame(first, SkillTree.topology());
        SkillTree.register(node("b", SkillTier.SMALL, 0f, 0f));
        assertNotSame(first, SkillTree.topology());
        SkillTree.unregister("a");
        assertEquals(List.of("b"), ids(SkillTree.topology().nonRoots()));
        assertThrows(UnsupportedOperationException.class, () -> SkillTree.getAllNodes().clear());
        assertTrue(SkillTree.getAllTypes().isEmpty());
    }
}
