package exiledsector.ui.atlas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasTreeViewTest {

    private static final float DELTA = 0.001f;

    @Test
    void aWheelsRingsAreFaintUntilTouchedAndBrightenWithEachAllocatedNode() {
        float untouched = AtlasTreeView.wheelRingAlpha(0, 3);
        float one = AtlasTreeView.wheelRingAlpha(1, 3);
        float two = AtlasTreeView.wheelRingAlpha(2, 3);

        assertTrue(untouched > 0f);
        assertTrue(untouched < one);
        assertTrue(one < two);
        assertEquals(1f, AtlasTreeView.wheelRingAlpha(3, 3), DELTA);
    }
}
