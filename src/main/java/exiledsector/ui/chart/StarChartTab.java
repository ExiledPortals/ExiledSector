package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import exiledsector.charts.StarCharts;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.ui.socket.SocketPanelPlacement;
import exiledsector.ui.socket.SocketPanelTab;
import exiledsector.ui.socket.StarChartDetailPanel;
import exiledsector.ui.socket.StarChartStoragePanel;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class StarChartTab {

    public interface Listener {

        void closed();

        void wormholeOpened(String message);

        void pressedInside();
    }

    public static final float DETAIL_WIDTH = 540f;
    static final float MIN_DETAIL_WIDTH = 360f;
    static final float PANEL_GAP = 12f;

    private final StarChartBrowser browser = new StarChartBrowser();
    private final Listener hostListener;
    private ItemStore chartStore;
    private StarChartStoragePanel storagePanel;
    private StarChartDetailPanel detailPanel;
    private boolean closedReported;
    private boolean wormholeOpened;

    StarChartTab(Listener hostListener) {
        this.hostListener = hostListener;
    }

    public static StarChartTab open(CustomPanelAPI hostPanel, SocketPanelPlacement storagePlacement, SocketPanelPlacement detailPlacement,
                                    Consumer<SocketPanelTab> tabListener, Set<SocketPanelTab> shownTabs, Listener hostListener) {
        StarChartTab chartTab = new StarChartTab(hostListener);
        int moved = prepare(Global.getSector(), chartTab.browser);
        chartTab.chartStore = ItemStore.starChartsIfCreated();
        chartTab.storagePanel = StarChartStoragePanel.open(hostPanel, storagePlacement, chartTab.browser, chartTab.new StorageListener(),
                movedNotice(moved), tabListener, shownTabs);
        chartTab.detailPanel = StarChartDetailPanel.open(hostPanel, detailPlacement, chartTab.chartStore, StarChartOpening.FROM_STORE,
                chartTab.browser.selectedChart(), chartTab.new DetailListener());
        return chartTab;
    }

    public static SocketPanelPlacement detailBeside(SocketPanelPlacement storagePlacement, float roomRight, float maxWidth) {
        float detailLeft = storagePlacement.panelLeft() + storagePlacement.panelWidth() + PANEL_GAP;
        float detailWidth = Math.max(MIN_DETAIL_WIDTH, Math.min(maxWidth, roomRight - detailLeft));
        return new SocketPanelPlacement(detailLeft, storagePlacement.panelTop(), detailWidth, storagePlacement.panelHeight(),
                storagePlacement.instantly());
    }

    static int prepare(SectorAPI sector, StarChartBrowser browser) {
        int moved = sector == null ? 0 : StarCharts.absorbCharts(sector);
        ItemStore store = ItemStore.starChartsIfCreated();
        browser.reload(store == null ? List.of() : store.owned());
        return moved;
    }

    static String movedNotice(int moved) {
        return moved > 0 ? I18n.forGameText(() -> Translation.msg("chart.ui.storage.moved").count(moved).text()) : null;
    }

    public boolean escape() {
        return detailPanel != null && detailPanel.escape();
    }

    public void close() {
        if (storagePanel != null) {
            storagePanel.close();
        }
        if (detailPanel != null) {
            detailPanel.close();
        }
    }

    public void swapOut() {
        if (storagePanel != null) {
            storagePanel.swapOut();
        }
        if (detailPanel != null) {
            detailPanel.swapOut();
        }
    }

    public boolean contains(float x, float y) {
        return (storagePanel != null && storagePanel.contains(x, y)) || (detailPanel != null && detailPanel.contains(x, y));
    }

    public boolean isVisible() {
        return (storagePanel != null && storagePanel.isVisible()) || (detailPanel != null && detailPanel.isVisible());
    }

    public void hideTooltips() {
        if (storagePanel != null) {
            storagePanel.hideTooltip();
        }
        if (detailPanel != null) {
            detailPanel.hideTooltip();
        }
    }

    public void setShownTabs(Set<SocketPanelTab> shownTabs) {
        if (storagePanel != null) {
            storagePanel.setShownTabs(shownTabs);
        }
    }

    boolean hasOpenedWormhole() {
        return wormholeOpened;
    }

    void chartClicked(Item chart) {
        if (wormholeOpened || detailPanel == null) {
            return;
        }
        browser.toggle(chart);
        detailPanel.show(browser.selectedChart());
    }

    void chartChanged(Item selectedAfter) {
        if (chartStore == null) {
            chartStore = ItemStore.starChartsIfCreated();
        }
        browser.reload(chartStore == null ? List.of() : chartStore.owned());
        browser.select(selectedAfter);
        if (storagePanel != null) {
            storagePanel.refresh();
        }
    }

    void storageClosed() {
        if (detailPanel != null) {
            detailPanel.close();
        }
        if (!closedReported) {
            closedReported = true;
            hostListener.closed();
        }
    }

    void wormholeOpened(String message) {
        if (wormholeOpened) {
            return;
        }
        wormholeOpened = true;
        hostListener.wormholeOpened(message);
    }

    private final class StorageListener implements StarChartStoragePanel.Listener {

        @Override
        public void chartClicked(Item chart) {
            StarChartTab.this.chartClicked(chart);
        }

        @Override
        public void closed() {
            storageClosed();
        }

        @Override
        public void pressedInside() {
            if (detailPanel != null) {
                detailPanel.hideTooltip();
            }
            hostListener.pressedInside();
        }
    }

    private final class DetailListener implements StarChartDetailPanel.Listener {

        @Override
        public void chartChanged(Item selectedAfter) {
            StarChartTab.this.chartChanged(selectedAfter);
        }

        @Override
        public void wormholeOpened(String message) {
            StarChartTab.this.wormholeOpened(message);
        }

        @Override
        public void pressedInside() {
            if (storagePanel != null) {
                storagePanel.hideTooltip();
            }
            hostListener.pressedInside();
        }
    }
}
