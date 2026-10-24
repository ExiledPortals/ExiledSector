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
import exiledlib.PlayerFleet;
import exiledlib.i18n.I18n;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledsector.items.SocketCustody;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemType;
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
import java.util.LinkedHashSet;
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
    private static final float WORKBENCH_EDGE_MARGIN = 16f;
    private static final float BATCH_CONFIRM_HEIGHT = 170f;
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

    private record ChipColors(Color base, Color dark, Color bright) {

        static ChipColors of(Color base) {
            return new ChipColors(base, Misc.scaleColorOnly(base, CHIP_DARK_SCALE), base.brighter());
        }

        static ChipColors player() {
            return new ChipColors(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor());
        }
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
    private List<Item> pendingDisassembly = List.of();
    private boolean disassemblyMode;
    private final Set<Item> markedForDisassembly = new LinkedHashSet<>();
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
        storagePanel.movedFromCargo = absorbPlayerCargo();
        storagePanel.materialsNotice = settleMaterials();
        storagePanel.setTabListener(tabListener);
        storagePanel.setShownTabs(shownTabs);
        storagePanel.attach(placement.panelLeft(), placement.panelTop(), placement.panelWidth(), placement.panelHeight(), placement.instantly());
        return storagePanel;
    }

    public static ItemStoragePanel openSubroutines(CustomPanelAPI hostPanel, SocketPanelPlacement placement,
                                                   Function<Item, String> installedShipLookup, Listener storageListener) {
        ItemStoragePanel storagePanel = new ItemStoragePanel(hostPanel, installedShipLookup, storageListener, ItemKind.SUBROUTINE);
        storagePanel.titleKey = "ui.subroutines.title";
        storagePanel.movedFromCargo = absorbPlayerCargo();
        storagePanel.materialsNotice = settleMaterials();
        storagePanel.attach(placement.panelLeft(), placement.panelTop(), placement.panelWidth(), placement.panelHeight(), placement.instantly());
        return storagePanel;
    }

    public static ItemStoragePanel openPicker(CustomPanelAPI hostPanel, SocketPanelPlacement placement, Function<Item, String> installedShipLookup,
                                              Listener storageListener, boolean sectionItemsOnly) {
        ItemStoragePanel storagePanel = new ItemStoragePanel(hostPanel, installedShipLookup, storageListener, null);
        storagePanel.pickerMode = true;
        storagePanel.sectionItemsOnly = sectionItemsOnly;
        storagePanel.movedFromCargo = absorbPlayerCargo();
        storagePanel.materialsNotice = settleMaterials();
        storagePanel.attach(placement.panelLeft(), placement.panelTop(), placement.panelWidth(), placement.panelHeight(), placement.instantly());
        return storagePanel;
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
            leaveDisassemblyMode();
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
            leaveDisassemblyMode();
        }
        queue(() -> rebuildFooter(ItemStorageQuery.apply(storageRows, storageFilter)));
    }

    public void refresh(Function<Item, String> installedShipLookup) {
        this.installedShipLookup = installedShipLookup;
        queue(() -> {
            if (pendingDisassembly.stream().anyMatch(SocketCustody::isInstalled)) {
                closeDisassemblyConfirm();
            }
            reloadRows();
            markedForDisassembly.removeIf(item -> SocketCustody.isInstalled(item)
                    || !ItemStore.get().owned().contains(item));
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
        if (disassemblyMode) {
            queue(() -> {
                leaveDisassemblyMode();
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
        disassemble(targets);
        if (workbenchPanel != null) {
            workbenchPanel.refresh();
        }
        storageListener.disassembled();
        markedForDisassembly.removeAll(targets);
        if (leaveMode && disassemblyMode) {
            leaveDisassemblyMode();
        }
        reloadRows();
        rebuildTools();
        keepGridScroll = true;
        rebuildControls();
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
            if (selectedItem != null) {
                storageListener.selected(null);
            }
            rebuildFooter(ItemStorageQuery.apply(storageRows, storageFilter));
        } else if (markedForDisassembly.isEmpty()) {
            leaveDisassemblyMode();
            rebuildFooter(ItemStorageQuery.apply(storageRows, storageFilter));
        } else {
            openDisassemblyConfirm(List.copyOf(markedForDisassembly));
        }
    }

    private void cellClicked(ItemStorageRow row) {
        if (isConfirmOpen() || row.installed()) {
            return;
        }
        if (disassemblyMode && ctrlHeld()) {
            queue(() -> disassembleNow(List.of(row.item()), false));
            return;
        }
        if (disassemblyMode) {
            queue(() -> {
                if (!markedForDisassembly.remove(row.item())) {
                    markedForDisassembly.add(row.item());
                }
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
                        if (pendingDisassembly.stream().anyMatch(item -> !owned.contains(item))) {
                            closeDisassemblyConfirm();
                        } else if (isConfirmOpen()) {
                            List<Item> pendingTargets = pendingDisassembly;
                            closeDisassemblyConfirm();
                            openDisassemblyConfirm(pendingTargets);
                        }
                        markedForDisassembly.removeIf(item -> !owned.contains(item));
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
        if (!isConfirmOpen() && !disassemblyMode && !row.installed()) {
            queue(() -> openDisassemblyConfirm(List.of(row.item())));
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
        ItemKind kind = listedKind(item);
        if (kindFilter != null) {
            return kind == kindFilter;
        }
        if (sectionItemsOnly && (kind == null || !kind.fitsSection())) {
            return false;
        }
        List<ItemKind> chipTypes = sectionItemsOnly ? ItemKind.sectionItemTypes() : List.of(ItemKind.values());
        return chipTypes.stream().noneMatch(chosenSocketTypes::contains) || chosenSocketTypes.contains(kind);
    }

    public static ItemKind listedKind(Item item) {
        return item.type() == ItemType.WORMHOLE_ANCHOR ? ItemKind.SUBROUTINE : item.kind();
    }

    private List<ItemKind> chipTypes() {
        if (kindFilter != null) {
            return List.of();
        }
        return sectionItemsOnly ? ItemKind.sectionItemTypes() : List.of(ItemKind.values());
    }

    private void rebuildControls() {
        List<ItemKind> chipTypes = chipTypes();
        int typeRows = typeChipRows(chipTypes);
        int controlRows = FIXED_CONTROL_ROWS + typeRows;
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), controlRows * CHIP_HEIGHT + (controlRows - 1) * GAP, false);
        float chipLeft = 0f;
        for (ItemStorageFilter.Status status : ItemStorageFilter.Status.values()) {
            String label = Translation.text("ui.socketStorage.status." + status.name().toLowerCase(Locale.ROOT));
            chipLeft += addChip(element, label, new StatusChip(status), ChipColors.player(), storageFilter.status == status, chipLeft, 0) + GAP;
        }
        chipLeft = 0f;
        for (ItemRarity rarity : ItemRarity.values()) {
            String label = Translation.text("ui.socketStorage.rarity." + rarity.name().toLowerCase(Locale.ROOT));
            chipLeft += addChip(element, label, new RarityChip(rarity), ChipColors.of(rarity.color()), storageFilter.rarities.contains(rarity),
                    chipLeft, 1) + GAP;
        }
        chipLeft = 0f;
        int chipRow = FIXED_CONTROL_ROWS;
        for (ItemKind socketType : chipTypes) {
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

    private int typeChipRows(List<ItemKind> chipTypes) {
        if (chipTypes.isEmpty()) {
            return 0;
        }
        int rows = 1;
        float chipLeft = 0f;
        for (ItemKind socketType : chipTypes) {
            float chipWidth = chipWidth(socketType.displayName());
            if (chipLeft > 0f && chipLeft + chipWidth > innerWidth()) {
                chipLeft = 0f;
                rows++;
            }
            chipLeft += chipWidth + GAP;
        }
        return rows;
    }

    private static Color typeColor(ItemKind socketType) {
        Color kindColor = CargoIconLooks.kindColor(socketType);
        return kindColor != null ? kindColor : Misc.getBasePlayerColor();
    }

    private static float addChip(TooltipMakerAPI element, String label, Object chipId, ChipColors colors, boolean checked, float chipLeft,
                                 int chipRow) {
        float chipWidth = chipWidth(label);
        ButtonAPI chip = element.addAreaCheckbox(label, chipId, colors.base(), colors.dark(), colors.bright(), chipWidth, CHIP_HEIGHT, 0f);
        chip.setChecked(checked);
        chip.getPosition().inTL(chipLeft, chipRow * (CHIP_HEIGHT + GAP));
        return chipWidth;
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
        int columns = columnsFor(panelWidth);
        float cellSize = (innerWidth() - SCROLLBAR_ROOM - (columns - 1) * GAP) / columns;
        float gridHeight = Math.max(cellSize, panelHeight - PAD - FOOTER_HEIGHT - GAP - gridTop);
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), gridHeight, true);
        if (matching.isEmpty()) {
            String emptyText;
            if (storageRows.isEmpty() && kindFilter != null) {
                emptyText = Translation.msg("ui.socketStorage.emptyType").arg("type", kindFilter.displayName()).text();
            } else {
                emptyText = Translation.text(storageRows.isEmpty() && chipTypes().stream().noneMatch(storageFilter.socketTypes::contains) ? "ui.socketStorage.empty" : "ui.socketStorage.noMatches");
            }
            VanillaText.plainPara(element, emptyText, 0f, Misc.getGrayColor());
        }
        for (int start = 0; start < matching.size(); start += columns) {
            CustomPanelAPI rowPanel = Global.getSettings().createCustom(innerWidth() - SCROLLBAR_ROOM, cellSize, null);
            for (int i = start; i < Math.min(matching.size(), start + columns); i++) {
                ItemStorageRow row = matching.get(i);
                CustomPanelAPI cellPanel = Global.getSettings().createCustom(cellSize, cellSize, cellFor(row));
                rowPanel.addComponent(cellPanel).inTL((i - start) * (cellSize + GAP), 0f);
            }
            element.addCustom(rowPanel, start == 0 ? 0f : GAP);
        }
        gridElement = replaceElement(placedComponent(gridElement), element, PAD, gridTop);
        if (restoredScroll > 0f && element.getExternalScroller() != null) {
            element.getExternalScroller().setYOffset(Math.min(restoredScroll, Math.max(0f, element.getHeightSoFar() - gridHeight)));
        }
        rebuildFooter(matching);
    }

    private void rebuildFooter(List<ItemStorageRow> matching) {
        int shownCount = matching.size();
        TooltipMakerAPI newNotice = panelRoot.createUIElement(innerWidth(), NOTICE_HEIGHT, false);
        if (disassemblyMode) {
            int markedParts = markedForDisassembly.stream().mapToInt(item -> item.rarity().disassemblyParts()).sum();
            String hint = markedForDisassembly.isEmpty() ? Translation.text("ui.socketStorage.disassemblyHint")
                    : Translation.msg("ui.socketStorage.disassemblyMarked").arg("count", markedForDisassembly.size()).arg("parts", markedParts)
                    .text();
            VanillaText.plainPara(newNotice, hint, 0f, Misc.getNegativeHighlightColor());
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
        String modeKey = !disassemblyMode ? "ui.socketStorage.disassemblyMode"
                : markedForDisassembly.isEmpty() ? "ui.socketStorage.exitDisassemblyMode" : "ui.socketStorage.confirmDisassembly";
        String modeLabel = Translation.text(modeKey);
        float modeWidth = Global.getSettings().computeStringWidth(modeLabel, Fonts.ORBITRON_20AA) + BUTTON_TEXT_PADDING;
        ButtonAPI modeButton = newSummary.addButton(modeLabel, Control.DISASSEMBLY_MODE, modeWidth, FOOTER_HEIGHT, 0f);
        modeButton.getPosition().inTR(0f, 0f);
        modeButton.setEnabled(disassemblyMode || storageRows.stream().anyMatch(row -> !row.installed()));
        summaryElement = replaceElement(summaryElement, newSummary, PAD, panelHeight - PAD - FOOTER_HEIGHT);
    }

    private void openDisassemblyConfirm(List<Item> targets) {
        if (targets.isEmpty() || isConfirmOpen()) {
            return;
        }
        pendingDisassembly = List.copyOf(targets);
        openConfirm(targets.size() == 1 ? CONFIRM_HEIGHT : BATCH_CONFIRM_HEIGHT, element -> writeDisassemblyConfirm(element, targets),
                "ui.socketStorage.confirm.disassemble", Control.CONFIRM_DISASSEMBLE, "ui.socketStorage.confirm.cancel", Control.CANCEL_DISASSEMBLE);
    }

    private void writeDisassemblyConfirm(TooltipMakerAPI element, List<Item> targets) {
        int totalParts = targets.stream().mapToInt(item -> item.rarity().disassemblyParts()).sum();
        if (targets.size() == 1) {
            Item item = targets.get(0);
            VanillaText.plainPara(element, Translation.msg("ui.socketStorage.confirm.title").arg("name", item.name()).text(), 0f,
                    item.rarity().color());
            VanillaText.plainPara(element, partsText("ui.socketStorage.confirm.body", totalParts), LINE_PAD * 2f, Misc.getTextColor());
        } else {
            VanillaText.plainPara(element, Translation.msg("ui.socketStorage.confirm.batchTitle").arg("count", targets.size()).text(), 0f,
                    Misc.getBasePlayerColor());
            addRarityBreakdown(element, targets);
            VanillaText.plainPara(element, Translation.msg("ui.socketStorage.confirm.batchBody").arg("count", totalParts).text(), LINE_PAD * 2f,
                    Misc.getTextColor());
            if (targets.stream().anyMatch(item -> item.rarity() == ItemRarity.UNIQUE)) {
                VanillaText.plainPara(element, Translation.text("ui.socketStorage.confirm.includesUnique"), LINE_PAD * 2f,
                        Misc.getNegativeHighlightColor());
            }
        }
    }

    private static void addRarityBreakdown(TooltipMakerAPI element, List<Item> targets) {
        List<String> rarityCounts = new ArrayList<>();
        List<Color> rarityColors = new ArrayList<>();
        for (ItemRarity rarity : ItemRarity.values()) {
            long count = targets.stream().filter(item -> item.rarity() == rarity).count();
            if (count > 0) {
                rarityCounts.add(Translation.msg("ui.socketStorage.confirm.rarityCount").arg("count", count)
                        .arg("rarity", Translation.text("ui.socketStorage.rarity." + rarity.name().toLowerCase(Locale.ROOT))).text());
                rarityColors.add(rarity.color());
            }
        }
        String format = String.join(VanillaText.escapeFormat(Translation.text("ui.socketStorage.confirm.raritySeparator")),
                rarityCounts.stream().map(text -> "%s").toList());
        element.addPara(format, LINE_PAD * 2f, rarityColors.toArray(new Color[0]), rarityCounts.toArray(new String[0]));
    }

    private void disassemble(List<Item> targets) {
        CraftingMaterials materials = playerMaterials();
        int totalParts = 0;
        int disassembledCount = 0;
        Item lastDisassembled = null;
        for (Item item : targets) {
            int gainedParts = ItemDisassembly.disassemble(item, materials);
            if (gainedParts > 0) {
                totalParts += gainedParts;
                disassembledCount++;
                lastDisassembled = item;
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

    private List<StyledText> cellFooter(ItemStorageRow row, boolean expanded) {
        if (row.installed()) {
            String installedKey = isSubroutine(row) ? "ui.socketStorage.cell.installed" : "ui.socketStorage.cell.installedFramework";
            return List.of(Translation.msg(installedKey).arg("ship", row.installedShipName()).styled());
        }
        if (disassemblyMode) {
            int partCount = row.item().rarity().disassemblyParts();
            String key = markedForDisassembly.contains(row.item()) ? "ui.socketStorage.cell.unmark" : "ui.socketStorage.cell.mark";
            return List.of(Translation.msg(key).count(partCount).arg("count", partCount).styled());
        }
        if (!expanded) {
            return List.of(installHint(row));
        }
        int partCount = row.item().rarity().disassemblyParts();
        return List.of(installHint(row), Translation.msg("ui.socketStorage.cell.disassemble").count(partCount).arg("count", partCount).styled());
    }

    private StyledText installHint(ItemStorageRow row) {
        if (workbenchPanel != null || pickerMode) {
            return Translation.styled("ui.socketStorage.cell.workbench");
        }
        if (targetingSocket) {
            return Translation.styled("ui.socketStorage.cell.freeTarget");
        }
        if (isSubroutine(row)) {
            return Translation.styled("ui.socketStorage.cell.free");
        }
        return Translation.msg("ui.socketStorage.cell.freeFramework").arg("type", row.item().kindName()).styled();
    }

    private static boolean isSubroutine(ItemStorageRow row) {
        return row.item().type() != ItemType.HULL_ITEM;
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
                return markedForDisassembly.contains(row.item());
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
