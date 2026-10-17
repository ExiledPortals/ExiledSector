package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.FrameworkSlots;
import exiledsector.hullitems.FrameworkSockets;
import exiledsector.hullitems.HullUpgradeTooltip;
import exiledsector.hullitems.SocketType;
import exiledsector.hullitems.HullItem;
import exiledsector.hullitems.HullItemStore;
import exiledsector.ui.decoration.ShipAnchors;
import exiledsector.ui.decoration.SkillTreeFleetRenderer;
import exiledsector.ui.framework.FrameworkSocketFlair;
import exiledsector.ui.framework.FrameworkSocketLayout;
import exiledsector.ui.node.TreeAllocationSession;
import exiledsector.ui.socket.HullUpgradePanel;
import exiledsector.ui.socket.HullItemHoverTooltip;
import exiledsector.ui.util.Fade;
import exiledsector.ui.util.TextLabel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class HullSocketsDisplay {

    record SocketBounds(float minCentreX, float maxCentreX, float minCentreY, float maxCentreY) {
        static final SocketBounds UNBOUNDED = new SocketBounds(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);

        float clampX(float centreX) {
            return Math.max(minCentreX, Math.min(maxCentreX, centreX));
        }

        float clampY(float centreY) {
            return Math.max(minCentreY, Math.min(maxCentreY, centreY));
        }
    }

    static final int MAX_SOCKETS = FrameworkSockets.MAX_POINTS;
    private static final float HOVER_SECONDS = 0.15f;
    private static final float LABEL_GAP = 8f;
    private static final float LABEL_FONT_SIZE = SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE;
    static final float VERTICAL_REACH = FrameworkSocketLayout.SOCKET_RADIUS + LABEL_GAP + LABEL_FONT_SIZE * 2f;
    private static final float OTHER_SOCKET_DIM = 0.3f;

    private final CustomPanelAPI hostPanel;
    private final TreeAllocationSession treeSession;
    private final FrameworkSocketLayout.Arrangement arrangement;
    private final HullItemHoverTooltip hoverTooltip;
    private final TextLabel[] typeLabels = new TextLabel[MAX_SOCKETS];
    private final TextLabel[] stateLabels = new TextLabel[MAX_SOCKETS];
    private final float[] hoverLevels = new float[MAX_SOCKETS];
    private final float[] socketCentreX = new float[MAX_SOCKETS];
    private final float[] socketCentreY = new float[MAX_SOCKETS];
    private CustomPanelAPI tooltipAnchor;
    private int tooltipSlot = -1;
    private float elapsedSeconds;
    private int shownRevision = Integer.MIN_VALUE;
    private HullItemStore upgradeStore;
    private int shownUpgradeRevision = Integer.MIN_VALUE;
    private Map<HullSize, Integer> shownUpgradeCounts = Map.of();
    private List<FrameworkSlots.Slot> slots = List.of();
    private List<FrameworkSocketLayout.Placement> placements = List.of();
    private int renderedSockets;

    HullSocketsDisplay(CustomPanelAPI hostPanel, TreeAllocationSession treeSession, FrameworkSocketLayout.Arrangement arrangement) {
        this.hostPanel = hostPanel;
        this.treeSession = treeSession;
        this.arrangement = arrangement;
        this.hoverTooltip = new HullItemHoverTooltip(hostPanel);
        for (int i = 0; i < MAX_SOCKETS; i++) {
            typeLabels[i] = new TextLabel(LABEL_FONT_SIZE, Color.WHITE);
            stateLabels[i] = new TextLabel(LABEL_FONT_SIZE, Misc.getGrayColor());
        }
    }

    List<FrameworkSlots.Slot> slots() {
        return slots;
    }

    boolean refreshIfChanged() {
        int revision = treeSession.statsRevision();
        HullItemStore store = upgradeStore();
        int upgradeRevision = store == null ? 0 : store.upgradeRevision();
        if (revision == shownRevision && upgradeRevision == shownUpgradeRevision) {
            return false;
        }
        shownRevision = revision;
        shownUpgradeRevision = upgradeRevision;
        shownUpgradeCounts = store == null ? Map.of() : store.upgradeCounts();
        slots = treeSession.frameworkSlots();
        if (slots.size() > MAX_SOCKETS) {
            slots = slots.subList(0, MAX_SOCKETS);
        }
        placements = List.of();
        refreshLabels();
        hideTooltip();
        return true;
    }

    void absorbUpgradesFromCargo() {
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        HullItemStore store = upgradeStore();
        if (playerFleet != null && store != null) {
            store.absorbFrom(playerFleet.getCargo());
            shownUpgradeRevision = store.upgradeRevision();
            shownUpgradeCounts = store.upgradeCounts();
        }
    }

    void advance(float amount, float mouseX, float mouseY, boolean socketsShown) {
        elapsedSeconds += amount;
        float hoverStep = amount / HOVER_SECONDS;
        int hovered = socketsShown ? socketAt(mouseX, mouseY) : -1;
        for (int i = 0; i < MAX_SOCKETS; i++) {
            hoverLevels[i] = Fade.step(hoverLevels[i], i == hovered, hoverStep);
        }
        updateTooltip(hovered);
    }

    int socketAt(float x, float y) {
        float radius = FrameworkSocketLayout.SOCKET_RADIUS;
        for (int i = 0; i < renderedSockets; i++) {
            if (Math.hypot(x - socketCentreX[i], y - socketCentreY[i]) <= radius) {
                return i;
            }
        }
        return -1;
    }

    void forgetRenderedSockets() {
        renderedSockets = 0;
    }

    private HullItemStore upgradeStore() {
        if (upgradeStore == null && Global.getSector() != null) {
            upgradeStore = HullItemStore.get();
        }
        return upgradeStore;
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
        hoverTooltip.show(hoveredSlot, hoveredSlot.item(), treeSession.hullSize(), () -> tooltipFooter(hoveredSlot), tooltipAnchor.getPosition());
    }

    private static List<StyledText> tooltipFooter(FrameworkSlots.Slot slot) {
        List<StyledText> footer = new ArrayList<>();
        if (!slot.active()) {
            footer.add(Translation.msg("framework.socket.inactive")
                    .arg("requirement", HullUpgradeTooltip.requirementText(slot.unmetRequirement())).styled());
        }
        footer.add(Translation.styled("framework.socket.removeHint"));
        return footer;
    }

    void hideTooltip() {
        hoverTooltip.hide();
        if (tooltipAnchor != null) {
            hostPanel.removeComponent(tooltipAnchor);
            tooltipAnchor = null;
        }
        tooltipSlot = -1;
    }

    HullUpgradePanel.Listener panelListener(Runnable closed) {
        return new HullUpgradePanel.Listener() {
            @Override
            public void installUpgrade() {
                treeSession.installUpgrade();
            }

            @Override
            public void toggleSocket(SocketType socketType) {
                if (treeSession.isSocketTypeUnlocked(socketType)) {
                    treeSession.lockSocket(socketType);
                } else {
                    treeSession.unlockSocket(socketType);
                }
            }

            @Override
            public void closed() {
                closed.run();
            }
        };
    }

    HullUpgradePanel.View panelView() {
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

    void render(SkillTreeFleetRenderer.InspectedShip ship, FrameworkSocketLayout.ShipExtent extent, float alpha, SocketBounds bounds,
                HullItem placing, SocketType targetedType) {
        renderedSockets = 0;
        if (ship == null || slots.isEmpty() || alpha <= 0f) {
            return;
        }
        if (placements.size() != slots.size()) {
            List<ShipAnchors.Anchor> anchors = new ArrayList<>(slots.size());
            slots.forEach(slot -> anchors.add(ship.anchors().forSocket(slot.type())));
            placements = FrameworkSocketLayout.place(anchors, arrangement);
        }
        for (int i = 0; i < slots.size(); i++) {
            FrameworkSlots.Slot slot = slots.get(i);
            FrameworkSocketLayout.Placement placement = placements.get(i);
            float socketX = bounds.clampX(FrameworkSocketLayout.socketX(placement, ship.screenX(), extent));
            float socketY = bounds.clampY(FrameworkSocketLayout.socketY(placement, ship.screenY(), extent));
            socketCentreX[i] = socketX;
            socketCentreY[i] = socketY;
            ShipAnchors.Anchor anchor = ship.anchors().forSocket(slot.type());
            Color accent = slot.active() ? FrameworkSocketFlair.colorOf(slot.type()) : FrameworkSocketFlair.inactiveColor();
            FrameworkSocketFlair.LeaderPath path = new FrameworkSocketFlair.LeaderPath(FrameworkSocketLayout.edgeX(placement, socketX),
                    FrameworkSocketLayout.edgeY(placement, socketY), FrameworkSocketLayout.elbowX(placement, socketX),
                    FrameworkSocketLayout.elbowY(placement, socketY), ship.anchorScreenX(anchor), ship.anchorScreenY(anchor));
            FrameworkSocketFlair.leader(path, accent, elapsedSeconds + i * 0.37f,
                    alpha * 0.9f * placingDim(placing, slot.type()));
        }
        renderedSockets = slots.size();
        float radius = FrameworkSocketLayout.SOCKET_RADIUS;
        for (int i = 0; i < renderedSockets; i++) {
            FrameworkSlots.Slot slot = slots.get(i);
            boolean selected = placing != null ? placing.kind() == slot.type() : targetedType == slot.type();
            FrameworkSocketFlair.SocketLook look = new FrameworkSocketFlair.SocketLook(hoverLevels[i], selected, slot.active(),
                    slot.item() == null ? null : slot.item().iconPath());
            float socketAlpha = alpha * placingDim(placing, slot.type());
            FrameworkSocketFlair.render(socketCentreX[i], socketCentreY[i], radius, FrameworkSocketFlair.colorOf(slot.type()), look,
                    elapsedSeconds + i * 0.53f, socketAlpha);
            drawCentred(typeLabels[i], socketCentreX[i], socketCentreY[i] + radius + LABEL_GAP + LABEL_FONT_SIZE, socketAlpha);
            drawCentred(stateLabels[i], socketCentreX[i], socketCentreY[i] - radius - LABEL_GAP, socketAlpha);
        }
    }

    private static float placingDim(HullItem placing, SocketType socketType) {
        return placing == null || placing.kind() == socketType ? 1f : OTHER_SOCKET_DIM;
    }

    private static void drawCentred(TextLabel label, float centreX, float top, float alpha) {
        label.setAlpha(alpha);
        label.draw(centreX - label.width() / 2f, top);
    }
}
