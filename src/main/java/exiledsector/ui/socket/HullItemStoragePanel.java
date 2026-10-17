package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.hullitems.SocketCustody;
import exiledsector.hullitems.CraftingMaterials;
import exiledsector.hullitems.HullItemType;
import exiledsector.hullitems.HullItem;
import exiledsector.hullitems.HullItemDisassembly;
import exiledsector.hullitems.HullItemRarity;
import exiledsector.hullitems.HullItemStore;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.VanillaText;
import exiledsector.ui.section.SectionFlair;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public final class HullItemStoragePanel extends HoloPanel {

    public interface Listener {

        void selected(HullItem hullItem);

        void closed();

        void pressedInside();
    }

    private static final float FIELD_HEIGHT = 26f;
    private static final float CHIP_HEIGHT = 24f;
    private static final float CHIP_TEXT_PADDING = 28f;
    private static final float CHIP_DARK_SCALE = 0.3f;
    private static final int FIXED_CONTROL_ROWS = 2;
    private static final float LABEL_WIDTH = 70f;
    private static final float NOTICE_HEIGHT = 56f;
    private static final float FOOTER_HEIGHT = 24f;
    private static final float SCROLLBAR_ROOM = 14f;
    private static final float MIN_CELL_SIZE = 72f;
    private static final int COMPACT_COLUMNS = 5;
    private static final int TREE_COLUMNS = 6;
    private static final float COLUMN_FIT_SLACK = 0.5f;
    public static final float COMPACT_WIDTH = widthFor(COMPACT_COLUMNS);
    public static final float WIDTH = widthFor(TREE_COLUMNS);
    private static final float CONFIRM_HEIGHT = 130f;
    private static final float WORKBENCH_GAP = 12f;
    private static final float BATCH_CONFIRM_HEIGHT = 170f;
    private static final float BUTTON_TEXT_PADDING = 30f;
    private static final float SEARCH_DELAY_SECONDS = 0.25f;
    private static final HullItemCell.Look CELL_LOOK = new HullItemCell.Look(7f, true, 1f);

    private enum Control {CLOSE, WORKBENCH, DISASSEMBLY_MODE, CONFIRM_DISASSEMBLE, CANCEL_DISASSEMBLE}

    private record StatusChip(HullItemStorageFilter.Status status) {
    }

    private record RarityChip(HullItemRarity rarity) {
    }

    private record TypeChip(HullItemType socketType) {
    }

    private record ChipColors(Color base, Color dark, Color bright) {

        static ChipColors of(Color base) {
            return new ChipColors(base, Misc.scaleColorOnly(base, CHIP_DARK_SCALE), base.brighter());
        }

        static ChipColors player() {
            return new ChipColors(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor());
        }
    }

    private final Listener storageListener;
    private final HullItemStorageFilter storageFilter = HullItemStorageFilter.SESSION;

    private Function<HullItem, String> installedShipLookup;
    private SocketWorkbenchPanel workbenchPanel;
    private SocketWorkbenchPanel fadingWorkbenchPanel;
    private float gridTop;
    private UIComponentAPI headerElement;
    private UIComponentAPI searchRowElement;
    private UIComponentAPI toolsElement;
    private UIComponentAPI controlsElement;
    private TooltipMakerAPI gridElement;
    private UIComponentAPI noticeElement;
    private UIComponentAPI summaryElement;
    private TextFieldAPI searchField;
    private String typedQuery;
    private float searchDelay;
    private List<HullItemStorageRow> storageRows = List.of();
    private HullItem selectedHullItem;
    private String placingHint;
    private boolean targetingSocket;
    private List<HullItem> pendingDisassembly = List.of();
    private boolean disassemblyMode;
    private final Set<HullItem> markedForDisassembly = new LinkedHashSet<>();
    private int movedFromCargo;
    private String materialsNotice;
    private String disassembledNotice;
    private HullItemType kindFilter;

    private HullItemStoragePanel(CustomPanelAPI hostPanel, Function<HullItem, String> installedShipLookup, Listener storageListener,
                               HullItemType kindFilter) {
        super(hostPanel);
        this.kindFilter = kindFilter;
        this.installedShipLookup = installedShipLookup;
        this.storageListener = storageListener;
    }

    public static HullItemStoragePanel open(CustomPanelAPI hostPanel, SocketPanelPlacement placement, Function<HullItem, String> installedShipLookup,
                                          Listener storageListener, HullItemType kindFilter, Consumer<SocketPanelTab> tabListener) {
        HullItemStoragePanel storagePanel = new HullItemStoragePanel(hostPanel, installedShipLookup, storageListener, kindFilter);
        storagePanel.movedFromCargo = absorbPlayerCargo();
        storagePanel.materialsNotice = settleMaterials();
        storagePanel.setTabListener(tabListener);
        storagePanel.attach(placement.panelLeft(), placement.panelTop(), placement.panelWidth(), placement.panelHeight(), placement.instantly());
        return storagePanel;
    }

    private static int absorbPlayerCargo() {
        CargoAPI cargo = PlayerFleet.cargoOrNull();
        return cargo == null ? 0 : HullItemStore.get().absorbFrom(cargo);
    }

    private String cargoNotice() {
        String movedText = movedFromCargo > 0
                ? Translation.msg("ui.socketStorage.moved").count(movedFromCargo).arg("count", movedFromCargo).text() : null;
        if (movedText == null || materialsNotice == null) {
            return movedText == null ? materialsNotice : movedText;
        }
        return movedText + Translation.text("ui.socketStorage.noticeSeparator") + materialsNotice;
    }

    private static String settleMaterials() {
        CraftingMaterials materials = playerMaterials();
        int movedCount = materials.settle();
        if (movedCount <= 0) {
            return null;
        }
        String key = materials.isStoring() ? "ui.socketStorage.materialsStored" : "ui.socketStorage.materialsReturned";
        return Translation.msg(key).count(movedCount).arg("count", movedCount).text();
    }

    @Override
    protected void onClose() {
        hideHoverTooltip();
        if (workbenchPanel != null) {
            workbenchPanel.close();
        }
        storageListener.closed();
    }

    @Override
    protected void onSwappedOut() {
        if (workbenchPanel != null) {
            workbenchPanel.close();
        }
    }

    @Override
    protected SocketPanelTab tab() {
        return SocketPanelTab.HULL_ITEMS;
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement, searchRowElement, toolsElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{controlsElement, placedComponent(gridElement), noticeElement, summaryElement};
    }

    public void restrictTo(HullItemType socketType) {
        if (socketType == kindFilter) {
            return;
        }
        kindFilter = socketType;
        queue(() -> {
            closeDisassemblyConfirm();
            reloadRows();
            rebuildControls();
        });
    }

    public void setSelected(HullItem hullItem) {
        setSelected(hullItem, null);
    }

    public void setSelected(HullItem hullItem, String hint) {
        selectedHullItem = hullItem;
        placingHint = hint;
        if (hullItem != null) {
            leaveDisassemblyMode();
        }
        queue(() -> rebuildFooter(HullItemStorageQuery.apply(storageRows, storageFilter)));
    }

    public void setTargetingSocket(boolean targeting) {
        if (targeting == targetingSocket) {
            return;
        }
        targetingSocket = targeting;
        if (targeting) {
            leaveDisassemblyMode();
        }
        queue(() -> rebuildFooter(HullItemStorageQuery.apply(storageRows, storageFilter)));
    }

    public void refresh(Function<HullItem, String> installedShipLookup) {
        this.installedShipLookup = installedShipLookup;
        queue(() -> {
            if (pendingDisassembly.stream().anyMatch(SocketCustody::isInstalled)) {
                closeDisassemblyConfirm();
            }
            reloadRows();
            markedForDisassembly.removeIf(hullItem -> SocketCustody.isInstalled(hullItem)
                    || !HullItemStore.get().owned().contains(hullItem));
            rebuildGrid();
            if (workbenchPanel != null) {
                workbenchPanel.refresh();
            }
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
                rebuildGrid();
            });
            return true;
        }
        if (workbenchPanel != null) {
            queue(workbenchPanel::close);
            return true;
        }
        return false;
    }

    private void leaveDisassemblyMode() {
        disassemblyMode = false;
        markedForDisassembly.clear();
    }

    @Override
    protected void onScrollInside() {
        queue(this::hideHoverTooltip);
    }

    @Override
    protected void onPressInside() {
        storageListener.pressedInside();
    }

    @Override
    protected void advanceOpen(float amount) {
        hoverTooltip.refreshIfExpansionChanged();
        if (searchField == null || isConfirmOpen()) {
            return;
        }
        String searchText = searchField.getText();
        if (!searchText.equals(typedQuery)) {
            typedQuery = searchText;
            searchDelay = SEARCH_DELAY_SECONDS;
        } else if (searchDelay > 0f) {
            searchDelay -= amount;
            if (searchDelay <= 0f) {
                storageFilter.setQuery(typedQuery);
                I18n.forGameText(this::rebuildGrid);
            }
        }
    }

    @Override
    public void buttonPressed(Object id) {
        if (tabPressed(id)) {
            return;
        }
        queue(() -> handleButton(id));
    }

    private void handleButton(Object id) {
        if (isConfirmOpen()) {
            if (id == Control.CONFIRM_DISASSEMBLE) {
                List<HullItem> targets = pendingDisassembly.stream().filter(hullItem -> !SocketCustody.isInstalled(hullItem)).toList();
                if (selectedHullItem != null && targets.contains(selectedHullItem)) {
                    storageListener.selected(null);
                }
                disassemble(targets);
                if (workbenchPanel != null) {
                    workbenchPanel.refresh();
                }
                if (disassemblyMode) {
                    leaveDisassemblyMode();
                }
                closeDisassemblyConfirm();
                reloadRows();
                rebuildTools();
                rebuildControls();
            } else if (id == Control.CANCEL_DISASSEMBLE) {
                closeDisassemblyConfirm();
            }
            return;
        }
        if (id == Control.CLOSE) {
            close();
        } else if (id == Control.WORKBENCH) {
            toggleWorkbench();
        } else if (id == Control.DISASSEMBLY_MODE) {
            disassemblyModePressed();
        } else if (id instanceof StatusChip chip) {
            storageFilter.setStatus(chip.status());
            rebuildControls();
        } else if (id instanceof RarityChip chip) {
            storageFilter.toggleRarity(chip.rarity());
            rebuildControls();
        } else if (id instanceof TypeChip chip) {
            storageFilter.toggleSocketType(chip.socketType());
            reloadRows();
            rebuildControls();
        }
    }

    private void disassemblyModePressed() {
        if (!disassemblyMode) {
            disassemblyMode = true;
            if (selectedHullItem != null) {
                storageListener.selected(null);
            }
            rebuildFooter(HullItemStorageQuery.apply(storageRows, storageFilter));
        } else if (markedForDisassembly.isEmpty()) {
            leaveDisassemblyMode();
            rebuildFooter(HullItemStorageQuery.apply(storageRows, storageFilter));
        } else {
            openDisassemblyConfirm(List.copyOf(markedForDisassembly));
        }
    }

    private void cellClicked(HullItemStorageRow row) {
        if (isConfirmOpen() || row.installed()) {
            return;
        }
        if (disassemblyMode) {
            queue(() -> {
                if (!markedForDisassembly.remove(row.hullItem())) {
                    markedForDisassembly.add(row.hullItem());
                }
                rebuildFooter(HullItemStorageQuery.apply(storageRows, storageFilter));
            });
            return;
        }
        if (workbenchPanel != null) {
            workbenchPanel.load(row.hullItem());
            return;
        }
        queue(() -> storageListener.selected(row.hullItem() == selectedHullItem ? null : row.hullItem()));
    }

    public boolean workbenchContains(float x, float y) {
        return workbenchPanel != null && workbenchPanel.contains(x, y);
    }

    public boolean isWorkbenchOpen() {
        return workbenchPanel != null || (fadingWorkbenchPanel != null && fadingWorkbenchPanel.isVisible());
    }

    private void toggleWorkbench() {
        if (workbenchPanel != null) {
            workbenchPanel.close();
            return;
        }
        if (selectedHullItem != null) {
            storageListener.selected(null);
        }
        SkillTreeSounds.panelOpened();
        workbenchPanel = SocketWorkbenchPanel.open(hostPanel, panelLeft + panelWidth + WORKBENCH_GAP, panelTop, panelHeight,
                new SocketWorkbenchPanel.Listener() {
                    @Override
                    public void changed(HullItem loaded) {
                        List<HullItem> owned = HullItemStore.get().owned();
                        if (pendingDisassembly.stream().anyMatch(hullItem -> !owned.contains(hullItem))) {
                            closeDisassemblyConfirm();
                        } else if (isConfirmOpen()) {
                            List<HullItem> pendingTargets = pendingDisassembly;
                            closeDisassemblyConfirm();
                            openDisassemblyConfirm(pendingTargets);
                        }
                        markedForDisassembly.removeIf(hullItem -> !owned.contains(hullItem));
                        reloadRows();
                        rebuildTools();
                        rebuildGrid();
                    }

                    @Override
                    public void closed() {
                        fadingWorkbenchPanel = workbenchPanel;
                        workbenchPanel = null;
                    }

                    @Override
                    public void pressedInside() {
                        storageListener.pressedInside();
                    }
                });
    }

    private void cellRightClicked(HullItemStorageRow row) {
        if (!isConfirmOpen() && !disassemblyMode && !row.installed()) {
            queue(() -> openDisassemblyConfirm(List.of(row.hullItem())));
        }
    }

    private float innerWidth() {
        return panelWidth - PAD * 2f;
    }

    private static float widthFor(int columns) {
        return PAD * 2f + SCROLLBAR_ROOM + columns * MIN_CELL_SIZE + (columns - 1) * GAP;
    }

    static int columnsFor(float panelWidth) {
        float gridWidth = panelWidth - PAD * 2f - SCROLLBAR_ROOM;
        return Math.max(1, (int) ((gridWidth + GAP + COLUMN_FIT_SLACK) / (MIN_CELL_SIZE + GAP)));
    }

    @Override
    protected void build() {
        rebuildHeader();
        buildSearch();
        rebuildTools();
        reloadRows();
        rebuildControls();
    }

    @Override
    protected void rebuildHeader() {
        closeDisassemblyConfirm();
        headerElement = finishHeader(startHeader("ui.socketStorage.title", "ui.socketStorage.close", Control.CLOSE), headerElement);
        refreshOpacity();
    }

    private static float workbenchButtonWidth() {
        return Global.getSettings().computeStringWidth(Translation.text("ui.socketStorage.workbench"), Fonts.ORBITRON_20AA) + BUTTON_TEXT_PADDING;
    }

    private static float toolsWidth() {
        return workbenchButtonWidth() + GAP + PARTS_ROOM;
    }

    private float searchRowTop() {
        return PAD + HEADER_HEIGHT + GAP;
    }

    private void buildSearch() {
        float searchWidth = innerWidth() - toolsWidth() - GAP;
        TooltipMakerAPI element = panelRoot.createUIElement(searchWidth, FIELD_HEIGHT, false);
        VanillaText.plainPara(element, Translation.text("ui.socketStorage.search"), 0f, Misc.getGrayColor())
                .getPosition().inTL(0f, LINE_PAD * 2f);
        searchField = element.addTextField(searchWidth - LABEL_WIDTH, FIELD_HEIGHT, Fonts.DEFAULT_SMALL, 0f);
        searchField.getPosition().inTL(LABEL_WIDTH, 0f);
        searchField.setText(storageFilter.query);
        typedQuery = storageFilter.query;
        panelRoot.addUIElement(element).inTL(PAD, searchRowTop());
        searchRowElement = element;
    }

    private void rebuildTools() {
        float workbenchWidth = workbenchButtonWidth();
        TooltipMakerAPI element = panelRoot.createUIElement(toolsWidth(), FIELD_HEIGHT, false);
        element.addButton(Translation.text("ui.socketStorage.workbench"), Control.WORKBENCH, workbenchWidth, HEADER_BUTTON_HEIGHT, 0f)
                .getPosition().inTR(0f, (FIELD_HEIGHT - HEADER_BUTTON_HEIGHT) / 2f);
        addPartsCount(element, workbenchWidth + GAP);
        toolsElement = replaceElement(toolsElement, element, PAD + innerWidth() - toolsWidth(), searchRowTop());
        refreshOpacity();
    }

    private void reloadRows() {
        List<HullItem> owned = HullItemStore.get().owned();
        List<HullItemStorageRow> built = new ArrayList<>(owned.size());
        for (int i = 0; i < owned.size(); i++) {
            if (showsKind(owned.get(i).kind())) {
                built.add(HullItemStorageRow.of(owned.get(i), i, installedShipLookup));
            }
        }
        storageRows = built;
    }

    private boolean showsKind(HullItemType kind) {
        if (kindFilter != null) {
            return kind == kindFilter;
        }
        return storageFilter.socketTypes.isEmpty() || storageFilter.socketTypes.contains(kind);
    }

    private void rebuildControls() {
        List<HullItemType> chipTypes = kindFilter == null ? List.of(HullItemType.values()) : List.of();
        int typeRows = typeChipRows(chipTypes);
        int controlRows = FIXED_CONTROL_ROWS + typeRows;
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), controlRows * CHIP_HEIGHT + (controlRows - 1) * GAP, false);
        float chipLeft = 0f;
        for (HullItemStorageFilter.Status status : HullItemStorageFilter.Status.values()) {
            String label = Translation.text("ui.socketStorage.status." + status.name().toLowerCase(Locale.ROOT));
            chipLeft += addChip(element, label, new StatusChip(status), ChipColors.player(), storageFilter.status == status, chipLeft, 0) + GAP;
        }
        chipLeft = 0f;
        for (HullItemRarity rarity : HullItemRarity.values()) {
            String label = Translation.text("ui.socketStorage.rarity." + rarity.name().toLowerCase(Locale.ROOT));
            chipLeft += addChip(element, label, new RarityChip(rarity), ChipColors.of(rarity.color()), storageFilter.rarities.contains(rarity),
                    chipLeft, 1) + GAP;
        }
        chipLeft = 0f;
        int chipRow = FIXED_CONTROL_ROWS;
        for (HullItemType socketType : chipTypes) {
            String label = socketType.displayName();
            float chipWidth = chipWidth(label);
            if (chipLeft > 0f && chipLeft + chipWidth > innerWidth()) {
                chipLeft = 0f;
                chipRow++;
            }
            chipLeft += addChip(element, label, new TypeChip(socketType), ChipColors.of(typeColor(socketType)),
                    storageFilter.socketTypes.contains(socketType), chipLeft, chipRow) + GAP;
        }
        float controlsTop = PAD + HEADER_HEIGHT + GAP + FIELD_HEIGHT + GAP;
        controlsElement = replaceElement(controlsElement, element, PAD, controlsTop);
        gridTop = controlsTop + controlRows * (CHIP_HEIGHT + GAP) + NOTICE_HEIGHT;
        rebuildGrid();
    }

    private static float chipWidth(String label) {
        return Global.getSettings().computeStringWidth(label, Fonts.ORBITRON_12) + CHIP_TEXT_PADDING;
    }

    private int typeChipRows(List<HullItemType> chipTypes) {
        if (chipTypes.isEmpty()) {
            return 0;
        }
        int rows = 1;
        float chipLeft = 0f;
        for (HullItemType socketType : chipTypes) {
            float chipWidth = chipWidth(socketType.displayName());
            if (chipLeft > 0f && chipLeft + chipWidth > innerWidth()) {
                chipLeft = 0f;
                rows++;
            }
            chipLeft += chipWidth + GAP;
        }
        return rows;
    }

    private static Color typeColor(HullItemType socketType) {
        return socketType.fitsSection() ? SectionFlair.colorOf(socketType) : Misc.getBasePlayerColor();
    }

    private static float addChip(TooltipMakerAPI element, String label, Object chipId, ChipColors colors, boolean checked, float chipLeft,
                                 int chipRow) {
        float chipWidth = chipWidth(label);
        ButtonAPI chip = element.addAreaCheckbox(label, chipId, colors.base(), colors.dark(), colors.bright(), chipWidth, CHIP_HEIGHT, 0f);
        chip.setChecked(checked);
        chip.getPosition().inTL(chipLeft, chipRow * (CHIP_HEIGHT + GAP));
        return chipWidth;
    }

    private void rebuildGrid() {
        hideHoverTooltip();
        List<HullItemStorageRow> matching = HullItemStorageQuery.apply(storageRows, storageFilter);
        int columns = columnsFor(panelWidth);
        float cellSize = (innerWidth() - SCROLLBAR_ROOM - (columns - 1) * GAP) / columns;
        float gridHeight = Math.max(cellSize, panelHeight - PAD - FOOTER_HEIGHT - GAP - gridTop);
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), gridHeight, true);
        if (matching.isEmpty()) {
            String emptyText;
            if (storageRows.isEmpty() && kindFilter != null) {
                emptyText = Translation.msg("ui.socketStorage.emptyType").arg("type", kindFilter.displayName()).text();
            } else {
                emptyText = Translation.text(storageRows.isEmpty() && storageFilter.socketTypes.isEmpty() ? "ui.socketStorage.empty" : "ui.socketStorage.noMatches");
            }
            VanillaText.plainPara(element, emptyText, 0f, Misc.getGrayColor());
        }
        for (int start = 0; start < matching.size(); start += columns) {
            CustomPanelAPI rowPanel = Global.getSettings().createCustom(innerWidth() - SCROLLBAR_ROOM, cellSize, null);
            for (int i = start; i < Math.min(matching.size(), start + columns); i++) {
                HullItemStorageRow row = matching.get(i);
                CustomPanelAPI cellPanel = Global.getSettings().createCustom(cellSize, cellSize, cellFor(row));
                rowPanel.addComponent(cellPanel).inTL((i - start) * (cellSize + GAP), 0f);
            }
            element.addCustom(rowPanel, start == 0 ? 0f : GAP);
        }
        gridElement = replaceElement(placedComponent(gridElement), element, PAD, gridTop);
        rebuildFooter(matching);
    }

    private void rebuildFooter(List<HullItemStorageRow> matching) {
        int shownCount = matching.size();
        TooltipMakerAPI newNotice = panelRoot.createUIElement(innerWidth(), NOTICE_HEIGHT, false);
        if (disassemblyMode) {
            int markedParts = markedForDisassembly.stream().mapToInt(hullItem -> hullItem.rarity().disassemblyParts()).sum();
            String hint = markedForDisassembly.isEmpty() ? Translation.text("ui.socketStorage.disassemblyHint")
                    : Translation.msg("ui.socketStorage.disassemblyMarked").arg("count", markedForDisassembly.size()).arg("parts", markedParts)
                    .text();
            VanillaText.plainPara(newNotice, hint, 0f, Misc.getNegativeHighlightColor());
        } else if (selectedHullItem != null) {
            VanillaText.plainPara(newNotice,
                    placingHint != null ? placingHint : Translation.msg("ui.socketStorage.placingHint").arg("name", selectedHullItem.name()).text(),
                    0f, Misc.getPositiveHighlightColor());
        } else if (targetingSocket) {
            String targetHint = kindFilter == null ? Translation.text("ui.socketStorage.targetHint")
                    : Translation.msg("ui.socketStorage.frameworkTargetHint").arg("type", kindFilter.displayName()).text();
            VanillaText.plainPara(newNotice, targetHint, 0f, Misc.getPositiveHighlightColor());
        } else if (disassembledNotice != null) {
            VanillaText.plainPara(newNotice, disassembledNotice, 0f, Misc.getPositiveHighlightColor());
        } else if (movedFromCargo > 0 || materialsNotice != null) {
            VanillaText.plainPara(newNotice, cargoNotice(), 0f, Misc.getPositiveHighlightColor());
        }
        noticeElement = replaceElement(noticeElement, newNotice, PAD, gridTop - NOTICE_HEIGHT);

        TooltipMakerAPI newSummary = panelRoot.createUIElement(innerWidth(), FOOTER_HEIGHT, false);
        int installedCount = (int) storageRows.stream().filter(HullItemStorageRow::installed).count();
        VanillaText.plainPara(newSummary, Translation.msg("ui.socketStorage.summary")
                .arg("shown", shownCount).arg("stored", storageRows.size()).arg("installed", installedCount).text(), 0f, Misc.getGrayColor())
                .getPosition().inTL(0f, PARTS_TEXT_TOP);
        String modeKey = !disassemblyMode ? "ui.socketStorage.disassemblyMode"
                : markedForDisassembly.isEmpty() ? "ui.socketStorage.exitDisassemblyMode" : "ui.socketStorage.confirmDisassembly";
        String modeLabel = Translation.text(modeKey);
        float modeWidth = Global.getSettings().computeStringWidth(modeLabel, Fonts.ORBITRON_20AA) + BUTTON_TEXT_PADDING;
        ButtonAPI modeButton = newSummary.addButton(modeLabel, Control.DISASSEMBLY_MODE, modeWidth, FOOTER_HEIGHT, 0f);
        modeButton.getPosition().inTR(0f, 0f);
        modeButton.setEnabled(disassemblyMode || storageRows.stream().anyMatch(row -> !row.installed()));
        summaryElement = replaceElement(summaryElement, newSummary, PAD, panelHeight - PAD - FOOTER_HEIGHT);
    }

    private void openDisassemblyConfirm(List<HullItem> targets) {
        if (targets.isEmpty() || isConfirmOpen()) {
            return;
        }
        pendingDisassembly = List.copyOf(targets);
        openConfirm(targets.size() == 1 ? CONFIRM_HEIGHT : BATCH_CONFIRM_HEIGHT, element -> writeDisassemblyConfirm(element, targets),
                "ui.socketStorage.confirm.disassemble", Control.CONFIRM_DISASSEMBLE, "ui.socketStorage.confirm.cancel", Control.CANCEL_DISASSEMBLE);
    }

    private void writeDisassemblyConfirm(TooltipMakerAPI element, List<HullItem> targets) {
        int totalParts = targets.stream().mapToInt(hullItem -> hullItem.rarity().disassemblyParts()).sum();
        if (targets.size() == 1) {
            HullItem hullItem = targets.get(0);
            VanillaText.plainPara(element, Translation.msg("ui.socketStorage.confirm.title").arg("name", hullItem.name()).text(), 0f,
                    hullItem.rarity().color());
            VanillaText.plainPara(element, partsText("ui.socketStorage.confirm.body", totalParts), LINE_PAD * 2f, Misc.getTextColor());
        } else {
            VanillaText.plainPara(element, Translation.msg("ui.socketStorage.confirm.batchTitle").arg("count", targets.size()).text(), 0f,
                    Misc.getBasePlayerColor());
            addRarityBreakdown(element, targets);
            VanillaText.plainPara(element, Translation.msg("ui.socketStorage.confirm.batchBody").arg("count", totalParts).text(), LINE_PAD * 2f,
                    Misc.getTextColor());
            if (targets.stream().anyMatch(hullItem -> hullItem.rarity() == HullItemRarity.UNIQUE)) {
                VanillaText.plainPara(element, Translation.text("ui.socketStorage.confirm.includesUnique"), LINE_PAD * 2f,
                        Misc.getNegativeHighlightColor());
            }
        }
    }

    private static void addRarityBreakdown(TooltipMakerAPI element, List<HullItem> targets) {
        List<String> rarityCounts = new ArrayList<>();
        List<Color> rarityColors = new ArrayList<>();
        for (HullItemRarity rarity : HullItemRarity.values()) {
            long count = targets.stream().filter(hullItem -> hullItem.rarity() == rarity).count();
            if (count > 0) {
                rarityCounts.add(Translation.msg("ui.socketStorage.confirm.rarityCount").arg("count", count)
                        .arg("rarity", Translation.text("ui.socketStorage.rarity." + rarity.name().toLowerCase(Locale.ROOT))).text());
                rarityColors.add(rarity.color());
            }
        }
        String format = String.join(Translation.text("ui.socketStorage.confirm.raritySeparator"),
                rarityCounts.stream().map(text -> "%s").toList());
        element.addPara(format, LINE_PAD * 2f, rarityColors.toArray(new Color[0]), rarityCounts.toArray(new String[0]));
    }

    private void disassemble(List<HullItem> targets) {
        CraftingMaterials materials = playerMaterials();
        int totalParts = 0;
        int disassembledCount = 0;
        HullItem lastDisassembled = null;
        for (HullItem hullItem : targets) {
            int gainedParts = HullItemDisassembly.disassemble(hullItem, materials);
            if (gainedParts > 0) {
                totalParts += gainedParts;
                disassembledCount++;
                lastDisassembled = hullItem;
            }
        }
        if (disassembledCount == 1) {
            disassembledNotice = Translation.msg("ui.socketStorage.disassembled").count(totalParts)
                    .arg("name", lastDisassembled.name()).arg("count", totalParts).text();
        } else if (disassembledCount > 1) {
            disassembledNotice = Translation.msg("ui.socketStorage.disassembledMany").arg("count", disassembledCount)
                    .arg("parts", totalParts).text();
        }
    }

    private static String partsText(String key, int partCount) {
        return Translation.msg(key).count(partCount).arg("count", partCount).text();
    }

    private void closeDisassemblyConfirm() {
        if (!isConfirmOpen()) {
            return;
        }
        closeConfirm();
        pendingDisassembly = List.of();
    }

    private List<StyledText> cellFooter(HullItemStorageRow row) {
        if (row.installed()) {
            String installedKey = isSubroutine(row) ? "ui.socketStorage.cell.installed" : "ui.socketStorage.cell.installedFramework";
            return List.of(Translation.msg(installedKey).arg("ship", row.installedShipName()).styled());
        }
        if (disassemblyMode) {
            int partCount = row.hullItem().rarity().disassemblyParts();
            String key = markedForDisassembly.contains(row.hullItem()) ? "ui.socketStorage.cell.unmark" : "ui.socketStorage.cell.mark";
            return List.of(Translation.msg(key).count(partCount).arg("count", partCount).styled());
        }
        StyledText installHint;
        if (workbenchPanel != null) {
            installHint = Translation.styled("ui.socketStorage.cell.workbench");
        } else if (targetingSocket) {
            installHint = Translation.styled("ui.socketStorage.cell.freeTarget");
        } else if (isSubroutine(row)) {
            installHint = Translation.styled("ui.socketStorage.cell.free");
        } else {
            installHint = Translation.msg("ui.socketStorage.cell.freeFramework").arg("type", row.hullItem().kind().displayName()).styled();
        }
        return List.of(installHint, Translation.msg("ui.socketStorage.cell.disassemble")
                .count(row.hullItem().rarity().disassemblyParts()).arg("count", row.hullItem().rarity().disassemblyParts()).styled());
    }

    private static boolean isSubroutine(HullItemStorageRow row) {
        return row.hullItem().kind() == null || row.hullItem().kind() == HullItemType.SUBROUTINE;
    }

    private HullItemCell cellFor(HullItemStorageRow row) {
        return new HullItemCell(row.hullItem(), CELL_LOOK, new HullItemCell.Listener() {
            @Override
            public void hovered(PositionAPI cellPosition) {
                cellHovered(row, cellPosition);
            }

            @Override
            public void left() {
                cellLeft(row);
            }

            @Override
            public boolean clicked() {
                cellClicked(row);
                return true;
            }

            @Override
            public boolean rightClicked() {
                cellRightClicked(row);
                return true;
            }

            @Override
            public boolean selected() {
                return row.hullItem() == selectedHullItem || (workbenchPanel != null && row.hullItem() == workbenchPanel.loaded());
            }

            @Override
            public boolean marked() {
                return markedForDisassembly.contains(row.hullItem());
            }

            @Override
            public boolean dimmed() {
                return row.installed();
            }
        });
    }

    private void cellHovered(HullItemStorageRow row, PositionAPI cellPosition) {
        queue(() -> showHoverTooltip(row, cellPosition));
    }

    private void cellLeft(HullItemStorageRow row) {
        queue(() -> {
            if (hoverTooltip.isShowing(row)) {
                hideHoverTooltip();
            }
        });
    }

    private void showHoverTooltip(HullItemStorageRow row, PositionAPI cellPosition) {
        if (isConfirmOpen()) {
            return;
        }
        hoverTooltip.show(row, row.hullItem(), () -> cellFooter(row), cellPosition);
    }

    private void hideHoverTooltip() {
        hoverTooltip.hide();
    }
}
