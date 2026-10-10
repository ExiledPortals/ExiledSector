package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.FrameworkSlots;
import exiledsector.socketables.FrameworkSockets;
import exiledsector.socketables.HullUpgradeTooltip;
import exiledsector.socketables.SocketType;
import exiledsector.socketables.SocketableStore;
import exiledsector.ui.decoration.ShipAnchors;
import exiledsector.ui.decoration.SkillTreeFleetRenderer;
import exiledsector.ui.framework.FrameworkSocketFlair;
import exiledsector.ui.framework.FrameworkSocketLayout;
import exiledsector.ui.node.TreeAllocationSession;
import exiledsector.ui.socket.HullUpgradePanel;
import exiledsector.ui.socket.SocketStoragePanel;
import exiledsector.ui.socket.SocketableHoverTooltip;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.TextLabel;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class FrameworkInspection {

    private static final int MAX_SOCKETS = FrameworkSockets.MAX_POINTS;
    private static final float INSPECT_SHIP_SHARE = 0.4f;
    private static final float INSPECT_FADE_SECONDS = 0.6f;
    private static final float HOVER_SECONDS = 0.15f;
    private static final float PANEL_MARGIN = 16f;
    private static final float PANEL_TOP = 100f;
    private static final float LABEL_GAP = 8f;
    private static final float GRID_SPACING = 48f;
    private static final float GRID_ALPHA = 0.06f;
    private static final float LABEL_FONT_SIZE = SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE;
    private static final Color GRID_COLOR = SkillTreePanelStyle.GLOW_COLOR;
    private static final float BUTTON_GAP = 12f;
    private static final float FREE_SPACE_MARGIN = 24f;
    private static final float SOCKET_LABEL_ROOM = 70f;
    private static final float MIN_SHIP_PIXELS = 120f;
    private static final float BUTTON_MIN_WIDTH = 150f;

    private final CustomPanelAPI hostPanel;
    private final TreeAllocationSession treeSession;
    private final SocketPlacement socketPlacement;
    private final SocketableHoverTooltip hoverTooltip;
    private final SkillTreeUiButton upgradesButton = new SkillTreeUiButton("");
    private final TextLabel[] typeLabels = new TextLabel[MAX_SOCKETS];
    private final TextLabel[] stateLabels = new TextLabel[MAX_SOCKETS];
    private final float[] hoverLevels = new float[MAX_SOCKETS];
    private final float[] socketCentreX = new float[MAX_SOCKETS];
    private final float[] socketCentreY = new float[MAX_SOCKETS];
    private HullUpgradePanel frameworkPanel;
    private boolean panelDismissed;
    private CustomPanelAPI tooltipAnchor;
    private int tooltipSlot = -1;
    private boolean active;
    private float inspectLevel;
    private float elapsedSeconds;
    private int shownRevision = Integer.MIN_VALUE;
    private Map<HullSize, Integer> shownUpgradeCounts = Map.of();
    private List<FrameworkSlots.Slot> slots = List.of();
    private List<FrameworkSocketLayout.Placement> placements = List.of();
    private int renderedSockets;

    FrameworkInspection(CustomPanelAPI hostPanel, TreeAllocationSession treeSession, SocketPlacement socketPlacement) {
        this.hostPanel = hostPanel;
        this.treeSession = treeSession;
        this.socketPlacement = socketPlacement;
        this.hoverTooltip = new SocketableHoverTooltip(hostPanel);
        for (int i = 0; i < MAX_SOCKETS; i++) {
            typeLabels[i] = new TextLabel(LABEL_FONT_SIZE, Color.WHITE);
            stateLabels[i] = new TextLabel(LABEL_FONT_SIZE, Misc.getGrayColor());
        }
        upgradesButton.setLabel(Translation.text("framework.button"));
    }

    boolean isPanelOpen() {
        return frameworkPanel != null;
    }

    boolean isInspecting() {
        return inspectLevel > 0f;
    }

    float inspectLevel() {
        return inspectLevel;
    }

    void advance(float amount, PositionAPI canvasPosition, TreeCamera camera, SkillTreeFleetRenderer fleetRenderer, float mouseX, float mouseY) {
        elapsedSeconds += amount;
        active = camera.isFollowSettled();
        refreshIfChanged();
        float fadeStep = amount / INSPECT_FADE_SECONDS;
        inspectLevel = Math.max(0f, Math.min(1f, inspectLevel + (active ? fadeStep : -fadeStep)));
        if (active && fleetRenderer.soloLongestSide() > 0f) {
            fitShipToFreeSpace(canvasPosition, camera, fleetRenderer.soloLongestSide());
        } else if (camera.isFollowing()) {
            camera.stopInspecting();
        }
        if (!camera.isFollowing()) {
            panelDismissed = false;
        }
        boolean wantsPanel = active && !panelDismissed && !socketPlacement.isEngaged();
        if (wantsPanel && frameworkPanel == null) {
            openPanel(canvasPosition);
        } else if (!wantsPanel && frameworkPanel != null) {
            frameworkPanel.close();
        }
        float hoverStep = amount / HOVER_SECONDS;
        int hovered = socketAt(mouseX, mouseY);
        for (int i = 0; i < MAX_SOCKETS; i++) {
            hoverLevels[i] = Math.max(0f, Math.min(1f, hoverLevels[i] + (i == hovered ? hoverStep : -hoverStep)));
        }
        updateTooltip(hovered);
        placeUpgradesButton();
    }

    private void fitShipToFreeSpace(PositionAPI canvasPosition, TreeCamera camera, float shipTreeLength) {
        float panelRight = 0f;
        if (socketPlacement.isStorageOpen()) {
            panelRight = PANEL_MARGIN + SocketStoragePanel.WIDTH;
        } else if (frameworkPanel != null) {
            panelRight = PANEL_MARGIN + HullUpgradePanel.WIDTH;
        }
        float freeLeft = panelRight + FREE_SPACE_MARGIN;
        float freeRight = canvasPosition.getWidth() - FREE_SPACE_MARGIN;
        float socketColumnRoom = slots.isEmpty() ? 0f
                : FrameworkSocketLayout.COLUMN_GAP + FrameworkSocketLayout.SOCKET_RADIUS * 2f + SOCKET_LABEL_ROOM;
        float shipPixelsForWidth = Math.max(MIN_SHIP_PIXELS, freeRight - freeLeft - socketColumnRoom * 2f);
        float shipPixels = Math.min(canvasPosition.getHeight() * INSPECT_SHIP_SHARE, shipPixelsForWidth);
        camera.inspect(shipPixels / shipTreeLength);
        camera.setFollowOffset((freeLeft + freeRight) / 2f - canvasPosition.getWidth() / 2f);
    }

    private void placeUpgradesButton() {
        ScreenRect storageButton = socketPlacement.storageButtonBounds();
        if (!active || storageButton == ScreenRect.NONE) {
            upgradesButton.hide();
            return;
        }
        float buttonWidth = Math.max(BUTTON_MIN_WIDTH, upgradesButton.preferredWidth());
        upgradesButton.place(storageButton.left() + storageButton.width() + BUTTON_GAP, storageButton.bottom(),
                buttonWidth, storageButton.height());
        upgradesButton.setSelected(frameworkPanel != null);
    }

    boolean buttonContains(float x, float y) {
        return upgradesButton.contains(x, y);
    }

    private void toggleFrameworkPanel() {
        if (frameworkPanel != null) {
            frameworkPanel.close();
            return;
        }
        panelDismissed = false;
        socketPlacement.close();
    }

    void stop() {
        hideTooltip();
        upgradesButton.hide();
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
        if (upgradesButton.isClickable(x, y)) {
            toggleFrameworkPanel();
            SkillTreeSounds.panelOpened();
            return true;
        }
        int socketIndex = inspectLevel < 0.5f ? -1 : socketAt(x, y);
        if (socketIndex < 0) {
            return false;
        }
        socketPlacement.openForFrameworkSocket(slots.get(socketIndex).type(), canvasPosition, shipCard);
        SkillTreeSounds.panelOpened();
        return true;
    }

    boolean pressRight(float x, float y) {
        int socketIndex = socketAt(x, y);
        if (socketIndex < 0 || slots.get(socketIndex).item() == null) {
            return false;
        }
        treeSession.unsocketFrameworkItem(slots.get(socketIndex).type());
        socketPlacement.refresh();
        return true;
    }

    private void updateTooltip(int hovered) {
        FrameworkSlots.Slot hoveredSlot = hovered < 0 || hovered >= slots.size() ? null : slots.get(hovered);
        if (hoveredSlot == null || hoveredSlot.item() == null) {
            hideTooltip();
            return;
        }
        if (hovered == tooltipSlot) {
            hoverTooltip.refreshIfExpansionChanged();
            return;
        }
        hideTooltip();
        float radius = FrameworkSocketLayout.SOCKET_RADIUS;
        PositionAPI hostPosition = hostPanel.getPosition();
        tooltipAnchor = Global.getSettings().createCustom(radius * 2f, radius * 2f, null);
        hostPanel.addComponent(tooltipAnchor).inTL(socketCentreX[hovered] - radius - hostPosition.getX(),
                hostPosition.getY() + hostPosition.getHeight() - socketCentreY[hovered] - radius);
        tooltipSlot = hovered;
        List<StyledText> footer = new ArrayList<>();
        if (!hoveredSlot.active()) {
            footer.add(Translation.msg("framework.socket.inactive")
                    .arg("requirement", HullUpgradeTooltip.requirementText(hoveredSlot.unmetRequirement())).styled());
        }
        footer.add(Translation.styled("framework.socket.removeHint"));
        hoverTooltip.show(hoveredSlot, hoveredSlot.item(), () -> footer, tooltipAnchor.getPosition());
    }

    private void hideTooltip() {
        hoverTooltip.hide();
        if (tooltipAnchor != null) {
            hostPanel.removeComponent(tooltipAnchor);
            tooltipAnchor = null;
        }
        tooltipSlot = -1;
    }

    private void refreshIfChanged() {
        int revision = treeSession.statsRevision();
        Map<HullSize, Integer> upgradeCounts = Global.getSector() == null ? Map.of() : SocketableStore.get().upgradeCounts();
        if (revision == shownRevision && upgradeCounts.equals(shownUpgradeCounts)) {
            return;
        }
        shownRevision = revision;
        shownUpgradeCounts = upgradeCounts;
        slots = treeSession.frameworkSlots();
        if (slots.size() > MAX_SOCKETS) {
            slots = slots.subList(0, MAX_SOCKETS);
        }
        placements = List.of();
        refreshLabels();
        hideTooltip();
        if (frameworkPanel != null) {
            frameworkPanel.update(panelView());
        }
    }

    private void refreshLabels() {
        for (int i = 0; i < slots.size(); i++) {
            FrameworkSlots.Slot slot = slots.get(i);
            typeLabels[i].set(slot.type().displayName());
            typeLabels[i].setColor(FrameworkSocketFlair.colorOf(slot.type()));
            if (!slot.active()) {
                stateLabels[i].set(Translation.msg("framework.socket.inactive")
                        .arg("requirement", HullUpgradeTooltip.requirementText(slot.unmetRequirement())).text());
                stateLabels[i].setColor(FrameworkSocketFlair.inactiveColor());
            } else if (slot.item() == null) {
                stateLabels[i].set(Translation.text("framework.socket.empty"));
                stateLabels[i].setColor(Misc.getGrayColor());
            } else {
                stateLabels[i].set(slot.item().name());
                stateLabels[i].setColor(slot.item().rarity().color());
            }
        }
    }

    private void openPanel(PositionAPI canvasPosition) {
        CampaignFleetAPI playerFleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        if (playerFleet != null) {
            SocketableStore.get().absorbFrom(playerFleet.getCargo());
            shownUpgradeCounts = SocketableStore.get().upgradeCounts();
        }
        float panelHeight = socketPlacement.sidePanelHeight(canvasPosition);
        frameworkPanel = HullUpgradePanel.open(hostPanel, PANEL_MARGIN, PANEL_TOP, panelHeight, panelView(), new HullUpgradePanel.Listener() {
            @Override
            public void installUpgrade() {
                treeSession.installUpgrade();
                refreshIfChanged();
            }

            @Override
            public void toggleSocket(SocketType socketType) {
                if (treeSession.isSocketTypeUnlocked(socketType)) {
                    treeSession.lockSocket(socketType);
                } else {
                    treeSession.unlockSocket(socketType);
                }
                refreshIfChanged();
            }

            @Override
            public void closed() {
                frameworkPanel = null;
                panelDismissed = true;
            }
        });
        SkillTreeSounds.panelOpened();
    }

    private HullUpgradePanel.View panelView() {
        HullSize hullSize = treeSession.hullSize();
        int upgradesInStorage = shownUpgradeCounts.getOrDefault(hullSize, 0);
        FrameworkSockets.UpgradeBlock upgradeBlock = null;
        if (treeSession.frameworkPoints() >= FrameworkSockets.MAX_POINTS) {
            upgradeBlock = FrameworkSockets.UpgradeBlock.AT_MAX_POINTS;
        } else if (upgradesInStorage <= 0) {
            upgradeBlock = FrameworkSockets.UpgradeBlock.NONE_IN_STORAGE;
        }
        List<HullUpgradePanel.SocketEntry> socketEntries = new ArrayList<>();
        for (SocketType socketType : SocketType.frameworkTypes()) {
            socketEntries.add(socketEntry(socketType));
        }
        return new HullUpgradePanel.View(hullSize, treeSession.frameworkPoints(), treeSession.unspentFrameworkPoints(), upgradesInStorage,
                upgradeBlock, List.copyOf(socketEntries));
    }

    private HullUpgradePanel.SocketEntry socketEntry(SocketType socketType) {
        if (treeSession.isSocketTypeUnlocked(socketType)) {
            String itemIcon = null;
            for (FrameworkSlots.Slot slot : slots) {
                if (slot.type() == socketType && slot.item() != null) {
                    itemIcon = slot.item().iconPath();
                }
            }
            return new HullUpgradePanel.SocketEntry(socketType, HullUpgradePanel.SocketState.UNLOCKED, null, itemIcon);
        }
        String unmetRequirement = treeSession.unmetRequirement(socketType);
        if (unmetRequirement != null) {
            return new HullUpgradePanel.SocketEntry(socketType, HullUpgradePanel.SocketState.RESTRICTED,
                    HullUpgradeTooltip.requirementText(unmetRequirement), null);
        }
        HullUpgradePanel.SocketState state = treeSession.unspentFrameworkPoints() > 0 ? HullUpgradePanel.SocketState.AVAILABLE
                : HullUpgradePanel.SocketState.NO_POINTS;
        return new HullUpgradePanel.SocketEntry(socketType, state, null, null);
    }

    private int socketAt(float x, float y) {
        if (!isInspecting()) {
            return -1;
        }
        float radius = FrameworkSocketLayout.SOCKET_RADIUS;
        for (int i = 0; i < renderedSockets; i++) {
            if (Math.hypot(x - socketCentreX[i], y - socketCentreY[i]) <= radius) {
                return i;
            }
        }
        return -1;
    }

    void renderBackdrop(PositionAPI canvasPosition, float alphaMult) {
        if (inspectLevel <= 0f) {
            return;
        }
        float gridAlpha = GRID_ALPHA * inspectLevel * alphaMult;
        float left = canvasPosition.getX();
        float bottom = canvasPosition.getY();
        float width = canvasPosition.getWidth();
        float height = canvasPosition.getHeight();
        GLDraw.horizontalLines(left, bottom, width, height, GRID_SPACING, GRID_COLOR, gridAlpha);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Misc.setColor(GRID_COLOR, gridAlpha);
        GL11.glBegin(GL11.GL_LINES);
        for (float x = left + GRID_SPACING; x < left + width; x += GRID_SPACING) {
            GL11.glVertex2f(x, bottom);
            GL11.glVertex2f(x, bottom + height);
        }
        GL11.glEnd();
    }

    void renderButton(float mouseX, float mouseY, float alphaMult) {
        upgradesButton.render(mouseX, mouseY, alphaMult);
    }

    void render(PositionAPI canvasPosition, TreeViewport viewport, SkillTreeFleetRenderer fleetRenderer, float alphaMult) {
        renderedSockets = 0;
        if (inspectLevel <= 0f) {
            return;
        }
        float eased = inspectLevel * inspectLevel * (3f - 2f * inspectLevel);
        float alpha = alphaMult * eased;
        SkillTreeFleetRenderer.InspectedShip ship = slots.isEmpty() ? null : fleetRenderer.inspectedShip(viewport);
        if (ship == null) {
            return;
        }
        if (placements.size() != slots.size()) {
            List<ShipAnchors.Anchor> anchors = new ArrayList<>(slots.size());
            slots.forEach(slot -> anchors.add(ship.anchors().forSocket(slot.type())));
            placements = FrameworkSocketLayout.place(anchors);
        }
        for (int i = 0; i < slots.size(); i++) {
            FrameworkSlots.Slot slot = slots.get(i);
            FrameworkSocketLayout.Placement placement = placements.get(i);
            float socketX = FrameworkSocketLayout.socketX(placement, ship.screenX(), ship.longestSidePixels());
            float socketY = FrameworkSocketLayout.socketY(placement, ship.screenY());
            socketCentreX[i] = socketX;
            socketCentreY[i] = socketY;
            ShipAnchors.Anchor anchor = ship.anchors().forSocket(slot.type());
            Color accent = slot.active() ? FrameworkSocketFlair.colorOf(slot.type()) : FrameworkSocketFlair.inactiveColor();
            FrameworkSocketFlair.leader(FrameworkSocketLayout.edgeX(placement, socketX), socketY, FrameworkSocketLayout.elbowX(placement, socketX),
                    ship.anchorScreenX(anchor), ship.anchorScreenY(anchor), accent, elapsedSeconds + i * 0.37f, alpha * 0.9f);
        }
        renderedSockets = slots.size();
        float radius = FrameworkSocketLayout.SOCKET_RADIUS;
        for (int i = 0; i < renderedSockets; i++) {
            FrameworkSlots.Slot slot = slots.get(i);
            boolean selected = socketPlacement.isFrameworkTargeted() && socketPlacement.targetFrameworkType() == slot.type();
            FrameworkSocketFlair.SocketLook look = new FrameworkSocketFlair.SocketLook(hoverLevels[i], selected, slot.active(),
                    slot.item() == null ? null : slot.item().iconPath());
            FrameworkSocketFlair.render(socketCentreX[i], socketCentreY[i], radius, FrameworkSocketFlair.colorOf(slot.type()), look,
                    elapsedSeconds + i * 0.53f, alpha);
            drawCentred(typeLabels[i], socketCentreX[i], socketCentreY[i] + radius + LABEL_GAP + LABEL_FONT_SIZE, alpha);
            drawCentred(stateLabels[i], socketCentreX[i], socketCentreY[i] - radius - LABEL_GAP, alpha);
        }
    }

    private static void drawCentred(TextLabel label, float centreX, float top, float alpha) {
        label.setAlpha(alpha);
        label.draw(centreX - label.width() / 2f, top);
    }
}
