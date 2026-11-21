package exiledsector.ui.belt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BandArcTest {

    @Test
    void aWholeBandDrawsEverySegment() {
        assertTrue(BandArc.WHOLE.whole());
        assertArrayEquals(new int[]{0, 96}, BandArc.WHOLE.vertexRange(37f, 96));
    }

    @Test
    void anArcCoversItsShareOfTheScreenAnglesWhateverTheBandsRotation() {
        assertArrayEquals(new int[]{0, 90}, new BandArc(0, 4).vertexRange(0f, 360));
        assertArrayEquals(new int[]{0, 90}, new BandArc(1, 4).vertexRange(90f, 360));
        assertArrayEquals(new int[]{270, 360}, new BandArc(0, 4).vertexRange(90f, 360));
        assertEquals(180f + 45f, new BandArc(2, 4).middleDegrees());
    }

    @Test
    void theArcsOfABandDrawEachSegmentExactlyOnce() {
        int segments = 96;
        BandArc[] arcs = BandArc.split(12);
        for (float rotation : new float[]{0f, 37.3f, -200.5f, 719.9f}) {
            int[] drawn = new int[segments];
            for (BandArc arc : arcs) {
                int[] range = arc.vertexRange(rotation, segments);
                for (int vertex = range[0]; vertex < range[1]; vertex++) {
                    drawn[vertex % segments]++;
                }
            }
            for (int segment = 0; segment < segments; segment++) {
                assertEquals(1, drawn[segment], "segment " + segment + " at rotation " + rotation);
            }
        }
    }
}
