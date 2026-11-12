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
import exiledsector.DockedStorage;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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
    private static final float MIN_MENU_BUTTON_WIDTH = 150f;
    private static final int NARROW_MENU_COLUMNS = 2;
    private static final float LABEL_WIDTH = 70f;
    private static final float NOTICE_HEIGHT = 38f;
    private static final float FOOTER_HEIGHT = 24f;
    private static final int COMPACT_COLUMNS = 5;
    private static final int TREE_COLUMNS = 6;
    public static final float COMPACT_WIDTH = widthFor(COMPACT_COLUMNS);
    public static final float WIDTH = widthFor(TREE_COLUMNS);
    private static final float SEARCH_TOOLTIP_WIDTH = 360f;
    private static final float CRAFTING_BENCH_GAP = 12f;
    private static final float CRAFTING_BENCH_EDGE_MARGIN = 16f;
    private static final float BUTTON_TEXT_PADDING = 30f;
    private static final float SEARCH_DELAY_SECONDS = 0.25f;
    private static final ItemCell.Look CELL_LOOK = new ItemCell.Look(7f, true, 1f);
    public static final Predicate<Item> ANY_ITEM = item -> true;

    private enum Control {CLOSE, CRAFTING_BENCH, DISASSEMBLY_MODE, CONFIRM_DISASSEMBLE, CANCEL_DISASSEMBLE, RESET_FILTERS, ADVANCED_SEARCH}

    private enum Menu {STATUS, RARITY, TYPE, SORT}

    private record MenuButton(Menu menu) {
    }

    private record StatusChip(ItemStorageFilter.Status status) {
    }

    private record RarityChip(ItemRarity rarity) {
    }

    private record SortChip(ItemStorageFilter.Sort sort) {
    }

    private record TypeChip(ItemKind sectionKind) {
    }

    private final Listener storageListener;
    private final ItemStorageFilter storageFilter = ItemStorageFilter.SESSION;
    private final FilterMenu filterMenu = new FilterMenu(this::buttonPressed, () -> queue(this::closeFilterMenu));
    private final Map<Menu, ButtonAPI> menuButtons = new EnumMap<>(Menu.class);

    private Function<Item, String> installedShipLookup;
    private CraftingBenchPanel craftingBenchPanel;
    private CraftingBenchPanel fadingCraftingBenchPanel;
    private float gridTop;
    private UIComponentAPI headerElement;
    private UIComponentAPI searchRowElement;
    private UIComponentAPI feedstockElement;
    private UIComponentAPI controlsElement;
    private ButtonAPI resetFiltersButton;
    private ButtonAPI advancedSearchButton;
    private AdvancedSearchRoom advancedSearchRoom;
    private AdvancedSearchPane advancedSearchPane;
    private boolean advancedSearchOpening;
    private TooltipMakerAPI gridElement;
    private boolean keepGridScroll;
    private UIComponentAPI noticeElement;
    private UIComponentAPI footerElement;
    private TextFieldAPI searchField;
    private String typedQuery;
    private float searchDelay;
    private List<ItemStorageRow> storageRows = List.of();
    private Item selectedItem;
    private Item spotlightItem;
    private Item craftedItem;
    private String placingHint;
    private String socketRefusalNotice;
    private boolean targetingSocket;
    private final DisassemblySelection disassembly = new DisassemblySelection(DisassemblySelection.ITEM_TEXTS);
    private int movedFromCargo;
    private String materialsNotice;
    private String disassembledNotice;
    private ItemKind kindFilter;
    private Predicate<Item> targetFits = ANY_ITEM;
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
                                                    Predicate<Item> targetFits, Consumer<SocketPanelTab> tabListener, Set<SocketPanelTab> shownTabs) {
        ItemStoragePanel storagePanel = new ItemStoragePanel(hostPanel, installedShipLookup, storageListener, kindFilter);
        storagePanel.targetFits = targetFits;
        storagePanel.sectionItemsOnly = true;
        storagePanel.setTabListener(tabListener);
        storagePanel.setShownTabs(shownTabs);
        storagePanel.attachFresh(placement);
        return storagePanel;
    }

    public static ItemStoragePanel openSocketItems(CustomPanelAPI hostPanel, SocketPanelPlacement placement,
                                                   Function<Item, String> installedShipLookup, Listener storageListener) {
        ItemStoragePanel storagePanel = new ItemStoragePanel(hostPanel, installedShipLookup, storageListener, ItemKind.SUBROUTINE);
        storagePanel.titleKey = "ui.socketItems.title";
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
        advancedSearchRoom = placement.advancedSearchRoom();
        attach(placement.panelLeft(), placement.panelTop(), placement.panelWidth(), placement.panelHeight(), placement.instantly());
    }

    public void setModifyHandler(Runnable handler) {
        modifyHandler = handler;
    }

    private static int absorbPlayerCargo() {
        int movedCount = 0;
        for (CargoAPI cargo : DockedStorage.withPlayerCargo(PlayerFleet.cargoOrNull())) {
            movedCount += ItemStore.get().absorbFrom(cargo);
        }
        return movedCount;
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
        closeFilterMenu();
        closeAdvancedSearch();
        if (craftingBenchPanel != null) {
            craftingBenchPanel.close();
        }
        storageListener.closed();
    }

    @Override
    protected void onSwappedOut() {
        closeFilterMenu();
        closeAdvancedSearch();
        if (craftingBenchPanel != null) {
            craftingBenchPanel.close();
        }
    }

    @Override
    protected SocketPanelTab tab() {
        return SocketPanelTab.HULL_ITEMS;
    }

    @Override
    protected float headerToolsWidth() {
        return PARTS_ROOM + GAP;
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement, searchRowElement, feedstockElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{controlsElement, placedComponent(gridElement), noticeElement, footerElement, filterMenu.panel()};
    }

    public void restrictTo(ItemKind sectionKind, Predicate<Item> sectionFits) {
        if (sectionKind == kindFilter && sectionFits == targetFits) {
            return;
        }
        kindFilter = sectionKind;
        targetFits = sectionFits;
        queue(() -> {
            closeFilterMenu();
            closeDisassemblyConfirm();
            reloadRows();
            rebuildControls();
            refreshAdvancedSearch();
        });
    }

    public void setSelected(Item item) {
        setSelected(item, null);
    }

    public void showRefusal(String refusal) {
        socketRefusalNotice = refusal;
        queue(this::rebuildFooter);
    }

    public void clearRefusal() {
        if (socketRefusalNotice == null) {
            return;
        }
        socketRefusalNotice = null;
        queue(this::rebuildFooter);
    }

    public void spotlight(Item item) {
        spotlightItem = item;
        queue(() -> {
            if (item != null && ItemTypes.stacksInStorage(item)) {
                reloadRows();
            }
            rebuildGrid();
        });
    }

    public void setSelected(Item item, String hint) {
        selectedItem = item;
        placingHint = hint;
        socketRefusalNotice = null;
        if (item != null) {
            disassembly.leave();
        }
        queue(this::rebuildFooter);
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
        queue(this::rebuildFooter);
    }

    public void refresh(Function<Item, String> installedShipLookup) {
        this.installedShipLookup = installedShipLookup;
        queue(() -> {
            if (disassembly.pending().stream().anyMatch(SocketCustody::isInstalled)) {
                closeDisassemblyConfirm();
            }
            reloadRows();
            disassembly.forget(item -> SocketCustody.isInstalled(item) || !ItemStore.get().owned().contains(item));
            rebuildFeedstock();
            rebuildGridKeepingScroll();
            if (craftingBenchPanel != null) {
                craftingBenchPanel.refresh();
            }
        });
    }

    public boolean escape() {
        if (filterMenu.isOpen()) {
            queue(this::closeFilterMenu);
            return true;
        }
        if (isConfirmOpen()) {
            queue(this::closeDisassemblyConfirm);
            return true;
        }
        if (advancedSearchPane != null && advancedSearchPane.escape()) {
            return true;
        }
        if (advancedSearchOpening) {
            queue(this::closeAdvancedSearch);
            return true;
        }
        if (disassembly.active()) {
            queue(() -> {
                disassembly.leave();
                rebuildGridKeepingScroll();
            });
            return true;
        }
        if (craftingBenchPanel != null && craftingBenchPanel.escape()) {
            return true;
        }
        if (craftingBenchPanel != null) {
            queue(craftingBenchPanel::close);
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
        if (craftingBenchPanel != null) {
            craftingBenchPanel.refresh();
        }
        storageListener.disassembled();
        if (leaveMode) {
            disassembly.leave();
        }
        reloadRows();
        rebuildFeedstock();
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
        } else if (id == Control.CRAFTING_BENCH) {
            toggleCraftingBench();
        } else if (id == Control.DISASSEMBLY_MODE) {
            disassemblyModePressed();
        } else if (id == Control.RESET_FILTERS) {
            resetFilters();
        } else if (id == Control.ADVANCED_SEARCH) {
            toggleAdvancedSearch();
        } else if (id instanceof MenuButton button) {
            menuButtonPressed(button.menu());
        } else if (id instanceof StatusChip chip) {
            storageFilter.toggleStatus(chip.status());
            filtersChanged();
        } else if (id instanceof RarityChip chip) {
            storageFilter.toggleRarity(chip.rarity());
            filtersChanged();
            refreshAdvancedSearch();
        } else if (id instanceof SortChip chip) {
            storageFilter.setSort(chip.sort());
            closeFilterMenu();
            rebuildControls();
        } else if (id instanceof TypeChip chip) {
            storageFilter.toggleSection(chip.sectionKind());
            reloadRows();
            filtersChanged();
            refreshAdvancedSearch();
        }
    }

    private void menuButtonPressed(Menu menu) {
        if (filterMenu.isOpen(menu)) {
            closeFilterMenu();
            return;
        }
        openFilterMenu(menu);
    }

    private void resetFilters() {
        closeFilterMenu();
        storageFilter.reset();
        if (searchField != null) {
            searchField.setText("");
        }
        typedQuery = "";
        searchDelay = 0f;
        reloadRows();
        rebuildControls();
        refreshAdvancedSearch();
    }

    private boolean offersAdvancedSearch() {
        return advancedSearchRoom != null;
    }

    public boolean advancedSearchContains(float x, float y) {
        return advancedSearchPane != null && advancedSearchPane.contains(x, y);
    }

    private void toggleAdvancedSearch() {
        if (advancedSearchPane != null || advancedSearchOpening) {
            closeAdvancedSearch();
            return;
        }
        if (!offersAdvancedSearch()) {
            return;
        }
        closeFilterMenu();
        hideHoverTooltip();
        advancedSearchOpening = true;
        syncAdvancedSearchButton();
        advancedSearchRoom.open(area -> queue(() -> openAdvancedSearchIn(area)));
    }

    private void openAdvancedSearchIn(FreeArea area) {
        if (!advancedSearchOpening) {
            return;
        }
        advancedSearchOpening = false;
        advancedSearchPane = AdvancedSearchPane.open(hostPanel, AdvancedSearchLayout.in(area), storageFilter, new AdvancedSearchHost());
        syncAdvancedSearchButton();
    }

    private void closeAdvancedSearch() {
        AdvancedSearchPane closingPane = advancedSearchPane;
        advancedSearchPane = null;
        if (closingPane != null) {
            closingPane.close();
        } else if (advancedSearchOpening) {
            advancedSearchOpening = false;
            if (!isClosing()) {
                advancedSearchRoom.closed();
            }
        }
        syncAdvancedSearchButton();
    }

    private void refreshAdvancedSearch() {
        if (advancedSearchPane != null) {
            advancedSearchPane.refresh();
        }
    }

    private void syncAdvancedSearchButton() {
        if (advancedSearchButton != null) {
            advancedSearchButton.setChecked(advancedSearchPane != null || advancedSearchOpening);
        }
    }

    private final class AdvancedSearchHost implements AdvancedSearchPane.Host {

        @Override
        public List<ItemKind> typeChoices() {
            return chipTypes();
        }

        @Override
        public Collection<ItemKind> rollableKinds() {
            if (kindFilter != null) {
                return List.of(kindFilter);
            }
            return chipTypes().stream().filter(storageFilter.sectionKinds::contains).toList();
        }

        @Override
        public void filtersPicked(boolean sectionsChanged) {
            queue(() -> {
                closeDisassemblyConfirm();
                if (sectionsChanged) {
                    reloadRows();
                }
                filtersChanged();
            });
        }

        @Override
        public void searchApplied() {
            queue(() -> {
                closeDisassemblyConfirm();
                rebuildGrid();
            });
        }

        @Override
        public void pressedInside() {
            hideHoverTooltip();
            storageListener.pressedInside();
        }

        @Override
        public void closed() {
            advancedSearchPane = null;
            syncAdvancedSearchButton();
            if (!isClosing()) {
                advancedSearchRoom.closed();
            }
        }

        @Override
        public boolean wantsEnter() {
            return isConfirmOpen() || (searchField != null && searchField.hasFocus());
        }
    }

    private void filtersChanged() {
        Object openMenu = filterMenu.openMenuId();
        rebuildControls();
        if (openMenu instanceof Menu menu) {
            openFilterMenu(menu);
        }
    }

    private void openFilterMenu(Menu menu) {
        List<Menu> menus = menus();
        int menuIndex = menus.indexOf(menu);
        if (menuIndex < 0 || isConfirmOpen()) {
            return;
        }
        hideHoverTooltip();
        int columns = controlColumns(menus);
        float buttonWidth = menuButtonWidth(columns);
        float buttonLeft = PAD + (menuIndex % columns) * (buttonWidth + GAP);
        float buttonTop = controlsTop() + (menuIndex / columns) * (ChipFlow.CHIP_HEIGHT + GAP);
        filterMenu.open(panelRoot, panelWidth, panelHeight, menu, new FilterMenu.Anchor(buttonLeft, buttonTop + ChipFlow.CHIP_HEIGHT + LINE_PAD, buttonWidth),
                menuOptions(menu));
        syncMenuButtons();
        refreshOpacity();
    }

    private void closeFilterMenu() {
        filterMenu.close();
        syncMenuButtons();
    }

    private void syncMenuButtons() {
        menuButtons.forEach((menu, button) -> button.setChecked(filterMenu.isOpen(menu)));
    }

    private void disassemblyModePressed() {
        if (!disassembly.active()) {
            disassembly.enter();
            if (selectedItem != null) {
                storageListener.selected(null);
            }
            rebuildFooter();
        } else if (disassembly.nothingMarked()) {
            disassembly.leave();
            rebuildFooter();
        } else {
            openDisassemblyConfirm(disassembly.marked());
        }
    }

    private void cellClicked(ItemStorageRow row) {
        if (isConfirmOpen() || !clickable(row.installed(), loadsIntoBench(), disassembly.active())) {
            return;
        }
        if (disassembly.active() && ctrlHeld()) {
            queue(() -> disassembleNow(List.of(row.item()), false));
            return;
        }
        if (disassembly.active()) {
            queue(() -> {
                disassembly.toggle(row.item());
                rebuildFooter();
            });
            return;
        }
        if (craftingBenchPanel != null) {
            craftingBenchPanel.load(row.item() == craftingBenchPanel.loaded() ? null : row.item());
            return;
        }
        queue(() -> storageListener.selected(row.item() == selectedItem ? null : row.item()));
    }

    private boolean loadsIntoBench() {
        return pickerMode || craftingBenchPanel != null;
    }

    static boolean clickable(boolean installed, boolean loadsIntoBench, boolean disassembling) {
        return !installed || (loadsIntoBench && !disassembling);
    }

    public boolean craftingBenchContains(float x, float y) {
        return craftingBenchPanel != null && craftingBenchPanel.contains(x, y);
    }

    public boolean isCraftingBenchOpen() {
        return craftingBenchPanel != null || (fadingCraftingBenchPanel != null && fadingCraftingBenchPanel.isVisible());
    }

    private void toggleCraftingBench() {
        if (craftingBenchPanel != null) {
            craftingBenchPanel.close();
            return;
        }
        if (modifyHandler != null) {
            modifyHandler.run();
            return;
        }
        if (selectedItem != null) {
            storageListener.selected(null);
        }
        closeFilterMenu();
        SkillTreeSounds.panelOpened();
        float benchLeft = panelLeft + panelWidth + CRAFTING_BENCH_GAP;
        float benchWidth = hostPanel.getPosition().getWidth() - benchLeft - CRAFTING_BENCH_EDGE_MARGIN;
        craftingBenchPanel = CraftingBenchPanel.open(hostPanel, new SocketPanelPlacement(benchLeft, panelTop, benchWidth, panelHeight, false), false,
                new CraftingBenchPanel.Listener() {
                    @Override
                    public void changed(Item loaded) {
                        if (loaded != null) {
                            craftedItem = loaded;
                        }
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
                        rebuildFeedstock();
                        rebuildGridKeepingScroll();
                    }

                    @Override
                    public void closed() {
                        fadingCraftingBenchPanel = craftingBenchPanel;
                        craftingBenchPanel = null;
                        Item benchItem = craftedItem;
                        craftedItem = null;
                        if (benchItem != null && !isClosing()) {
                            queue(() -> backFromBench(benchItem));
                        }
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
        reloadRows();
        rebuildControls();
    }

    @Override
    protected void rebuildHeader() {
        closeDisassemblyConfirm();
        headerElement = finishHeader(startHeader(titleKey,"ui.socketStorage.close", Control.CLOSE), headerElement);
        rebuildFeedstock();
    }

    private void rebuildFeedstock() {
        TooltipMakerAPI element = panelRoot.createUIElement(PARTS_ROOM, HEADER_HEIGHT, false);
        addFeedstockCount(element, 0f);
        feedstockElement = replaceElement(feedstockElement, element, PAD + innerWidth() - CLOSE_BUTTON_WIDTH - GAP - PARTS_ROOM, PAD);
        refreshOpacity();
    }

    private static float craftingBenchButtonWidth() {
        return Global.getSettings().computeStringWidth(Translation.text("ui.socketStorage.craftingBench"), Fonts.ORBITRON_20AA) + BUTTON_TEXT_PADDING;
    }

    private void backFromBench(Item item) {
        if (!ItemStore.get().owned().contains(item)) {
            return;
        }
        spotlightItem = item;
        rebuildGrid();
        if (!SocketCustody.isInstalled(item) && !targetingSocket && !pickerMode) {
            storageListener.selected(item);
        }
    }

    private void buildSearch() {
        float searchWidth = innerWidth();
        TooltipMakerAPI element = panelRoot.createUIElement(searchWidth, FIELD_HEIGHT, false);
        VanillaText.plainPara(element, Translation.text("ui.socketStorage.search"), 0f, Misc.getGrayColor())
                .getPosition().inTL(0f, LINE_PAD * 2f);
        String searchTooltip = Translation.text("ui.socketStorage.search.tooltip");
        element.addTooltipToPrevious(new GameTextTooltip(SEARCH_TOOLTIP_WIDTH, tooltip -> VanillaText.plainPara(tooltip, searchTooltip, 0f,
                Misc.getTextColor())), TooltipMakerAPI.TooltipLocation.BELOW);
        searchField = element.addTextField(searchWidth - LABEL_WIDTH, FIELD_HEIGHT, Fonts.DEFAULT_SMALL, 0f);
        searchField.getPosition().inTL(LABEL_WIDTH, 0f);
        searchField.setText(storageFilter.query);
        typedQuery = storageFilter.query;
        panelRoot.addUIElement(element).inTL(PAD, PAD + HEADER_HEIGHT + GAP);
        searchRowElement = element;
    }

    private void reloadRows() {
        storageRows = ItemStorageStacks.rows(ItemStore.get().owned(), this::showsItem, installedShipLookup, this::preferredInStack);
    }

    private boolean preferredInStack(Item item) {
        return item == selectedItem || item == spotlightItem || disassembly.isMarked(item)
                || (craftingBenchPanel != null && item == craftingBenchPanel.loaded());
    }

    private boolean showsItem(Item item) {
        return showsItem(item, kindFilter, sectionItemsOnly, storageFilter.sectionKinds) && targetFits.test(item);
    }

    static boolean showsItem(Item item, ItemKind kindFilter, boolean sectionItemsOnly, Set<ItemKind> chosenSections) {
        ItemKind kind = ItemTypes.listedKind(item);
        if (kindFilter != null) {
            return kind == kindFilter;
        }
        if (sectionItemsOnly && (kind == null || !kind.fitsSection())) {
            return false;
        }
        List<ItemKind> chipTypes = sectionItemsOnly ? ItemKind.sectionItemTypes() : List.of(ItemKind.values());
        return chipTypes.stream().noneMatch(chosenSections::contains) || chosenSections.contains(kind);
    }

    private List<ItemKind> chipTypes() {
        if (kindFilter != null) {
            return List.of();
        }
        return sectionItemsOnly ? ItemKind.sectionItemTypes() : List.of(ItemKind.values());
    }

    private static String sortKey(ItemStorageFilter.Sort sort) {
        return "ui.socketStorage.sort." + sort.name().toLowerCase(Locale.ROOT);
    }

    private static String statusLabel(ItemStorageFilter.Status status) {
        return Translation.text("ui.socketStorage.status." + status.name().toLowerCase(Locale.ROOT));
    }

    static String rarityLabel(ItemRarity rarity) {
        return Translation.text("ui.socketStorage.rarity." + rarity.name().toLowerCase(Locale.ROOT));
    }

    private List<Menu> menus() {
        List<Menu> shown = new ArrayList<>(List.of(Menu.STATUS, Menu.RARITY));
        if (!chipTypes().isEmpty()) {
            shown.add(Menu.TYPE);
        }
        shown.add(Menu.SORT);
        return shown;
    }

    private int controlSlots(List<Menu> menus) {
        return menus.size() + (offersAdvancedSearch() ? 2 : 1);
    }

    private int controlColumns(List<Menu> menus) {
        int slotCount = controlSlots(menus);
        float oneRowWidth = slotCount * MIN_MENU_BUTTON_WIDTH + (slotCount - 1) * GAP;
        return innerWidth() >= oneRowWidth ? slotCount : Math.min(slotCount, NARROW_MENU_COLUMNS);
    }

    private float menuButtonWidth(int columns) {
        return (innerWidth() - (columns - 1) * GAP) / columns;
    }

    private float controlsTop() {
        return PAD + HEADER_HEIGHT + GAP + FIELD_HEIGHT + GAP;
    }

    private List<FilterMenu.Option> menuOptions(Menu menu) {
        return switch (menu) {
            case STATUS -> Arrays.stream(ItemStorageFilter.Status.values()).map(status -> new FilterMenu.Option(statusLabel(status),
                    new StatusChip(status), ChipFlow.Colors.player(), storageFilter.statuses.contains(status), null)).toList();
            case RARITY -> FilterMenu.rarityOptions(storageFilter.rarities, RarityChip::new);
            case TYPE -> FilterMenu.typeOptions(chipTypes(), storageFilter.sectionKinds, TypeChip::new);
            case SORT -> Arrays.stream(ItemStorageFilter.Sort.values()).map(sort -> new FilterMenu.Option(Translation.text(sortKey(sort)),
                    new SortChip(sort), ChipFlow.Colors.player(), storageFilter.sort == sort, Translation.text(sortKey(sort) + ".tooltip"))).toList();
        };
    }

    private String menuButtonLabel(Menu menu, float buttonWidth) {
        List<String> chosenLabels = switch (menu) {
            case STATUS -> FilterMenu.chosenLabels(Arrays.asList(ItemStorageFilter.Status.values()), storageFilter.statuses,
                    ItemStoragePanel::statusLabel);
            case RARITY -> FilterMenu.chosenLabels(Arrays.asList(ItemRarity.values()), storageFilter.rarities, ItemStoragePanel::rarityLabel);
            case TYPE -> FilterMenu.chosenLabels(chipTypes(), storageFilter.sectionKinds, ItemKind::displayName);
            case SORT -> List.of(Translation.text(sortKey(storageFilter.sort)));
        };
        return FilterMenu.buttonLabel("ui.socketStorage.menu." + menu.name().toLowerCase(Locale.ROOT), chosenLabels, buttonWidth);
    }

    private void rebuildControls() {
        List<Menu> menus = menus();
        int columns = controlColumns(menus);
        int rows = (controlSlots(menus) + columns - 1) / columns;
        float buttonWidth = menuButtonWidth(columns);
        TooltipMakerAPI element = panelRoot.createUIElement(innerWidth(), ChipFlow.height(rows), false);
        ChipFlow.Colors colors = ChipFlow.Colors.player();
        menuButtons.clear();
        for (int index = 0; index < menus.size(); index++) {
            Menu menu = menus.get(index);
            ButtonAPI menuButton = element.addAreaCheckbox(menuButtonLabel(menu, buttonWidth), new MenuButton(menu), colors.base(), colors.dark(),
                    colors.bright(), buttonWidth, ChipFlow.CHIP_HEIGHT, 0f);
            menuButton.getPosition().inTL((index % columns) * (buttonWidth + GAP), (index / columns) * (ChipFlow.CHIP_HEIGHT + GAP));
            menuButtons.put(menu, menuButton);
        }
        resetFiltersButton = element.addAreaCheckbox(Translation.text("ui.socketStorage.resetFilters"), Control.RESET_FILTERS, colors.base(),
                colors.dark(), colors.bright(), buttonWidth, ChipFlow.CHIP_HEIGHT, 0f);
        resetFiltersButton.getPosition().inTL((columns - 1) * (buttonWidth + GAP), (rows - 1) * (ChipFlow.CHIP_HEIGHT + GAP));
        advancedSearchButton = null;
        if (offersAdvancedSearch()) {
            advancedSearchButton = element.addAreaCheckbox(Translation.text("ui.advancedSearch.button"), Control.ADVANCED_SEARCH, colors.base(),
                    colors.dark(), colors.bright(), buttonWidth, ChipFlow.CHIP_HEIGHT, 0f);
            advancedSearchButton.getPosition().inTL((columns - 2) * (buttonWidth + GAP), (rows - 1) * (ChipFlow.CHIP_HEIGHT + GAP));
            syncAdvancedSearchButton();
        }
        syncMenuButtons();
        controlsElement = replaceElement(controlsElement, element, PAD, controlsTop());
        gridTop = controlsTop() + ChipFlow.rowsHeight(rows) + NOTICE_HEIGHT;
        rebuildGrid();
    }

    static Color typeColor(ItemKind sectionKind) {
        Color kindColor = CargoIconLooks.kindColor(sectionKind);
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
        if (resetFiltersButton != null) {
            resetFiltersButton.setEnabled(!storageFilter.isDefault(offersAdvancedSearch()));
        }
        List<ItemStorageRow> matching = spotlightFirst(ItemStorageQuery.apply(storageRows, storageFilter, offersAdvancedSearch()));
        int columns = ItemGrid.columnsFor(panelWidth);
        float gridHeight = Math.max(ItemGrid.cellSize(innerWidth(), columns), panelHeight - PAD - FOOTER_HEIGHT - GAP - gridTop);
        TooltipMakerAPI element = ItemGrid.build(panelRoot, innerWidth(), gridHeight, columns, matching, this::cellFor,
                row -> row.stacked() ? String.valueOf(row.stackSize()) : null, emptyText());
        gridElement = replaceElement(placedComponent(gridElement), element, PAD, gridTop);
        if (restoredScroll > 0f && element.getExternalScroller() != null) {
            element.getExternalScroller().setYOffset(Math.min(restoredScroll, Math.max(0f, element.getHeightSoFar() - gridHeight)));
        }
        rebuildFooter();
    }

    private List<ItemStorageRow> spotlightFirst(List<ItemStorageRow> matching) {
        ItemStorageRow spotlightRow = storageRows.stream().filter(row -> row.item() == spotlightItem && !row.installed()).findFirst().orElse(null);
        if (spotlightRow == null) {
            spotlightItem = null;
            return matching;
        }
        List<ItemStorageRow> ordered = new ArrayList<>(matching.size() + 1);
        ordered.add(spotlightRow);
        matching.stream().filter(row -> row != spotlightRow).forEach(ordered::add);
        return ordered;
    }

    private String emptyText() {
        if (storageRows.isEmpty() && kindFilter != null) {
            return Translation.msg("ui.socketStorage.emptyType").arg("type", kindFilter.displayName()).text();
        }
        return Translation.text(storageRows.isEmpty() && chipTypes().stream().noneMatch(storageFilter.sectionKinds::contains) ? "ui.socketStorage.empty"
                : "ui.socketStorage.noMatches");
    }

    private void rebuildFooter() {
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

        TooltipMakerAPI newFooter = panelRoot.createUIElement(innerWidth(), FOOTER_HEIGHT, false);
        if (!pickerMode && !disassembly.active()) {
            newFooter.addButton(Translation.text("ui.socketStorage.craftingBench"), Control.CRAFTING_BENCH, craftingBenchButtonWidth(), FOOTER_HEIGHT, 0f)
                    .getPosition().inTL(0f, 0f);
        }
        disassembly.addModeButton(newFooter, Control.DISASSEMBLY_MODE, FOOTER_HEIGHT, storageRows.stream().anyMatch(row -> !row.installed()));
        footerElement = replaceElement(footerElement, newFooter, PAD, panelHeight - PAD - FOOTER_HEIGHT);
        filterMenu.raise();
    }

    private void openDisassemblyConfirm(List<Item> targets) {
        if (targets.isEmpty() || isConfirmOpen()) {
            return;
        }
        closeFilterMenu();
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
        if (row.stacked()) {
            footerLines.add(Translation.msg("ui.socketStorage.cell.stack").arg("count", row.stackSize()).styled());
        }
        if (craftingBenchPanel == null && !pickerMode) {
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
                return row.item() == selectedItem || (craftingBenchPanel != null && row.item() == craftingBenchPanel.loaded());
            }

            @Override
            public boolean marked() {
                return disassembly.isMarked(row.item());
            }

            @Override
            public boolean dimmed() {
                return !clickable(row.installed(), loadsIntoBench(), disassembly.active());
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
        if (isConfirmOpen() || filterMenu.isOpen()) {
            return;
        }
        hoverTooltip.show(row, row.item(), expanded -> cellFooter(row, expanded), cellPosition);
    }

    private void hideHoverTooltip() {
        hoverTooltip.hide();
    }
}
