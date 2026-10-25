package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HyperspaceCloudsTest {

    @Test
    void aTilesCloudShowsAboutAsOftenAsItsCover() {
        int shown = 0;
        int tiles = 0;
        for (int tileX = -60; tileX < 60; tileX++) {
            for (int tileY = -60; tileY < 60; tileY++) {
                tiles++;
                shown += HyperspaceClouds.present(7L, tileX, tileY, 0.3f) ? 1 : 0;
                assertFalse(HyperspaceClouds.present(7L, tileX, tileY, 0f));
                assertTrue(HyperspaceClouds.present(7L, tileX, tileY, 1f));
            }
        }
        float share = (float) shown / tiles;

        assertTrue(share > 0.27f && share < 0.33f, "clouds on " + share + " of tiles");
        assertEquals(HyperspaceClouds.present(7L, 3, -4, 0.5f), HyperspaceClouds.present(7L, 3, -4, 0.5f));
    }

    @Test
    void theStormGridWrapsSoAnEndlessMapReusesIt() {
        assertEquals(0, HyperspaceClouds.wrap(0));
        assertEquals(HyperspaceClouds.AUTOMATON_SIZE - 1, HyperspaceClouds.wrap(-1));
        assertEquals(5, HyperspaceClouds.wrap(HyperspaceClouds.AUTOMATON_SIZE * 3 + 5));
    }

    @Test
    void cornersSwayBackAndForthRatherThanSpinning() {
        assertEquals(0f, HyperspaceClouds.triangle(0f), 1e-6f);
        assertEquals(1f, HyperspaceClouds.triangle(0.5f), 1e-6f);
        assertEquals(0.5f, HyperspaceClouds.triangle(1.25f), 1e-6f);
        assertEquals(0.5f, HyperspaceClouds.triangle(-0.25f), 1e-6f);
    }
}
