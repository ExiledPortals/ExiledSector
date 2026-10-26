package exiledsector.ui.node;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectorFlowTest {

    private static final float SPACING = ConnectorFlow.WAVE_INTERVAL_SECONDS * ConnectorFlow.SPEED_PER_SECOND;

    @Test
    void distancesFollowTheShortestAllocatedPathFromTheRoot() {
        Map<String, List<String>> links = Map.of(
                "root", List.of("a", "b"),
                "a", List.of("root", "c"),
                "b", List.of("root", "c"),
                "c", List.of("a", "b"));
        Map<String, Float> lengths = Map.of("root-a", 100f, "root-b", 30f, "a-c", 10f, "b-c", 50f);
        ConnectorFlow flow = new ConnectorFlow();
        flow.recompute("root", new ConnectorFlow.Graph() {
            @Override
            public List<String> neighbours(String nodeId) {
                return links.getOrDefault(nodeId, List.of());
            }

            @Override
            public float length(String fromId, String toId) {
                Float length = lengths.get(fromId + "-" + toId);
                return length != null ? length : lengths.get(toId + "-" + fromId);
            }
        });

        assertEquals(0f, flow.distance("root"));
        assertEquals(30f, flow.distance("b"));
        assertEquals(80f, flow.distance("c"));
        assertEquals(90f, flow.distance("a"));
        assertTrue(Float.isNaN(flow.distance("unreached")));
    }

    @Test
    void bubblesLeaveTheRootInWavesAndCrossEachConnectorInTurn() {
        float[] out = new float[4];
        float nearDistance = 50f;
        float length = 100f;
        float arrival = nearDistance / ConnectorFlow.SPEED_PER_SECOND;
        float crossing = length / ConnectorFlow.SPEED_PER_SECOND;

        assertEquals(1, ConnectorFlow.bubbles(arrival + crossing / 2f, nearDistance, length, out));
        assertEquals(0.5f, out[0], 0.001f);
        assertEquals(1, ConnectorFlow.bubbles(arrival + ConnectorFlow.WAVE_INTERVAL_SECONDS + crossing / 4f, nearDistance, length, out));
        assertEquals(0.25f, out[0], 0.001f);
        assertEquals(0, ConnectorFlow.bubbles(arrival + crossing * 1.5f, nearDistance, length, out));
    }

    @Test
    void longConnectorsCarrySeveralWavesAtOnce() {
        float[] out = new float[4];
        int count = ConnectorFlow.bubbles(10f, 0f, SPACING * 2.5f, out);

        assertEquals(3, count);
        assertEquals(out[0] - out[1], 1f / 2.5f, 0.001f);
        assertEquals(out[1] - out[2], 1f / 2.5f, 0.001f);
    }
}
