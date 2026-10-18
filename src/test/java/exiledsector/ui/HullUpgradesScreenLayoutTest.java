package exiledsector.ui;

import exiledsector.ui.section.SectionLayout;
import exiledsector.ui.refit.RefitScreenLayout;
import exiledsector.ui.chart.StarChartTab;
import exiledsector.ui.socket.ItemStoragePanel;
import exiledsector.ui.socket.SocketPanelPlacement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HullUpgradesScreenLayoutTest {

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
        assertEquals(-5000f, SectionsDisplay.SectionBounds.UNBOUNDED.clampX(-5000f), 0.001f);
    }

    @Test
    void theShipSettlesAgainstTheRightEdgeAndThePanelTakesTheRoomLeftOfTheSocketColumn() {
        SectionLayout.ShipExtent capital = new SectionLayout.ShipExtent(160f, 180f);
        HullUpgradesLayout screenLayout = HullUpgradesLayout.of(WIDE_FRAME, capital);
        float frameRight = WIDE_FRAME.frameX() + WIDE_FRAME.frameWidth();

        assertEquals(frameRight - HullUpgradesLayout.SHIP_EDGE_MARGIN - 160f, screenLayout.settledShipX(), 0.001f);
        assertTrue(screenLayout.panelWidth() > ItemStoragePanel.COMPACT_WIDTH, "a wide frame widens the panel: " + screenLayout.panelWidth());
        float leftSocketX = SectionLayout.sectionX(new SectionLayout.Placement(SectionLayout.Side.LEFT, 0, 1),
                screenLayout.settledShipX(), capital);
        assertEquals(leftSocketX, screenLayout.sectionBounds().clampX(leftSocketX), 0.001f, "the left column is not pushed by the panel");
    }

    @Test
    void aSmallShipNeverStretchesThePanelPastItsCap() {
        HullUpgradesLayout screenLayout = HullUpgradesLayout.of(WIDE_FRAME, new SectionLayout.ShipExtent(30f, 50f));

        assertEquals(HullUpgradesLayout.MAX_PANEL_WIDTH, screenLayout.panelWidth(), 0.001f);
    }

    @Test
    void evenTheSmallestRefitFrameKeepsTheCompactPanelWidthAndRoomForASocketColumn() {
        HullUpgradesLayout screenLayout = HullUpgradesLayout.of(NARROW_FRAME, new SectionLayout.ShipExtent(200f, 250f));
        SectionsDisplay.SectionBounds bounds = screenLayout.sectionBounds();

        assertEquals(ItemStoragePanel.COMPACT_WIDTH, screenLayout.panelWidth(), 0.001f);
        assertTrue(bounds.minCentreX() <= bounds.maxCentreX(), "band " + bounds.minCentreX() + ".." + bounds.maxCentreX());
        assertTrue(bounds.minCentreY() < bounds.maxCentreY());
    }

    @Test
    void theStarChartsTabKeepsTheItemsPanelPlaceAndPutsTheChartDetailOverTheShipInsideTheFrame() {
        for (RefitScreenLayout frame : List.of(WIDE_FRAME, NARROW_FRAME)) {
            HullUpgradesLayout screenLayout = HullUpgradesLayout.of(frame, new SectionLayout.ShipExtent(160f, 180f));
            SocketPanelPlacement storage = new SocketPanelPlacement(screenLayout.panelLeft(), 20f, screenLayout.panelWidth(), 700f, true);
            float roomRight = HullUpgradesLayout.chartsRoomRight(frame);

            SocketPanelPlacement detail = StarChartTab.detailBeside(storage, roomRight, Float.MAX_VALUE);

            assertEquals(frame.frameX() + frame.frameWidth() - HullUpgradesLayout.PANEL_MARGIN, roomRight, 0.001f);
            assertTrue(detail.panelLeft() > screenLayout.panelRight(), "the detail sits right of the storage panel");
            assertEquals(roomRight, detail.panelLeft() + detail.panelWidth(), 0.001f);
            assertTrue(detail.panelWidth() >= 360f, "room for the currency row: " + detail.panelWidth());
            assertEquals(storage.panelTop(), detail.panelTop(), 0.001f);
            assertEquals(storage.panelHeight(), detail.panelHeight(), 0.001f);
        }
    }
}
