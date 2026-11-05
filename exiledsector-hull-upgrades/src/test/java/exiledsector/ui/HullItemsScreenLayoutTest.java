package exiledsector.ui;

import exiledsector.ui.section.SectionLayout;
import exiledsector.ui.refit.RefitScreenLayout;
import exiledsector.ui.socket.ItemStoragePanel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HullItemsScreenLayoutTest {

    private static final RefitScreenLayout WIDE_FRAME = new RefitScreenLayout(334f, 114f, 1252f, 852f, 545f, 115f, 1037f, 850f, 1f);
    private static final RefitScreenLayout NARROW_FRAME = new RefitScreenLayout(7f, 100f, RefitScreenLayout.MIN_TAB_WIDTH + 2f, 852f,
            218f, 101f, 790f, 850f, 1f);

    @Test
    void gridLinesAreCentredOnTheShipSoTheyLineUpWithTheRefitGrid() {
        assertArrayEquals(new float[]{50f, 75f, 100f, 125f, 150f}, RefitBackdrop.gridLines(100f, 40f, 160f), 0.001f);
    }

    @Test
    void boundsPullSocketsInsideTheFrameAndLeaveOthersAlone() {
        SectionsDisplay.SectionBounds bounds = new SectionsDisplay.SectionBounds(300f, 900f, 100f, 700f);

        assertEquals(300f, bounds.clampX(120f), 0.001f);
        assertEquals(900f, bounds.clampX(1200f), 0.001f);
        assertEquals(500f, bounds.clampX(500f), 0.001f);
        assertEquals(700f, bounds.clampY(900f), 0.001f);
    }

    @Test
    void theShipSettlesAgainstTheRightEdgeAndThePanelTakesTheRoomLeftOfTheSocketColumn() {
        SectionLayout.ShipExtent cruiser = new SectionLayout.ShipExtent(100f, 180f);
        HullItemsLayout screenLayout = HullItemsLayout.of(WIDE_FRAME, cruiser);
        float frameRight = WIDE_FRAME.frameX() + WIDE_FRAME.frameWidth();

        assertEquals(frameRight - HullItemsLayout.SHIP_EDGE_MARGIN - SectionLayout.SECTION_RADIUS - HullItemsLayout.SECTION_LABEL_OVERHANG,
                screenLayout.settledShipX(), 0.001f);
        assertTrue(screenLayout.panelWidth() > ItemStoragePanel.COMPACT_WIDTH, "a wide frame widens the panel: " + screenLayout.panelWidth());
        float leftSocketX = SectionLayout.sectionX(new SectionLayout.Placement(SectionLayout.Side.LEFT, 0, 1),
                screenLayout.settledShipX(), cruiser);
        assertEquals(leftSocketX, screenLayout.sectionBounds().clampX(leftSocketX), 0.001f, "the left column is not pushed by the panel");
    }

    @Test
    void thePanelStartsRightOfTheVanillaFleetList() {
        HullItemsLayout screenLayout = HullItemsLayout.of(WIDE_FRAME, new SectionLayout.ShipExtent(100f, 180f));

        assertEquals(WIDE_FRAME.fleetListRight() + HullItemsLayout.PANEL_MARGIN, screenLayout.panelLeft(), 0.001f);
        assertTrue(WIDE_FRAME.insideFleetList(WIDE_FRAME.frameX() + 50f, 500f));
        assertFalse(WIDE_FRAME.insideFleetList(screenLayout.panelLeft(), 500f));
    }

    @Test
    void aSmallShipNeverStretchesThePanelPastItsCap() {
        RefitScreenLayout extraWideFrame = new RefitScreenLayout(0f, 114f, 2000f, 852f, 211f, 115f, 1785f, 850f, 1f);
        HullItemsLayout screenLayout = HullItemsLayout.of(extraWideFrame, new SectionLayout.ShipExtent(30f, 50f));

        assertEquals(HullItemsLayout.MAX_PANEL_WIDTH, screenLayout.panelWidth(), 0.001f);
    }

    @Test
    void theWidestVanillaCapitalKeepsTheRefitZoom() {
        SectionLayout.ShipExtent invictus = new SectionLayout.ShipExtent(182f, 210f);

        assertEquals(1f, HullItemsLayout.settledShipZoom(WIDE_FRAME, invictus), 0.001f);
    }

    @Test
    void anAbsurdlyLargeHullShrinksUntilASocketColumnFitsBesideTheCompactPanel() {
        SectionLayout.ShipExtent supercapital = new SectionLayout.ShipExtent(480f, 520f);
        float zoom = HullItemsLayout.settledShipZoom(WIDE_FRAME, supercapital);
        SectionLayout.ShipExtent settled = supercapital.scaled(zoom);
        HullItemsLayout screenLayout = HullItemsLayout.of(WIDE_FRAME, settled);
        float hullLeftEdge = screenLayout.settledShipX() - settled.halfWidth();
        float socketColumnWidth = SectionLayout.SECTION_RADIUS * 2f + HullItemsLayout.SECTION_LABEL_OVERHANG;

        assertTrue(zoom < 1f, "zoom " + zoom);
        assertEquals(HullItemsLayout.maxShipHalfWidth(WIDE_FRAME), settled.halfWidth(), 0.001f);
        assertTrue(hullLeftEdge - socketColumnWidth >= screenLayout.panelLeft() + ItemStoragePanel.COMPACT_WIDTH
                + HullItemsLayout.SECTION_BAND_MARGIN - 0.001f, "hull's left edge at " + hullLeftEdge);
    }

    @Test
    void anOverlyLongHullShrinksToFitTheFrameHeight() {
        SectionLayout.ShipExtent longHull = new SectionLayout.ShipExtent(100f, 600f);
        float zoom = HullItemsLayout.settledShipZoom(WIDE_FRAME, longHull);

        assertEquals(HullItemsLayout.maxShipHalfLength(WIDE_FRAME), longHull.scaled(zoom).halfLength(), 0.001f);
        assertTrue(longHull.scaled(zoom).halfLength() < WIDE_FRAME.frameHeight() / 2f);
    }

    @Test
    void aShipZoomedOutOnTheRefitScreenGrowsBackToItsNaturalSize() {
        RefitScreenLayout zoomedOut = new RefitScreenLayout(334f, 114f, 1252f, 852f, 545f, 115f, 1037f, 850f, 0.6f);

        assertEquals(HullItemsLayout.NATURAL_SHIP_ZOOM, HullItemsLayout.settledShipZoom(zoomedOut, new SectionLayout.ShipExtent(100f, 180f)), 0.001f);
    }

    @Test
    void aSmallShipZoomedInOnTheRefitScreenKeepsThatZoom() {
        RefitScreenLayout zoomedIn = new RefitScreenLayout(334f, 114f, 1252f, 852f, 545f, 115f, 1037f, 850f, 1.5f);

        assertEquals(1.5f, HullItemsLayout.settledShipZoom(zoomedIn, new SectionLayout.ShipExtent(30f, 50f)), 0.001f);
    }

    @Test
    void evenTheSmallestRefitFrameKeepsTheCompactPanelWidthAndRoomForASocketColumn() {
        HullItemsLayout screenLayout = HullItemsLayout.of(NARROW_FRAME, new SectionLayout.ShipExtent(200f, 250f));
        SectionsDisplay.SectionBounds bounds = screenLayout.sectionBounds();

        assertEquals(ItemStoragePanel.COMPACT_WIDTH, screenLayout.panelWidth(), 0.001f);
        assertTrue(bounds.minCentreX() <= bounds.maxCentreX(), "band " + bounds.minCentreX() + ".." + bounds.maxCentreX());
        assertTrue(bounds.minCentreY() < bounds.maxCentreY());
    }
}
