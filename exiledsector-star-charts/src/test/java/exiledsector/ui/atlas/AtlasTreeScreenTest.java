package exiledsector.ui.atlas;

import exiledsector.ui.CanvasSearchBar;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasTreeScreenTest {

    private static final float SMALLEST_PANEL_WIDTH = 1280f;

    @Test
    void thePointsBarMentionsUnusedPointsOnlyWhenThereAreAny() {
        assertEquals("atlasTree.pointsBar", AtlasTreeScreen.pointsBarKey(0));
        assertEquals("atlasTree.pointsBarUnused", AtlasTreeScreen.pointsBarKey(1));
    }

    @Test
    void thePointsBarFitsBesideTheCentredSearchBar() {
        float barRight = AtlasScreen.PANEL_MARGIN + AtlasScreen.CHROME_PAD + AtlasTreeScreen.POINTS_BAR_WIDTH;
        float innerWidth = SMALLEST_PANEL_WIDTH - AtlasScreen.PANEL_MARGIN * 2f;
        float searchLeft = AtlasScreen.PANEL_MARGIN + (innerWidth - CanvasSearchBar.DEFAULT_WIDTH) / 2f;

        assertTrue(barRight < searchLeft);
    }
}
