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
import exiledsector.charts.wormhole.ChartSupply;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.Item;
import exiledsector.items.ItemDisassembly;
import exiledsector.items.ItemRarity;
import exiledsector.items.ItemStore;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.VanillaText;
import exiledsector.ui.chart.StarChartBrowser;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

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
    private static final float BUTTON_TEXT_PADDING = 30f;
    private static final float CONFIRM_HEIGHT = 130f;
    private static final float BATCH_CONFIRM_HEIGHT = 170f;
    private static final ItemCell.Look CELL_LOOK = new ItemCell.Look(7f, true, 1f);

    private enum Control {CLOSE, COMBINE, DISASSEMBLY_MODE, CONFIRM_DISASSEMBLE, CANCEL_DISASSEMBLE}

    private record TierChip(int tier) {
    }

    private record RarityChip(ItemRarity rarity) {
    }

    private final StarChartBrowser browser;
    private final Listener storageListener;
    private final String openNotice;
    private String actionNotice;
    private boolean disassemblyMode;
    private final Set<Item> markedForDisassembly = new LinkedHashSet<>();
    private List<Item> pendingDisassembly = List.of();
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
        queue(() -> {
            List<Item> owned = ownedCharts();
            if (pendingDisassembly.stream().anyMatch(chart -> !owned.contains(chart))) {
                closeDisassemblyConfirm();
            }
            markedForDisassembly.removeIf(chart -> !owned.contains(chart));
            rebuildControls();
        });
    }

    public boolean escape() {
        if (isConfirmOpen()) {
            queue(this::closeDisassemblyConfirm);
            return true;
        }
        if (disassemblyMode) {
            queue(() -> {
                leaveDisassemblyMode();
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
                List<Item> targets = pendingDisassembly;
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
        markedForDisassembly.removeIf(chart -> !store.owned().contains(chart));
        rebuildControls();
    }

    private static List<Item> ownedCharts() {
        ItemStore store = ItemStore.starChartsIfCreated();
        return store == null ? List.of() : store.owned();
    }

    private void disassemblyModePressed() {
        if (!disassemblyMode) {
            disassemblyMode = true;
            rebuildFooter();
        } else if (markedForDisassembly.isEmpty()) {
            leaveDisassemblyMode();
            rebuildFooter();
        } else {
            openDisassemblyConfirm(List.copyOf(markedForDisassembly));
        }
    }

    private void leaveDisassemblyMode() {
        disassemblyMode = false;
        markedForDisassembly.clear();
    }

    private void chartClicked(Item chart) {
        if (isConfirmOpen()) {
            return;
        }
        if (disassemblyMode && ctrlHeld()) {
            queue(() -> disassembleNow(List.of(chart), false));
        } else if (disassemblyMode) {
            queue(() -> {
                if (!markedForDisassembly.remove(chart)) {
                    markedForDisassembly.add(chart);
                }
                rebuildFooter();
            });
        } else {
            queue(() -> storageListener.chartClicked(chart));
        }
    }

    private void chartRightClicked(Item chart) {
        if (!isConfirmOpen() && !disassemblyMode) {
            queue(() -> openDisassemblyConfirm(List.of(chart)));
        }
    }

    private void openDisassemblyConfirm(List<Item> targets) {
        if (targets.isEmpty() || isConfirmOpen()) {
            return;
        }
        hoverTooltip.hide();
        pendingDisassembly = List.copyOf(targets);
        openConfirm(targets.size() == 1 ? CONFIRM_HEIGHT : BATCH_CONFIRM_HEIGHT, element -> writeDisassemblyConfirm(element, targets),
                "ui.socketStorage.confirm.disassemble", Control.CONFIRM_DISASSEMBLE, "ui.socketStorage.confirm.cancel", Control.CANCEL_DISASSEMBLE);
    }

    private static void writeDisassemblyConfirm(TooltipMakerAPI element, List<Item> targets) {
        int totalParts = targets.stream().mapToInt(chart -> chart.rarity().disassemblyParts()).sum();
        if (targets.size() == 1) {
            Item chart = targets.get(0);
            VanillaText.plainPara(element, Translation.msg("ui.socketStorage.confirm.title").arg("name", chart.name()).text(), 0f,
                    chart.rarity().color());
            VanillaText.plainPara(element, partsText("ui.socketStorage.confirm.body", totalParts), LINE_PAD * 2f, Misc.getTextColor());
            return;
        }
        VanillaText.plainPara(element, Translation.msg("chart.ui.storage.confirm.batchTitle").arg("count", targets.size()).text(), 0f,
                Misc.getBasePlayerColor());
        addRarityBreakdown(element, targets);
        VanillaText.plainPara(element, Translation.msg("ui.socketStorage.confirm.batchBody").arg("count", totalParts).text(), LINE_PAD * 2f,
                Misc.getTextColor());
        if (targets.stream().anyMatch(chart -> chart.rarity() == ItemRarity.UNIQUE)) {
            VanillaText.plainPara(element, Translation.text("chart.ui.storage.confirm.includesUnique"), LINE_PAD * 2f,
                    Misc.getNegativeHighlightColor());
        }
    }

    private static void addRarityBreakdown(TooltipMakerAPI element, List<Item> targets) {
        List<String> rarityCounts = new ArrayList<>();
        List<Color> rarityColors = new ArrayList<>();
        for (ItemRarity rarity : ItemRarity.values()) {
            long count = targets.stream().filter(chart -> chart.rarity() == rarity).count();
            if (count > 0) {
                rarityCounts.add(Translation.msg("ui.socketStorage.confirm.rarityCount").arg("count", count).arg("rarity", rarityLabel(rarity)).text());
                rarityColors.add(rarity.color());
            }
        }
        String format = String.join(VanillaText.escapeFormat(Translation.text("ui.socketStorage.confirm.raritySeparator")),
                rarityCounts.stream().map(text -> "%s").toList());
        element.addPara(format, LINE_PAD * 2f, rarityColors.toArray(new Color[0]), rarityCounts.toArray(new String[0]));
    }

    private void closeDisassemblyConfirm() {
        if (!isConfirmOpen()) {
            return;
        }
        closeConfirm();
        pendingDisassembly = List.of();
    }

    private void disassembleNow(List<Item> requested, boolean leaveMode) {
        ItemStore store = ItemStore.starChartsIfCreated();
        List<Item> owned = ownedCharts();
        List<Item> targets = requested.stream().filter(owned::contains).toList();
        if (store == null || targets.isEmpty()) {
            return;
        }
        CraftingMaterials materials = playerMaterials();
        int totalParts = 0;
        int disassembledCount = 0;
        Item lastDisassembled = null;
        for (Item chart : targets) {
            int gainedParts = ItemDisassembly.disassemble(chart, materials, store);
            if (gainedParts > 0) {
                totalParts += gainedParts;
                disassembledCount++;
                lastDisassembled = chart;
            }
        }
        if (disassembledCount == 1) {
            actionNotice = Translation.msg("ui.socketStorage.disassembled").count(totalParts)
                    .arg("name", lastDisassembled.name()).arg("count", totalParts).text();
        } else if (disassembledCount > 1) {
            actionNotice = Translation.msg("chart.ui.storage.disassembledMany").arg("count", disassembledCount).arg("parts", totalParts).text();
        }
        markedForDisassembly.removeAll(targets);
        if (leaveMode) {
            leaveDisassemblyMode();
        }
        browser.reload(store.owned());
        storageListener.disassembled();
        rebuildControls();
    }

    private static String partsText(String key, int partCount) {
        return Translation.msg(key).count(partCount).arg("count", partCount).text();
    }

    private String footerNotice() {
        if (!disassemblyMode) {
            return actionNotice != null ? actionNotice : openNotice;
        }
        if (markedForDisassembly.isEmpty()) {
            return Translation.text("chart.ui.storage.disassemblyHint");
        }
        int markedParts = markedForDisassembly.stream().mapToInt(chart -> chart.rarity().disassemblyParts()).sum();
        return Translation.msg("ui.socketStorage.disassemblyMarked").arg("count", markedForDisassembly.size()).arg("parts", markedParts).text();
    }

    private void rebuildFooter() {
        TooltipMakerAPI notice = panelRoot.createUIElement(innerWidth(), NOTICE_HEIGHT, false);
        String shownNotice = footerNotice();
        if (shownNotice != null) {
            VanillaText.plainPara(notice, shownNotice, 0f, disassemblyMode ? Misc.getNegativeHighlightColor() : Misc.getPositiveHighlightColor());
        }
        noticeElement = replaceElement(noticeElement, notice, PAD, gridTop - NOTICE_HEIGHT);
        TooltipMakerAPI summary = panelRoot.createUIElement(innerWidth(), FOOTER_HEIGHT, false);
        VanillaText.plainPara(summary, Translation.msg("chart.ui.storage.summary").arg("shown", browser.shownCharts().size())
                .arg("tier", browser.selectedTier()).arg("stored", browser.storedCount()).text(), 0f, Misc.getGrayColor())
                .getPosition().inTL(0f, PARTS_TEXT_TOP);
        String modeKey = !disassemblyMode ? "ui.socketStorage.disassemblyMode"
                : markedForDisassembly.isEmpty() ? "ui.socketStorage.exitDisassemblyMode" : "ui.socketStorage.confirmDisassembly";
        String modeLabel = Translation.text(modeKey);
        float modeWidth = Global.getSettings().computeStringWidth(modeLabel, Fonts.ORBITRON_20AA) + BUTTON_TEXT_PADDING;
        ButtonAPI modeButton = summary.addButton(modeLabel, Control.DISASSEMBLY_MODE, modeWidth, FOOTER_HEIGHT, 0f);
        modeButton.getPosition().inTR(0f, 0f);
        modeButton.setEnabled(disassemblyMode || browser.storedCount() > 0);
        int tier = browser.selectedTier();
        if (!disassemblyMode && !ChartSupply.combinable(ItemStore.starChartsIfCreated(), tier, browser.selectedChart()).isEmpty()) {
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
                queue(() -> hoverTooltip.showSplit(chart, chart, expanded -> cellFooter(chart, expanded), cellPosition));
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
                return markedForDisassembly.contains(chart);
            }
        });
    }

    private List<StyledText> cellFooter(Item chart, boolean expanded) {
        int partCount = chart.rarity().disassemblyParts();
        if (disassemblyMode) {
            String key = markedForDisassembly.contains(chart) ? "ui.socketStorage.cell.unmark" : "ui.socketStorage.cell.mark";
            return List.of(Translation.msg(key).count(partCount).arg("count", partCount).styled());
        }
        StyledText selectHint = Translation.styled(browser.isSelected(chart) ? "chart.ui.storage.cell.selected" : "chart.ui.storage.cell.select");
        if (!expanded) {
            return List.of(selectHint);
        }
        return List.of(selectHint, Translation.msg("ui.socketStorage.cell.disassemble").count(partCount).arg("count", partCount).styled());
    }
}
