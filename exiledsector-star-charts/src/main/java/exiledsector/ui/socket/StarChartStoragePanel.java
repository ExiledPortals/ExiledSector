package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.wormhole.ChartSupply;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.Item;
import exiledsector.items.ItemDisassembly;
import exiledsector.items.ItemStore;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.VanillaText;
import exiledsector.ui.chart.StarChartBrowser;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class StarChartStoragePanel extends HoloPanel {

    public interface Listener {

        void chartClicked(Item chart);

        void closed();

        void pressedInside();

        default void disassembled() {
        }
    }

    private static final float EMPTY_TIER_SCALE = 0.55f;
    private static final float NOTICE_HEIGHT = 24f;
    private static final float FOOTER_HEIGHT = 24f;
    private static final float COMBINE_BUTTON_WIDTH = 130f;
    private static final float COMBINE_TOOLTIP_WIDTH = 300f;
    private static final ItemCell.Look CELL_LOOK = new ItemCell.Look(7f, true, 1f);
    private static final DisassemblySelection.Texts DISASSEMBLY_TEXTS = new DisassemblySelection.Texts("chart.ui.storage.disassemblyHint",
            "chart.ui.storage.confirm.batchTitle", "chart.ui.storage.disassembledMany", false);

    private enum Control {CLOSE, COMBINE, DISASSEMBLY_MODE, CONFIRM_DISASSEMBLE, CANCEL_DISASSEMBLE}

    private record TierChip(int tier) {
    }

    private final StarChartBrowser browser;
    private final Listener storageListener;
    private final String openNotice;
    private String actionNotice;
    private final DisassemblySelection disassembly = new DisassemblySelection(DISASSEMBLY_TEXTS);
    private UIComponentAPI headerElement;
    private UIComponentAPI controlsElement;
    private TooltipMakerAPI gridElement;
    private UIComponentAPI noticeElement;
    private UIComponentAPI summaryElement;
    private float controlsBottom;
    private float noticeHeight = NOTICE_HEIGHT;
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
        queue(() -> {
            List<Item> owned = ownedCharts();
            if (disassembly.pending().stream().anyMatch(chart -> !owned.contains(chart))) {
                closeDisassemblyConfirm();
            }
            disassembly.forget(chart -> !owned.contains(chart));
            rebuildControls();
        });
    }

    public boolean escape() {
        if (isConfirmOpen()) {
            queue(this::closeDisassemblyConfirm);
            return true;
        }
        if (disassembly.active()) {
            queue(() -> {
                disassembly.leave();
                rebuildFooter();
            });
            return true;
        }
        return false;
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
        if (isConfirmOpen()) {
            if (id == Control.CONFIRM_DISASSEMBLE) {
                List<Item> targets = disassembly.pending();
                closeDisassemblyConfirm();
                disassembleNow(targets, true);
            } else if (id == Control.CANCEL_DISASSEMBLE) {
                closeDisassemblyConfirm();
            }
            return;
        }
        if (id == Control.CLOSE) {
            close();
        } else if (id == Control.COMBINE) {
            combineSelectedTier();
        } else if (id == Control.DISASSEMBLY_MODE) {
            disassemblyModePressed();
        } else if (id instanceof TierChip chip) {
            browser.selectTier(chip.tier());
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

    private void rebuildControls() {
        List<String> tierLabels = new ArrayList<>();
        for (int tier = StarChartBrowser.LOWEST_TIER; tier <= StarChartBrowser.HIGHEST_TIER; tier++) {
            tierLabels.add(tierLabel(tier, browser.count(tier)));
        }
        float chipWidth = ChipFlow.uniformWidth(tierLabels, innerWidth());
        int tierRows = ChipFlow.uniformRows(tierLabels.size(), chipWidth, innerWidth());
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), ChipFlow.height(tierRows), false);
        ChipFlow chips = new ChipFlow(element, innerWidth());
        for (int tier = StarChartBrowser.LOWEST_TIER; tier <= StarChartBrowser.HIGHEST_TIER; tier++) {
            boolean stocked = browser.count(tier) > 0;
            Color base = stocked ? Misc.getBasePlayerColor() : Misc.scaleColorOnly(Misc.getGrayColor(), EMPTY_TIER_SCALE);
            Color bright = stocked ? Misc.getBrightPlayerColor() : Misc.getGrayColor();
            chips.place(tierLabels.get(tier - StarChartBrowser.LOWEST_TIER), new TierChip(tier), ChipFlow.Colors.of(base, bright),
                    tier == browser.selectedTier(), chipWidth);
        }
        float controlsTop = PAD + HEADER_HEIGHT + GAP;
        controlsElement = replaceElement(controlsElement, element, PAD, controlsTop);
        controlsBottom = controlsTop + ChipFlow.rowsHeight(tierRows);
        gridTop = controlsBottom + noticeHeight;
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
        return Translation.msg("chart.ui.storage.emptyTier").arg("tier", browser.selectedTier()).text();
    }

    private void combineSelectedTier() {
        ItemStore store = ItemStore.starChartsIfCreated();
        int tier = browser.selectedTier();
        Item made = ChartSupply.combine(store, tier, browser.selectedChart(), new Random());
        if (made == null) {
            SkillTreeSounds.refused();
            return;
        }
        SkillTreeSounds.crafted();
        actionNotice = Translation.msg("chart.ui.storage.combined").arg("tier", tier).arg("name", made.name()).text();
        browser.reload(store.owned());
        disassembly.forget(chart -> !store.owned().contains(chart));
        rebuildControls();
    }

    private static List<Item> ownedCharts() {
        ItemStore store = ItemStore.starChartsIfCreated();
        return store == null ? List.of() : store.owned();
    }

    private void disassemblyModePressed() {
        if (!disassembly.active()) {
            disassembly.enter();
            rebuildFooter();
        } else if (disassembly.nothingMarked()) {
            disassembly.leave();
            rebuildFooter();
        } else {
            openDisassemblyConfirm(disassembly.marked());
        }
    }

    private void chartClicked(Item chart) {
        if (isConfirmOpen()) {
            return;
        }
        if (disassembly.active() && ctrlHeld()) {
            queue(() -> disassembleNow(List.of(chart), false));
        } else if (disassembly.active()) {
            queue(() -> {
                disassembly.toggle(chart);
                rebuildFooter();
            });
        } else {
            queue(() -> storageListener.chartClicked(chart));
        }
    }

    private void chartRightClicked(Item chart) {
        if (!isConfirmOpen() && !disassembly.active()) {
            queue(() -> openDisassemblyConfirm(List.of(chart)));
        }
    }

    private void openDisassemblyConfirm(List<Item> targets) {
        if (targets.isEmpty() || isConfirmOpen()) {
            return;
        }
        hoverTooltip.hide();
        disassembly.confirming(targets);
        openConfirm(disassembly.confirmHeight(targets), element -> disassembly.writeConfirm(element, targets), "ui.socketStorage.confirm.disassemble",
                Control.CONFIRM_DISASSEMBLE, "ui.socketStorage.confirm.cancel", Control.CANCEL_DISASSEMBLE);
    }

    private void closeDisassemblyConfirm() {
        if (!isConfirmOpen()) {
            return;
        }
        closeConfirm();
        disassembly.confirmClosed();
    }

    private void disassembleNow(List<Item> requested, boolean leaveMode) {
        ItemStore store = ItemStore.starChartsIfCreated();
        List<Item> owned = ownedCharts();
        List<Item> targets = requested.stream().filter(owned::contains).toList();
        if (store == null || targets.isEmpty()) {
            return;
        }
        CraftingMaterials materials = playerMaterials();
        String notice = disassembly.disassemble(targets, chart -> ItemDisassembly.disassemble(chart, materials, store));
        if (notice != null) {
            actionNotice = notice;
        }
        if (leaveMode) {
            disassembly.leave();
        }
        browser.reload(store.owned());
        storageListener.disassembled();
        rebuildControls();
    }

    private String footerNotice() {
        if (!disassembly.active()) {
            return actionNotice != null ? actionNotice : openNotice;
        }
        return disassembly.hint();
    }

    private void rebuildFooter() {
        String shownNotice = footerNotice();
        TooltipMakerAPI notice = panelRoot.createUIElement(innerWidth(), NOTICE_HEIGHT * 3f, false);
        if (shownNotice != null) {
            VanillaText.plainPara(notice, shownNotice, 0f, disassembly.active() ? Misc.getNegativeHighlightColor() : Misc.getPositiveHighlightColor());
        }
        float neededHeight = Math.max(NOTICE_HEIGHT, notice.getHeightSoFar() + GAP);
        if (Math.abs(neededHeight - noticeHeight) > 0.5f) {
            noticeHeight = neededHeight;
            gridTop = controlsBottom + noticeHeight;
            rebuildGrid();
            return;
        }
        notice.getPosition().setSize(innerWidth(), noticeHeight);
        noticeElement = replaceElement(noticeElement, notice, PAD, controlsBottom);
        TooltipMakerAPI summary = panelRoot.createUIElement(innerWidth(), FOOTER_HEIGHT, false);
        float modeWidth = disassembly.addModeButton(summary, Control.DISASSEMBLY_MODE, FOOTER_HEIGHT, browser.storedCount() > 0);
        int tier = browser.selectedTier();
        if (!disassembly.active() && !ChartSupply.combinable(ItemStore.starChartsIfCreated(), tier, browser.selectedChart()).isEmpty()) {
            summary.addButton(Translation.text("chart.ui.storage.combine"), Control.COMBINE, COMBINE_BUTTON_WIDTH, FOOTER_HEIGHT, 0f)
                    .getPosition().inTR(modeWidth + GAP, 0f);
            summary.addTooltipToPrevious(new GameTextTooltip(COMBINE_TOOLTIP_WIDTH, tooltip -> VanillaText.plainPara(tooltip,
                    Translation.msg("chart.ui.storage.combine.tooltip").arg("tier", tier).arg("next", tier + 1).text(), 0f, Misc.getTextColor())),
                    TooltipMakerAPI.TooltipLocation.ABOVE);
        }
        summaryElement = replaceElement(summaryElement, summary, PAD, panelHeight - PAD - FOOTER_HEIGHT);
    }

    private ItemCell cellFor(Item chart) {
        return new ItemCell(chart, CELL_LOOK, new ItemCell.Listener() {
            @Override
            public void hovered(PositionAPI cellPosition) {
                queue(() -> hoverTooltip.show(chart, chart, expanded -> cellFooter(chart, expanded), cellPosition));
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
                chartClicked(chart);
                return true;
            }

            @Override
            public boolean rightClicked() {
                chartRightClicked(chart);
                return true;
            }

            @Override
            public boolean selected() {
                return browser.isSelected(chart);
            }

            @Override
            public boolean marked() {
                return disassembly.isMarked(chart);
            }
        });
    }

    private List<StyledText> cellFooter(Item chart, boolean expanded) {
        if (disassembly.active()) {
            return List.of(disassembly.cellLine(chart));
        }
        StyledText selectHint = Translation.styled(browser.isSelected(chart) ? "chart.ui.storage.cell.selected" : "chart.ui.storage.cell.select");
        if (!expanded) {
            return List.of(selectHint);
        }
        return List.of(selectHint, DisassemblySelection.disassembleHint(chart));
    }
}
