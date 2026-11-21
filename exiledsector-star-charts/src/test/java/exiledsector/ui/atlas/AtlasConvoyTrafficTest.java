package exiledsector.ui.atlas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtlasConvoyTrafficTest {

    private static final float DELTA = 0.01f;

    @Test
    void smallConvoysFadeOutWhenTheTreeIsZoomedTooFarOutToSeeThem() {
        assertEquals(0f, ConvoyLooks.smallVisibility(ConvoyLooks.HIDDEN_BELOW_ZOOM), DELTA);
        assertEquals(1f, ConvoyLooks.smallVisibility(ConvoyLooks.SHOWN_FROM_ZOOM), DELTA);
        assertEquals(1f, ConvoyLooks.smallVisibility(1f), DELTA);
    }

    @Test
    void largeConvoysTakeOverAsTheSmallOnesFade() {
        for (float zoom = 0.1f; zoom <= 1.2f; zoom += 0.05f) {
            assertEquals(1f, ConvoyLooks.smallVisibility(zoom) + ConvoyLooks.largeVisibility(zoom), DELTA, "zoom " + zoom);
        }
        assertEquals(1f, ConvoyLooks.largeVisibility(ConvoyLooks.HIDDEN_BELOW_ZOOM), DELTA);
        assertEquals(0f, ConvoyLooks.largeVisibility(ConvoyLooks.SHOWN_FROM_ZOOM), DELTA);
    }
}
