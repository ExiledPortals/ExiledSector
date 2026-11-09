package exiledsector.skills.loader;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.decoration.AbyssSerpent;
import exiledsector.skills.decoration.Orbit;
import exiledsector.skills.decoration.Decoration;
import exiledsector.skills.decoration.Decorations;
import exiledsector.skills.decoration.RingBelt;
import exiledsector.skills.decoration.Star;
import exiledsector.skills.decoration.StaticImage;
import exiledsector.skills.decoration.ThreatUnit;
import exiledsector.skills.layout.ConnectorCurve;
import exiledsector.skills.layout.Rotation;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.tags.SkillTags;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TreeVolumeFilterTest {

    private static final SkillType TYPE = new SkillType.Builder("t", "T", "a.png", SkillTier.SMALL).build();

    private static SkillNode node(String id, String volume, List<String> connectedTo) {
        return new SkillNode(id, TYPE, connectedTo, 0f, 0f, SkillNodeDecoration.NONE, List.of(volume));
    }

    private static SkillNode wormhole(String id, String volume, String pairedWith, List<String> connectedTo) {
        return new SkillNode(id, TYPE, connectedTo, 0f, 0f, new SkillNodeDecoration(null, null, null, null, pairedWith), List.of(volume));
    }

    private static SkillTreeLoader.ParsedTree tree() {
        List<SkillNode> nodes = List.of(
                node("root", "core", List.of("gate_out", "plain")),
                node("plain", "core", List.of("root")),
                wormhole("gate_out", "core", "gate_in", List.of("root", "gate_in")),
                wormhole("gate_in", "enigma", "gate_out", List.of("gate_out", "heart")),
                node("heart", "enigma", List.of("gate_in")));
        Map<String, ConnectorCurve> curves = new LinkedHashMap<>();
        curves.put(SkillTree.curveKey("root", "plain"), new ConnectorCurve(1f, 1f));
        curves.put(SkillTree.curveKey("root", "gate_out"), new ConnectorCurve(2f, 2f));
        curves.put(SkillTree.curveKey("gate_in", "heart"), new ConnectorCurve(3f, 3f));
        return new SkillTreeLoader.ParsedTree(nodes, nodes.size(), curves,
                Set.of(SkillTree.curveKey("root", "gate_out"), SkillTree.curveKey("root", "plain")),
                new Decorations(List.of(
                        new StaticImage("cloud", 0f, 0f, new StaticImage.Shape(1f, 1f, "a.png", new Rotation(0f, 0f)), SkillTags.treeTags(List.of("core"))),
                        new StaticImage("untagged", 0f, 0f, new StaticImage.Shape(1f, 1f, "a.png", new Rotation(0f, 0f)), SkillTags.treeTags(List.of())),
                        new RingBelt("belt", 0f, 0f, new RingBelt.Shape(1f, 2f, "a.png", new Rotation(0f, 0f)), SkillTags.treeTags(List.of("enigma"))),
                        new Star("sun", 0f, 0f, 1f, "star_yellow", null, SkillTags.treeTags(List.of("core"))),
                        new Star("frozen", 0f, 0f, 1f, "star_yellow", null, SkillTags.treeTags(List.of("enigma"))),
                        new ThreatUnit("hive", 0f, 0f, ThreatUnit.HIVE, 1f, 0f, SkillTags.treeTags(List.of("core"))),
                        new ThreatUnit("lost_fabricator", 0f, 0f, ThreatUnit.FABRICATOR, 1f, 0f, SkillTags.treeTags(List.of("enigma"))),
                        new AbyssSerpent("worm", 0f, 0f, new Orbit(1f, 1f, true, 0f), AbyssSerpent.Wobble.NONE, SkillTags.treeTags(List.of("enigma"))))));
    }

    @Test
    void anVolumeThatIsOnLeavesTheTreeUntouched() {
        SkillTreeLoader.ParsedTree tree = tree();

        assertSame(tree, TreeVolumeFilter.apply(tree, Set.of()));
    }

    @Test
    void switchingAnVolumeOffRemovesItsNodesAndTheWormholeEndsThatLeadIntoIt() {
        SkillTreeLoader.ParsedTree filtered = TreeVolumeFilter.apply(tree(), Set.of("enigma"));

        assertEquals(List.of("root", "plain"), filtered.nodes.stream().map(SkillNode::getId).toList());
        assertEquals(List.of("plain"), filtered.nodes.get(0).getConnectedNodeIds());
        assertEquals(Set.of(SkillTree.curveKey("root", "plain")), filtered.connectorCurves.keySet());
        assertEquals(Set.of(SkillTree.curveKey("root", "plain")), filtered.hiddenConnectors);
        assertEquals(5, filtered.declaredNodeCount);
    }

    @Test
    void switchingAnVolumeOffHidesItsStarsAndBeltsButKeepsUntaggedDecorations() {
        SkillTreeLoader.ParsedTree filtered = TreeVolumeFilter.apply(tree(), Set.of("enigma"));

        assertEquals(List.of("cloud", "untagged", "sun", "hive"), filtered.decorations.all().stream().map(Decoration::getId).toList());
        assertEquals(List.of("sun"), filtered.decorations.stars().stream().map(Star::getId).toList());
        assertEquals(List.of(), filtered.decorations.ringBelts());
        assertEquals(List.of(), filtered.decorations.serpents());
    }

    @Test
    void theWormholeEndsLeadingIntoADisabledVolumeAreDisabledToo() {
        assertEquals(Set.of("gate_in", "heart", "gate_out"), TreeVolumeFilter.disabledNodeIds(tree().nodes, Set.of("enigma")));
    }
}
