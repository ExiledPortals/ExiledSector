package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.socketables.SocketCustody;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableStore;
import exiledsector.ui.node.NodeSearch;
import exiledsector.ui.node.SkillTreeNodeRenderer;
import exiledsector.ui.socket.SocketStoragePanel;

import java.util.Map;

final class SocketPlacement {

    private static final float BUTTON_MARGIN = 16f;
    private static final float BUTTON_HEIGHT = 40f;
    private static final float BUTTON_MIN_WIDTH = 150f;
    private static final float PANEL_MARGIN = 16f;
    private static final float PANEL_TOP = 100f;
    private static final float PANEL_GAP = 12f;

    private final CustomPanelAPI host;
    private final SkillTreeNodeRenderer nodeRenderer;
    private final NodeSearch search;
    private final SkillTreeSearchBar searchBar;
    private final SkillTreeUiButton button = new SkillTreeUiButton("");
    private SocketStoragePanel panel;
    private Socketable placing;
    private SkillNode targetSocket;
    private int storageRevision;
    private Map<String, FleetMemberAPI> ownedShips = Map.of();

    SocketPlacement(CustomPanelAPI host, SkillTreeNodeRenderer nodeRenderer, NodeSearch search, SkillTreeSearchBar searchBar) {
        this.host = host;
        this.nodeRenderer = nodeRenderer;
        this.search = search;
        this.searchBar = searchBar;
        refreshButtonLabel();
    }

    boolean isEngaged() {
        return panel != null || placing != null;
    }

    boolean panelContains(float x, float y) {
        return panel != null && panel.contains(x, y);
    }

    boolean buttonContains(float x, float y) {
        return button.contains(x, y);
    }

    boolean isButtonClickable(float x, float y) {
        return button.isClickable(x, y);
    }

    void escape() {
        if (panel != null && panel.escape()) {
            return;
        }
        if (placing != null) {
            stopPlacing();
        } else if (targetSocket != null && panel != null) {
            clearTargetSocket();
        } else if (panel != null) {
            panel.close();
        }
    }

    boolean emptySocket(SkillNode node) {
        if (node == null || node.getType().getTier() != SkillTier.SOCKET) {
            return false;
        }
        if (nodeRenderer.emptySocket(node)) {
            refresh();
        }
        return true;
    }

    void advance(boolean mustClose) {
        if (panel == null) {
            return;
        }
        if (mustClose) {
            panel.close();
        } else if (nodeRenderer.statsRevision() != storageRevision) {
            refresh();
            if (targetSocket != null && !nodeRenderer.isAllocatedSocket(targetSocket)) {
                clearTargetSocket();
            }
        }
    }

    void layoutButton(PositionAPI position, ScreenRect shipCard, boolean shown) {
        if (!shown) {
            button.hide();
            return;
        }
        float width = Math.max(BUTTON_MIN_WIDTH, button.preferredWidth());
        button.place(shipCard.left() + shipCard.width() + BUTTON_MARGIN, position.getY() + BUTTON_MARGIN, width, BUTTON_HEIGHT);
    }

    void renderButton(float mouseX, float mouseY, float alphaMult) {
        button.render(mouseX, mouseY, alphaMult);
    }

    void toggle(PositionAPI position, ScreenRect shipCard) {
        if (panel != null) {
            panel.close();
        } else {
            open(position, shipCard);
        }
    }

    boolean placeInto(SkillNode node) {
        if (placing == null) {
            return false;
        }
        if (nodeRenderer.installInSocket(node, placing)) {
            stopPlacing();
            refresh();
        }
        return true;
    }

    boolean handleSocketClick(SkillNode node, boolean ctrlDown, PositionAPI position, ScreenRect shipCard) {
        if (ctrlDown && node.getType().getTier() == SkillTier.SOCKET) {
            if (!nodeRenderer.isAllocated(node)) {
                nodeRenderer.toggleAllocation(node, false);
            }
            if (nodeRenderer.isAllocated(node)) {
                setTargetSocket(node);
                open(position, shipCard);
            }
            return true;
        }
        if (targetSocket != null && panel != null && nodeRenderer.isAllocatedSocket(node)) {
            setTargetSocket(node);
            return true;
        }
        return false;
    }

    void refresh() {
        refreshButtonLabel();
        if (panel != null) {
            storageRevision = nodeRenderer.statsRevision();
            panel.refresh(SocketCustody.shipNames(ownedShips));
        }
    }

    private void refreshButtonLabel() {
        int stored = Global.getSector() == null ? 0 : SocketableStore.get().owned().size();
        button.setLabel(Translation.msg("ui.socketStorage.button").arg("count", stored).text());
    }

    private void open(PositionAPI position, ScreenRect shipCard) {
        if (panel != null) {
            return;
        }
        SkillTreeSounds.panelOpened();
        nodeRenderer.closeDropdown();
        searchBar.unfocus();
        ownedShips = SocketCustody.reconcile();
        storageRevision = nodeRenderer.statsRevision();
        float bottom = shipCard.bottom() + shipCard.height() - position.getY() + PANEL_GAP;
        panel = SocketStoragePanel.open(host, PANEL_MARGIN, PANEL_TOP, position.getHeight() - PANEL_TOP - bottom,
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
                        panel = null;
                        stopPlacing();
                        clearTargetSocket();
                        refreshButtonLabel();
                    }
                });
        panel.setTargetingSocket(targetSocket != null);
    }

    private void startPlacing(Socketable socketable) {
        placing = socketable;
        search.setSocketFocus(true);
        if (panel != null) {
            panel.setSelected(socketable);
        }
    }

    private void stopPlacing() {
        placing = null;
        search.setSocketFocus(false);
        if (panel != null) {
            panel.setSelected(null);
        }
    }

    private void setTargetSocket(SkillNode socket) {
        stopPlacing();
        targetSocket = socket;
        nodeRenderer.setTargetedSocket(socket);
        if (panel != null) {
            panel.setTargetingSocket(socket != null);
        }
    }

    private void clearTargetSocket() {
        targetSocket = null;
        nodeRenderer.setTargetedSocket(null);
        if (panel != null) {
            panel.setTargetingSocket(false);
        }
    }

    private boolean installInTargetSocket(Socketable socketable) {
        if (targetSocket == null) {
            return false;
        }
        if (!nodeRenderer.isAllocatedSocket(targetSocket)) {
            clearTargetSocket();
            return false;
        }
        if (nodeRenderer.installInSocket(targetSocket, socketable)) {
            refresh();
        }
        return true;
    }
}
