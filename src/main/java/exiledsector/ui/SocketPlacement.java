package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.items.HullUpgradesSetting;
import exiledsector.items.SocketCustody;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemType;
import exiledsector.items.ShipSection;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.ui.node.NodeSearch;
import exiledsector.ui.node.TreeAllocationSession;
import exiledsector.ui.socket.SocketPanelPlacement;
import exiledsector.ui.socket.SocketPanelTab;
import exiledsector.ui.socket.ItemStoragePanel;

import java.util.Map;
import java.util.function.Consumer;

final class SocketPlacement {

    interface HullUpgradesTab {

        boolean available();

        boolean isOpen();

        void swapOut();

        void swapIn();

        void open();

        void close();
    }

    private static final HullUpgradesTab NO_HULL_UPGRADES_TAB = new HullUpgradesTab() {
        @Override
        public boolean available() {
            return false;
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public void swapOut() {
        }

        @Override
        public void swapIn() {
        }

        @Override
        public void open() {
        }

        @Override
        public void close() {
        }
    };

    private static final float PANEL_TOP = SkillTreeChrome.sidePanelTop();
    private static final float PANEL_GAP = 12f;

    private final CustomPanelAPI hostPanel;
    private final TreeAllocationSession treeSession;
    private final NodeSearch nodeSearch;
    private final SkillTreeSearchBar searchBar;
    private final SkillTreeUiButton storageButton = new SkillTreeUiButton("");
    private ItemStoragePanel storagePanel;
    private Item placingItem;
    private SkillNode targetSocket;
    private ShipSection targetSection;
    private boolean inspectingShip;
    private int storageRevision;
    private int shownStoredCount = -1;
    private Map<String, FleetMemberAPI> ownedShips = Map.of();
    private ScreenRect lastShipCard = ScreenRect.NONE;
    private PositionAPI treeCanvasPosition;
    private HullUpgradesTab hullUpgradesTab = NO_HULL_UPGRADES_TAB;
    private final Consumer<SocketPanelTab> storageTabListener = this::storageTabChosen;

    SocketPlacement(CustomPanelAPI hostPanel, TreeAllocationSession treeSession, NodeSearch nodeSearch, SkillTreeSearchBar searchBar) {
        this.hostPanel = hostPanel;
        this.treeSession = treeSession;
        this.nodeSearch = nodeSearch;
        this.searchBar = searchBar;
        refreshButtonLabel();
    }

    float sidePanelHeight(PositionAPI canvasPosition) {
        return sidePanelHeight(canvasPosition, lastShipCard);
    }

    private static float sidePanelHeight(PositionAPI canvasPosition, ScreenRect shipCard) {
        float reservedBottom = shipCard.bottom() + shipCard.height() - canvasPosition.getY() + PANEL_GAP;
        return canvasPosition.getHeight() - PANEL_TOP - reservedBottom;
    }

    boolean isStorageOpen() {
        return storagePanel != null;
    }

    boolean isEngaged() {
        return storagePanel != null || placingItem != null;
    }

    boolean isWorkbenchOpen() {
        return storagePanel != null && storagePanel.isWorkbenchOpen();
    }

    boolean panelContains(float x, float y) {
        return storagePanel != null && storagePanel.contains(x, y);
    }

    boolean buttonContains(float x, float y) {
        return storageButton.contains(x, y);
    }

    ScreenRect storageButtonBounds() {
        return storageButton.bounds();
    }

    boolean isButtonClickable(float x, float y) {
        return storageButton.isClickable(x, y);
    }

    boolean isSectionTargeted() {
        return targetSection != null;
    }

    ShipSection targetSection() {
        return targetSection;
    }

    void setHullUpgradesTab(HullUpgradesTab tab) {
        hullUpgradesTab = tab;
    }

    void setCanvasPosition(PositionAPI canvasPosition) {
        treeCanvasPosition = canvasPosition;
    }

    void openForSection(ShipSection section, PositionAPI canvasPosition, ScreenRect shipCard) {
        stopPlacing();
        clearTargetSocket();
        targetSection = section;
        if (storagePanel != null) {
            storagePanel.restrictTo(section.itemType());
        } else {
            open(canvasPosition, shipCard, section.itemType());
        }
        storagePanel.setTargetingSocket(true);
    }

    void close() {
        if (storagePanel != null) {
            storagePanel.close();
        }
    }

    void swapToHullUpgrades() {
        if (storagePanel == null || !hullUpgradesTab.available()) {
            return;
        }
        ItemStoragePanel swapped = storagePanel;
        forgetStorage();
        swapped.swapOut();
        hullUpgradesTab.swapIn();
    }

    void openFromHullUpgradesTab() {
        if (treeCanvasPosition != null) {
            open(treeCanvasPosition, lastShipCard);
        }
    }

    private void storageTabChosen(SocketPanelTab tab) {
        if (tab == SocketPanelTab.HULL_UPGRADES) {
            swapToHullUpgrades();
        }
    }

    void escape() {
        if (storagePanel != null && storagePanel.escape()) {
            return;
        }
        if (targetSection != null && storagePanel != null) {
            if (hullUpgradesTab.available()) {
                swapToHullUpgrades();
            } else {
                storagePanel.close();
            }
            return;
        }
        if (placingItem != null) {
            stopPlacing();
        } else if (targetSocket != null && storagePanel != null) {
            clearTargetSocket();
        } else if (storagePanel != null && hullUpgradesTab.available()) {
            swapToHullUpgrades();
        } else if (storagePanel != null) {
            storagePanel.close();
        }
    }

    boolean emptySocket(SkillNode node) {
        if (node == null || node.getType().getTier() != SkillTier.SOCKET) {
            return false;
        }
        if (treeSession.emptySocket(node)) {
            refresh();
        }
        return true;
    }

    void advance(boolean mustClose) {
        if (storagePanel == null) {
            return;
        }
        refreshButtonLabel();
        if (mustClose) {
            storagePanel.close();
        } else if (treeSession.statsRevision() != storageRevision) {
            refresh();
            if (targetSocket != null && !treeSession.isAllocatedSocket(targetSocket)) {
                clearTargetSocket();
            }
        }
    }

    void layoutButton(PositionAPI canvasPosition, ScreenRect shipCard, boolean buttonShown) {
        lastShipCard = shipCard;
        if (!buttonShown) {
            storageButton.hide();
            return;
        }
        float buttonWidth = Math.max(ChromeMetrics.MIN_BUTTON_WIDTH, storageButton.preferredWidth());
        storageButton.place(shipCard.left() + shipCard.width() + ChromeMetrics.EDGE_MARGIN, canvasPosition.getY() + ChromeMetrics.EDGE_MARGIN,
                buttonWidth, ChromeMetrics.CONTROL_HEIGHT);
    }

    void renderButton(float mouseX, float mouseY, float alphaMult, boolean live) {
        storageButton.setEnabled(live);
        storageButton.render(mouseX, mouseY, alphaMult);
    }

    void toggle(PositionAPI canvasPosition, ScreenRect shipCard) {
        if (storagePanel != null) {
            storagePanel.close();
        } else if (hullUpgradesTab.isOpen()) {
            hullUpgradesTab.close();
        } else if (hullUpgradesTab.available()) {
            hullUpgradesTab.open();
        } else {
            open(canvasPosition, shipCard);
        }
    }

    void refreshButtonSelected() {
        storageButton.setSelected(storagePanel != null || hullUpgradesTab.isOpen());
    }

    boolean placeInto(SkillNode node) {
        if (placingItem == null) {
            return false;
        }
        if (treeSession.installInSocket(node, placingItem)) {
            stopPlacing();
            refresh();
        } else {
            SkillTreeSounds.refused();
        }
        return true;
    }

    boolean handleSocketClick(SkillNode node, boolean ctrlDown, PositionAPI canvasPosition, ScreenRect shipCard) {
        if (ctrlDown && node.getType().getTier() == SkillTier.SOCKET) {
            if (!treeSession.isAllocated(node)) {
                treeSession.clickNode(node, false);
            }
            if (treeSession.isAllocated(node)) {
                setTargetSocket(node);
                open(canvasPosition, shipCard);
            }
            return true;
        }
        if (targetSocket != null && storagePanel != null && treeSession.isAllocatedSocket(node)) {
            setTargetSocket(node);
            return true;
        }
        return false;
    }

    void refresh() {
        refreshButtonLabel();
        if (storagePanel != null) {
            storageRevision = treeSession.statsRevision();
            storagePanel.refresh(SocketCustody.shipNames(ownedShips));
            if (placingItem != null) {
                storagePanel.setSelected(placingItem, placingHint(placingItem));
            }
        }
    }

    private void refreshButtonLabel() {
        int storedCount = Global.getSector() == null ? 0 : ItemStore.get().owned().size();
        if (storedCount == shownStoredCount) {
            return;
        }
        shownStoredCount = storedCount;
        storageButton.setLabel(Translation.msg("ui.socketStorage.button").arg("count", storedCount).text());
    }

    private void open(PositionAPI canvasPosition, ScreenRect shipCard) {
        open(canvasPosition, shipCard, targetSocket != null ? ItemKind.SUBROUTINE : null);
    }

    private void open(PositionAPI canvasPosition, ScreenRect shipCard, ItemKind kindFilter) {
        if (storagePanel != null) {
            return;
        }
        boolean swapping = hullUpgradesTab.isOpen();
        if (swapping) {
            hullUpgradesTab.swapOut();
        } else {
            SkillTreeSounds.panelOpened();
        }
        treeSession.closeDropdown();
        searchBar.unfocus();
        ownedShips = SocketCustody.reconcile();
        storageRevision = treeSession.statsRevision();
        SocketPanelPlacement placement = new SocketPanelPlacement(ChromeMetrics.EDGE_MARGIN, PANEL_TOP, ItemStoragePanel.WIDTH,
                sidePanelHeight(canvasPosition, shipCard), swapping);
        storagePanel = ItemStoragePanel.open(hostPanel, placement, SocketCustody.shipNames(ownedShips), new ItemStoragePanel.Listener() {
                    @Override
                    public void selected(Item item) {
                        if (item == null) {
                            stopPlacing();
                        } else if (!installInTargetSocket(item)) {
                            startPlacing(item);
                        }
                    }

                    @Override
                    public void closed() {
                        forgetStorage();
                    }

                    @Override
                    public void pressedInside() {
                        searchBar.unfocus();
                    }
                }, kindFilter, storageTabs());
        storagePanel.setTargetingSocket(targetSocket != null);
        refreshButtonSelected();
    }

    private Consumer<SocketPanelTab> storageTabs() {
        return hullUpgradesTab.available() ? storageTabListener : null;
    }

    private void forgetStorage() {
        storagePanel = null;
        stopPlacing();
        clearTargetSocket();
        targetSection = null;
        refreshButtonLabel();
        refreshButtonSelected();
    }

    private void startPlacing(Item item) {
        placingItem = item;
        nodeSearch.setSocketFocus(true, isSubroutine(item));
        if (storagePanel != null) {
            storagePanel.setSelected(item, placingHint(item));
        }
    }

    Item placingItem() {
        return placingItem;
    }

    void setInspectingShip(boolean inspecting) {
        if (inspecting == inspectingShip) {
            return;
        }
        inspectingShip = inspecting;
        if (storagePanel != null) {
            storagePanel.setTabListener(storageTabs());
        }
        if (placingItem != null && storagePanel != null) {
            storagePanel.setSelected(placingItem, placingHint(placingItem));
        }
    }

    boolean placeIntoSection(ShipSection section) {
        if (placingItem == null) {
            return false;
        }
        if (placingItem.canSocketInto(section) && treeSession.installInSection(section, placingItem)) {
            stopPlacing();
            refresh();
        } else {
            SkillTreeSounds.refused();
        }
        return true;
    }

    private String placingHint(Item item) {
        String name = item.name();
        if (isSubroutine(item)) {
            return Translation.msg(inspectingShip ? "ui.socketStorage.placingHintShipView" : "ui.socketStorage.placingHint")
                    .arg("name", name).text();
        }
        ItemKind socketType = item.kind();
        String key;
        if (!HullUpgradesSetting.enabled()) {
            key = "ui.socketStorage.placingHintFrameworkDisabled";
        } else if (!treeSession.isSectionUnlocked(socketType.section())) {
            key = "ui.socketStorage.placingHintFrameworkLocked";
        } else {
            key = inspectingShip ? "ui.socketStorage.placingHintFramework" : "ui.socketStorage.placingHintFrameworkZoom";
        }
        return Translation.msg(key).arg("name", name).arg("type", socketType.displayName()).text();
    }

    private static boolean isSubroutine(Item item) {
        return item.type() != ItemType.HULL_ITEM;
    }

    private void stopPlacing() {
        placingItem = null;
        nodeSearch.setSocketFocus(false);
        if (storagePanel != null) {
            storagePanel.setSelected(null);
        }
    }

    private void setTargetSocket(SkillNode socket) {
        stopPlacing();
        targetSocket = socket;
        treeSession.setTargetedSocket(socket);
        if (storagePanel != null) {
            storagePanel.setTargetingSocket(socket != null);
            storagePanel.restrictTo(socket != null ? ItemKind.SUBROUTINE : null);
        }
    }

    private void clearTargetSocket() {
        targetSocket = null;
        treeSession.setTargetedSocket(null);
        if (storagePanel != null) {
            storagePanel.setTargetingSocket(false);
            if (targetSection == null) {
                storagePanel.restrictTo(null);
            }
        }
    }

    private boolean installInTargetSocket(Item item) {
        if (targetSection != null) {
            if (treeSession.installInSection(targetSection, item)) {
                refresh();
            }
            return true;
        }
        if (targetSocket == null) {
            return false;
        }
        if (!treeSession.isAllocatedSocket(targetSocket)) {
            clearTargetSocket();
            return false;
        }
        if (treeSession.installInSocket(targetSocket, item)) {
            refresh();
        }
        return true;
    }
}
