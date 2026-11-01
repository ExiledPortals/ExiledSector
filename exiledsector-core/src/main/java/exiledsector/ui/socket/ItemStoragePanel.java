package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
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
import exiledsector.items.SocketCustody;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemTypes;
import exiledsector.items.Item;
import exiledsector.items.ItemDisassembly;
import exiledsector.items.ItemRarity;
import exiledsector.items.ItemStore;
import exiledsector.items.MaterialMigration;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.VanillaText;
import exiledsector.ui.cargo.CargoIconLooks;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ItemStoragePanel extends HoloPanel {

    public interface Listener {

        void selected(Item item);

        void closed();

        void pressedInside();

        default void disassembled() {
        }

        default void hovered(Item item) {
        }
    }

    private static final float FIELD_HEIGHT = 26f;
    private static final int FIXED_CONTROL_ROWS = 2;
    private static final float LABEL_WIDTH = 70f;
    private static final float NOTICE_HEIGHT = 56f;
    private static final float FOOTER_HEIGHT = 24f;
    private static final int COMPACT_COLUMNS = 5;
    private static final int TREE_COLUMNS = 6;
    public static final float COMPACT_WIDTH = widthFor(COMPACT_COLUMNS);
    public static final float WIDTH = widthFor(TREE_COLUMNS);
    private static final float WORKBENCH_GAP = 12f;
    private static final float WORKBENCH_EDGE_MARGIN = 16f;
    private static final float BUTTON_TEXT_PADDING = 30f;
    private static final float SEARCH_DELAY_SECONDS = 0.25f;
    private static final ItemCell.Look CELL_LOOK = new ItemCell.Look(7f, true, 1f);

    private enum Control {CLOSE, WORKBENCH, DISASSEMBLY_MODE, CONFIRM_DISASSEMBLE, CANCEL_DISASSEMBLE}

    private record StatusChip(ItemStorageFilter.Status status) {
    }

    private record RarityChip(ItemRarity rarity) {
    }

    private record TypeChip(ItemKind socketType) {
    }

    private final Listener storageListener;
    private final ItemStorageFilter storageFilter = ItemStorageFilter.SESSION;

    private Function<Item, String> installedShipLookup;
    private SocketWorkbenchPanel workbenchPanel;
    private SocketWorkbenchPanel fadingWorkbenchPanel;
    private float gridTop;
    private UIComponentAPI headerElement;
    private UIComponentAPI searchRowElement;
    private UIComponentAPI toolsElement;
    private UIComponentAPI controlsElement;
    private TooltipMakerAPI gridElement;
    private boolean keepGridScroll;
    private UIComponentAPI noticeElement;
    private UIComponentAPI summaryElement;
    private TextFieldAPI searchField;
    private String typedQuery;
    private float searchDelay;
    private List<ItemStorageRow> storageRows = List.of();
    private Item selectedItem;
    private String placingHint;
    private String socketRefusalNotice;
    private boolean targetingSocket;
    private final DisassemblySelection disassembly = new DisassemblySelection(DisassemblySelection.ITEM_TEXTS);
    private int movedFromCargo;
    private String materialsNotice;
    private String disassembledNotice;
    private ItemKind kindFilter;
    private Runnable modifyHandler;
    private boolean pickerMode;
    private String titleKey = "ui.socketStorage.title";
    private boolean sectionItemsOnly;

    private ItemStoragePanel(CustomPanelAPI hostPanel, Function<Item, String> installedShipLookup, Listener storageListener,
                               ItemKind kindFilter) {
        super(hostPanel);
        this.kindFilter = kindFilter;
        this.installedShipLookup = installedShipLookup;
        this.storageListener = storageListener;
    }

    public static ItemStoragePanel openSectionItems(CustomPanelAPI hostPanel, SocketPanelPlacement placement,
                                                    Function<Item, String> installedShipLookup, Listener storageListener, ItemKind kindFilter,
                                                    Consumer<SocketPanelTab> tabListener, Set<SocketPanelTab> shownTabs) {
        ItemStoragePanel storagePanel = new ItemStoragePanel(hostPanel, installedShipLookup, storageListener, kindFilter);
        storagePanel.sectionItemsOnly = true;
        storagePanel.setTabListener(tabListener);
        storagePanel.setShownTabs(shownTabs);
        storagePanel.attachFresh(placement);
        return storagePanel;
    }

    public static ItemStoragePanel openSubroutines(CustomPanelAPI hostPanel, SocketPanelPlacement placement,
                                                   Function<Item, String> installedShipLookup, Listener storageListener) {
        ItemStoragePanel storagePanel = new ItemStoragePanel(hostPanel, installedShipLookup, storageListener, ItemKind.SUBROUTINE);
        storagePanel.titleKey = "ui.subroutines.title";
        storagePanel.attachFresh(placement);
        return storagePanel;
    }

    public static ItemStoragePanel openPicker(CustomPanelAPI hostPanel, SocketPanelPlacement placement, Function<Item, String> installedShipLookup,
                                              Listener storageListener, boolean sectionItemsOnly) {
        ItemStoragePanel storagePanel = new ItemStoragePanel(hostPanel, installedShipLookup, storageListener, null);
        storagePanel.pickerMode = true;
        storagePanel.sectionItemsOnly = sectionItemsOnly;
        storagePanel.attachFresh(placement);
        return storagePanel;
    }

    private void attachFresh(SocketPanelPlacement placement) {
        movedFromCargo = absorbPlayerCargo();
        materialsNotice = settleMaterials();
        attach(placement.panelLeft(), placement.panelTop(), placement.panelWidth(), placement.panelHeight(), placement.instantly());
    }

    public void setModifyHandler(Runnable handler) {
        modifyHandler = handler;
    }

    private static int absorbPlayerCargo() {
        CargoAPI cargo = PlayerFleet.cargoOrNull();
        return cargo == null ? 0 : ItemStore.get().absorbFrom(cargo);
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
        MaterialMigration.convert(PlayerFleet.cargoOrNull());
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

    public void restrictTo(ItemKind socketType) {
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

    public void setSelected(Item item) {
        setSelected(item, null);
    }

    public void showRefusal(String refusal) {
        socketRefusalNotice = refusal;
        queue(() -> rebuildFooter(ItemStorageQuery.apply(storageRows, storageFilter)));
    }

    public void clearRefusal() {
        if (socketRefusalNotice == null) {
            return;
        }
        socketRefusalNotice = null;
        queue(() -> rebuildFooter(ItemStorageQuery.apply(storageRows, storageFilter)));
    }

    public void setSelected(Item item, String hint) {
        selectedItem = item;
        placingHint = hint;
        socketRefusalNotice = null;
        if (item != null) {
            disassembly.leave();
        }
        queue(() -> rebuildFooter(ItemStorageQuery.apply(storageRows, storageFilter)));
    }

    public void setTargetingSocket(boolean targeting) {
        if (targeting == targetingSocket) {
            clearRefusal();
            return;
        }
        socketRefusalNotice = null;
        targetingSocket = targeting;
        if (targeting) {
            disassembly.leave();
        }
        queue(() -> rebuildFooter(ItemStorageQuery.apply(storageRows, storageFilter)));
    }

    public void refresh(Function<Item, String> installedShipLookup) {
        this.installedShipLookup = installedShipLookup;
        queue(() -> {
            if (disassembly.pending().stream().anyMatch(SocketCustody::isInstalled)) {
                closeDisassemblyConfirm();
            }
            reloadRows();
            disassembly.forget(item -> SocketCustody.isInstalled(item) || !ItemStore.get().owned().contains(item));
            rebuildTools();
            rebuildGridKeepingScroll();
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
        if (disassembly.active()) {
            queue(() -> {
                disassembly.leave();
                rebuildGridKeepingScroll();
            });
            return true;
        }
        if (workbenchPanel != null && workbenchPanel.escape()) {
            return true;
        }
        if (workbenchPanel != null) {
            queue(workbenchPanel::close);
            return true;
        }
        return false;
    }

    private void disassembleNow(List<Item> requested, boolean leaveMode) {
        List<Item> targets = requested.stream().filter(item -> !SocketCustody.isInstalled(item)
                && ItemStore.get().owned().contains(item)).toList();
        if (targets.isEmpty()) {
            return;
        }
        if (selectedItem != null && targets.contains(selectedItem)) {
            storageListener.selected(null);
        }
        CraftingMaterials materials = playerMaterials();
        String notice = disassembly.disassemble(targets, item -> ItemDisassembly.disassemble(item, materials));
        if (notice != null) {
            disassembledNotice = notice;
        }
        if (workbenchPanel != null) {
            workbenchPanel.refresh();
        }
        storageListener.disassembled();
        if (leaveMode) {
            disassembly.leave();
        }
        reloadRows();
        rebuildTools();
        keepGridScroll = true;
        rebuildControls();
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
        if (!disassembly.active()) {
            disassembly.enter();
            if (selectedItem != null) {
                storageListener.selected(null);
            }
            rebuildFooter(ItemStorageQuery.apply(storageRows, storageFilter));
        } else if (disassembly.nothingMarked()) {
            disassembly.leave();
            rebuildFooter(ItemStorageQuery.apply(storageRows, storageFilter));
        } else {
            openDisassemblyConfirm(disassembly.marked());
        }
    }

    private void cellClicked(ItemStorageRow row) {
        if (isConfirmOpen() || row.installed()) {
            return;
        }
        if (disassembly.active() && ctrlHeld()) {
            queue(() -> disassembleNow(List.of(row.item()), false));
            return;
        }
        if (disassembly.active()) {
            queue(() -> {
                disassembly.toggle(row.item());
                rebuildFooter(ItemStorageQuery.apply(storageRows, storageFilter));
            });
            return;
        }
        if (workbenchPanel != null) {
            workbenchPanel.load(row.item() == workbenchPanel.loaded() ? null : row.item());
            return;
        }
        queue(() -> storageListener.selected(row.item() == selectedItem ? null : row.item()));
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
        if (modifyHandler != null) {
            modifyHandler.run();
            return;
        }
        if (selectedItem != null) {
            storageListener.selected(null);
        }
        SkillTreeSounds.panelOpened();
        float benchLeft = panelLeft + panelWidth + WORKBENCH_GAP;
        float benchWidth = hostPanel.getPosition().getWidth() - benchLeft - WORKBENCH_EDGE_MARGIN;
        workbenchPanel = SocketWorkbenchPanel.open(hostPanel, new SocketPanelPlacement(benchLeft, panelTop, benchWidth, panelHeight, false), false,
                new SocketWorkbenchPanel.Listener() {
                    @Override
                    public void changed(Item loaded) {
                        List<Item> owned = ItemStore.get().owned();
                        if (disassembly.pending().stream().anyMatch(item -> !owned.contains(item))) {
                            closeDisassemblyConfirm();
                        } else if (isConfirmOpen()) {
                            List<Item> pendingTargets = disassembly.pending();
                            closeDisassemblyConfirm();
                            openDisassemblyConfirm(pendingTargets);
                        }
                        disassembly.forget(item -> !owned.contains(item));
                        reloadRows();
                        rebuildTools();
                        rebuildGridKeepingScroll();
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

    private void cellRightClicked(ItemStorageRow row) {
        if (!isConfirmOpen() && !disassembly.active() && !row.installed()) {
            queue(() -> openDisassemblyConfirm(List.of(row.item())));
        }
    }

    private static float widthFor(int columns) {
        return PAD * 2f + ItemGrid.SCROLLBAR_ROOM + columns * ItemGrid.MIN_CELL_SIZE + (columns - 1) * GAP;
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
        headerElement = finishHeader(startHeader(titleKey,"ui.socketStorage.close", Control.CLOSE), headerElement);
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
        TooltipMakerAPI element = panelRoot.createUIElement(toolsWidth(), FIELD_HEIGHT, false);
        if (pickerMode) {
            addPartsCount(element, 0f);
        } else {
            float workbenchWidth = workbenchButtonWidth();
            element.addButton(Translation.text("ui.socketStorage.workbench"), Control.WORKBENCH, workbenchWidth, HEADER_BUTTON_HEIGHT, 0f)
                    .getPosition().inTR(0f, (FIELD_HEIGHT - HEADER_BUTTON_HEIGHT) / 2f);
            addPartsCount(element, workbenchWidth + GAP);
        }
        toolsElement = replaceElement(toolsElement, element, PAD + innerWidth() - toolsWidth(), searchRowTop());
        refreshOpacity();
    }

    private void reloadRows() {
        List<Item> owned = ItemStore.get().owned();
        List<ItemStorageRow> built = new ArrayList<>(owned.size());
        for (int i = 0; i < owned.size(); i++) {
            if (showsItem(owned.get(i))) {
                built.add(ItemStorageRow.of(owned.get(i), i, installedShipLookup));
            }
        }
        storageRows = built;
    }

    private boolean showsItem(Item item) {
        return showsItem(item, kindFilter, sectionItemsOnly, storageFilter.socketTypes);
    }

    static boolean showsItem(Item item, ItemKind kindFilter, boolean sectionItemsOnly, Set<ItemKind> chosenSocketTypes) {
        ItemKind kind = ItemTypes.listedKind(item);
        if (kindFilter != null) {
            return kind == kindFilter;
        }
        if (sectionItemsOnly && (kind == null || !kind.fitsSection())) {
            return false;
        }
        List<ItemKind> chipTypes = sectionItemsOnly ? ItemKind.sectionItemTypes() : List.of(ItemKind.values());
        return chipTypes.stream().noneMatch(chosenSocketTypes::contains) || chosenSocketTypes.contains(kind);
    }

    private List<ItemKind> chipTypes() {
        if (kindFilter != null) {
            return List.of();
        }
        return sectionItemsOnly ? ItemKind.sectionItemTypes() : List.of(ItemKind.values());
    }

    private void rebuildControls() {
        List<ItemKind> chipTypes = chipTypes();
        int typeRows = ChipFlow.rowsFor(chipTypes.stream().map(ItemKind::displayName).toList(), innerWidth());
        int controlRows = FIXED_CONTROL_ROWS + typeRows;
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), ChipFlow.height(controlRows), false);
        ChipFlow chips = new ChipFlow(element, innerWidth());
        for (ItemStorageFilter.Status status : ItemStorageFilter.Status.values()) {
            String label = Translation.text("ui.socketStorage.status." + status.name().toLowerCase(Locale.ROOT));
            chips.placeInRow(label, new StatusChip(status), ChipFlow.Colors.player(), storageFilter.status == status);
        }
        chips.startRow(1);
        for (ItemRarity rarity : ItemRarity.values()) {
            String label = Translation.text("ui.socketStorage.rarity." + rarity.name().toLowerCase(Locale.ROOT));
            chips.placeInRow(label, new RarityChip(rarity), ChipFlow.Colors.of(rarity.color()), storageFilter.rarities.contains(rarity));
        }
        chips.startRow(FIXED_CONTROL_ROWS);
        for (ItemKind socketType : chipTypes) {
            chips.place(socketType.displayName(), new TypeChip(socketType), ChipFlow.Colors.of(typeColor(socketType)),
                    storageFilter.socketTypes.contains(socketType));
        }
        float controlsTop = PAD + HEADER_HEIGHT + GAP + FIELD_HEIGHT + GAP;
        controlsElement = replaceElement(controlsElement, element, PAD, controlsTop);
        gridTop = controlsTop + ChipFlow.rowsHeight(controlRows) + NOTICE_HEIGHT;
        rebuildGrid();
    }

    private static Color typeColor(ItemKind socketType) {
        Color kindColor = CargoIconLooks.kindColor(socketType);
        return kindColor != null ? kindColor : Misc.getBasePlayerColor();
    }

    private void rebuildGridKeepingScroll() {
        keepGridScroll = true;
        rebuildGrid();
    }

    private float gridScrollOffset() {
        if (gridElement == null || gridElement.getExternalScroller() == null) {
            return 0f;
        }
        return gridElement.getExternalScroller().getYOffset();
    }

    private void rebuildGrid() {
        float restoredScroll = keepGridScroll ? gridScrollOffset() : 0f;
        keepGridScroll = false;
        hideHoverTooltip();
        List<ItemStorageRow> matching = ItemStorageQuery.apply(storageRows, storageFilter);
        int columns = ItemGrid.columnsFor(panelWidth);
        float gridHeight = Math.max(ItemGrid.cellSize(innerWidth(), columns), panelHeight - PAD - FOOTER_HEIGHT - GAP - gridTop);
        TooltipMakerAPI element = ItemGrid.build(panelRoot, innerWidth(), gridHeight, columns, matching, this::cellFor, emptyText());
        gridElement = replaceElement(placedComponent(gridElement), element, PAD, gridTop);
        if (restoredScroll > 0f && element.getExternalScroller() != null) {
            element.getExternalScroller().setYOffset(Math.min(restoredScroll, Math.max(0f, element.getHeightSoFar() - gridHeight)));
        }
        rebuildFooter(matching);
    }

    private String emptyText() {
        if (storageRows.isEmpty() && kindFilter != null) {
            return Translation.msg("ui.socketStorage.emptyType").arg("type", kindFilter.displayName()).text();
        }
        return Translation.text(storageRows.isEmpty() && chipTypes().stream().noneMatch(storageFilter.socketTypes::contains) ? "ui.socketStorage.empty"
                : "ui.socketStorage.noMatches");
    }

    private void rebuildFooter(List<ItemStorageRow> matching) {
        int shownCount = matching.size();
        TooltipMakerAPI newNotice = panelRoot.createUIElement(innerWidth(), NOTICE_HEIGHT, false);
        if (disassembly.active()) {
            VanillaText.plainPara(newNotice, disassembly.hint(), 0f, Misc.getNegativeHighlightColor());
        } else if (socketRefusalNotice != null) {
            VanillaText.plainPara(newNotice, socketRefusalNotice, 0f, Misc.getNegativeHighlightColor());
        } else if (selectedItem != null && !pickerMode) {
            VanillaText.plainPara(newNotice,
                    placingHint != null ? placingHint : Translation.msg("ui.socketStorage.placingHint").arg("name", selectedItem.name()).text(),
                    0f, Misc.getPositiveHighlightColor());
        } else if (targetingSocket) {
            String targetHint = kindFilter == null || !kindFilter.fitsSection() ? Translation.text("ui.socketStorage.targetHint")
                    : Translation.msg("ui.socketStorage.frameworkTargetHint").arg("type", kindFilter.displayName()).text();
            VanillaText.plainPara(newNotice, targetHint, 0f, Misc.getPositiveHighlightColor());
        } else if (disassembledNotice != null) {
            VanillaText.plainPara(newNotice, disassembledNotice, 0f, Misc.getPositiveHighlightColor());
        } else if (movedFromCargo > 0 || materialsNotice != null) {
            VanillaText.plainPara(newNotice, cargoNotice(), 0f, Misc.getPositiveHighlightColor());
        }
        noticeElement = replaceElement(noticeElement, newNotice, PAD, gridTop - NOTICE_HEIGHT);

        TooltipMakerAPI newSummary = panelRoot.createUIElement(innerWidth(), FOOTER_HEIGHT, false);
        int installedCount = (int) storageRows.stream().filter(ItemStorageRow::installed).count();
        VanillaText.plainPara(newSummary, Translation.msg("ui.socketStorage.summary")
                .arg("shown", shownCount).arg("stored", storageRows.size()).arg("installed", installedCount).text(), 0f, Misc.getGrayColor())
                .getPosition().inTL(0f, PARTS_TEXT_TOP);
        disassembly.addModeButton(newSummary, Control.DISASSEMBLY_MODE, FOOTER_HEIGHT, storageRows.stream().anyMatch(row -> !row.installed()));
        summaryElement = replaceElement(summaryElement, newSummary, PAD, panelHeight - PAD - FOOTER_HEIGHT);
    }

    private void openDisassemblyConfirm(List<Item> targets) {
        if (targets.isEmpty() || isConfirmOpen()) {
            return;
        }
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

    private List<StyledText> cellFooter(ItemStorageRow row, boolean expanded) {
        if (row.installed()) {
            String installedKey = fitsSection(row) ? "ui.socketStorage.cell.installedFramework" : "ui.socketStorage.cell.installed";
            return List.of(Translation.msg(installedKey).arg("ship", row.installedShipName()).styled());
        }
        if (disassembly.active()) {
            return List.of(disassembly.cellLine(row.item()));
        }
        List<StyledText> footerLines = new ArrayList<>();
        if (workbenchPanel == null && !pickerMode) {
            footerLines.add(installHint(row));
        }
        if (expanded) {
            footerLines.add(DisassemblySelection.disassembleHint(row.item()));
        }
        return footerLines;
    }

    private StyledText installHint(ItemStorageRow row) {
        if (targetingSocket) {
            return Translation.styled("ui.socketStorage.cell.freeTarget");
        }
        if (!fitsSection(row)) {
            return Translation.styled("ui.socketStorage.cell.free");
        }
        return Translation.msg("ui.socketStorage.cell.freeFramework").arg("type", row.item().kindName()).styled();
    }

    private static boolean fitsSection(ItemStorageRow row) {
        ItemKind kind = ItemTypes.listedKind(row.item());
        return kind != null && kind.fitsSection();
    }

    private ItemCell cellFor(ItemStorageRow row) {
        return new ItemCell(row.item(), CELL_LOOK, new ItemCell.Listener() {
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
                return row.item() == selectedItem || (workbenchPanel != null && row.item() == workbenchPanel.loaded());
            }

            @Override
            public boolean marked() {
                return disassembly.isMarked(row.item());
            }

            @Override
            public boolean dimmed() {
                return row.installed();
            }
        });
    }

    private void cellHovered(ItemStorageRow row, PositionAPI cellPosition) {
        storageListener.hovered(row.item());
        queue(() -> showHoverTooltip(row, cellPosition));
    }

    private void cellLeft(ItemStorageRow row) {
        storageListener.hovered(null);
        queue(() -> {
            if (hoverTooltip.isShowing(row)) {
                hideHoverTooltip();
            }
        });
    }

    private void showHoverTooltip(ItemStorageRow row, PositionAPI cellPosition) {
        if (isConfirmOpen()) {
            return;
        }
        hoverTooltip.show(row, row.item(), expanded -> cellFooter(row, expanded), cellPosition);
    }

    private void hideHoverTooltip() {
        hoverTooltip.hide();
    }
}
