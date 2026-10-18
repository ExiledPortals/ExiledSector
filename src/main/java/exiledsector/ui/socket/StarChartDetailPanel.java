package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.StarChartSettings;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.Item;
import exiledsector.items.ItemName;
import exiledsector.items.ItemStore;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.VanillaText;
import exiledsector.ui.chart.StarChartOpening;
import exiledsector.ui.chart.StarChartPreviewText;
import exiledsector.ui.chart.WormholeOpenCheck;

import java.awt.Color;
import java.util.List;
import java.util.Random;

public final class StarChartDetailPanel extends HoloPanel {

    public interface Listener {

        void wormholeOpened(String message);

        void pressedInside();

        void modifyRequested();
    }

    private static final float MODIFY_BUTTON_HEIGHT = 30f;
    private static final float NOTICE_HEIGHT = 36f;
    private static final float REASON_HEIGHT = 20f;
    private static final float OPEN_BUTTON_HEIGHT = 44f;
    private static final float CONFIRM_HEIGHT = 170f;
    private static final float TOOLTIP_WIDTH = 340f;

    private enum Control {MODIFY, OPEN_WORMHOLE, CONFIRM_OPEN, CANCEL_CONFIRM}

    private final Listener detailListener;
    private final ItemStore chartStore;
    private final StarChartOpening.Opener opener;
    private final Random openingRandom = new Random();
    private UIComponentAPI headerElement;
    private UIComponentAPI detailsElement;
    private UIComponentAPI modifyElement;
    private UIComponentAPI noticeElement;
    private UIComponentAPI openElement;
    private Item shownChart;
    private Item pendingChart;
    private String noticeText;
    private boolean noticeIsWarning;
    private boolean wormholeOpened;

    private StarChartDetailPanel(CustomPanelAPI hostPanel, ItemStore chartStore, StarChartOpening.Opener opener, Listener detailListener) {
        super(hostPanel);
        this.chartStore = chartStore;
        this.opener = opener;
        this.detailListener = detailListener;
    }

    public static StarChartDetailPanel open(CustomPanelAPI hostPanel, SocketPanelPlacement placement, ItemStore chartStore,
                                            StarChartOpening.Opener opener, Item selectedChart, Listener detailListener) {
        StarChartDetailPanel detailPanel = new StarChartDetailPanel(hostPanel, chartStore, opener, detailListener);
        detailPanel.shownChart = selectedChart;
        detailPanel.attach(placement.panelLeft(), placement.panelTop(), placement.panelWidth(), placement.panelHeight(), placement.instantly());
        return detailPanel;
    }

    public void show(Item chart) {
        queue(() -> {
            if (chart == shownChart) {
                return;
            }
            shownChart = chart;
            noticeText = null;
            cancelConfirm();
            rebuildContent();
        });
    }

    public void hideTooltip() {
        queue(hoverTooltip::hide);
    }

    public boolean escape() {
        if (isConfirmOpen()) {
            queue(this::cancelConfirm);
            return true;
        }
        return false;
    }

    @Override
    protected void onClose() {
        hoverTooltip.hide();
    }

    @Override
    protected void onPressInside() {
        detailListener.pressedInside();
    }

    @Override
    protected void onScrollInside() {
        queue(hoverTooltip::hide);
    }

    @Override
    protected void advanceOpen(float amount) {
        hoverTooltip.refreshIfExpansionChanged();
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{detailsElement, modifyElement, noticeElement, openElement};
    }

    @Override
    public void buttonPressed(Object id) {
        queue(() -> handleButton(id));
    }

    private void handleButton(Object id) {
        if (wormholeOpened) {
            return;
        }
        if (id == Control.MODIFY) {
            if (!isConfirmOpen() && shownChart != null) {
                detailListener.modifyRequested();
            }
        } else if (id == Control.OPEN_WORMHOLE) {
            openPressed();
        } else if (id == Control.CONFIRM_OPEN) {
            confirmOpen();
        } else if (id == Control.CANCEL_CONFIRM) {
            cancelConfirm();
        }
    }

    @Override
    protected void build() {
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), HEADER_HEIGHT, false);
        element.setParaFont(Fonts.ORBITRON_20AABOLD);
        VanillaText.plainPara(element, Translation.text("chart.ui.detail.title"), 0f, Misc.getBasePlayerColor());
        headerElement = replaceElement(headerElement, element, PAD, PAD);
        rebuildContent();
    }

    private float innerWidth() {
        return panelWidth - PAD * 2f;
    }

    private float openTop() {
        return panelHeight - PAD - OPEN_BUTTON_HEIGHT - GAP - REASON_HEIGHT;
    }

    private float noticeTop() {
        return openTop() - GAP - NOTICE_HEIGHT;
    }

    private float modifyTop() {
        return noticeTop() - GAP - MODIFY_BUTTON_HEIGHT;
    }

    private static float detailsTop() {
        return PAD + HEADER_HEIGHT + GAP;
    }

    private void rebuildContent() {
        rebuildDetails();
        rebuildModifyButton();
        rebuildNotice();
        rebuildOpenButton();
        refreshOpacity();
    }

    private void rebuildDetails() {
        float detailsHeight = Math.max(OPEN_BUTTON_HEIGHT, modifyTop() - GAP - detailsTop());
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), detailsHeight, true);
        if (shownChart == null) {
            VanillaText.plainPara(element, Translation.text(chartStore == null || chartStore.owned().isEmpty()
                    ? "chart.ui.detail.emptyStorage" : "chart.ui.detail.empty"), 0f, Misc.getGrayColor());
        } else {
            writeDetails(element, shownChart);
        }
        detailsElement = placedComponent(replaceElement(detailsElement, element, PAD, detailsTop()));
    }

    private static void writeDetails(TooltipMakerAPI element, Item chart) {
        ItemName displayName = chart.displayName();
        element.setParaFont(Fonts.ORBITRON_20AA);
        VanillaText.plainPara(element, displayName.title(), 0f, displayName.rarity().color());
        element.setParaFontDefault();
        if (displayName.baseName() != null) {
            VanillaText.plainPara(element, displayName.baseName(), LINE_PAD, Misc.getGrayColor());
        }
        addLines(element, chart.headerLines(), Misc.getGrayColor(), GAP);
        addSection(element, Translation.text("hullItem.tooltip.implicit"), chart.implicitLines(true));
        addSection(element, Translation.text("socketable.tooltip.primaryData"), chart.effectLines(true));
        element.addSectionHeading(Translation.text("chart.ui.preview.heading"), Alignment.MID, GAP * 2f);
        List<StyledText> previewLines = StarChartPreviewText.modifierLines(chart);
        if (previewLines.isEmpty()) {
            VanillaText.plainPara(element, Translation.text("chart.ui.preview.neutral"), GAP, Misc.getGrayColor());
        } else {
            addLines(element, previewLines, Misc.getTextColor(), GAP);
        }
        VanillaText.addPara(element, StarChartPreviewText.tierLine(chart.tier()), GAP, Misc.getGrayColor());
    }

    private static void addSection(TooltipMakerAPI element, String heading, List<StyledText> lines) {
        if (lines.isEmpty()) {
            return;
        }
        element.addSectionHeading(heading, Alignment.MID, GAP * 2f);
        addLines(element, lines, Misc.getTextColor(), GAP);
    }

    private static void addLines(TooltipMakerAPI element, List<StyledText> lines, Color color, float firstPad) {
        float linePad = firstPad;
        for (StyledText line : lines) {
            VanillaText.addPara(element, line, linePad, color);
            linePad = LINE_PAD;
        }
    }

    private void rebuildModifyButton() {
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), MODIFY_BUTTON_HEIGHT, false);
        ButtonAPI modifyButton = element.addButton(Translation.text("ui.socketStorage.workbench"), Control.MODIFY, Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.ALL, innerWidth(), MODIFY_BUTTON_HEIGHT, 0f);
        modifyButton.setEnabled(shownChart != null && !wormholeOpened);
        modifyButton.setShowTooltipWhileInactive(true);
        element.addTooltipToPrevious(new GameTextTooltip(TOOLTIP_WIDTH, tooltip -> VanillaText.plainPara(tooltip,
                Translation.text(shownChart == null ? "chart.ui.modify.noChart" : "chart.ui.modify.tooltip"), 0f, Misc.getTextColor())),
                TooltipMakerAPI.TooltipLocation.ABOVE);
        modifyElement = replaceElement(modifyElement, element, PAD, modifyTop());
    }

    private void showNotice(String text, boolean warning) {
        noticeText = text;
        noticeIsWarning = warning;
        rebuildNotice();
        refreshOpacity();
    }

    private void rebuildNotice() {
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), NOTICE_HEIGHT, false);
        if (noticeText != null) {
            VanillaText.plainPara(element, noticeText, 0f, noticeIsWarning ? Misc.getNegativeHighlightColor() : Misc.getPositiveHighlightColor());
        }
        noticeElement = replaceElement(noticeElement, element, PAD, noticeTop());
    }

    private WormholeOpenCheck.Blocker blocker() {
        return WormholeOpenCheck.current(Global.getSector(), chartStore, shownChart);
    }

    private void rebuildOpenButton() {
        WormholeOpenCheck.Blocker blocker = blocker();
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), REASON_HEIGHT + GAP + OPEN_BUTTON_HEIGHT, false);
        String reason = blocker.allowsOpening() ? Translation.text("chart.ui.open.ready") : Translation.text(blocker.reasonKey());
        Color reasonColor = blocker.allowsOpening() ? Misc.getGrayColor() : Misc.getNegativeHighlightColor();
        VanillaText.plainPara(element, reason, 0f, reasonColor).setAlignment(Alignment.MID);
        element.setButtonFontOrbitron24Bold();
        ButtonAPI openButton = element.addButton(Translation.text("chart.ui.open.button"), Control.OPEN_WORMHOLE, Misc.getBrightPlayerColor(),
                Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.ALL, innerWidth(), OPEN_BUTTON_HEIGHT, 0f);
        openButton.getPosition().inTL(0f, REASON_HEIGHT + GAP);
        openButton.setEnabled(blocker.allowsOpening() && !wormholeOpened);
        openButton.setShowTooltipWhileInactive(true);
        element.addTooltipToPrevious(new GameTextTooltip(TOOLTIP_WIDTH, tooltip -> VanillaText.plainPara(tooltip, blocker.allowsOpening()
                ? Translation.text("chart.ui.open.tooltip") : Translation.text(blocker.reasonKey()), 0f, Misc.getTextColor())),
                TooltipMakerAPI.TooltipLocation.ABOVE);
        element.setButtonFontDefault();
        openElement = replaceElement(openElement, element, PAD, openTop());
    }

    private void openPressed() {
        WormholeOpenCheck.Blocker blocker = blocker();
        if (!blocker.allowsOpening()) {
            SkillTreeSounds.refused();
            showNotice(Translation.text(blocker.reasonKey()), true);
            rebuildOpenButton();
            refreshOpacity();
            return;
        }
        pendingChart = shownChart;
        int entries = StarChartSettings.wormholeEntries();
        Item confirmedChart = pendingChart;
        openConfirm(CONFIRM_HEIGHT, element -> writeConfirm(element, confirmedChart, entries), "chart.ui.open.confirm.yes", Control.CONFIRM_OPEN,
                "chart.ui.open.confirm.no", Control.CANCEL_CONFIRM);
    }

    private static void writeConfirm(TooltipMakerAPI element, Item chart, int entries) {
        VanillaText.plainPara(element, Translation.msg("chart.ui.open.confirm.title").arg("name", chart.name()).text(), 0f,
                chart.rarity().color());
        VanillaText.plainPara(element, Translation.text("chart.ui.open.confirm.body"), LINE_PAD * 2f, Misc.getTextColor());
        VanillaText.plainPara(element, Translation.msg("chart.ui.open.confirm.entries").count(entries).text(), LINE_PAD * 2f,
                Misc.getGrayColor());
    }

    private void cancelConfirm() {
        pendingChart = null;
        closeConfirm();
    }

    private void confirmOpen() {
        Item chart = pendingChart;
        cancelConfirm();
        if (chart == null || chart != shownChart) {
            return;
        }
        StarChartOpening.Outcome outcome = StarChartOpening.open(opener, Global.getSector(), chartStore, chart,
                StarChartSettings.wormholeEntries(), openingRandom);
        String message = Translation.msg(outcome.messageKey()).arg("name", chart.name()).text();
        if (outcome.opened()) {
            wormholeOpened = true;
            SkillTreeSounds.wormholeJumped();
            rebuildModifyButton();
            rebuildOpenButton();
            refreshOpacity();
            detailListener.wormholeOpened(message);
            return;
        }
        SkillTreeSounds.refused();
        showNotice(message, true);
        rebuildOpenButton();
        refreshOpacity();
    }
}
