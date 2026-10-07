package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.FrameworkSlots;
import exiledsector.socketables.HullFramework;
import exiledsector.socketables.HullFrameworkTooltip;
import exiledsector.socketables.HullFrameworks;
import exiledsector.socketables.SocketCustody;
import exiledsector.socketables.SocketableStore;
import exiledsector.ui.decoration.ShipAnchors;
import exiledsector.ui.decoration.SkillTreeFleetRenderer;
import exiledsector.ui.framework.FrameworkSocketFlair;
import exiledsector.ui.framework.FrameworkSocketLayout;
import exiledsector.ui.node.TreeAllocationSession;
import exiledsector.ui.socket.FrameworkPickerPanel;
import exiledsector.ui.socket.SocketableHoverTooltip;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.TextLabel;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class FrameworkInspection {

    private static final int MAX_SOCKETS = 4;
    private static final float INSPECT_SHIP_SHARE = 0.4f;
    private static final float INSPECT_FADE_SECONDS = 0.6f;
    private static final float HOVER_SECONDS = 0.15f;
    private static final float PANEL_MARGIN = 16f;
    private static final float PANEL_TOP = 100f;
    private static final float PANEL_BOTTOM_ROOM = 24f;
    private static final float LABEL_GAP = 8f;
    private static final float BUTTON_HEIGHT = 32f;
    private static final float BUTTON_MIN_WIDTH = 190f;
    private static final float HEADER_TOP_MARGIN = 24f;
    private static final float GRID_SPACING = 48f;
    private static final float GRID_ALPHA = 0.06f;
    private static final float LABEL_FONT_SIZE = SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE;
    private static final float TITLE_FONT_SIZE = SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE;
    private static final Color GRID_COLOR = SkillTreePanelStyle.GLOW_COLOR;

    private final CustomPanelAPI hostPanel;
    private final TreeAllocationSession treeSession;
    private final SocketPlacement socketPlacement;
    private final SkillTreeUiButton removeButton = new SkillTreeUiButton("");
    private final TextLabel titleLabel = new TextLabel(TITLE_FONT_SIZE, Color.WHITE);
    private final TextLabel[] typeLabels = new TextLabel[MAX_SOCKETS];
    private final TextLabel[] stateLabels = new TextLabel[MAX_SOCKETS];
    private final float[] hoverLevels = new float[MAX_SOCKETS];
    private final float[] socketCentreX = new float[MAX_SOCKETS];
    private final float[] socketCentreY = new float[MAX_SOCKETS];
    private FrameworkPickerPanel pickerPanel;
    private boolean closingPickerForStorage;
    private final SocketableHoverTooltip hoverTooltip;
    private CustomPanelAPI tooltipAnchor;
    private int tooltipSlot = -1;
    private boolean pickerDismissed;
    private boolean active;
    private float inspectLevel;
    private float elapsedSeconds;
    private int shownRevision = Integer.MIN_VALUE;
    private int shownFrameworkCount = -1;
    private HullFramework framework;
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
        removeButton.setLabel(Translation.text("framework.remove"));
    }

    boolean isPickerOpen() {
        return pickerPanel != null;
    }

    boolean isInspecting() {
        return framework != null && inspectLevel > 0f;
    }

    float inspectLevel() {
        return inspectLevel;
    }

    void advance(float amount, PositionAPI canvasPosition, TreeCamera camera, SkillTreeFleetRenderer fleetRenderer, float mouseX, float mouseY) {
        elapsedSeconds += amount;
        active = camera.isFollowSettled();
        refreshIfChanged();
        boolean inspecting = active && framework != null;
        float fadeStep = amount / INSPECT_FADE_SECONDS;
        inspectLevel = Math.max(0f, Math.min(1f, inspectLevel + (inspecting ? fadeStep : -fadeStep)));
        if (inspecting && fleetRenderer.soloLongestSide() > 0f) {
            camera.inspect(canvasPosition.getHeight() * INSPECT_SHIP_SHARE / fleetRenderer.soloLongestSide());
        } else if (camera.isFollowing()) {
            camera.stopInspecting();
        }
        if (!camera.isFollowing()) {
            pickerDismissed = false;
        }
        boolean wantsPicker = active && framework == null && !pickerDismissed && !socketPlacement.isEngaged();
        if (wantsPicker && pickerPanel == null) {
            openPicker(canvasPosition);
        } else if (!wantsPicker && pickerPanel != null) {
            closingPickerForStorage = true;
            pickerPanel.close();
            closingPickerForStorage = false;
        }
        float hoverStep = amount / HOVER_SECONDS;
        int hovered = socketAt(mouseX, mouseY);
        for (int i = 0; i < MAX_SOCKETS; i++) {
            hoverLevels[i] = Math.max(0f, Math.min(1f, hoverLevels[i] + (i == hovered ? hoverStep : -hoverStep)));
        }
        updateTooltip(hovered);
        placeRemoveButton(canvasPosition);
    }

    void stop() {
        hideTooltip();
        if (pickerPanel != null) {
            pickerPanel.close();
        }
        pickerDismissed = false;
        if (socketPlacement.isFrameworkTargeted()) {
            socketPlacement.close();
        }
    }

    boolean escape() {
        if (pickerPanel != null) {
            pickerPanel.close();
            return true;
        }
        return false;
    }

    boolean pressLeft(float x, float y, PositionAPI canvasPosition, ScreenRect shipCard) {
        if (!isInspecting() || inspectLevel < 0.5f) {
            return false;
        }
        if (removeButton.isClickable(x, y)) {
            socketPlacement.close();
            treeSession.removeFramework();
            refreshIfChanged();
            return true;
        }
        int socketIndex = socketAt(x, y);
        if (socketIndex < 0) {
            return false;
        }
        FrameworkSlots.Slot slot = slots.get(socketIndex);
        socketPlacement.openForFrameworkSlot(slot.index(), slot.type(), canvasPosition, shipCard);
        SkillTreeSounds.panelOpened();
        return true;
    }

    boolean pressRight(float x, float y) {
        int socketIndex = isInspecting() ? socketAt(x, y) : -1;
        if (socketIndex < 0 || slots.get(socketIndex).item() == null) {
            return false;
        }
        treeSession.unsocketFrameworkItem(slots.get(socketIndex).index());
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
                    .arg("requirement", HullFrameworkTooltip.requirementText(hoveredSlot.unmetRequirement())).styled());
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
        int frameworkCount = Global.getSector() == null ? 0 : SocketableStore.get().frameworks().size();
        if (revision == shownRevision && frameworkCount == shownFrameworkCount) {
            return;
        }
        shownRevision = revision;
        shownFrameworkCount = frameworkCount;
        framework = treeSession.installedFramework();
        slots = framework == null ? List.of() : treeSession.frameworkSlots();
        if (slots.size() > MAX_SOCKETS) {
            slots = slots.subList(0, MAX_SOCKETS);
        }
        placements = List.of();
        refreshLabels();
        hideTooltip();
        if (pickerPanel != null) {
            pickerPanel.update(pickerEntries(), otherSizeCount());
        }
    }

    private void refreshLabels() {
        if (framework != null) {
            titleLabel.set(framework.name());
            titleLabel.setColor(framework.rarity().color());
        }
        for (int i = 0; i < slots.size(); i++) {
            FrameworkSlots.Slot slot = slots.get(i);
            typeLabels[i].set(slot.type().displayName());
            typeLabels[i].setColor(FrameworkSocketFlair.colorOf(slot.type()));
            if (!slot.active()) {
                stateLabels[i].set(Translation.msg("framework.socket.inactive")
                        .arg("requirement", HullFrameworkTooltip.requirementText(slot.unmetRequirement())).text());
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

    private void openPicker(PositionAPI canvasPosition) {
        CampaignFleetAPI playerFleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        if (playerFleet != null) {
            SocketableStore.get().absorbFrom(playerFleet.getCargo());
        }
        float panelLeft = PANEL_MARGIN;
        float panelHeight = canvasPosition.getHeight() - PANEL_TOP - PANEL_BOTTOM_ROOM;
        pickerPanel = FrameworkPickerPanel.open(hostPanel, panelLeft, PANEL_TOP, panelHeight, treeSession.hullSize(), pickerEntries(),
                otherSizeCount(), new FrameworkPickerPanel.Listener() {
                    @Override
                    public void chosen(HullFramework chosenFramework) {
                        if (treeSession.installFramework(chosenFramework)) {
                            refreshIfChanged();
                        }
                    }

                    @Override
                    public void closed() {
                        pickerPanel = null;
                        if (framework == null && !closingPickerForStorage) {
                            pickerDismissed = true;
                        }
                    }
                });
        SkillTreeSounds.panelOpened();
    }

    private List<FrameworkPickerPanel.Entry> pickerEntries() {
        List<FrameworkPickerPanel.Entry> entries = new ArrayList<>();
        if (Global.getSector() == null) {
            return entries;
        }
        for (HullFramework ownedFramework : SocketableStore.get().frameworks()) {
            if (ownedFramework.hullSize() != treeSession.hullSize() || SocketCustody.frameworkShipId(ownedFramework) != null) {
                continue;
            }
            entries.add(new FrameworkPickerPanel.Entry(ownedFramework, blockReason(treeSession.frameworkInstallBlock(ownedFramework))));
        }
        entries.sort(Comparator.comparing((FrameworkPickerPanel.Entry entry) -> entry.blockReason() != null)
                .thenComparing(entry -> -entry.framework().rarity().ordinal())
                .thenComparing(entry -> -entry.framework().slotCount()));
        return entries;
    }

    private int otherSizeCount() {
        if (Global.getSector() == null) {
            return 0;
        }
        int count = 0;
        for (HullFramework ownedFramework : SocketableStore.get().frameworks()) {
            if (ownedFramework.hullSize() != treeSession.hullSize() && SocketCustody.frameworkShipId(ownedFramework) == null) {
                count++;
            }
        }
        return count;
    }

    private static String blockReason(HullFrameworks.InstallBlock block) {
        if (block == null) {
            return null;
        }
        return switch (block.reason()) {
            case UNMET_REQUIREMENT -> Translation.msg("framework.block.requirement").arg("type", block.socketType().displayName())
                    .arg("requirement", HullFrameworkTooltip.requirementText(block.requirementTag())).text();
            case WRONG_HULL_SIZE, INSTALLED_ELSEWHERE -> Translation.text("framework.block.unavailable");
        };
    }

    private void placeRemoveButton(PositionAPI canvasPosition) {
        if (framework == null || inspectLevel < 0.5f) {
            removeButton.hide();
            return;
        }
        float buttonWidth = Math.max(BUTTON_MIN_WIDTH, removeButton.preferredWidth());
        float canvasTop = canvasPosition.getY() + canvasPosition.getHeight();
        float buttonBottom = canvasTop - HEADER_TOP_MARGIN - TITLE_FONT_SIZE - LABEL_GAP - BUTTON_HEIGHT;
        removeButton.place(canvasPosition.getX() + (canvasPosition.getWidth() - buttonWidth) / 2f, buttonBottom, buttonWidth, BUTTON_HEIGHT);
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

    void render(PositionAPI canvasPosition, TreeViewport viewport, SkillTreeFleetRenderer fleetRenderer, float alphaMult, float mouseX,
                float mouseY) {
        renderedSockets = 0;
        if (framework == null || inspectLevel <= 0f) {
            return;
        }
        SkillTreeFleetRenderer.InspectedShip ship = fleetRenderer.inspectedShip(viewport);
        if (ship == null) {
            return;
        }
        if (placements.size() != slots.size()) {
            List<ShipAnchors.Anchor> anchors = new ArrayList<>(slots.size());
            slots.forEach(slot -> anchors.add(ship.anchors().forSocket(slot.type())));
            placements = FrameworkSocketLayout.place(anchors);
        }
        float eased = inspectLevel * inspectLevel * (3f - 2f * inspectLevel);
        float alpha = alphaMult * eased;
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
            boolean selected = socketPlacement.isFrameworkTargeted() && socketPlacement.targetFrameworkSlot() == slot.index();
            FrameworkSocketFlair.SocketLook look = new FrameworkSocketFlair.SocketLook(hoverLevels[i], selected, slot.active(),
                    slot.item() == null ? null : slot.item().iconPath());
            FrameworkSocketFlair.render(socketCentreX[i], socketCentreY[i], radius, FrameworkSocketFlair.colorOf(slot.type()), look,
                    elapsedSeconds + i * 0.53f, alpha);
            drawCentred(typeLabels[i], socketCentreX[i], socketCentreY[i] + radius + LABEL_GAP + LABEL_FONT_SIZE, alpha);
            drawCentred(stateLabels[i], socketCentreX[i], socketCentreY[i] - radius - LABEL_GAP, alpha);
        }
        float canvasTop = canvasPosition.getY() + canvasPosition.getHeight();
        drawCentred(titleLabel, canvasPosition.getX() + canvasPosition.getWidth() / 2f, canvasTop - HEADER_TOP_MARGIN, alpha);
        removeButton.render(mouseX, mouseY, alphaMult);
    }

    private static void drawCentred(TextLabel label, float centreX, float top, float alpha) {
        label.setAlpha(alpha);
        label.draw(centreX - label.width() / 2f, top);
    }
}
