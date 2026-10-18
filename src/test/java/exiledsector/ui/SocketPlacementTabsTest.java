package exiledsector.ui;

import exiledsector.ui.chart.StarChartTab;
import exiledsector.ui.socket.ItemStoragePanel;
import exiledsector.ui.socket.SocketPanelPlacement;
import exiledsector.ui.socket.SocketPanelTab;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketPlacementTabsTest {

    @Test
    void theSkillTreeShowsStarChartsBesideHullItemsEvenWhenNoShipIsInspected() {
        assertEquals(EnumSet.of(SocketPanelTab.HULL_ITEMS, SocketPanelTab.STAR_CHARTS), SocketPlacement.shownTabs(false));
        assertEquals(EnumSet.allOf(SocketPanelTab.class), SocketPlacement.shownTabs(true));
    }

    @Test
    void theChartDetailPanelStopsShortOfTheStatPanel() {
        float canvasWidth = 1920f;
        float statPanelWidth = 420f;
        SocketPanelPlacement storage = new SocketPanelPlacement(ChromeMetrics.EDGE_MARGIN, 60f, ItemStoragePanel.WIDTH, 800f, false);

        float roomRight = SocketPlacement.chartsRoomRight(canvasWidth, statPanelWidth);
        SocketPanelPlacement detail = StarChartTab.detailBeside(storage, roomRight, StarChartTab.DETAIL_WIDTH);

        assertEquals(canvasWidth - statPanelWidth - ChromeMetrics.EDGE_MARGIN, roomRight, 0.01f);
        assertTrue(detail.panelLeft() > storage.panelLeft() + storage.panelWidth());
        assertTrue(detail.panelLeft() + detail.panelWidth() <= roomRight);
        assertEquals(StarChartTab.DETAIL_WIDTH, detail.panelWidth(), 0.01f);
        assertEquals(storage.panelHeight(), detail.panelHeight(), 0.01f);
    }
}
