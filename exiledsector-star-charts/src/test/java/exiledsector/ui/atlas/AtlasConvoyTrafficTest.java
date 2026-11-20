package exiledsector.ui.atlas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtlasConvoyTrafficTest {

    private static final float DELTA = 0.01f;

    @Test
    void convoysFadeOutWhenTheTreeIsZoomedTooFarOutToSeeThem() {
        assertEquals(0f, AtlasConvoyTraffic.zoomVisibility(AtlasConvoyTraffic.HIDDEN_BELOW_ZOOM), DELTA);
        assertEquals(1f, AtlasConvoyTraffic.zoomVisibility(AtlasConvoyTraffic.SHOWN_FROM_ZOOM), DELTA);
        assertEquals(1f, AtlasConvoyTraffic.zoomVisibility(1f), DELTA);
    }
}
