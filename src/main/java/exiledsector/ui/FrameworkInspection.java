package exiledsector.ui;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.skills.FrameworkSlots;
import exiledsector.hullitems.HullUpgradesSetting;
import exiledsector.ui.decoration.SkillTreeFleetRenderer;
import exiledsector.ui.framework.FrameworkSocketLayout;
import exiledsector.ui.node.TreeAllocationSession;
import exiledsector.ui.socket.HullUpgradePanel;
import exiledsector.ui.socket.SocketPanelPlacement;
import exiledsector.ui.socket.SocketPanelTab;
import exiledsector.ui.socket.SocketStoragePanel;
import exiledsector.ui.util.Fade;
import exiledsector.ui.util.GLDraw;

import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;

final class FrameworkInspection {

    private static final float INSPECT_SHIP_SHARE = 0.4f;
    private static final float INSPECT_FADE_SECONDS = 0.6f;
    private static final float PANEL_TOP = SkillTreeChrome.sidePanelTop();
    private static final float GRID_SPACING = 48f;
    private static final float GRID_ALPHA = 0.06f;
    private static final Color GRID_COLOR = SkillTreePanelStyle.GLOW_COLOR;
    private static final float FREE_SPACE_MARGIN = 24f;
    private static final float SOCKET_LABEL_ROOM = 70f;
    private static final float MIN_SHIP_PIXELS = 120f;

    private final CustomPanelAPI hostPanel;
    private final TreeAllocationSession treeSession;
    private final SocketPlacement socketPlacement;
    private final SkillTreeStatPanel statPanel;
    private final HullSocketsDisplay sockets;
    private HullUpgradePanel frameworkPanel;
    private boolean panelDismissed;
    private boolean active;
    private float inspectLevel;
    private PositionAPI treeCanvasPosition;
    private FrameworkSocketLayout.ShipExtent treeShipExtent = FrameworkSocketLayout.ShipExtent.square(0f);
    private final Consumer<SocketPanelTab> upgradesTabListener;

    FrameworkInspection(CustomPanelAPI hostPanel, TreeAllocationSession treeSession, SocketPlacement socketPlacement,
                        SkillTreeStatPanel statPanel) {
        this.hostPanel = hostPanel;
        this.statPanel = statPanel;
        this.treeSession = treeSession;
        this.socketPlacement = socketPlacement;
        this.sockets = new HullSocketsDisplay(hostPanel, treeSession, FrameworkSocketLayout.Arrangement.BOTH_SIDES);
        this.upgradesTabListener = tab -> {
            if (tab == SocketPanelTab.HULL_ITEMS) {
                socketPlacement.openFromHullUpgradesTab();
            }
        };
        socketPlacement.setHullUpgradesTab(new SocketPlacement.HullUpgradesTab() {
            @Override
            public boolean available() {
                return active && HullUpgradesSetting.enabled();
            }

            @Override
            public boolean isOpen() {
                return frameworkPanel != null;
            }

            @Override
            public void swapOut() {
                if (frameworkPanel == null) {
                    return;
                }
                HullUpgradePanel swapped = frameworkPanel;
                frameworkPanel = null;
                panelDismissed = true;
                swapped.swapOut();
                socketPlacement.refreshButtonSelected();
            }

            @Override
            public void swapIn() {
                panelDismissed = false;
                if (frameworkPanel == null && treeCanvasPosition != null) {
                    openPanel(treeCanvasPosition, true);
                }
            }

            @Override
            public void open() {
                panelDismissed = false;
                if (frameworkPanel == null && treeCanvasPosition != null) {
                    openPanel(treeCanvasPosition, false);
                }
            }

            @Override
            public void close() {
                if (frameworkPanel != null) {
                    frameworkPanel.close();
                }
            }
        });
    }

    void setCanvasPosition(PositionAPI canvasPosition) {
        treeCanvasPosition = canvasPosition;
    }

    boolean isPanelOpen() {
        return frameworkPanel != null;
    }

    boolean isInspecting() {
        return inspectLevel > 0f;
    }

    boolean hasSocketedItems() {
        return treeSession.frameworkSlots().stream().anyMatch(slot -> slot.item() != null);
    }

    float inspectLevel() {
        return inspectLevel;
    }

    void advance(float amount, PositionAPI canvasPosition, TreeCamera camera, SkillTreeFleetRenderer fleetRenderer, boolean fleetFocused,
                 float mouseX, float mouseY) {
        active = fleetFocused && camera.isFollowSettled();
        socketPlacement.setInspectingShip(active);
        if (sockets.refreshIfChanged() && frameworkPanel != null) {
            frameworkPanel.update(sockets.panelView());
        }
        float fadeStep = amount / INSPECT_FADE_SECONDS;
        inspectLevel = Fade.step(inspectLevel, active, fadeStep);
        if (active && fleetRenderer.soloLongestSide() > 0f) {
            fitShipToFreeSpace(canvasPosition, camera, fleetRenderer.soloLongestSide());
        } else if (fleetFocused && camera.isFollowing()) {
            camera.stopInspecting();
        }
        if (!camera.isFollowing()) {
            panelDismissed = false;
        }
        boolean wantsPanel = active && !panelDismissed && !socketPlacement.isEngaged() && HullUpgradesSetting.enabled();
        if (wantsPanel && frameworkPanel == null) {
            openPanel(canvasPosition, false);
        } else if (!wantsPanel && frameworkPanel != null) {
            frameworkPanel.close();
        }
        sockets.advance(amount, mouseX, mouseY, isInspecting());
    }

    private void fitShipToFreeSpace(PositionAPI canvasPosition, TreeCamera camera, float shipTreeLength) {
        float panelRight = 0f;
        if (socketPlacement.isStorageOpen()) {
            panelRight = ChromeMetrics.EDGE_MARGIN + SocketStoragePanel.WIDTH;
        } else if (frameworkPanel != null) {
            panelRight = ChromeMetrics.EDGE_MARGIN + HullUpgradePanel.WIDTH;
        }
        float freeLeft = panelRight + FREE_SPACE_MARGIN;
        float freeRight = canvasPosition.getWidth() - statPanel.reservedWidth() - FREE_SPACE_MARGIN;
        float socketColumnRoom = sockets.slots().isEmpty() ? 0f
                : FrameworkSocketLayout.COLUMN_GAP + FrameworkSocketLayout.SOCKET_RADIUS * 2f + SOCKET_LABEL_ROOM;
        float shipPixelsForWidth = Math.max(MIN_SHIP_PIXELS, freeRight - freeLeft - socketColumnRoom * 2f);
        float shipPixels = Math.min(canvasPosition.getHeight() * INSPECT_SHIP_SHARE, shipPixelsForWidth);
        camera.inspect(shipPixels / shipTreeLength);
        camera.setFollowOffset((freeLeft + freeRight) / 2f - canvasPosition.getWidth() / 2f);
    }

    void stop() {
        sockets.hideTooltip();
        if (frameworkPanel != null) {
            frameworkPanel.close();
        }
        panelDismissed = false;
        if (socketPlacement.isFrameworkTargeted()) {
            socketPlacement.close();
        }
    }

    boolean escape() {
        if (frameworkPanel != null) {
            if (!frameworkPanel.escape()) {
                frameworkPanel.close();
            }
            return true;
        }
        return false;
    }

    boolean pressLeft(float x, float y, PositionAPI canvasPosition, ScreenRect shipCard) {
        int socketIndex = inspectLevel < 0.5f ? -1 : socketAt(x, y);
        if (socketIndex < 0) {
            return false;
        }
        if (!HullUpgradesSetting.enabled()) {
            SkillTreeSounds.refused();
            return true;
        }
        List<FrameworkSlots.Slot> slots = sockets.slots();
        if (socketPlacement.placeIntoFrameworkSocket(slots.get(socketIndex).type())) {
            return true;
        }
        socketPlacement.openForFrameworkSocket(slots.get(socketIndex).type(), canvasPosition, shipCard);
        SkillTreeSounds.panelOpened();
        return true;
    }

    boolean pressRight(float x, float y) {
        int socketIndex = socketAt(x, y);
        if (socketIndex < 0 || sockets.slots().get(socketIndex).item() == null) {
            return false;
        }
        treeSession.unsocketFrameworkItem(sockets.slots().get(socketIndex).type());
        socketPlacement.refresh();
        return true;
    }

    private void openPanel(PositionAPI canvasPosition, boolean instantly) {
        sockets.absorbUpgradesFromCargo();
        float panelHeight = socketPlacement.sidePanelHeight(canvasPosition);
        SocketPanelPlacement placement = new SocketPanelPlacement(ChromeMetrics.EDGE_MARGIN, PANEL_TOP, HullUpgradePanel.WIDTH, panelHeight,
                instantly);
        frameworkPanel = HullUpgradePanel.open(hostPanel, placement, sockets.panelView(), sockets.panelListener(() -> {
                    frameworkPanel = null;
                    panelDismissed = true;
                    socketPlacement.refreshButtonSelected();
                }), upgradesTabListener, HullUpgradePanel.Sizing.FIT_HEIGHT);
        socketPlacement.refreshButtonSelected();
        if (!instantly) {
            SkillTreeSounds.panelOpened();
        }
    }

    private int socketAt(float x, float y) {
        return isInspecting() ? sockets.socketAt(x, y) : -1;
    }

    void renderBackdrop(PositionAPI canvasPosition, float alphaMult) {
        if (inspectLevel <= 0f) {
            return;
        }
        GLDraw.grid(canvasPosition.getX(), canvasPosition.getY(), canvasPosition.getWidth(), canvasPosition.getHeight(), GRID_SPACING,
                GRID_COLOR, GRID_ALPHA * inspectLevel * alphaMult);
    }

    void render(PositionAPI canvasPosition, TreeViewport viewport, SkillTreeFleetRenderer fleetRenderer, float alphaMult) {
        if (inspectLevel <= 0f) {
            sockets.forgetRenderedSockets();
            return;
        }
        float alpha = alphaMult * Fade.smooth(inspectLevel);
        SkillTreeFleetRenderer.InspectedShip ship = sockets.slots().isEmpty() ? null : fleetRenderer.inspectedShip(viewport);
        if (ship != null && ship.longestSidePixels() != treeShipExtent.halfLength() * 2f) {
            treeShipExtent = FrameworkSocketLayout.ShipExtent.square(ship.longestSidePixels());
        }
        sockets.render(ship, treeShipExtent, alpha,
                HullSocketsDisplay.SocketBounds.UNBOUNDED, socketPlacement.placingHullItem(),
                socketPlacement.isFrameworkTargeted() ? socketPlacement.targetFrameworkType() : null);
    }
}
