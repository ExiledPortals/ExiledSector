package exiledsector.ui.belt;

import exiledsector.ui.TreeViewport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RingBeltRendererTest {

    private static TreeViewport clip(float left, float bottom, float right, float top) {
        return new TreeViewport(0f, 0f, 1f, left, bottom, right, top);
    }

    @Test
    void aBeltWithNoClipOrWhoseCentreIsOnScreenIsDrawnWhole() {
        assertArrayEquals(new int[]{0, 360}, RingBeltRenderer.visibleVertexRange(0f, 0f, 900f, 1000f, 0f, 360, null));
        assertArrayEquals(new int[]{0, 360}, RingBeltRenderer.visibleVertexRange(0f, 0f, 900f, 1000f, 0f, 360, clip(-1000f, -1000f, 1000f, 1000f)));
    }

    @Test
    void aBeltThatMissesTheScreenOrSurroundsItIsSkipped() {
        assertEquals(0, RingBeltRenderer.visibleVertexRange(0f, 0f, 100f, 120f, 0f, 360, clip(500f, 500f, 600f, 600f)).length);
        assertEquals(0, RingBeltRenderer.visibleVertexRange(0f, 0f, 1000f, 1100f, 0f, 360, clip(-10f, -10f, 10f, 10f)).length);
    }

    @Test
    void onlyTheArcFacingTheScreenIsDrawnAcrossTheSeamAndUnderRotation() {
        TreeViewport rightOfCentre = clip(800f, -100f, 1100f, 100f);

        assertArrayEquals(new int[]{351, 369}, RingBeltRenderer.visibleVertexRange(0f, 0f, 900f, 1000f, 0f, 360, rightOfCentre));
        assertArrayEquals(new int[]{261, 279}, RingBeltRenderer.visibleVertexRange(0f, 0f, 900f, 1000f, 90f, 360, rightOfCentre));
    }

    @Test
    void aWholeRingArcDrawsEverySegment() {
        assertArrayEquals(new int[]{0, 96}, RingBeltRenderer.arcVertexRange(37f, 96, RingBeltRenderer.WHOLE_RING));
    }

    @Test
    void anArcCoversItsShareOfTheScreenAnglesWhateverTheBeltsRotation() {
        assertArrayEquals(new int[]{0, 90}, RingBeltRenderer.arcVertexRange(0f, 360, new RingBeltRenderer.Arc(0, 4)));
        assertArrayEquals(new int[]{0, 90}, RingBeltRenderer.arcVertexRange(90f, 360, new RingBeltRenderer.Arc(1, 4)));
        assertArrayEquals(new int[]{270, 360}, RingBeltRenderer.arcVertexRange(90f, 360, new RingBeltRenderer.Arc(0, 4)));
        assertEquals(180f + 45f, new RingBeltRenderer.Arc(2, 4).middleDegrees());
    }

    @Test
    void theArcsOfABeltDrawEachSegmentExactlyOnce() {
        int segments = 96;
        for (float rotation : new float[]{0f, 37.3f, -200.5f, 719.9f}) {
            int[] drawn = new int[segments];
            for (int index = 0; index < 12; index++) {
                int[] range = RingBeltRenderer.arcVertexRange(rotation, segments, new RingBeltRenderer.Arc(index, 12));
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
