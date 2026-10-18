package exiledsector.ui;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.skills.ShipSections;
import exiledsector.items.HullUpgradesSetting;
import exiledsector.ui.decoration.SkillTreeFleetRenderer;
import exiledsector.ui.section.SectionLayout;
import exiledsector.ui.node.TreeAllocationSession;
import exiledsector.ui.socket.HullUpgradePanel;
import exiledsector.ui.socket.SocketPanelPlacement;
import exiledsector.ui.socket.SocketPanelTab;
import exiledsector.ui.socket.ItemStoragePanel;
import exiledsector.ui.util.Fade;
import exiledsector.ui.util.GLDraw;

import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;

final class SectionInspection {

    private static final float INSPECT_SHIP_SHARE = 0.4f;
    private static final float INSPECT_FADE_SECONDS = 0.6f;
    private static final float PANEL_TOP = SkillTreeChrome.sidePanelTop();
    private static final float GRID_SPACING = 48f;
    private static final float GRID_ALPHA = 0.06f;
    private static final Color GRID_COLOR = SkillTreePanelStyle.GLOW_COLOR;
    private static final float FREE_SPACE_MARGIN = 24f;
    private static final float SECTION_LABEL_ROOM = 70f;
    private static final float MIN_SHIP_PIXELS = 120f;

    private final CustomPanelAPI hostPanel;
    private final TreeAllocationSession treeSession;
    private final SocketPlacement socketPlacement;
    private final SkillTreeStatPanel statPanel;
    private final SectionsDisplay sectionsDisplay;
    private HullUpgradePanel hullUpgradesPanel;
    private boolean panelDismissed;
    private boolean active;
    private float inspectLevel;
    private PositionAPI treeCanvasPosition;
    private SectionLayout.ShipExtent treeShipExtent = SectionLayout.ShipExtent.square(0f);
    private final Consumer<SocketPanelTab> upgradesTabListener;

    SectionInspection(CustomPanelAPI hostPanel, TreeAllocationSession treeSession, SocketPlacement socketPlacement,
                        SkillTreeStatPanel statPanel) {
        this.hostPanel = hostPanel;
        this.statPanel = statPanel;
        this.treeSession = treeSession;
        this.socketPlacement = socketPlacement;
        this.sectionsDisplay = new SectionsDisplay(hostPanel, treeSession, SectionLayout.Arrangement.BOTH_SIDES);
        this.upgradesTabListener = tab -> {
            if (tab != SocketPanelTab.HULL_UPGRADES) {
                socketPlacement.showTab(tab);
            }
        };
        socketPlacement.setHullUpgradesTab(new SocketPlacement.HullUpgradesTab() {
            @Override
            public boolean available() {
                return active && HullUpgradesSetting.enabled();
            }

            @Override
            public boolean isOpen() {
                return hullUpgradesPanel != null;
            }

            @Override
            public void swapOut() {
                if (hullUpgradesPanel == null) {
                    return;
                }
                HullUpgradePanel swapped = hullUpgradesPanel;
                hullUpgradesPanel = null;
                panelDismissed = true;
                swapped.swapOut();
                socketPlacement.refreshButtonSelected();
            }

            @Override
            public void swapIn() {
                panelDismissed = false;
                if (hullUpgradesPanel == null && treeCanvasPosition != null) {
                    openPanel(treeCanvasPosition, true);
                }
            }

            @Override
            public void open() {
                panelDismissed = false;
                if (hullUpgradesPanel == null && treeCanvasPosition != null) {
                    openPanel(treeCanvasPosition, false);
                }
            }

            @Override
            public void close() {
                if (hullUpgradesPanel != null) {
                    hullUpgradesPanel.close();
                }
            }
        });
    }

    void setCanvasPosition(PositionAPI canvasPosition) {
        treeCanvasPosition = canvasPosition;
    }

    boolean isPanelOpen() {
        return hullUpgradesPanel != null;
    }

    boolean isInspecting() {
        return inspectLevel > 0f;
    }

    boolean hasSectionItems() {
        return treeSession.sections().stream().anyMatch(unlocked -> unlocked.item() != null);
    }

    float inspectLevel() {
        return inspectLevel;
    }

    void advance(float amount, PositionAPI canvasPosition, TreeCamera camera, SkillTreeFleetRenderer fleetRenderer, boolean fleetFocused,
                 float mouseX, float mouseY) {
        active = fleetFocused && camera.isFollowSettled();
        socketPlacement.setInspectingShip(active);
        if (sectionsDisplay.refreshIfChanged() && hullUpgradesPanel != null) {
            hullUpgradesPanel.update(sectionsDisplay.panelView());
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
        if (wantsPanel && hullUpgradesPanel == null) {
            openPanel(canvasPosition, false);
        } else if (!wantsPanel && hullUpgradesPanel != null) {
            hullUpgradesPanel.close();
        }
        sectionsDisplay.advance(amount, mouseX, mouseY, isInspecting());
    }

    private void fitShipToFreeSpace(PositionAPI canvasPosition, TreeCamera camera, float shipTreeLength) {
        float panelRight = 0f;
        if (socketPlacement.isChartsOpen()) {
            panelRight = socketPlacement.chartsRight();
        } else if (socketPlacement.isStorageOpen()) {
            panelRight = ChromeMetrics.EDGE_MARGIN + ItemStoragePanel.WIDTH;
        } else if (hullUpgradesPanel != null) {
            panelRight = ChromeMetrics.EDGE_MARGIN + HullUpgradePanel.WIDTH;
        }
        float freeLeft = panelRight + FREE_SPACE_MARGIN;
        float freeRight = canvasPosition.getWidth() - statPanel.reservedWidth() - FREE_SPACE_MARGIN;
        float sectionColumnRoom = sectionsDisplay.sections().isEmpty() ? 0f
                : SectionLayout.COLUMN_GAP + SectionLayout.SECTION_RADIUS * 2f + SECTION_LABEL_ROOM;
        float shipPixelsForWidth = Math.max(MIN_SHIP_PIXELS, freeRight - freeLeft - sectionColumnRoom * 2f);
        float shipPixels = Math.min(canvasPosition.getHeight() * INSPECT_SHIP_SHARE, shipPixelsForWidth);
        camera.inspect(shipPixels / shipTreeLength);
        camera.setFollowOffset((freeLeft + freeRight) / 2f - canvasPosition.getWidth() / 2f);
    }

    void stop() {
        sectionsDisplay.hideTooltip();
        if (hullUpgradesPanel != null) {
            hullUpgradesPanel.close();
        }
        panelDismissed = false;
        if (socketPlacement.isSectionTargeted()) {
            socketPlacement.close();
        }
    }

    boolean escape() {
        if (hullUpgradesPanel != null) {
            if (!hullUpgradesPanel.escape()) {
                hullUpgradesPanel.close();
            }
            return true;
        }
        return false;
    }

    boolean pressLeft(float x, float y, PositionAPI canvasPosition, ScreenRect shipCard) {
        int sectionIndex = inspectLevel < 0.5f ? -1 : sectionAt(x, y);
        if (sectionIndex < 0) {
            return false;
        }
        if (!HullUpgradesSetting.enabled()) {
            SkillTreeSounds.refused();
            return true;
        }
        List<ShipSections.UnlockedSection> unlockedSections = sectionsDisplay.sections();
        if (socketPlacement.placeIntoSection(unlockedSections.get(sectionIndex).section())) {
            return true;
        }
        socketPlacement.openForSection(unlockedSections.get(sectionIndex).section(), canvasPosition, shipCard);
        SkillTreeSounds.panelOpened();
        return true;
    }

    boolean pressRight(float x, float y) {
        int sectionIndex = sectionAt(x, y);
        if (sectionIndex < 0 || sectionsDisplay.sections().get(sectionIndex).item() == null) {
            return false;
        }
        treeSession.removeFromSection(sectionsDisplay.sections().get(sectionIndex).section());
        socketPlacement.refresh();
        return true;
    }

    private void openPanel(PositionAPI canvasPosition, boolean instantly) {
        sectionsDisplay.absorbUpgradesFromCargo();
        float panelHeight = socketPlacement.sidePanelHeight(canvasPosition);
        SocketPanelPlacement placement = new SocketPanelPlacement(ChromeMetrics.EDGE_MARGIN, PANEL_TOP, HullUpgradePanel.WIDTH, panelHeight,
                instantly);
        hullUpgradesPanel = HullUpgradePanel.open(hostPanel, placement, sectionsDisplay.panelView(), sectionsDisplay.panelListener(() -> {
                    hullUpgradesPanel = null;
                    panelDismissed = true;
                    socketPlacement.refreshButtonSelected();
                }), upgradesTabListener, HullUpgradePanel.Sizing.FIT_HEIGHT);
        socketPlacement.refreshButtonSelected();
        if (!instantly) {
            SkillTreeSounds.panelOpened();
        }
    }

    private int sectionAt(float x, float y) {
        return isInspecting() ? sectionsDisplay.sectionAt(x, y) : -1;
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
            sectionsDisplay.forgetRenderedSections();
            return;
        }
        float alpha = alphaMult * Fade.smooth(inspectLevel);
        SkillTreeFleetRenderer.InspectedShip ship = sectionsDisplay.sections().isEmpty() ? null : fleetRenderer.inspectedShip(viewport);
        if (ship != null && ship.longestSidePixels() != treeShipExtent.halfLength() * 2f) {
            treeShipExtent = SectionLayout.ShipExtent.square(ship.longestSidePixels());
        }
        sectionsDisplay.render(ship, treeShipExtent, alpha,
                SectionsDisplay.SectionBounds.UNBOUNDED, socketPlacement.placingItem(),
                socketPlacement.isSectionTargeted() ? socketPlacement.targetSection() : null);
    }
}
