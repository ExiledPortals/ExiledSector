package exiledsector.charts.tree;

import exiledsector.ProjectFiles;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.skills.SkillTier;
import exiledsector.skills.layout.TreeTopology;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasTreeDataTest {

    @Test
    void typesReadTheirTierEffectsFlavourAndTagsAndSkipUnknownEffects() {
        AtlasTreeData data = AtlasTreeFixtures.data();

        AtlasTreeType salvage = data.type("salvage");
        assertEquals(SkillTier.SMALL, salvage.tier());
        assertEquals(1, salvage.effects().size());
        assertEquals(ChartModifier.CHART_ITEM_PERCENT, salvage.effects().get(0).effect());
        assertEquals(10f, salvage.effects().get(0).value());
        AtlasTreeType hoard = data.type("hoard");
        assertEquals(SkillTier.NOTABLE, hoard.tier());
        assertEquals("Crates everywhere.", hoard.flavour());
        assertEquals(List.of("loot"), hoard.tags());
        assertEquals(SkillTier.ROOT, data.type("gate").tier());
    }

    @Test
    void nodesTakeTheirVolumeFromTheDeclaredVolumesOnly() {
        AtlasTreeData data = AtlasTreeFixtures.data();

        assertEquals(List.of("salvors", "far_reach"), data.volumes());
        assertEquals("salvors", data.node("a").getVolume());
        assertEquals("far_reach", data.node("e").getVolume());
        assertNull(data.node("root").getVolume());
        assertEquals("salvors", data.decorations().stars().get(0).getVolume());
    }

    @Test
    void theTopologyKnowsTheRootWormholePairsCurvesAndHiddenConnectors() {
        TreeTopology<AtlasTreeNode> topology = AtlasTreeFixtures.data().topology();

        assertEquals(List.of("root"), topology.roots().stream().map(AtlasTreeNode::getId).toList());
        assertEquals(1, topology.wormholePairs().size());
        assertTrue(topology.connectors().stream().noneMatch(connector -> connector.from().getId().startsWith("w")
                && connector.to().getId().startsWith("w")));
        assertNotNull(topology.connectors().stream().filter(connector -> connector.curve() != null).findFirst().orElse(null));
    }

    @Test
    void aNodeOfAnUnknownTypeIsSkipped() {
        AtlasTreeData data = AtlasTreeFixtures.parse(AtlasTreeFixtures.TYPES,
                "{ \"nodes\": [ { \"id\": \"root\", \"type\": \"gate\" }, { \"id\": \"lost\", \"type\": \"missing\" } ] }");

        assertNotNull(data.node("root"));
        assertNull(data.node("lost"));
    }

    @Test
    void aTypeWithAnUnknownTierIsSkippedInsteadOfFailingTheLoad() {
        AtlasTreeData data = AtlasTreeFixtures.parse(
                "{ \"skillTypes\": [ { \"id\": \"gate\", \"tier\": \"ROOT\" }, { \"id\": \"odd\", \"tier\": \"notable\" } ] }",
                "{ \"nodes\": [ { \"id\": \"root\", \"type\": \"gate\" } ] }");

        assertNotNull(data.type("gate"));
        assertNull(data.type("odd"));
    }

    @Test
    void theShippedAtlasTreeLoadsWithARoot() throws Exception {
        JSONObject types = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasTreeData.TYPES_PATH));
        JSONObject tree = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasTreeData.TREE_PATH));

        AtlasTreeData data = AtlasTreeData.parse(types, tree);

        assertFalse(data.topology().roots().isEmpty());
        for (AtlasTreeNode node : data.topology().nodes()) {
            for (String connectedId : node.getConnectedNodeIds()) {
                AtlasTreeNode other = data.node(connectedId);
                assertNotNull(other, node.getId() + " connects to missing " + connectedId);
                assertTrue(other.getConnectedNodeIds().contains(node.getId()), node.getId() + " and " + connectedId + " connect one way only");
            }
        }
    }
}
