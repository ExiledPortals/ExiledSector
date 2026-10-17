package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import exiledsector.i18n.Translation;
import exiledsector.skills.FrameworkSlots;
import exiledsector.socketables.SocketCustody;
import exiledsector.socketables.SocketType;
import exiledsector.socketables.Socketable;
import exiledsector.ui.decoration.RefitShipPortrait;
import exiledsector.ui.decoration.SkillTreeFleetRenderer;
import exiledsector.ui.framework.FrameworkSocketLayout;
import exiledsector.ui.node.NodeSearch;
import exiledsector.ui.node.TreeAllocationSession;
import exiledsector.ui.refit.RefitScreenLayout;
import exiledsector.ui.socket.HullUpgradePanel;
import exiledsector.ui.socket.SocketPanelPlacement;
import exiledsector.ui.socket.SocketPanelTab;
import exiledsector.ui.socket.SocketStoragePanel;
import exiledsector.ui.util.Fade;
import lunalib.lunaRefit.BaseRefitButton;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

final class HullUpgradesScreen extends BaseCustomUIPanelPlugin {

    static final float OPEN_SECONDS = 0.25f;
    static final float CONTENT_SECONDS = 0.3f;

    private final FleetMemberAPI member;
    private final ShipVariantAPI variant;
    private final BaseRefitButton refitButton;
    private final RefitScreenLayout layout;
    private final RefitBackdrop backdrop = new RefitBackdrop();
    private final RefitShipPortrait portrait;
    private final FrameworkSocketLayout.ShipExtent shipExtent;
    private final HullUpgradesLayout screenLayout;
    private final Consumer<SocketPanelTab> tabListener = this::switchTab;
    private CustomPanelAPI hostPanel;
    private TreeAllocationSession treeSession;
    private HullSocketsDisplay sockets;
    private SocketStoragePanel storagePanel;
    private HullUpgradePanel upgradesPanel;
    private Map<String, FleetMemberAPI> ownedShips = Map.of();
    private Socketable placingSocketable;
    private SocketType targetedType;
    private int storageRevision = Integer.MIN_VALUE;
    private float openLevel;
    private float contentLevel;
    private boolean panelOpened;
    private boolean closing;
    private boolean closeRequested;
    private boolean pointerOverPanel;
    private float mouseX;
    private float mouseY;

    HullUpgradesScreen(FleetMemberAPI member, ShipVariantAPI variant, BaseRefitButton refitButton, RefitScreenLayout layout) {
        this.member = member;
        this.variant = variant;
        this.refitButton = refitButton;
        this.layout = layout;
        this.portrait = RefitShipPortrait.of(member, variant);
        this.shipExtent = portrait == null ? FrameworkSocketLayout.ShipExtent.square(0f)
                : new FrameworkSocketLayout.ShipExtent(portrait.halfWidthPixels(layout.shipZoom()), portrait.halfLengthPixels(layout.shipZoom()));
        this.screenLayout = HullUpgradesLayout.of(layout, shipExtent);
    }

    void attach(CustomPanelAPI panel) {
        hostPanel = panel;
        ownedShips = SocketCustody.reconcile();
        treeSession = new TreeAllocationSession(member, variant, refitButton, new NodeSearch());
        sockets = new HullSocketsDisplay(panel, treeSession, FrameworkSocketLayout.Arrangement.LEFT_ABOVE_BELOW);
        sockets.refreshIfChanged();
    }

    @Override
    public void advance(float amount) {
        if (hostPanel == null) {
            return;
        }
        boolean contentWanted = !closing && openLevel >= 1f;
        contentLevel = Fade.step(contentLevel, contentWanted, amount / CONTENT_SECONDS);
        openLevel = Fade.step(openLevel, !closing || contentLevel > 0f, amount / OPEN_SECONDS);
        if (contentWanted && !panelOpened) {
            panelOpened = true;
            sockets.absorbUpgradesFromCargo();
            openUpgrades(false);
        }
        if (sockets.refreshIfChanged() && upgradesPanel != null) {
            upgradesPanel.update(sockets.panelView());
        }
        if (storagePanel != null && treeSession.statsRevision() != storageRevision) {
            refreshStorage();
        }
        sockets.advance(amount, mouseX, mouseY, contentLevel >= 0.5f && !pointerOverPanel);
        if (closing && openLevel <= 0f && !closeRequested) {
            closeRequested = true;
            sockets.hideTooltip();
            if (portrait != null) {
                portrait.cleanup();
            }
            refitButton.closePanel();
        }
    }

    private float frameRight() {
        return layout.frameX() + layout.frameWidth();
    }

    private float panelTop() {
        return Global.getSettings().getScreenHeight() - layout.frameTop() + HullUpgradesLayout.PANEL_MARGIN;
    }

    private float panelHeight() {
        return layout.frameHeight() - HullUpgradesLayout.PANEL_MARGIN * 2f;
    }

    private SocketPanelPlacement placement(boolean instantly) {
        return new SocketPanelPlacement(screenLayout.panelLeft(), panelTop(), screenLayout.panelWidth(), panelHeight(), instantly);
    }

    private void openUpgrades(boolean instantly) {
        upgradesPanel = HullUpgradePanel.open(hostPanel, placement(instantly), sockets.panelView(), sockets.panelListener(() -> {
            upgradesPanel = null;
            close();
        }), tabListener, HullUpgradePanel.Sizing.SCROLL);
        if (!instantly) {
            SkillTreeSounds.panelOpened();
        }
    }

    private void openStorage(SocketType kindFilter) {
        storagePanel = SocketStoragePanel.open(hostPanel, placement(true), SocketCustody.shipNames(ownedShips), new SocketStoragePanel.Listener() {
            @Override
            public void selected(Socketable socketable) {
                chooseFromStorage(socketable);
            }

            @Override
            public void closed() {
                forgetStorage();
                close();
            }

            @Override
            public void pressedInside() {
                sockets.hideTooltip();
            }
        }, kindFilter, tabListener);
        storageRevision = treeSession.statsRevision();
    }

    private void switchTab(SocketPanelTab tab) {
        if (closing) {
            return;
        }
        if (tab == SocketPanelTab.SOCKETABLES && upgradesPanel != null) {
            showStorage(null);
        } else if (tab == SocketPanelTab.HULL_UPGRADES && storagePanel != null) {
            SocketStoragePanel swapped = storagePanel;
            forgetStorage();
            swapped.swapOut();
            openUpgrades(true);
        }
    }

    private void showStorage(SocketType kindFilter) {
        if (upgradesPanel != null) {
            HullUpgradePanel swapped = upgradesPanel;
            upgradesPanel = null;
            swapped.swapOut();
        }
        if (storagePanel == null) {
            openStorage(kindFilter);
        } else {
            storagePanel.restrictTo(kindFilter);
        }
    }

    private void forgetStorage() {
        storagePanel = null;
        placingSocketable = null;
        targetedType = null;
    }

    private float shipCentreX() {
        return layout.shipCentreX() + (screenLayout.settledShipX() - layout.shipCentreX()) * Fade.smooth(openLevel);
    }

    private void refreshStorage() {
        storageRevision = treeSession.statsRevision();
        storagePanel.refresh(SocketCustody.shipNames(ownedShips));
        if (placingSocketable != null) {
            storagePanel.setSelected(placingSocketable, placingHint(placingSocketable));
        }
    }

    private void chooseFromStorage(Socketable socketable) {
        if (socketable == null) {
            stopPlacing();
            return;
        }
        if (targetedType != null) {
            if (!treeSession.socketFrameworkItem(targetedType, socketable)) {
                SkillTreeSounds.refused();
            }
            return;
        }
        placingSocketable = socketable;
        if (storagePanel != null) {
            storagePanel.setSelected(socketable, placingHint(socketable));
        }
    }

    private String placingHint(Socketable socketable) {
        SocketType socketType = socketable.kind();
        if (socketType == null || socketType == SocketType.SUBROUTINE) {
            return Translation.msg("ui.hullUpgrades.placingHintSubroutine").arg("name", socketable.name()).text();
        }
        String key = treeSession.isSocketTypeUnlocked(socketType) ? "ui.socketStorage.placingHintFramework"
                : "ui.socketStorage.placingHintFrameworkLocked";
        return Translation.msg(key).arg("name", socketable.name()).arg("type", socketType.displayName()).text();
    }

    private void stopPlacing() {
        placingSocketable = null;
        if (storagePanel != null) {
            storagePanel.setSelected(null);
        }
    }

    private void clearTarget() {
        targetedType = null;
        if (storagePanel != null) {
            storagePanel.setTargetingSocket(false);
            storagePanel.restrictTo(null);
        }
    }

    private void close() {
        if (closing) {
            return;
        }
        closing = true;
        sockets.hideTooltip();
        if (storagePanel != null) {
            storagePanel.close();
        }
        if (upgradesPanel != null) {
            upgradesPanel.close();
        }
    }

    private void pressLeft(float x, float y) {
        if (!insideFrame(x, y)) {
            close();
            return;
        }
        int socketIndex = socketIndexAt(x, y);
        if (socketIndex < 0) {
            stopPlacing();
            clearTarget();
            return;
        }
        SocketType socketType = sockets.slots().get(socketIndex).type();
        if (placingSocketable != null) {
            if (placingSocketable.kind() == socketType && treeSession.socketFrameworkItem(socketType, placingSocketable)) {
                stopPlacing();
            } else {
                SkillTreeSounds.refused();
            }
            return;
        }
        targetedType = socketType;
        showStorage(socketType);
        storagePanel.setTargetingSocket(true);
        SkillTreeSounds.panelOpened();
    }

    private int socketIndexAt(float x, float y) {
        boolean socketsLive = contentLevel >= 0.5f && (storagePanel == null || !storagePanel.isWorkbenchOpen());
        return socketsLive ? sockets.socketAt(x, y) : -1;
    }

    private void pressRight(float x, float y) {
        int socketIndex = socketIndexAt(x, y);
        if (socketIndex < 0) {
            return;
        }
        FrameworkSlots.Slot slot = sockets.slots().get(socketIndex);
        if (slot.item() != null) {
            treeSession.unsocketFrameworkItem(slot.type());
        }
    }

    private void escape() {
        if (storagePanel != null && storagePanel.escape()) {
            return;
        }
        if (upgradesPanel != null && upgradesPanel.escape()) {
            return;
        }
        if (placingSocketable != null) {
            stopPlacing();
        } else if (targetedType != null) {
            clearTarget();
            switchTab(SocketPanelTab.HULL_UPGRADES);
        } else if (storagePanel != null) {
            switchTab(SocketPanelTab.HULL_UPGRADES);
        } else {
            close();
        }
    }

    private boolean insideFrame(float x, float y) {
        return x >= layout.frameX() && x <= frameRight() && y >= layout.frameY() && y <= layout.frameTop();
    }

    private boolean panelContains(float x, float y) {
        return (storagePanel != null && (storagePanel.contains(x, y) || storagePanel.workbenchContains(x, y)))
                || (upgradesPanel != null && upgradesPanel.contains(x, y));
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        for (InputEventAPI event : events) {
            if (event.isMouseEvent()) {
                mouseX = event.getX();
                mouseY = event.getY();
                pointerOverPanel = panelContains(mouseX, mouseY);
            }
            if (event.isConsumed()) {
                continue;
            }
            if (event.isKeyUpEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
                if (!closing) {
                    escape();
                }
            } else if (event.isLMBDownEvent() && !closing) {
                pressLeft(event.getX(), event.getY());
            } else if (event.isRMBDownEvent() && !closing) {
                pressRight(event.getX(), event.getY());
            }
            event.consume();
        }
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (hostPanel == null) {
            return;
        }
        float frameAlpha = alphaMult * Fade.smooth(openLevel);
        backdrop.drawFrame(layout, frameAlpha);
        backdrop.drawShipArea(layout, frameAlpha);
        if (portrait == null) {
            return;
        }
        float shipX = shipCentreX();
        portrait.draw(shipX, layout.shipCentreY(), layout.shipZoom(), frameAlpha);
        SkillTreeFleetRenderer.InspectedShip ship = portrait.at(shipX, layout.shipCentreY(), layout.shipZoom());
        sockets.render(ship, shipExtent, alphaMult * Fade.smooth(contentLevel), screenLayout.socketBounds(), placingSocketable, targetedType);
    }
}
