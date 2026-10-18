package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.StarChartSettings;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.Item;
import exiledsector.items.ItemCurrency;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemName;
import exiledsector.items.ItemStore;
import exiledsector.items.ItemUnlock;
import exiledsector.items.MaterialItems;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.VanillaText;
import exiledsector.ui.cargo.CargoIconLooks;
import exiledsector.ui.chart.StarChartCrafting;
import exiledsector.ui.chart.StarChartOpening;
import exiledsector.ui.chart.StarChartPreviewText;
import exiledsector.ui.chart.WormholeOpenCheck;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class StarChartDetailPanel extends HoloPanel {

    public interface Listener {

        void chartChanged(Item selectedAfter);

        void wormholeOpened(String message);

        void pressedInside();
    }

    private static final float TILE_SIZE = 52f;
    private static final float COUNT_INSET = 4f;
    private static final float CURRENCY_LABEL_HEIGHT = 20f;
    private static final float CURRENCY_HEIGHT = CURRENCY_LABEL_HEIGHT + TILE_SIZE;
    private static final float NOTICE_HEIGHT = 36f;
    private static final float REASON_HEIGHT = 20f;
    private static final float OPEN_BUTTON_HEIGHT = 44f;
    private static final float CONFIRM_HEIGHT = 170f;
    private static final float TOOLTIP_WIDTH = 340f;
    private static final String FALLBACK_ICON = ItemDefinition.FALLBACK_ICON;

    private enum Control {OPEN_WORMHOLE, CONFIRM_OPEN, CANCEL_OPEN}

    private final Listener detailListener;
    private final ItemStore chartStore;
    private final StarChartOpening.Opener opener;
    private final Random craftingRandom = new Random();
    private final Random openingRandom = new Random();
    private UIComponentAPI headerElement;
    private UIComponentAPI detailsElement;
    private UIComponentAPI currencyElement;
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
            closeOpenConfirm();
            rebuildContent();
        });
    }

    public void hideTooltip() {
        queue(hoverTooltip::hide);
    }

    public boolean escape() {
        if (isConfirmOpen()) {
            queue(this::closeOpenConfirm);
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
        return new UIComponentAPI[]{detailsElement, currencyElement, noticeElement, openElement};
    }

    @Override
    public void buttonPressed(Object id) {
        queue(() -> handleButton(id));
    }

    private void handleButton(Object id) {
        if (wormholeOpened) {
            return;
        }
        if (id == Control.OPEN_WORMHOLE) {
            openPressed();
        } else if (id == Control.CONFIRM_OPEN) {
            confirmOpen();
        } else if (id == Control.CANCEL_OPEN) {
            closeOpenConfirm();
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

    private float currencyTop() {
        return noticeTop() - GAP - CURRENCY_HEIGHT;
    }

    private static float detailsTop() {
        return PAD + HEADER_HEIGHT + GAP;
    }

    private void rebuildContent() {
        rebuildDetails();
        rebuildCurrencies();
        rebuildNotice();
        rebuildOpenButton();
        refreshOpacity();
    }

    private void rebuildDetails() {
        float detailsHeight = Math.max(OPEN_BUTTON_HEIGHT, currencyTop() - GAP - detailsTop());
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

    private static Predicate<ItemDefinition> allowedUniques() {
        return definition -> ItemUnlock.unlockConditionMet(definition, Global.getSector());
    }

    private void rebuildCurrencies() {
        hoverTooltip.hide();
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), CURRENCY_HEIGHT, false);
        VanillaText.plainPara(element, Translation.text("chart.ui.currency.heading"), 0f, Misc.getGrayColor());
        List<ItemCurrency> currencies = StarChartCrafting.currencies();
        if (currencies.isEmpty()) {
            VanillaText.plainPara(element, Translation.text("chart.ui.currency.none"), LINE_PAD * 2f, Misc.getGrayColor());
        } else {
            CraftingMaterials materials = playerMaterials();
            int perRow = Math.max(1, (int) ((innerWidth() + GAP) / (TILE_SIZE + GAP)));
            for (int i = 0; i < Math.min(currencies.size(), perRow); i++) {
                addTile(element, currencies.get(i), i * (TILE_SIZE + GAP), CURRENCY_LABEL_HEIGHT, materials);
            }
        }
        currencyElement = replaceElement(currencyElement, element, PAD, currencyTop());
    }

    private void addTile(TooltipMakerAPI element, ItemCurrency currency, float tileLeft, float tileTop, CraftingMaterials materials) {
        String iconPath = MaterialItems.loadedIconPath(currency.itemId());
        WorkbenchTile tile = new WorkbenchTile(List.of(iconPath == null ? FALLBACK_ICON : iconPath), CargoIconLooks.forMaterial(currency.itemId()),
                tileHandler(currency));
        CustomPanelAPI tilePanel = Global.getSettings().createCustom(TILE_SIZE, TILE_SIZE, tile);
        element.addCustom(tilePanel, 0f).getPosition().inTL(tileLeft, tileTop);
        int ownedCount = materials.count(currency.itemId());
        element.setParaFont(Fonts.ORBITRON_12);
        LabelAPI countLabel = VanillaText.plainPara(element, String.valueOf(ownedCount), 0f,
                ownedCount > 0 ? Misc.getBrightPlayerColor() : Misc.getGrayColor());
        countLabel.setAlignment(Alignment.RMID);
        countLabel.autoSizeToWidth(TILE_SIZE - COUNT_INSET * 2f).inTL(tileLeft + COUNT_INSET,
                tileTop + TILE_SIZE - countLabel.getPosition().getHeight() - COUNT_INSET);
        element.setParaFontDefault();
    }

    private TileInput.Handler tileHandler(ItemCurrency currency) {
        return new TileInput.Handler() {
            @Override
            public void entered(PositionAPI tilePosition) {
                queue(() -> showCurrencyTooltip(currency, tilePosition));
            }

            @Override
            public void left() {
                queue(() -> {
                    if (hoverTooltip.isShowing(currency)) {
                        hoverTooltip.hide();
                    }
                });
            }

            @Override
            public boolean clicked() {
                queue(() -> useCurrency(currency));
                return true;
            }
        };
    }

    private void showCurrencyTooltip(ItemCurrency currency, PositionAPI anchor) {
        if (isClosing() || isConfirmOpen()) {
            hoverTooltip.hide();
            return;
        }
        hoverTooltip.show(currency, TOOLTIP_WIDTH, (element, expanded) -> writeCurrencyTooltip(element, currency), anchor);
    }

    private void writeCurrencyTooltip(TooltipMakerAPI element, ItemCurrency currency) {
        element.setParaFont(Fonts.ORBITRON_20AA);
        VanillaText.plainPara(element, MaterialItems.displayName(currency.itemId()), 0f, Misc.getBrightPlayerColor());
        element.setParaFontDefault();
        String descriptionKey = currencyDescriptionKey(currency);
        if (Translation.has(descriptionKey)) {
            VanillaText.plainPara(element, Translation.text(descriptionKey), GAP, Misc.getTextColor());
        }
        CraftingMaterials materials = playerMaterials();
        VanillaText.plainPara(element, Translation.msg("ui.workbench.tile.inStorage").arg("count", materials.count(currency.itemId())).text(), GAP,
                Misc.getGrayColor());
        StarChartCrafting.Refusal refusal = StarChartCrafting.refusal(currency, shownChart, chartStore, materials, allowedUniques());
        String hint = refusal == StarChartCrafting.Refusal.NONE
                ? Translation.msg("chart.ui.currency.use").arg("target", shownChart.name()).text() : refusalText(currency, refusal);
        VanillaText.plainPara(element, hint, GAP, refusal == StarChartCrafting.Refusal.NONE ? Misc.getPositiveHighlightColor() : Misc.getGrayColor());
    }

    static String currencyDescriptionKey(ItemCurrency currency) {
        return "ui.workbench.recipe." + currency.key() + ".description";
    }

    private String refusalText(ItemCurrency currency, StarChartCrafting.Refusal refusal) {
        return Translation.msg(refusal.noticeKey()).arg("name", MaterialItems.displayName(currency.itemId()))
                .arg("target", shownChart == null ? "" : shownChart.name()).text();
    }

    private void useCurrency(ItemCurrency currency) {
        hoverTooltip.hide();
        if (isConfirmOpen()) {
            return;
        }
        CraftingMaterials materials = playerMaterials();
        StarChartCrafting.Refusal refusal = StarChartCrafting.refusal(currency, shownChart, chartStore, materials, allowedUniques());
        if (refusal != StarChartCrafting.Refusal.NONE) {
            SkillTreeSounds.refused();
            showNotice(refusalText(currency, refusal), true);
            return;
        }
        String nameBefore = shownChart.name();
        StarChartCrafting.Used used = StarChartCrafting.use(currency, shownChart, chartStore, materials, craftingRandom, allowedUniques());
        if (used == null) {
            SkillTreeSounds.refused();
            showNotice(refusalText(currency, StarChartCrafting.Refusal.CANNOT_USE), true);
            return;
        }
        shownChart = used.selectedAfter();
        noticeText = Translation.msg("chart.ui.currency.used").arg("currency", MaterialItems.displayName(currency.itemId()))
                .arg("before", nameBefore).arg("name", used.result().name()).text();
        noticeIsWarning = false;
        SkillTreeSounds.socketed();
        rebuildContent();
        detailListener.chartChanged(shownChart);
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
                "chart.ui.open.confirm.no", Control.CANCEL_OPEN);
    }

    private static void writeConfirm(TooltipMakerAPI element, Item chart, int entries) {
        VanillaText.plainPara(element, Translation.msg("chart.ui.open.confirm.title").arg("name", chart.name()).text(), 0f,
                chart.rarity().color());
        VanillaText.plainPara(element, Translation.text("chart.ui.open.confirm.body"), LINE_PAD * 2f, Misc.getTextColor());
        VanillaText.plainPara(element, Translation.msg("chart.ui.open.confirm.entries").count(entries).text(), LINE_PAD * 2f,
                Misc.getGrayColor());
    }

    private void closeOpenConfirm() {
        pendingChart = null;
        closeConfirm();
    }

    private void confirmOpen() {
        Item chart = pendingChart;
        closeOpenConfirm();
        if (chart == null || chart != shownChart) {
            return;
        }
        StarChartOpening.Outcome outcome = StarChartOpening.open(opener, Global.getSector(), chartStore, chart,
                StarChartSettings.wormholeEntries(), openingRandom);
        String message = Translation.msg(outcome.messageKey()).arg("name", chart.name()).text();
        if (outcome.opened()) {
            wormholeOpened = true;
            SkillTreeSounds.wormholeJumped();
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
