package exiledsector.skills.loader;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.decoration.RingBelt;
import exiledsector.skills.layout.ConnectorCurve;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillTreeLoaderTest {

    private static final Map<String, SkillType> SKILL_TYPES = Map.of(
            "capacitors", new SkillType.Builder("capacitors", "Capacitors", "graphics/hullmods/flux_coil_adjunct.png", SkillTier.SMALL)
                    .effects(List.of())
                    .build(),
            "bare", new SkillType.Builder("bare", "Bare", "graphics/icons/skills/combat.png", SkillTier.SMALL)
                    .effects(List.of())
                    .build(),
            "gate", new SkillType.Builder("gate", "Gate", "graphics/icons/skills/combat.png", SkillTier.WORMHOLE)
                    .effects(List.of())
                    .build()
    );

    @Test
    void parsesAllFieldsOfANodeAndResolvesItsType() throws Exception {
        JSONObject root = new JSONObject("{ \"nodes\": [ {"
                + "\"id\": \"capacitors_1\","
                + "\"type\": \"capacitors\","
                + "\"connectedTo\": [\"vents_1\"],"
                + "\"x\": -180,"
                + "\"y\": 180,"
                + "\"ringBeltPath\": \"graphics/planets/ring_band_ice.png\","
                + "\"ringBeltColor\": \"#8c78ff\","
                + "\"ringBeltWidth\": 1.4,"
                + "\"wormholeColor\": \"#ff5ad1\","
                + "\"pairedWith\": \"capacitors_2\""
                + "} ] }");

        List<SkillNode> nodes = SkillTreeLoader.parseNodes(root, SKILL_TYPES);

        assertEquals(1, nodes.size());
        SkillNode node = nodes.get(0);
        assertEquals("capacitors_1", node.getId());
        assertEquals("Capacitors", node.getDisplayName());
        assertEquals("graphics/hullmods/flux_coil_adjunct.png", node.getIconPath());
        assertEquals(List.of("vents_1"), node.getConnectedNodeIds());
        assertEquals(-180f, node.getOffsetX());
        assertEquals(180f, node.getOffsetY());
        assertEquals("graphics/planets/ring_band_ice.png", node.getRingBeltPath());
        assertEquals("#8c78ff", node.getRingBeltColor());
        assertEquals(1.4f, node.getRingBeltWidth());
        assertEquals("#ff5ad1", node.getWormholeColor());
        assertNull(node.getPairedNodeId());
    }

    @Test
    void keepsPairedWithOnAWormholeTierNode() throws Exception {
        JSONObject root = new JSONObject("{ \"nodes\": [ { \"id\": \"gate_1\", \"type\": \"gate\", \"pairedWith\": \"gate_2\" } ] }");

        List<SkillNode> nodes = SkillTreeLoader.parseNodes(root, SKILL_TYPES);

        assertEquals("gate_2", nodes.get(0).getPairedNodeId());
    }

    @Test
    void missingOptionalFieldsFallBackToDefaults() throws Exception {
        JSONObject root = new JSONObject("{ \"nodes\": [ {"
                + "\"id\": \"bare_node\","
                + "\"type\": \"bare\""
                + "} ] }");

        SkillNode node = SkillTreeLoader.parseNodes(root, SKILL_TYPES).get(0);

        assertTrue(node.getConnectedNodeIds().isEmpty());
        assertEquals(0f, node.getOffsetX());
        assertEquals(0f, node.getOffsetY());
        assertNull(node.getRingBeltPath());
        assertNull(node.getRingBeltColor());
        assertNull(node.getRingBeltWidth());
        assertNull(node.getWormholeColor());
        assertNull(node.getPairedNodeId());
        assertTrue(node.getTags().isEmpty());
    }

    @Test
    void parsesNodeTagsIntoAnOrderedList() throws Exception {
        JSONObject root = new JSONObject("{ \"nodes\": [ {"
                + "\"id\": \"capacitors_1\","
                + "\"type\": \"capacitors\","
                + "\"tags\": [\"luddic\", \"req_flagship\"]"
                + "} ] }");

        SkillNode node = SkillTreeLoader.parseNodes(root, SKILL_TYPES).get(0);

        assertEquals(List.of("luddic", "req_flagship"), node.getTags());
    }

    @Test
    void parsesMultipleNodesInOrder() throws Exception {
        JSONObject root = new JSONObject("{ \"nodes\": ["
                + "{\"id\": \"a\", \"type\": \"bare\"},"
                + "{\"id\": \"b\", \"type\": \"capacitors\"}"
                + "] }");

        List<SkillNode> nodes = SkillTreeLoader.parseNodes(root, SKILL_TYPES);

        assertEquals(List.of("a", "b"), List.of(nodes.get(0).getId(), nodes.get(1).getId()));
    }

    @Test
    void nodesWithAnUnknownTypeOrMissingFieldsAreSkippedAndTheRestStillLoad() throws Exception {
        JSONObject root = new JSONObject("{ \"nodes\": ["
                + "{\"id\": \"a\", \"type\": \"bare\"},"
                + "{\"id\": \"mystery\", \"type\": \"does_not_exist\"},"
                + "{\"type\": \"bare\"},"
                + "\"not_an_object\","
                + "{\"id\": \"b\", \"type\": \"capacitors\"}"
                + "] }");

        List<SkillNode> nodes = SkillTreeLoader.parseNodes(root, SKILL_TYPES);

        assertEquals(List.of("a", "b"), nodes.stream().map(SkillNode::getId).toList());
    }

    @Test
    void missingConnectorCurvesFieldMeansNoCurves() throws Exception {
        JSONObject root = new JSONObject("{ \"nodes\": [] }");

        Map<String, ConnectorCurve> curves = SkillTreeLoader.parseConnectorCurves(root);

        assertTrue(curves.isEmpty());
    }

    @Test
    void parsesConnectorCurvesKeyedByCanonicalPair() throws Exception {
        JSONObject root = new JSONObject("{ \"nodes\": [], \"connectorCurves\": [ {"
                + "\"a\": \"capacitors_1\","
                + "\"b\": \"bare_node\","
                + "\"controlX\": 12.5,"
                + "\"controlY\": -8"
                + "} ] }");

        Map<String, ConnectorCurve> curves = SkillTreeLoader.parseConnectorCurves(root);

        assertEquals(1, curves.size());
        ConnectorCurve curve = curves.get(SkillTree.curveKey("capacitors_1", "bare_node"));
        assertEquals(12.5f, curve.getControlOffsetX());
        assertEquals(-8f, curve.getControlOffsetY());
        assertEquals(curves.get(SkillTree.curveKey("bare_node", "capacitors_1")), curve);
    }

    @Test
    void missingHiddenConnectorsFieldMeansNoneHidden() throws Exception {
        JSONObject root = new JSONObject("{ \"nodes\": [] }");

        Set<String> hidden = SkillTreeLoader.parseHiddenConnectors(root);

        assertTrue(hidden.isEmpty());
    }

    @Test
    void parsesHiddenConnectorsKeyedByCanonicalPair() throws Exception {
        JSONObject root = new JSONObject("{ \"nodes\": [], \"hiddenConnectors\": [ {"
                + "\"a\": \"capacitors_1\","
                + "\"b\": \"bare_node\""
                + "} ] }");

        Set<String> hidden = SkillTreeLoader.parseHiddenConnectors(root);

        assertEquals(1, hidden.size());
        assertTrue(hidden.contains(SkillTree.curveKey("capacitors_1", "bare_node")));
        assertTrue(hidden.contains(SkillTree.curveKey("bare_node", "capacitors_1")));
    }

    @Test
    void loadRootReturnsNullWhenTheJsonFailsToLoad() throws Exception {
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.loadJSON(anyString())).thenThrow(new IOException("boom"));

        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            globalMock.when(Global::getSettings).thenReturn(settings);

            assertNull(SkillTreeLoader.loadRoot());
        }
    }

    @Test
    void loadAllReturnsAllEmptyCollectionsWhenTheJsonFailsToLoad() throws Exception {
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.loadJSON(anyString())).thenThrow(new IOException("boom"));

        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            globalMock.when(Global::getSettings).thenReturn(settings);

            SkillTreeLoader.ParsedTree parsed = SkillTreeLoader.loadAll(SKILL_TYPES);

            assertTrue(parsed.nodes.isEmpty());
            assertTrue(parsed.connectorCurves.isEmpty());
            assertTrue(parsed.hiddenConnectors.isEmpty());
            assertTrue(parsed.decorations.all().isEmpty());
        }
    }

    @Test
    void loadAllParsesEverySectionFromASingleRoot() throws Exception {
        JSONObject root = new JSONObject("{"
                + "\"nodes\": [ {\"id\": \"bare_node\", \"type\": \"bare\"} ],"
                + "\"connectorCurves\": [ {\"a\": \"a\", \"b\": \"b\", \"controlX\": 1, \"controlY\": 2} ],"
                + "\"hiddenConnectors\": [ {\"a\": \"a\", \"b\": \"b\"} ],"
                + "\"decorations\": [ {\"kind\": \"image\", \"id\": \"img\", \"x\": 0, \"y\": 0, \"width\": 10, \"height\": 10, \"imagePath\": \"a.png\"},"
                + " {\"kind\": \"ringBelt\", \"id\": \"belt\", \"x\": 0, \"y\": 0, \"innerRadius\": 10, \"outerRadius\": 20, \"ringArtPath\": \"a.png\"},"
                + " {\"kind\": \"star\", \"id\": \"star\", \"x\": 0, \"y\": 0, \"radius\": 100} ]"
                + "}");
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.loadJSON(anyString())).thenReturn(root);

        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            globalMock.when(Global::getSettings).thenReturn(settings);

            SkillTreeLoader.ParsedTree parsed = SkillTreeLoader.loadAll(SKILL_TYPES);

            assertEquals(1, parsed.nodes.size());
            assertEquals(1, parsed.connectorCurves.size());
            assertEquals(1, parsed.hiddenConnectors.size());
            assertEquals(1, parsed.decorations.images().size());
            assertEquals(1, parsed.decorations.ringBelts().size());
            assertEquals(1, parsed.decorations.stars().size());
            verify(settings, times(1)).loadJSON(anyString());
        }
    }

    @Test
    void loadAllSkipsAMalformedLayoutEntryWithoutLosingTheRestOfItsSection() throws Exception {
        JSONObject root = new JSONObject("{"
                + "\"nodes\": [ {\"id\": \"bare_node\", \"type\": \"bare\"} ],"
                + "\"decorations\": [ {\"kind\": \"ringBelt\", \"id\": \"broken\", \"x\": 0, \"y\": 0, \"innerRadius\": 10, \"outerRadius\": 20},"
                + " {\"kind\": \"ringBelt\", \"id\": \"belt\", \"x\": 0, \"y\": 0, \"innerRadius\": 10, \"outerRadius\": 20, \"ringArtPath\": \"a.png\"} ]"
                + "}");
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.loadJSON(anyString())).thenReturn(root);

        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            globalMock.when(Global::getSettings).thenReturn(settings);

            SkillTreeLoader.ParsedTree parsed = SkillTreeLoader.loadAll(SKILL_TYPES);

            assertEquals(1, parsed.nodes.size());
            assertEquals(List.of("belt"), parsed.decorations.ringBelts().stream().map(RingBelt::getId).toList());
        }
    }
}
