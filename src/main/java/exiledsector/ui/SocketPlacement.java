package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.socketables.SocketCustody;
import exiledsector.socketables.SocketType;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableStore;
import exiledsector.ui.node.NodeSearch;
import exiledsector.ui.node.TreeAllocationSession;
import exiledsector.ui.socket.SocketStoragePanel;

import java.util.Map;

final class SocketPlacement {

    private static final float BUTTON_MARGIN = 16f;
    private static final float BUTTON_HEIGHT = 40f;
    private static final float BUTTON_MIN_WIDTH = 150f;
    private static final float PANEL_MARGIN = 16f;
    private static final float PANEL_TOP = 100f;
    private static final float PANEL_GAP = 12f;

    private final CustomPanelAPI hostPanel;
    private final TreeAllocationSession treeSession;
    private final NodeSearch nodeSearch;
    private final SkillTreeSearchBar searchBar;
    private final SkillTreeUiButton storageButton = new SkillTreeUiButton("");
    private SocketStoragePanel storagePanel;
    private Socketable placingSocketable;
    private SkillNode targetSocket;
    private SocketType targetFrameworkType;
    private int storageRevision;
    private Map<String, FleetMemberAPI> ownedShips = Map.of();
    private ScreenRect lastShipCard = ScreenRect.NONE;

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
        return storagePanel != null || placingSocketable != null;
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

    void openForFrameworkSocket(SocketType socketType, PositionAPI canvasPosition, ScreenRect shipCard) {
        if (storagePanel != null) {
            storagePanel.close();
        }
        stopPlacing();
        clearTargetSocket();
        targetFrameworkType = socketType;
        open(canvasPosition, shipCard, socketType);
        storagePanel.setTargetingSocket(true);
    }

    void close() {
        if (storagePanel != null) {
            storagePanel.close();
        }
    }

    void escape() {
        if (storagePanel != null && storagePanel.escape()) {
            return;
        }
        if (targetFrameworkType != null && storagePanel != null) {
            storagePanel.close();
            return;
        }
        if (placingSocketable != null) {
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
        float buttonWidth = Math.max(BUTTON_MIN_WIDTH, storageButton.preferredWidth());
        storageButton.place(shipCard.left() + shipCard.width() + BUTTON_MARGIN, canvasPosition.getY() + BUTTON_MARGIN, buttonWidth,
                BUTTON_HEIGHT);
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

    boolean placeInto(SkillNode node) {
        if (placingSocketable == null) {
            return false;
        }
        if (treeSession.installInSocket(node, placingSocketable)) {
            stopPlacing();
            refresh();
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
        }
    }

    private void refreshButtonLabel() {
        int storedCount = Global.getSector() == null ? 0 : SocketableStore.get().owned().size();
        storageButton.setLabel(Translation.msg("ui.socketStorage.button").arg("count", storedCount).text());
    }

    private void open(PositionAPI canvasPosition, ScreenRect shipCard) {
        open(canvasPosition, shipCard, targetSocket != null ? SocketType.SUBROUTINE : null);
    }

    private void open(PositionAPI canvasPosition, ScreenRect shipCard, SocketType kindFilter) {
        if (storagePanel != null) {
            return;
        }
        SkillTreeSounds.panelOpened();
        treeSession.closeDropdown();
        searchBar.unfocus();
        ownedShips = SocketCustody.reconcile();
        storageRevision = treeSession.statsRevision();
        storagePanel = SocketStoragePanel.open(hostPanel, PANEL_MARGIN, PANEL_TOP, sidePanelHeight(canvasPosition, shipCard),
                SocketCustody.shipNames(ownedShips), new SocketStoragePanel.Listener() {
                    @Override
                    public void selected(Socketable socketable) {
                        if (socketable == null) {
                            stopPlacing();
                        } else if (!installInTargetSocket(socketable)) {
                            startPlacing(socketable);
                        }
                    }

                    @Override
                    public void closed() {
                        storagePanel = null;
                        stopPlacing();
                        clearTargetSocket();
                        targetFrameworkType = null;
                        refreshButtonLabel();
                    }

                    @Override
                    public void pressedInside() {
                        searchBar.unfocus();
                    }
                }, kindFilter);
        storagePanel.setTargetingSocket(targetSocket != null);
    }

    private void startPlacing(Socketable socketable) {
        placingSocketable = socketable;
        nodeSearch.setSocketFocus(true);
        if (storagePanel != null) {
            storagePanel.setSelected(socketable);
        }
    }

    private void stopPlacing() {
        placingSocketable = null;
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

    private boolean installInTargetSocket(Socketable socketable) {
        if (targetFrameworkType != null) {
            if (treeSession.socketFrameworkItem(targetFrameworkType, socketable)) {
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
        if (treeSession.installInSocket(targetSocket, socketable)) {
            refresh();
        }
        return true;
    }
}
