package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.items.ItemKind;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.SubroutineRadius;
import exiledsector.ui.node.NodeSearch;
import exiledsector.ui.node.TreeAllocationSession;
import exiledsector.ui.socket.SocketPanelPlacement;
import exiledsector.ui.socket.ItemStoragePanel;
import exiledsector.ui.socket.StoragePlacement;

import java.util.List;

final class SocketPlacement {

    private static final float PANEL_TOP = SkillTreeChrome.sidePanelTop();
    private static final float PANEL_GAP = 12f;

    private final CustomPanelAPI hostPanel;
    private final TreeAllocationSession treeSession;
    private final NodeSearch nodeSearch;
    private final SkillTreeSearchBar searchBar;
    private final SkillTreeUiButton storageButton = new SkillTreeUiButton("");
    private final StoragePlacement<SkillNode> storage = new StoragePlacement<>(SocketPlacement::placingHint);
    private Item hoveredStorageItem;

    SocketPlacement(CustomPanelAPI hostPanel, TreeAllocationSession treeSession, NodeSearch nodeSearch, SkillTreeSearchBar searchBar) {
        this.hostPanel = hostPanel;
        this.treeSession = treeSession;
        this.nodeSearch = nodeSearch;
        this.searchBar = searchBar;
        refreshButtonLabel();
    }

    private static float sidePanelHeight(PositionAPI canvasPosition, ScreenRect shipCard) {
        float reservedBottom = shipCard.bottom() + shipCard.height() - canvasPosition.getY() + PANEL_GAP;
        return canvasPosition.getHeight() - PANEL_TOP - reservedBottom;
    }

    Item placingItem() {
        return storage.placingItem();
    }

    SubroutineRadius.Footprint hoveredStorageFootprint() {
        if (!storage.isOpen() || hoveredStorageItem == null || !SubroutineRadius.hasRadius(hoveredStorageItem)) {
            return null;
        }
        String socketId = treeSession.socketHolding(hoveredStorageItem);
        if (socketId == null && storage.target() != null) {
            socketId = storage.target().getId();
        }
        return socketId == null ? null : SubroutineRadius.footprint(hoveredStorageItem, socketId);
    }

    boolean isEngaged() {
        return storage.isOpen() || storage.placingItem() != null;
    }

    boolean isWorkbenchOpen() {
        return storage.isOpen() && storage.panel().isWorkbenchOpen();
    }

    boolean panelContains(float x, float y) {
        return storage.isOpen() && storage.panel().contains(x, y);
    }

    boolean buttonContains(float x, float y) {
        return storageButton.contains(x, y);
    }

    boolean isButtonClickable(float x, float y) {
        return storageButton.isClickable(x, y);
    }

    void escape() {
        ItemStoragePanel storagePanel = storage.panel();
        if (storagePanel != null && storagePanel.escape()) {
            return;
        }
        if (storage.placingItem() != null) {
            stopPlacing();
        } else if (storage.target() != null && storagePanel != null) {
            clearTargetSocket();
        } else if (storagePanel != null) {
            storagePanel.close();
        }
    }

    void dismiss() {
        if (storage.placingItem() != null) {
            stopPlacing();
        }
        if (storage.isOpen()) {
            storage.panel().close();
        }
    }

    boolean emptySocket(SkillNode node) {
        if (node == null || node.getType().getTier() != SkillTier.SOCKET) {
            return false;
        }
        if (treeSession.emptySocket(node)) {
            refresh();
        } else if (storage.isOpen() && treeSession.releaseWarning(node) != null) {
            storage.panel().showRefusal(treeSession.releaseWarning(node));
        }
        return true;
    }

    void advance(boolean mustClose) {
        if (!storage.isOpen()) {
            return;
        }
        if (mustClose) {
            storage.panel().close();
        } else if (storage.isStale(treeSession.statsRevision())) {
            refresh();
            if (storage.target() != null && !treeSession.isAllocatedSocket(storage.target())) {
                clearTargetSocket();
            }
        }
    }

    void layoutButton(PositionAPI canvasPosition, ScreenRect shipCard, boolean buttonShown) {
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
        if (storage.isOpen()) {
            storage.panel().close();
        } else {
            open(canvasPosition, shipCard);
        }
    }

    private void refreshButtonSelected() {
        storageButton.setSelected(storage.isOpen());
    }

    boolean placeInto(SkillNode node) {
        Item placingItem = storage.placingItem();
        if (placingItem == null) {
            return false;
        }
        clearRefusal();
        if (treeSession.installInSocket(node, placingItem)) {
            stopPlacing();
            refresh();
        } else {
            SkillTreeSounds.refused();
            showRefusal(node, placingItem);
        }
        return true;
    }

    private void showRefusal(SkillNode node, Item item) {
        String refusal = treeSession.socketRefusal(node, item);
        if (refusal != null && storage.isOpen()) {
            storage.panel().showRefusal(refusal);
        }
    }

    private void clearRefusal() {
        if (storage.isOpen()) {
            storage.panel().clearRefusal();
        }
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
        if (storage.target() != null && storage.isOpen() && treeSession.isAllocatedSocket(node)) {
            setTargetSocket(node);
            return true;
        }
        return false;
    }

    void refresh() {
        storage.refresh(treeSession.statsRevision());
    }

    private void refreshButtonLabel() {
        List<Item> owned = Global.getSector() == null ? List.of() : ItemStore.get().owned();
        int subroutineCount = (int) owned.stream().filter(item -> ItemStoragePanel.listedKind(item) == ItemKind.SUBROUTINE).count();
        storageButton.setLabel(Translation.msg("ui.subroutines.button").arg("count", subroutineCount).text());
    }

    private void open(PositionAPI canvasPosition, ScreenRect shipCard) {
        if (storage.isOpen()) {
            return;
        }
        SkillTreeSounds.panelOpened();
        treeSession.closeDropdown();
        searchBar.unfocus();
        storage.reconcileOwnedShips();
        SocketPanelPlacement placement = new SocketPanelPlacement(ChromeMetrics.EDGE_MARGIN, PANEL_TOP, ItemStoragePanel.WIDTH,
                sidePanelHeight(canvasPosition, shipCard), false);
        ItemStoragePanel storagePanel = ItemStoragePanel.openSubroutines(hostPanel, placement, storage.shipNames(), new ItemStoragePanel.Listener() {
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

            @Override
            public void hovered(Item item) {
                hoveredStorageItem = item;
            }

            @Override
            public void disassembled() {
                refreshButtonLabel();
            }
        });
        storage.opened(storagePanel, treeSession.statsRevision());
        storagePanel.setTargetingSocket(storage.target() != null);
        showDoctrineRemovalWarning(storage.target());
        refreshButtonSelected();
    }

    private void forgetStorage() {
        storage.forget();
        hoveredStorageItem = null;
        nodeSearch.setSocketFocus(false);
        treeSession.setTargetedSocket(null);
        refreshButtonSelected();
    }

    private void startPlacing(Item item) {
        nodeSearch.setSocketFocus(true, true);
        storage.startPlacing(item);
    }

    private static String placingHint(Item item) {
        return Translation.msg("ui.socketStorage.placingHint").arg("name", item.name()).text();
    }

    private void stopPlacing() {
        storage.stopPlacing();
        nodeSearch.setSocketFocus(false);
    }

    private void setTargetSocket(SkillNode socket) {
        stopPlacing();
        storage.setTarget(socket);
        treeSession.setTargetedSocket(socket);
        showDoctrineRemovalWarning(socket);
    }

    private void showDoctrineRemovalWarning(SkillNode socket) {
        String warning = treeSession.doctrineRemovalWarning(socket);
        if (warning != null && storage.isOpen()) {
            storage.panel().showRefusal(warning);
        }
    }

    private void clearTargetSocket() {
        storage.setTarget(null);
        treeSession.setTargetedSocket(null);
    }

    private boolean installInTargetSocket(Item item) {
        SkillNode targetSocket = storage.target();
        if (targetSocket == null) {
            return false;
        }
        if (!treeSession.isAllocatedSocket(targetSocket)) {
            clearTargetSocket();
            return false;
        }
        clearRefusal();
        if (treeSession.installInSocket(targetSocket, item)) {
            refresh();
        } else if (treeSession.socketRefusal(targetSocket, item) != null) {
            SkillTreeSounds.refused();
            showRefusal(targetSocket, item);
        }
        return true;
    }
}
