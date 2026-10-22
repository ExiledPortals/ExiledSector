package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.items.Item;
import exiledlib.items.ItemRarity;
import exiledlib.ui.VanillaText;
import exiledsector.ui.chart.StarChartBrowser;

import java.awt.Color;
import java.util.List;
import java.util.Locale;

public final class StarChartStoragePanel extends HoloPanel {

    public interface Listener {

        void chartClicked(Item chart);

        void closed();

        void pressedInside();
    }

    private static final float CHIP_HEIGHT = 24f;
    private static final float CHIP_TEXT_PADDING = 24f;
    private static final float CHIP_DARK_SCALE = 0.3f;
    private static final float EMPTY_TIER_SCALE = 0.55f;
    private static final float NOTICE_HEIGHT = 24f;
    private static final float FOOTER_HEIGHT = 24f;
    private static final float SCROLLBAR_ROOM = 14f;
    private static final float MIN_CELL_SIZE = 72f;
    private static final float COLUMN_FIT_SLACK = 0.5f;
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

    private float innerWidth() {
        return panelWidth - PAD * 2f;
    }

    static int columnsFor(float panelWidth) {
        float gridWidth = panelWidth - PAD * 2f - SCROLLBAR_ROOM;
        return Math.max(1, (int) ((gridWidth + GAP + COLUMN_FIT_SLACK) / (MIN_CELL_SIZE + GAP)));
    }

    private static float chipWidth(String label) {
        return Global.getSettings().computeStringWidth(label, Fonts.ORBITRON_12) + CHIP_TEXT_PADDING;
    }

    private static String tierLabel(int tier, int count) {
        return Translation.msg("chart.ui.storage.tierTab").arg("tier", tier).arg("count", count).text();
    }

    private static String rarityLabel(ItemRarity rarity) {
        return Translation.text("ui.socketStorage.rarity." + rarity.name().toLowerCase(Locale.ROOT));
    }

    private int tierRows() {
        int rows = 1;
        float chipLeft = 0f;
        for (int tier = StarChartBrowser.LOWEST_TIER; tier <= StarChartBrowser.HIGHEST_TIER; tier++) {
            float chipWidth = chipWidth(tierLabel(tier, browser.count(tier)));
            if (chipLeft > 0f && chipLeft + chipWidth > innerWidth()) {
                chipLeft = 0f;
                rows++;
            }
            chipLeft += chipWidth + GAP;
        }
        return rows;
    }

    private void rebuildControls() {
        int tierRows = tierRows();
        int controlRows = tierRows + 1;
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), controlRows * CHIP_HEIGHT + (controlRows - 1) * GAP, false);
        float chipLeft = 0f;
        int chipRow = 0;
        for (int tier = StarChartBrowser.LOWEST_TIER; tier <= StarChartBrowser.HIGHEST_TIER; tier++) {
            int count = browser.count(tier);
            String label = tierLabel(tier, count);
            float chipWidth = chipWidth(label);
            if (chipLeft > 0f && chipLeft + chipWidth > innerWidth()) {
                chipLeft = 0f;
                chipRow++;
            }
            Color base = count > 0 ? Misc.getBasePlayerColor() : Misc.scaleColorOnly(Misc.getGrayColor(), EMPTY_TIER_SCALE);
            Color bright = count > 0 ? Misc.getBrightPlayerColor() : Misc.getGrayColor();
            addChip(element, label, new TierChip(tier), base, bright, tier == browser.selectedTier(), chipLeft, chipRow);
            chipLeft += chipWidth + GAP;
        }
        chipLeft = 0f;
        for (ItemRarity rarity : ItemRarity.values()) {
            String label = rarityLabel(rarity);
            addChip(element, label, new RarityChip(rarity), rarity.color(), rarity.color().brighter(), browser.showsRarity(rarity), chipLeft,
                    tierRows);
            chipLeft += chipWidth(label) + GAP;
        }
        float controlsTop = PAD + HEADER_HEIGHT + GAP;
        controlsElement = replaceElement(controlsElement, element, PAD, controlsTop);
        gridTop = controlsTop + controlRows * (CHIP_HEIGHT + GAP) + NOTICE_HEIGHT;
        rebuildGrid();
    }

    private static void addChip(TooltipMakerAPI element, String label, Object chipId, Color base, Color bright, boolean checked, float chipLeft,
                                int chipRow) {
        ButtonAPI chip = element.addAreaCheckbox(label, chipId, base, Misc.scaleColorOnly(base, CHIP_DARK_SCALE), bright, chipWidth(label),
                CHIP_HEIGHT, 0f);
        chip.setChecked(checked);
        chip.getPosition().inTL(chipLeft, chipRow * (CHIP_HEIGHT + GAP));
    }

    private void rebuildGrid() {
        hoverTooltip.hide();
        List<Item> shown = browser.shownCharts();
        int columns = columnsFor(panelWidth);
        float cellSize = (innerWidth() - SCROLLBAR_ROOM - (columns - 1) * GAP) / columns;
        float gridHeight = Math.max(cellSize, panelHeight - PAD - FOOTER_HEIGHT - GAP - gridTop);
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), gridHeight, true);
        if (shown.isEmpty()) {
            VanillaText.plainPara(element, emptyText(), 0f, Misc.getGrayColor());
        }
        for (int start = 0; start < shown.size(); start += columns) {
            CustomPanelAPI rowPanel = Global.getSettings().createCustom(innerWidth() - SCROLLBAR_ROOM, cellSize, null);
            for (int i = start; i < Math.min(shown.size(), start + columns); i++) {
                CustomPanelAPI cellPanel = Global.getSettings().createCustom(cellSize, cellSize, cellFor(shown.get(i)));
                rowPanel.addComponent(cellPanel).inTL((i - start) * (cellSize + GAP), 0f);
            }
            element.addCustom(rowPanel, start == 0 ? 0f : GAP);
        }
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
