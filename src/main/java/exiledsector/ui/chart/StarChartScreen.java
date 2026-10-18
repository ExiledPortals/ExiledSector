package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.StarCharts;
import exiledsector.i18n.Translation;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.ui.CampaignMessages;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.socket.SocketPanelPlacement;
import exiledsector.ui.socket.StarChartDetailPanel;
import exiledsector.ui.socket.StarChartStoragePanel;
import org.lwjgl.input.Keyboard;

import java.util.List;

public final class StarChartScreen implements CustomVisualDialogDelegate {

    static final float SCREEN_MARGIN = 60f;
    static final float OPTION_ROOM = 60f;
    static final float MAX_WIDTH = 1400f;
    static final float MAX_HEIGHT = 860f;
    static final float DETAIL_WIDTH = 540f;
    static final float PANEL_GAP = 12f;

    public record Layout(float storageWidth, float detailLeft, float detailWidth, float panelHeight) {

        static Layout within(float dialogWidth, float dialogHeight) {
            float detailWidth = Math.min(DETAIL_WIDTH, dialogWidth / 2f);
            float storageWidth = dialogWidth - detailWidth - PANEL_GAP;
            return new Layout(storageWidth, storageWidth + PANEL_GAP, detailWidth, dialogHeight);
        }
    }

    private final StarChartBrowser browser = new StarChartBrowser();
    private final Runnable onDismissed;
    private final ScreenInput screenInput = new ScreenInput();
    private CustomPanelAPI hostPanel;
    private DialogCallbacks dialogCallbacks;
    private ItemStore chartStore;
    private StarChartStoragePanel storagePanel;
    private StarChartDetailPanel detailPanel;
    private String closingMessage;
    private boolean closing;
    private boolean dismissRequested;

    StarChartScreen(Runnable onDismissed) {
        this.onDismissed = onDismissed;
    }

    public static boolean open() {
        SectorAPI sector = Global.getSector();
        CampaignUIAPI campaignUi = sector == null ? null : sector.getCampaignUI();
        if (campaignUi == null || sector.getPlayerFleet() == null) {
            return false;
        }
        InteractionDialogAPI currentDialog = campaignUi.getCurrentInteractionDialog();
        if (currentDialog != null) {
            currentDialog.showCustomVisualDialog(dialogWidth(), dialogHeight(), new StarChartScreen(null));
            return true;
        }
        return campaignUi.showInteractionDialog(dialogPlugin(), sector.getPlayerFleet());
    }

    public static InteractionDialogPlugin dialogPlugin() {
        return new StarChartDialogPlugin();
    }

    static float dialogWidth() {
        return width(Global.getSettings().getScreenWidth());
    }

    static float dialogHeight() {
        return height(Global.getSettings().getScreenHeight());
    }

    static float width(float screenWidth) {
        return Math.min(MAX_WIDTH, screenWidth - SCREEN_MARGIN * 2f);
    }

    static float height(float screenHeight) {
        return Math.min(MAX_HEIGHT, screenHeight - SCREEN_MARGIN * 2f - OPTION_ROOM);
    }

    static StarChartScreen forDialog(InteractionDialogAPI dialog) {
        return new StarChartScreen(dialog::dismiss);
    }

    static int prepare(SectorAPI sector, StarChartBrowser browser) {
        int moved = sector == null ? 0 : StarCharts.absorbCharts(sector);
        ItemStore store = ItemStore.starChartsIfCreated();
        browser.reload(store == null ? List.of() : store.owned());
        return moved;
    }

    static String movedNotice(int moved) {
        return moved > 0 ? Translation.msg("chart.ui.storage.moved").count(moved).text() : null;
    }

    @Override
    public void init(CustomPanelAPI dialogPanel, DialogCallbacks dialogCallbacks) {
        this.hostPanel = dialogPanel;
        this.dialogCallbacks = dialogCallbacks;
        int moved = prepare(Global.getSector(), browser);
        chartStore = ItemStore.starChartsIfCreated();
        Layout layout = Layout.within(dialogPanel.getPosition().getWidth(), dialogPanel.getPosition().getHeight());
        storagePanel = StarChartStoragePanel.open(hostPanel, new SocketPanelPlacement(0f, 0f, layout.storageWidth(), layout.panelHeight(), false),
                browser, new StorageListener(), movedNotice(moved));
        detailPanel = StarChartDetailPanel.open(hostPanel, new SocketPanelPlacement(layout.detailLeft(), 0f, layout.detailWidth(),
                layout.panelHeight(), false), chartStore, StarChartOpening.FROM_STORE, browser.selectedChart(), new DetailListener());
        SkillTreeSounds.panelOpened();
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return screenInput;
    }

    @Override
    public float getNoiseAlpha() {
        return 0f;
    }

    @Override
    public void advance(float amount) {
        if (!closing || dismissRequested || dialogCallbacks == null) {
            return;
        }
        boolean panelsGone = (storagePanel == null || !storagePanel.isVisible()) && (detailPanel == null || !detailPanel.isVisible());
        if (panelsGone) {
            dismissRequested = true;
            dialogCallbacks.dismissDialog();
        }
    }

    @Override
    public void reportDismissed(int option) {
        if (closingMessage != null) {
            CampaignMessages.plain(closingMessage, Misc.getPositiveHighlightColor());
            closingMessage = null;
        }
        if (onDismissed != null) {
            onDismissed.run();
        }
    }

    private void close() {
        if (closing) {
            return;
        }
        closing = true;
        if (storagePanel != null) {
            storagePanel.close();
        }
        if (detailPanel != null) {
            detailPanel.close();
        }
    }

    private void escape() {
        if (detailPanel != null && detailPanel.escape()) {
            return;
        }
        close();
    }

    private void chartClicked(Item chart) {
        if (closing) {
            return;
        }
        browser.toggle(chart);
        detailPanel.show(browser.selectedChart());
    }

    private void chartChanged(Item selectedAfter) {
        if (chartStore == null) {
            chartStore = ItemStore.starChartsIfCreated();
        }
        browser.reload(chartStore == null ? List.of() : chartStore.owned());
        browser.select(selectedAfter);
        storagePanel.refresh();
    }

    private final class StorageListener implements StarChartStoragePanel.Listener {

        @Override
        public void chartClicked(Item chart) {
            StarChartScreen.this.chartClicked(chart);
        }

        @Override
        public void closed() {
            close();
        }

        @Override
        public void pressedInside() {
            if (detailPanel != null) {
                detailPanel.hideTooltip();
            }
        }
    }

    private final class DetailListener implements StarChartDetailPanel.Listener {

        @Override
        public void chartChanged(Item selectedAfter) {
            StarChartScreen.this.chartChanged(selectedAfter);
        }

        @Override
        public void wormholeOpened(String message) {
            closingMessage = message;
            close();
        }

        @Override
        public void pressedInside() {
            if (storagePanel != null) {
                storagePanel.hideTooltip();
            }
        }
    }

    private final class ScreenInput extends BaseCustomUIPanelPlugin {

        @Override
        public void processInput(List<InputEventAPI> events) {
            for (InputEventAPI event : events) {
                if (event.isConsumed() || !event.isKeyboardEvent() || event.getEventValue() != Keyboard.KEY_ESCAPE) {
                    continue;
                }
                if (event.isKeyDownEvent() && !closing) {
                    escape();
                }
                event.consume();
            }
        }
    }
}
