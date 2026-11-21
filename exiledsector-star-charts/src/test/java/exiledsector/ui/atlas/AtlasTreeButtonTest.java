package exiledsector.ui.atlas;

import exiledsector.ui.ChromeMetrics;
import exiledsector.ui.socket.WormholeButtonFx;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasTreeButtonTest {

    private static final float SMALLEST_PANEL_WIDTH = 1280f;
    private static final float TITLE_ROOM = 200f;

    @Test
    void withNoPointsToSpendTheGlowIsDimAndSteady() {
        WormholeButtonFx.Intensity glow = AtlasTreeButton.intensity(0);

        assertSame(AtlasTreeButton.NOTHING_TO_SPEND, glow);
        assertEquals(0f, glow.pulseDepth(), 1e-6f);
        assertTrue(glow.strength() < WormholeButtonFx.Intensity.IDLE.strength());
    }

    @Test
    void anUnspentPointMakesTheGlowStrongerAndBreathe() {
        WormholeButtonFx.Intensity glow = AtlasTreeButton.intensity(1);

        assertSame(AtlasTreeButton.POINTS_TO_SPEND, glow);
        assertSame(glow, AtlasTreeButton.intensity(12));
        assertTrue(glow.strength() > WormholeButtonFx.Intensity.READY.strength());
        assertTrue(glow.pulseDepth() > WormholeButtonFx.Intensity.READY.pulseDepth());
    }

    @Test
    void theButtonSitsInTheTopCentre() {
        float buttonLeft = AtlasTreeButton.buttonLeft(SMALLEST_PANEL_WIDTH);

        assertEquals(SMALLEST_PANEL_WIDTH / 2f, buttonLeft + AtlasTreeButton.BUTTON_WIDTH / 2f, 1e-3f);
    }

    @Test
    void theButtonAndItsGlowClearTheRightHandToggles() {
        float buttonRight = AtlasTreeButton.buttonLeft(SMALLEST_PANEL_WIDTH) + AtlasTreeButton.BUTTON_WIDTH;
        float biomesLeft = SMALLEST_PANEL_WIDTH - AtlasScreen.PANEL_MARGIN - AtlasScreen.CHROME_PAD - AtlasScreen.CLOSE_WIDTH
                - AtlasScreen.BORDERS_WIDTH - AtlasScreen.BIOMES_WIDTH - AtlasScreen.BUTTON_GAP * 2f;
        float mouthReach = WormholeButtonFx.MOUTH_SIZE_SHARE * ChromeMetrics.CONTROL_HEIGHT / 2f;

        assertTrue(buttonRight + Math.max(WormholeButtonFx.GLOW_REACH, mouthReach) < biomesLeft);
    }

    @Test
    void theSearchBarSitsBetweenTheTitleAndTheButton() {
        float searchLeft = AtlasTreeButton.searchBarLeft(SMALLEST_PANEL_WIDTH, AtlasScreen.SEARCH_WIDTH);
        float buttonLeft = AtlasTreeButton.buttonLeft(SMALLEST_PANEL_WIDTH);

        assertEquals(buttonLeft - AtlasTreeButton.GLOW_ROOM, searchLeft + AtlasScreen.SEARCH_WIDTH, 1e-3f);
        assertTrue(AtlasTreeButton.GLOW_ROOM > WormholeButtonFx.GLOW_REACH);
        assertTrue(searchLeft > AtlasScreen.PANEL_MARGIN + AtlasScreen.CHROME_PAD + TITLE_ROOM);
    }
}
