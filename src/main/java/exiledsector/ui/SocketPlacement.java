package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.items.SocketCustody;
import exiledsector.items.ItemKind;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.ui.node.NodeSearch;
import exiledsector.ui.node.TreeAllocationSession;
import exiledsector.ui.socket.SocketPanelPlacement;
import exiledsector.ui.socket.ItemStoragePanel;

import java.util.List;
import java.util.Map;

final class SocketPlacement {

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
    private int storageRevision;
    private int countedStoreSize = -1;
    private int shownSubroutineCount = -1;
    private Map<String, FleetMemberAPI> ownedShips = Map.of();

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

    boolean isButtonClickable(float x, float y) {
        return storageButton.isClickable(x, y);
    }

    void escape() {
        if (storagePanel != null && storagePanel.escape()) {
            return;
        }
        if (placingItem != null) {
            stopPlacing();
        } else if (targetSocket != null && storagePanel != null) {
            clearTargetSocket();
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
        } else {
            open(canvasPosition, shipCard);
        }
    }

    private void refreshButtonSelected() {
        storageButton.setSelected(storagePanel != null);
    }

    boolean placeInto(SkillNode node) {
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
        if (refusal != null && storagePanel != null) {
            storagePanel.showRefusal(refusal);
        }
    }

    private void clearRefusal() {
        if (storagePanel != null) {
            storagePanel.clearRefusal();
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
        List<Item> owned = Global.getSector() == null ? List.of() : ItemStore.get().owned();
        if (owned.size() == countedStoreSize) {
            return;
        }
        countedStoreSize = owned.size();
        int subroutineCount = (int) owned.stream().filter(item -> ItemStoragePanel.listedKind(item) == ItemKind.SUBROUTINE).count();
        if (subroutineCount == shownSubroutineCount) {
            return;
        }
        shownSubroutineCount = subroutineCount;
        storageButton.setLabel(Translation.msg("ui.subroutines.button").arg("count", subroutineCount).text());
    }

    private void open(PositionAPI canvasPosition, ScreenRect shipCard) {
        if (storagePanel != null) {
            return;
        }
        SkillTreeSounds.panelOpened();
        treeSession.closeDropdown();
        searchBar.unfocus();
        ownedShips = SocketCustody.reconcile();
        storageRevision = treeSession.statsRevision();
        SocketPanelPlacement placement = new SocketPanelPlacement(ChromeMetrics.EDGE_MARGIN, PANEL_TOP, ItemStoragePanel.WIDTH,
                sidePanelHeight(canvasPosition, shipCard), false);
        storagePanel = ItemStoragePanel.openSubroutines(hostPanel, placement, SocketCustody.shipNames(ownedShips), new ItemStoragePanel.Listener() {
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
        });
        storagePanel.setTargetingSocket(targetSocket != null);
        refreshButtonLabel();
        refreshButtonSelected();
    }

    private void forgetStorage() {
        storagePanel = null;
        stopPlacing();
        clearTargetSocket();
        refreshButtonLabel();
        refreshButtonSelected();
    }

    private void startPlacing(Item item) {
        placingItem = item;
        nodeSearch.setSocketFocus(true, true);
        if (storagePanel != null) {
            storagePanel.setSelected(item, placingHint(item));
        }
    }

    private static String placingHint(Item item) {
        return Translation.msg("ui.socketStorage.placingHint").arg("name", item.name()).text();
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
        }
    }

    private void clearTargetSocket() {
        targetSocket = null;
        treeSession.setTargetedSocket(null);
        if (storagePanel != null) {
            storagePanel.setTargetingSocket(false);
        }
    }

    private boolean installInTargetSocket(Item item) {
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
