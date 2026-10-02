package exiledsector.skills.loader;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.ConnectorCurve;
import exiledsector.skills.layout.RingBelt;
import exiledsector.skills.layout.Rotation;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.layout.Star;
import exiledsector.skills.layout.StaticImage;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TreeRegionFilterTest {

    private static final SkillType TYPE = new SkillType.Builder("t", "T", "a.png", SkillTier.SMALL).build();

    private static SkillNode node(String id, String region, List<String> connectedTo) {
        return new SkillNode(id, TYPE, connectedTo, 0f, 0f, SkillNodeDecoration.NONE, List.of(region));
    }

    private static SkillNode wormhole(String id, String region, String pairedWith, List<String> connectedTo) {
        return new SkillNode(id, TYPE, connectedTo, 0f, 0f, new SkillNodeDecoration(null, null, null, null, pairedWith), List.of(region));
    }

    private static SkillTreeLoader.ParsedTree tree() {
        List<SkillNode> nodes = List.of(
                node("root", "inner", List.of("gate_out", "plain")),
                node("plain", "inner", List.of("root")),
                wormhole("gate_out", "inner", "gate_in", List.of("root", "gate_in")),
                wormhole("gate_in", "lost_sector", "gate_out", List.of("gate_out", "heart")),
                node("heart", "lost_sector", List.of("gate_in")));
        Map<String, ConnectorCurve> curves = new LinkedHashMap<>();
        curves.put(SkillTree.curveKey("root", "plain"), new ConnectorCurve(1f, 1f));
        curves.put(SkillTree.curveKey("root", "gate_out"), new ConnectorCurve(2f, 2f));
        curves.put(SkillTree.curveKey("gate_in", "heart"), new ConnectorCurve(3f, 3f));
        return new SkillTreeLoader.ParsedTree(nodes, nodes.size(), curves,
                Set.of(SkillTree.curveKey("root", "gate_out"), SkillTree.curveKey("root", "plain")),
                List.of(new StaticImage("cloud", 0f, 0f, 1f, 1f, "a.png", new Rotation(0f, 0f), List.of("inner")),
                        new StaticImage("untagged", 0f, 0f, 1f, 1f, "a.png", new Rotation(0f, 0f))),
                List.of(new RingBelt("belt", 0f, 0f, 1f, 2f, "a.png", new Rotation(0f, 0f), List.of("lost_sector"))),
                List.of(new Star("sun", 0f, 0f, 1f, "star_yellow", null, List.of("inner")),
                        new Star("frozen", 0f, 0f, 1f, "star_yellow", null, List.of("lost_sector"))));
    }

    @Test
    void anAreaThatIsOnLeavesTheTreeUntouched() {
        SkillTreeLoader.ParsedTree tree = tree();

        assertSame(tree, TreeRegionFilter.apply(tree, Set.of()));
    }

    @Test
    void switchingAnAreaOffRemovesItsNodesAndTheWormholeEndsThatLeadIntoIt() {
        SkillTreeLoader.ParsedTree filtered = TreeRegionFilter.apply(tree(), Set.of("lost_sector"));

        assertEquals(List.of("root", "plain"), filtered.nodes.stream().map(SkillNode::getId).toList());
        assertEquals(List.of("plain"), filtered.nodes.get(0).getConnectedNodeIds());
        assertEquals(Set.of(SkillTree.curveKey("root", "plain")), filtered.connectorCurves.keySet());
        assertEquals(Set.of(SkillTree.curveKey("root", "plain")), filtered.hiddenConnectors);
        assertEquals(5, filtered.declaredNodeCount);
    }

    @Test
    void switchingAnAreaOffHidesItsStarsAndBeltsButKeepsUntaggedDecorations() {
        SkillTreeLoader.ParsedTree filtered = TreeRegionFilter.apply(tree(), Set.of("lost_sector"));

        assertEquals(List.of("sun"), filtered.stars.stream().map(Star::getId).toList());
        assertEquals(List.of(), filtered.ringBelts);
        assertEquals(List.of("cloud", "untagged"), filtered.staticImages.stream().map(StaticImage::getId).toList());
    }

    @Test
    void theWormholeEndsLeadingIntoADisabledAreaAreDisabledToo() {
        assertEquals(Set.of("gate_in", "heart", "gate_out"), TreeRegionFilter.disabledNodeIds(tree().nodes, Set.of("lost_sector")));
    }
}
