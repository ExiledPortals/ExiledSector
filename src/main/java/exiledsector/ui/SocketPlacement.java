package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.hullitems.HullUpgradesSetting;
import exiledsector.hullitems.SocketCustody;
import exiledsector.hullitems.SocketType;
import exiledsector.hullitems.HullItem;
import exiledsector.hullitems.HullItemStore;
import exiledsector.ui.node.NodeSearch;
import exiledsector.ui.node.TreeAllocationSession;
import exiledsector.ui.socket.SocketPanelPlacement;
import exiledsector.ui.socket.SocketPanelTab;
import exiledsector.ui.socket.SocketStoragePanel;

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
    private SocketStoragePanel storagePanel;
    private HullItem placingHullItem;
    private SkillNode targetSocket;
    private SocketType targetFrameworkType;
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
        return storagePanel != null || placingHullItem != null;
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

    boolean isFrameworkTargeted() {
        return targetFrameworkType != null;
    }

    SocketType targetFrameworkType() {
        return targetFrameworkType;
    }

    void setHullUpgradesTab(HullUpgradesTab tab) {
        hullUpgradesTab = tab;
    }

    void setCanvasPosition(PositionAPI canvasPosition) {
        treeCanvasPosition = canvasPosition;
    }

    void openForFrameworkSocket(SocketType socketType, PositionAPI canvasPosition, ScreenRect shipCard) {
        stopPlacing();
        clearTargetSocket();
        targetFrameworkType = socketType;
        if (storagePanel != null) {
            storagePanel.restrictTo(socketType);
        } else {
            open(canvasPosition, shipCard, socketType);
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
        SocketStoragePanel swapped = storagePanel;
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
        if (targetFrameworkType != null && storagePanel != null) {
            if (hullUpgradesTab.available()) {
                swapToHullUpgrades();
            } else {
                storagePanel.close();
            }
            return;
        }
        if (placingHullItem != null) {
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
        if (placingHullItem == null) {
            return false;
        }
        if (treeSession.installInSocket(node, placingHullItem)) {
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
            if (placingHullItem != null) {
                storagePanel.setSelected(placingHullItem, placingHint(placingHullItem));
            }
        }
    }

    private void refreshButtonLabel() {
        int storedCount = Global.getSector() == null ? 0 : HullItemStore.get().owned().size();
        if (storedCount == shownStoredCount) {
            return;
        }
        shownStoredCount = storedCount;
        storageButton.setLabel(Translation.msg("ui.socketStorage.button").arg("count", storedCount).text());
    }

    private void open(PositionAPI canvasPosition, ScreenRect shipCard) {
        open(canvasPosition, shipCard, targetSocket != null ? SocketType.SUBROUTINE : null);
    }

    private void open(PositionAPI canvasPosition, ScreenRect shipCard, SocketType kindFilter) {
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
        SocketPanelPlacement placement = new SocketPanelPlacement(ChromeMetrics.EDGE_MARGIN, PANEL_TOP, SocketStoragePanel.WIDTH,
                sidePanelHeight(canvasPosition, shipCard), swapping);
        storagePanel = SocketStoragePanel.open(hostPanel, placement, SocketCustody.shipNames(ownedShips), new SocketStoragePanel.Listener() {
                    @Override
                    public void selected(HullItem hullItem) {
                        if (hullItem == null) {
                            stopPlacing();
                        } else if (!installInTargetSocket(hullItem)) {
                            startPlacing(hullItem);
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
        targetFrameworkType = null;
        refreshButtonLabel();
        refreshButtonSelected();
    }

    private void startPlacing(HullItem hullItem) {
        placingHullItem = hullItem;
        nodeSearch.setSocketFocus(true, isSubroutine(hullItem));
        if (storagePanel != null) {
            storagePanel.setSelected(hullItem, placingHint(hullItem));
        }
    }

    HullItem placingHullItem() {
        return placingHullItem;
    }

    void setInspectingShip(boolean inspecting) {
        if (inspecting == inspectingShip) {
            return;
        }
        inspectingShip = inspecting;
        if (storagePanel != null) {
            storagePanel.setTabListener(storageTabs());
        }
        if (placingHullItem != null && storagePanel != null) {
            storagePanel.setSelected(placingHullItem, placingHint(placingHullItem));
        }
    }

    boolean placeIntoFrameworkSocket(SocketType socketType) {
        if (placingHullItem == null) {
            return false;
        }
        if (placingHullItem.kind() == socketType && treeSession.socketFrameworkItem(socketType, placingHullItem)) {
            stopPlacing();
            refresh();
        } else {
            SkillTreeSounds.refused();
        }
        return true;
    }

    private String placingHint(HullItem hullItem) {
        String name = hullItem.name();
        if (isSubroutine(hullItem)) {
            return Translation.msg(inspectingShip ? "ui.socketStorage.placingHintShipView" : "ui.socketStorage.placingHint")
                    .arg("name", name).text();
        }
        SocketType socketType = hullItem.kind();
        String key;
        if (!HullUpgradesSetting.enabled()) {
            key = "ui.socketStorage.placingHintFrameworkDisabled";
        } else if (!treeSession.isSocketTypeUnlocked(socketType)) {
            key = "ui.socketStorage.placingHintFrameworkLocked";
        } else {
            key = inspectingShip ? "ui.socketStorage.placingHintFramework" : "ui.socketStorage.placingHintFrameworkZoom";
        }
        return Translation.msg(key).arg("name", name).arg("type", socketType.displayName()).text();
    }

    private static boolean isSubroutine(HullItem hullItem) {
        return hullItem.kind() == null || hullItem.kind() == SocketType.SUBROUTINE;
    }

    private void stopPlacing() {
        placingHullItem = null;
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
            storagePanel.restrictTo(socket != null ? SocketType.SUBROUTINE : null);
        }
    }

    private void clearTargetSocket() {
        targetSocket = null;
        treeSession.setTargetedSocket(null);
        if (storagePanel != null) {
            storagePanel.setTargetingSocket(false);
            if (targetFrameworkType == null) {
                storagePanel.restrictTo(null);
            }
        }
    }

    private boolean installInTargetSocket(HullItem hullItem) {
        if (targetFrameworkType != null) {
            if (treeSession.socketFrameworkItem(targetFrameworkType, hullItem)) {
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
        if (treeSession.installInSocket(targetSocket, hullItem)) {
            refresh();
        }
        return true;
    }
}
