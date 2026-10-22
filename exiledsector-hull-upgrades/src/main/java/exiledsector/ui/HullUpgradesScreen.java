package exiledsector.ui;

import exiledsector.items.HullItem;
import exiledsector.items.ShipSection;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import exiledlib.i18n.Translation;
import exiledsector.skills.ShipSections;
import exiledsector.items.SocketCustody;
import exiledlib.items.ItemKind;
import exiledlib.items.Item;
import exiledsector.ui.decoration.InspectedShip;
import exiledsector.ui.decoration.RefitShipPortrait;
import exiledsector.ui.decoration.ShipAnchors;
import exiledsector.ui.section.SectionLayout;
import exiledsector.ui.refit.RefitScreenLayout;
import exiledsector.ui.socket.HullUpgradePanel;
import exiledsector.ui.socket.SocketPanelPlacement;
import exiledsector.ui.socket.SocketPanelTab;
import exiledsector.ui.socket.ItemStoragePanel;
import exiledsector.ui.socket.ModificationBay;
import exiledlib.ui.util.GLDraw;
import exiledlib.ui.util.Fade;
import lunalib.lunaRefit.BaseRefitButton;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

final class HullUpgradesScreen extends BaseCustomUIPanelPlugin {

    static final float OPEN_SECONDS = 0.25f;
    static final float CONTENT_SECONDS = 0.3f;
    static final Set<SocketPanelTab> SHOWN_TABS = EnumSet.of(SocketPanelTab.HULL_ITEMS, SocketPanelTab.HULL_UPGRADES);
    private static final float BAY_BACKDROP_ALPHA = 0.8f;

    private final FleetMemberAPI member;
    private final ShipVariantAPI variant;
    private final BaseRefitButton refitButton;
    private final RefitScreenLayout layout;
    private final RefitBackdrop backdrop = new RefitBackdrop();
    private final RefitShipPortrait portrait;
    private final ShipAnchors sectionAnchors;
    private final SectionLayout.ShipExtent shipExtent;
    private final HullUpgradesLayout screenLayout;
    private final Consumer<SocketPanelTab> tabListener = this::switchTab;
    private CustomPanelAPI hostPanel;
    private SectionEditor sectionEditor;
    private SectionsDisplay sectionsDisplay;
    private ItemStoragePanel storagePanel;
    private HullUpgradePanel upgradesPanel;
    private ModificationBay bay;
    private float bayDim;
    private Map<String, FleetMemberAPI> ownedShips = Map.of();
    private Item placingItem;
    private ShipSection targetedSection;
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
        this.sectionAnchors = portrait == null ? null : ShipAnchors.of(portrait);
        this.shipExtent = portrait == null ? SectionLayout.ShipExtent.square(0f)
                : new SectionLayout.ShipExtent(portrait.halfWidthPixels(layout.shipZoom()), portrait.halfLengthPixels(layout.shipZoom()));
        this.screenLayout = HullUpgradesLayout.of(layout, shipExtent);
    }

    void attach(CustomPanelAPI panel) {
        hostPanel = panel;
        ownedShips = SocketCustody.reconcile();
        sectionEditor = new SectionEditor(member, variant, refitButton);
        sectionsDisplay = new SectionsDisplay(panel, sectionEditor, SectionLayout.Arrangement.LEFT_ABOVE_BELOW);
        sectionsDisplay.refreshIfChanged();
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
            sectionsDisplay.absorbUpgradesFromCargo();
            openUpgrades(false);
        }
        if (sectionsDisplay.refreshIfChanged() && upgradesPanel != null) {
            upgradesPanel.update(sectionsDisplay.panelView());
        }
        if (storagePanel != null && sectionEditor.revision() != storageRevision) {
            refreshStorage();
        }
        bayDim = Fade.step(bayDim, bay != null, amount / OPEN_SECONDS);
        sectionsDisplay.advance(amount, mouseX, mouseY, contentLevel >= 0.5f && !pointerOverPanel && bay == null);
        if (closing && openLevel <= 0f && !closeRequested) {
            closeRequested = true;
            sectionsDisplay.hideTooltip();
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
        upgradesPanel = HullUpgradePanel.open(hostPanel, placement(instantly), sectionsDisplay.panelView(), sectionsDisplay.panelListener(() -> {
            upgradesPanel = null;
            close();
        }), tabListener, SHOWN_TABS, HullUpgradePanel.Sizing.SCROLL);
        if (!instantly) {
            SkillTreeSounds.panelOpened();
        }
    }

    private void openStorage(ItemKind kindFilter) {
        storagePanel = ItemStoragePanel.openSectionItems(hostPanel, placement(true), SocketCustody.shipNames(ownedShips), new ItemStoragePanel.Listener() {
            @Override
            public void selected(Item item) {
                chooseFromStorage(item);
            }

            @Override
            public void closed() {
                forgetStorage();
                close();
            }

            @Override
            public void pressedInside() {
                sectionsDisplay.hideTooltip();
            }
        }, kindFilter, tabListener, SHOWN_TABS);
        storagePanel.setModifyHandler(this::openBay);
        storageRevision = sectionEditor.revision();
    }

    private void openBay() {
        if (storagePanel == null || bay != null || closing) {
            return;
        }
        swapOutStorage();
        SkillTreeSounds.panelOpened();
        bay = ModificationBay.openHullItemsOnly(hostPanel, ModificationBenchScreen.benchArea(Global.getSettings().getScreenWidth(),
                Global.getSettings().getScreenHeight(), false), SocketCustody.shipNames(ownedShips), new ModificationBay.Listener() {
            @Override
            public void closed() {
                bay = null;
                if (!closing) {
                    showStorage(null);
                }
            }

            @Override
            public void pressedInside() {
                sectionsDisplay.hideTooltip();
            }
        });
    }

    private void switchTab(SocketPanelTab tab) {
        if (closing || bay != null) {
            return;
        }
        if (tab == SocketPanelTab.HULL_ITEMS && storagePanel == null) {
            showStorage(null);
        } else if (tab == SocketPanelTab.HULL_UPGRADES && upgradesPanel == null) {
            swapOutStorage();
            openUpgrades(true);
        }
    }

    private void swapOutStorage() {
        if (storagePanel != null) {
            ItemStoragePanel swapped = storagePanel;
            forgetStorage();
            swapped.swapOut();
        }
    }

    private void swapOutUpgrades() {
        if (upgradesPanel != null) {
            HullUpgradePanel swapped = upgradesPanel;
            upgradesPanel = null;
            swapped.swapOut();
        }
    }

    private void showStorage(ItemKind kindFilter) {
        swapOutUpgrades();
        if (storagePanel == null) {
            openStorage(kindFilter);
        } else {
            storagePanel.restrictTo(kindFilter);
        }
    }

    private void forgetStorage() {
        storagePanel = null;
        placingItem = null;
        targetedSection = null;
    }

    private float shipCentreX() {
        return layout.shipCentreX() + (screenLayout.settledShipX() - layout.shipCentreX()) * Fade.smooth(openLevel);
    }

    private void refreshStorage() {
        storageRevision = sectionEditor.revision();
        storagePanel.refresh(SocketCustody.shipNames(ownedShips));
        if (placingItem != null) {
            storagePanel.setSelected(placingItem, placingHint(placingItem));
        }
    }

    private void chooseFromStorage(Item item) {
        if (item == null) {
            stopPlacing();
            return;
        }
        if (targetedSection != null) {
            if (!sectionEditor.installInSection(targetedSection, item)) {
                SkillTreeSounds.refused();
            }
            return;
        }
        placingItem = item;
        if (storagePanel != null) {
            storagePanel.setSelected(item, placingHint(item));
        }
    }

    private String placingHint(Item item) {
        ItemKind itemType = item.kind();
        String key = sectionEditor.isSectionUnlocked(ShipSection.of(itemType)) ? "ui.socketStorage.placingHintFramework"
                : "ui.socketStorage.placingHintFrameworkLocked";
        return Translation.msg(key).arg("name", item.name()).arg("type", itemType.displayName()).text();
    }

    private void stopPlacing() {
        placingItem = null;
        if (storagePanel != null) {
            storagePanel.setSelected(null);
        }
    }

    private void clearTarget() {
        targetedSection = null;
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
        sectionsDisplay.hideTooltip();
        if (bay != null) {
            bay.close();
        }
        if (storagePanel != null) {
            storagePanel.close();
        }
        if (upgradesPanel != null) {
            upgradesPanel.close();
        }
    }

    private void pressLeft(float x, float y) {
        if (bay != null) {
            if (!bay.contains(x, y)) {
                bay.close();
            }
            return;
        }
        if (!insideFrame(x, y)) {
            close();
            return;
        }
        int sectionIndex = sectionIndexAt(x, y);
        if (sectionIndex < 0) {
            stopPlacing();
            clearTarget();
            return;
        }
        ShipSection section = sectionsDisplay.sections().get(sectionIndex).section();
        if (placingItem != null) {
            if (HullItem.fits(placingItem, section) && sectionEditor.installInSection(section, placingItem)) {
                stopPlacing();
            } else {
                SkillTreeSounds.refused();
            }
            return;
        }
        targetedSection = section;
        showStorage(section.itemType());
        storagePanel.setTargetingSocket(true);
        SkillTreeSounds.panelOpened();
    }

    private int sectionIndexAt(float x, float y) {
        boolean sectionsLive = contentLevel >= 0.5f && bay == null && (storagePanel == null || !storagePanel.isWorkbenchOpen());
        return sectionsLive ? sectionsDisplay.sectionAt(x, y) : -1;
    }

    private void pressRight(float x, float y) {
        int sectionIndex = sectionIndexAt(x, y);
        if (sectionIndex < 0) {
            return;
        }
        ShipSections.UnlockedSection unlocked = sectionsDisplay.sections().get(sectionIndex);
        if (unlocked.item() != null) {
            sectionEditor.removeFromSection(unlocked.section());
        }
    }

    private void escape() {
        if (bay != null) {
            bay.escape();
            return;
        }
        if (storagePanel != null && storagePanel.escape()) {
            return;
        }
        if (upgradesPanel != null && upgradesPanel.escape()) {
            return;
        }
        if (placingItem != null) {
            stopPlacing();
        } else if (targetedSection != null) {
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
        return (bay != null && bay.contains(x, y))
                || (storagePanel != null && (storagePanel.contains(x, y) || storagePanel.workbenchContains(x, y)))
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
        InspectedShip ship = portrait.at(shipX, layout.shipCentreY(), layout.shipZoom());
        sectionsDisplay.render(ship, sectionAnchors, shipExtent, alphaMult * Fade.smooth(contentLevel), screenLayout.sectionBounds(), placingItem, targetedSection);
        if (bayDim > 0f) {
            GLDraw.fillQuad(0f, 0f, Global.getSettings().getScreenWidth(), Global.getSettings().getScreenHeight(), Color.BLACK,
                    BAY_BACKDROP_ALPHA * Fade.smooth(bayDim) * alphaMult);
        }
    }
}
