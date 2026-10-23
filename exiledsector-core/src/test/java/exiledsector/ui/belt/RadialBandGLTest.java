package exiledsector.ui.belt;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadialBandGLTest {

    @Test
    void segmentCountsSnapToPowersOfTwoAndTheirMidpoints() {
        assertEquals(1, RadialBandGL.quantised(0));
        assertEquals(1, RadialBandGL.quantised(1));
        assertEquals(2, RadialBandGL.quantised(2));
        assertEquals(3, RadialBandGL.quantised(3));
        assertEquals(64, RadialBandGL.quantised(64));
        assertEquals(96, RadialBandGL.quantised(65));
        assertEquals(96, RadialBandGL.quantised(96));
        assertEquals(128, RadialBandGL.quantised(97));
    }

    @Test
    void segmentsNeverDropBelowTheRequestedDetail() {
        for (int segments = 1; segments < 5000; segments++) {
            int snapped = RadialBandGL.quantised(segments);
            assertTrue(snapped >= segments && snapped <= segments * 2, segments + " -> " + snapped);
        }
    }

    @Test
    void aSmoothZoomOnlyVisitsAHandfulOfSegmentCounts() {
        Set<Integer> counts = new HashSet<>();
        for (float circumference = 600f; circumference <= 6000f; circumference += 0.5f) {
            counts.add(RadialBandGL.computeSegments(circumference, 6f, 48f));
        }
        assertTrue(counts.size() <= 8, counts.toString());
    }
}
