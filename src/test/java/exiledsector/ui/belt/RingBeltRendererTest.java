package exiledsector.ui.belt;

import exiledsector.ui.TreeViewport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        assertNull(RingBeltRenderer.visibleVertexRange(0f, 0f, 100f, 120f, 0f, 360, clip(500f, 500f, 600f, 600f)));
        assertNull(RingBeltRenderer.visibleVertexRange(0f, 0f, 1000f, 1100f, 0f, 360, clip(-10f, -10f, 10f, 10f)));
    }

    @Test
    void onlyTheArcFacingTheScreenIsDrawnAcrossTheSeamAndUnderRotation() {
        TreeViewport rightOfCentre = clip(800f, -100f, 1100f, 100f);

        assertArrayEquals(new int[]{351, 369}, RingBeltRenderer.visibleVertexRange(0f, 0f, 900f, 1000f, 0f, 360, rightOfCentre));
        assertArrayEquals(new int[]{261, 279}, RingBeltRenderer.visibleVertexRange(0f, 0f, 900f, 1000f, 90f, 360, rightOfCentre));
    }
}
