package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import exiledsector.charts.StarCharts;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.socket.BenchMode;
import exiledsector.ui.socket.SocketPanelPlacement;
import exiledsector.ui.socket.SocketPanelTab;
import exiledsector.ui.socket.SocketWorkbenchPanel;
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

        default boolean openModifyScreen(Item chart) {
            return false;
        }
    }

    public static final float DETAIL_WIDTH = 540f;
    static final float MIN_DETAIL_WIDTH = 360f;
    static final float PANEL_GAP = 12f;

    private final StarChartBrowser browser = new StarChartBrowser();
    private final Listener hostListener;
    private CustomPanelAPI hostPanel;
    private SocketPanelPlacement detailPlacement;
    private SocketPanelPlacement benchPlacement;
    private ItemStore chartStore;
    private StarChartStoragePanel storagePanel;
    private StarChartDetailPanel detailPanel;
    private SocketWorkbenchPanel chartBench;
    private boolean closedReported;
    private boolean closing;
    private boolean wormholeOpened;

    StarChartTab(Listener hostListener) {
        this.hostListener = hostListener;
    }

    public static StarChartTab open(CustomPanelAPI hostPanel, SocketPanelPlacement storagePlacement, SocketPanelPlacement detailPlacement,
                                    SocketPanelPlacement benchPlacement, Consumer<SocketPanelTab> tabListener, Set<SocketPanelTab> shownTabs,
                                    Listener hostListener) {
        StarChartTab chartTab = new StarChartTab(hostListener);
        int moved = prepare(Global.getSector(), chartTab.browser);
        chartTab.hostPanel = hostPanel;
        chartTab.detailPlacement = detailPlacement;
        chartTab.benchPlacement = benchPlacement;
        chartTab.chartStore = ItemStore.starChartsIfCreated();
        chartTab.storagePanel = StarChartStoragePanel.open(hostPanel, storagePlacement, chartTab.browser, chartTab.new StorageListener(),
                movedNotice(moved), tabListener, shownTabs);
        chartTab.openDetail(detailPlacement);
        return chartTab;
    }

    public static SocketPanelPlacement detailBeside(SocketPanelPlacement storagePlacement, float roomRight, float maxWidth) {
        float detailLeft = storagePlacement.panelLeft() + storagePlacement.panelWidth() + PANEL_GAP;
        float detailWidth = Math.max(MIN_DETAIL_WIDTH, Math.min(maxWidth, roomRight - detailLeft));
        return new SocketPanelPlacement(detailLeft, storagePlacement.panelTop(), detailWidth, storagePlacement.panelHeight(),
                storagePlacement.instantly());
    }

    public static int prepare(SectorAPI sector, StarChartBrowser browser) {
        int moved = sector == null ? 0 : StarCharts.absorbCharts(sector);
        ItemStore store = ItemStore.starChartsIfCreated();
        browser.reload(store == null ? List.of() : store.owned());
        return moved;
    }

    public static String movedNotice(int moved) {
        return moved > 0 ? I18n.forGameText(() -> Translation.msg("chart.ui.storage.moved").count(moved).text()) : null;
    }

    private void openDetail(SocketPanelPlacement placement) {
        detailPanel = StarChartDetailPanel.open(hostPanel, placement, chartStore, StarChartOpening.FROM_STORE, browser.selectedChart(),
                new DetailListener());
    }

    public boolean escape() {
        if (chartBench != null) {
            if (!chartBench.escape()) {
                chartBench.close();
            }
            return true;
        }
        return detailPanel != null && detailPanel.escape();
    }

    public void close() {
        closing = true;
        if (chartBench != null) {
            chartBench.close();
        }
        if (storagePanel != null) {
            storagePanel.close();
        }
        if (detailPanel != null) {
            detailPanel.close();
        }
    }

    public void swapOut() {
        closing = true;
        if (chartBench != null) {
            chartBench.swapOut();
            chartBench = null;
        }
        if (storagePanel != null) {
            storagePanel.swapOut();
        }
        if (detailPanel != null) {
            detailPanel.swapOut();
        }
    }

    public boolean contains(float x, float y) {
        return (storagePanel != null && storagePanel.contains(x, y)) || (detailPanel != null && detailPanel.contains(x, y))
                || (chartBench != null && chartBench.contains(x, y));
    }

    public boolean isVisible() {
        return (storagePanel != null && storagePanel.isVisible()) || (detailPanel != null && detailPanel.isVisible())
                || (chartBench != null && chartBench.isVisible());
    }

    public boolean isWorkbenchOpen() {
        return chartBench != null;
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
        if (wormholeOpened) {
            return;
        }
        if (chartBench != null) {
            browser.select(chart);
            chartBench.load(chart);
            if (storagePanel != null) {
                storagePanel.refresh();
            }
            return;
        }
        if (detailPanel == null) {
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

    void modifyRequested() {
        if (closing || wormholeOpened || chartBench != null || browser.selectedChart() == null) {
            return;
        }
        Item selected = browser.selectedChart();
        if (hostListener.openModifyScreen(selected)) {
            return;
        }
        if (detailPanel != null) {
            detailPanel.swapOut();
            detailPanel = null;
        }
        SkillTreeSounds.panelOpened();
        chartBench = SocketWorkbenchPanel.open(hostPanel, benchPlacement, false, BenchMode.STAR_CHARTS, new BenchListener());
        chartBench.load(selected);
    }

    void benchClosed() {
        chartBench = null;
        if (!closing && detailPanel == null) {
            openDetail(new SocketPanelPlacement(detailPlacement.panelLeft(), detailPlacement.panelTop(), detailPlacement.panelWidth(),
                    detailPlacement.panelHeight(), true));
        }
    }

    void storageClosed() {
        closing = true;
        if (chartBench != null) {
            chartBench.close();
        }
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

        @Override
        public void modifyRequested() {
            StarChartTab.this.modifyRequested();
        }
    }

    private final class BenchListener implements SocketWorkbenchPanel.Listener {

        @Override
        public void changed(Item loaded) {
            chartChanged(loaded);
        }

        @Override
        public void closed() {
            benchClosed();
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
