package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledsector.items.Item;
import exiledsector.items.ItemRarity;
import exiledsector.ui.VanillaText;
import exiledsector.ui.chart.StarChartBrowser;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StarChartStoragePanel extends HoloPanel {

    public interface Listener {

        void chartClicked(Item chart);

        void closed();

        void pressedInside();
    }

    private static final float EMPTY_TIER_SCALE = 0.55f;
    private static final float NOTICE_HEIGHT = 24f;
    private static final float FOOTER_HEIGHT = 24f;
    private static final ItemCell.Look CELL_LOOK = new ItemCell.Look(7f, true, 1f);

    private enum Control {CLOSE}

    private record TierChip(int tier) {
    }

    private record RarityChip(ItemRarity rarity) {
    }

    private final StarChartBrowser browser;
    private final Listener storageListener;
    private final String openNotice;
    private UIComponentAPI headerElement;
    private UIComponentAPI controlsElement;
    private TooltipMakerAPI gridElement;
    private UIComponentAPI noticeElement;
    private UIComponentAPI summaryElement;
    private float gridTop;

    private StarChartStoragePanel(CustomPanelAPI hostPanel, StarChartBrowser browser, Listener storageListener, String openNotice) {
        super(hostPanel);
        this.browser = browser;
        this.storageListener = storageListener;
        this.openNotice = openNotice;
    }

    public static StarChartStoragePanel open(CustomPanelAPI hostPanel, SocketPanelPlacement placement, StarChartBrowser browser,
                                             Listener storageListener, String openNotice) {
        StarChartStoragePanel storagePanel = new StarChartStoragePanel(hostPanel, browser, storageListener, openNotice);
        storagePanel.attach(placement.panelLeft(), placement.panelTop(), placement.panelWidth(), placement.panelHeight(), placement.instantly());
        return storagePanel;
    }

    public void refresh() {
        queue(this::rebuildControls);
    }

    public void hideTooltip() {
        queue(hoverTooltip::hide);
    }

    @Override
    protected void onClose() {
        hoverTooltip.hide();
        storageListener.closed();
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{controlsElement, placedComponent(gridElement), noticeElement, summaryElement};
    }

    @Override
    protected void onScrollInside() {
        queue(hoverTooltip::hide);
    }

    @Override
    protected void onPressInside() {
        storageListener.pressedInside();
    }

    @Override
    protected void advanceOpen(float amount) {
        hoverTooltip.refreshIfExpansionChanged();
    }

    @Override
    public void buttonPressed(Object id) {
        queue(() -> handleButton(id));
    }

    private void handleButton(Object id) {
        if (id == Control.CLOSE) {
            close();
        } else if (id instanceof TierChip chip) {
            browser.selectTier(chip.tier());
            rebuildControls();
        } else if (id instanceof RarityChip chip) {
            browser.toggleRarity(chip.rarity());
            rebuildControls();
        }
    }

    @Override
    protected void build() {
        rebuildHeader();
        rebuildControls();
    }

    @Override
    protected void rebuildHeader() {
        headerElement = finishHeader(startHeader("chart.ui.storage.title", "chart.ui.storage.close", Control.CLOSE), headerElement);
        refreshOpacity();
    }

    private static String tierLabel(int tier, int count) {
        return Translation.msg("chart.ui.storage.tierTab").arg("tier", tier).arg("count", count).text();
    }

    private static String rarityLabel(ItemRarity rarity) {
        return Translation.text("ui.socketStorage.rarity." + rarity.name().toLowerCase(Locale.ROOT));
    }

    private void rebuildControls() {
        List<String> tierLabels = new ArrayList<>();
        for (int tier = StarChartBrowser.LOWEST_TIER; tier <= StarChartBrowser.HIGHEST_TIER; tier++) {
            tierLabels.add(tierLabel(tier, browser.count(tier)));
        }
        int tierRows = ChipFlow.rowsFor(tierLabels, innerWidth());
        int controlRows = tierRows + 1;
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), ChipFlow.height(controlRows), false);
        ChipFlow chips = new ChipFlow(element, innerWidth());
        for (int tier = StarChartBrowser.LOWEST_TIER; tier <= StarChartBrowser.HIGHEST_TIER; tier++) {
            boolean stocked = browser.count(tier) > 0;
            Color base = stocked ? Misc.getBasePlayerColor() : Misc.scaleColorOnly(Misc.getGrayColor(), EMPTY_TIER_SCALE);
            Color bright = stocked ? Misc.getBrightPlayerColor() : Misc.getGrayColor();
            chips.place(tierLabels.get(tier - StarChartBrowser.LOWEST_TIER), new TierChip(tier), ChipFlow.Colors.of(base, bright),
                    tier == browser.selectedTier());
        }
        chips.startRow(tierRows);
        for (ItemRarity rarity : ItemRarity.values()) {
            chips.placeInRow(rarityLabel(rarity), new RarityChip(rarity), ChipFlow.Colors.of(rarity.color()), browser.showsRarity(rarity));
        }
        float controlsTop = PAD + HEADER_HEIGHT + GAP;
        controlsElement = replaceElement(controlsElement, element, PAD, controlsTop);
        gridTop = controlsTop + ChipFlow.rowsHeight(controlRows) + NOTICE_HEIGHT;
        rebuildGrid();
    }

    private void rebuildGrid() {
        hoverTooltip.hide();
        List<Item> shown = browser.shownCharts();
        int columns = ItemGrid.columnsFor(panelWidth);
        float gridHeight = Math.max(ItemGrid.cellSize(innerWidth(), columns), panelHeight - PAD - FOOTER_HEIGHT - GAP - gridTop);
        TooltipMakerAPI element = ItemGrid.build(panelRoot, innerWidth(), gridHeight, columns, shown, this::cellFor, emptyText());
        gridElement = replaceElement(placedComponent(gridElement), element, PAD, gridTop);
        rebuildFooter();
        refreshOpacity();
    }

    private String emptyText() {
        if (browser.storedCount() == 0) {
            return Translation.text("chart.ui.storage.empty");
        }
        if (browser.inSelectedTierCount() == 0) {
            return Translation.msg("chart.ui.storage.emptyTier").arg("tier", browser.selectedTier()).text();
        }
        return Translation.text("chart.ui.storage.noMatches");
    }

    private void rebuildFooter() {
        TooltipMakerAPI notice = panelRoot.createUIElement(innerWidth(), NOTICE_HEIGHT, false);
        if (openNotice != null) {
            VanillaText.plainPara(notice, openNotice, 0f, Misc.getPositiveHighlightColor());
        }
        noticeElement = replaceElement(noticeElement, notice, PAD, gridTop - NOTICE_HEIGHT);
        TooltipMakerAPI summary = panelRoot.createUIElement(innerWidth(), FOOTER_HEIGHT, false);
        VanillaText.plainPara(summary, Translation.msg("chart.ui.storage.summary").arg("shown", browser.shownCharts().size())
                .arg("tier", browser.selectedTier()).arg("stored", browser.storedCount()).text(), 0f, Misc.getGrayColor())
                .getPosition().inTL(0f, PARTS_TEXT_TOP);
        summaryElement = replaceElement(summaryElement, summary, PAD, panelHeight - PAD - FOOTER_HEIGHT);
    }

    private ItemCell cellFor(Item chart) {
        return new ItemCell(chart, CELL_LOOK, new ItemCell.Listener() {
            @Override
            public void hovered(PositionAPI cellPosition) {
                queue(() -> hoverTooltip.show(chart, chart, expanded -> cellFooter(chart), cellPosition));
            }

            @Override
            public void left() {
                queue(() -> {
                    if (hoverTooltip.isShowing(chart)) {
                        hoverTooltip.hide();
                    }
                });
            }

            @Override
            public boolean clicked() {
                queue(() -> storageListener.chartClicked(chart));
                return true;
            }

            @Override
            public boolean selected() {
                return browser.isSelected(chart);
            }
        });
    }

    private List<StyledText> cellFooter(Item chart) {
        return List.of(Translation.styled(browser.isSelected(chart) ? "chart.ui.storage.cell.selected" : "chart.ui.storage.cell.select"));
    }
}
