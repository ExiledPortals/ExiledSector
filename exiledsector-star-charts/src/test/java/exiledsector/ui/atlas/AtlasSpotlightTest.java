package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasCell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AtlasSpotlightTest {

    private static final AtlasCell CELL = new AtlasCell(3, 4);

    @Test
    void theSpotlightWaitsOutThePanThenEndsAfterItsLastRing() {
        AtlasSpotlight spotlight = new AtlasSpotlight();
        spotlight.start(CELL);

        spotlight.advance(AtlasSpotlight.totalSeconds() * 2f, true);
        assertEquals(CELL, spotlight.cell());

        spotlight.advance(AtlasSpotlight.totalSeconds() - 0.01f, false);
        assertEquals(CELL, spotlight.cell());

        spotlight.advance(0.02f, false);
        assertNull(spotlight.cell());
    }

    @Test
    void eachRingFadesInAndOutAndShinesFullyBetween() {
        assertEquals(0f, AtlasSpotlight.ringAlpha(0f), 1e-6f);
        assertEquals(1f, AtlasSpotlight.ringAlpha(0.5f), 1e-6f);
        assertEquals(0f, AtlasSpotlight.ringAlpha(1f), 1e-6f);
    }
}
