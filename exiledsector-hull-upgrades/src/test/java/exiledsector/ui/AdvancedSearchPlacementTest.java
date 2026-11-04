package exiledsector.ui;

import exiledsector.ui.refit.RefitScreenLayout;
import exiledsector.ui.section.SectionLayout;
import exiledsector.ui.socket.AdvancedSearchLayout;
import exiledsector.ui.socket.FreeArea;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedSearchPlacementTest {

    private static final float FLEET_LIST_WIDTH = 210f;
    private static final float FRAME_BORDER = 1f;
    private static final float FRAME_WIDTH = 1252f;
    private static final float FRAME_HEIGHT = 852f;
    private static final SectionLayout.ShipExtent CRUISER = new SectionLayout.ShipExtent(100f, 180f);

    private record Screen(float screenHeight, HullItemsLayout screenLayout, float panelTop, float panelHeight, AdvancedSearchLayout pane) {

        float panelBottom() {
            return panelTop + panelHeight;
        }
    }

    private static Screen screen(float screenWidth, float screenHeight) {
        float frameHeight = Math.min(FRAME_HEIGHT, Math.max(736f, screenHeight - 74f + 2f));
        float frameX = (screenWidth - FRAME_WIDTH) / 2f;
        float frameY = (screenHeight - frameHeight) / 2f;
        RefitScreenLayout refit = new RefitScreenLayout(frameX, frameY, FRAME_WIDTH, frameHeight, frameX + FRAME_BORDER + FLEET_LIST_WIDTH,
                frameY + 4f, FRAME_WIDTH - FLEET_LIST_WIDTH - 5f, frameHeight - 8f, 1f);
        HullItemsLayout screenLayout = HullItemsLayout.of(refit, CRUISER);
        float panelTop = screenHeight - refit.frameTop() + HullItemsLayout.PANEL_MARGIN;
        float panelHeight = refit.frameHeight() - HullItemsLayout.PANEL_MARGIN * 2f;
        FreeArea area = screenLayout.advancedSearchArea(panelTop, panelHeight);
        return new Screen(screenHeight, screenLayout, panelTop, panelHeight, AdvancedSearchLayout.in(area));
    }

    private static void assertPlacedLeftOfThePanel(Screen screen) {
        AdvancedSearchLayout pane = screen.pane();
        assertEquals(screen.screenLayout().panelLeft() - HullItemsLayout.PANEL_MARGIN, pane.paneLeft() + pane.paneWidth(), 0.001f);
        assertTrue(pane.paneLeft() >= HullItemsLayout.SCREEN_MARGIN - 0.001f, "left " + pane.paneLeft());
        assertEquals(screen.panelBottom(), pane.paneBottom(), 0.001f);
        assertEquals(screen.panelTop(), pane.paneTop(), 0.001f, "the pane matches the hull items panel's height");
        assertEquals(HullItemsLayout.SCREEN_MARGIN, pane.paneLeft(), 0.001f, "the pane reaches the screen's left margin");
    }

    @Test
    void at1280WideThePaneTakesTheFleetListsRoomAndWrapsEachRowOntoTwoLines() {
        Screen screen = screen(1280f, 720f);
        AdvancedSearchLayout pane = screen.pane();

        assertPlacedLeftOfThePanel(screen);
        assertTrue(pane.paneWidth() >= FLEET_LIST_WIDTH, "width " + pane.paneWidth());
        assertTrue(pane.twoLineRows());
    }

    @Test
    void at1920WideThePaneFillsTheRoomLeftOfThePanelWithOneLineRows() {
        Screen screen = screen(1920f, 1080f);
        AdvancedSearchLayout pane = screen.pane();

        assertPlacedLeftOfThePanel(screen);
        assertEquals(screen.screenLayout().panelLeft() - HullItemsLayout.PANEL_MARGIN - HullItemsLayout.SCREEN_MARGIN, pane.paneWidth(), 0.001f);
        assertTrue(pane.paneWidth() > 520f && pane.paneWidth() < 540f, "width " + pane.paneWidth());
        assertFalse(pane.twoLineRows());
    }

    @Test
    void at2560WideThePaneKeepsGrowingToFillTheRoomLeftOfThePanel() {
        Screen screen = screen(2560f, 1440f);
        AdvancedSearchLayout pane = screen.pane();

        assertPlacedLeftOfThePanel(screen);
        assertEquals(screen.screenLayout().panelLeft() - HullItemsLayout.PANEL_MARGIN - HullItemsLayout.SCREEN_MARGIN, pane.paneWidth(), 0.001f);
        assertFalse(pane.twoLineRows());
    }
}
