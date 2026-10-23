package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.PlayerFleet;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.skills.skilleffect.render.Fade;
import exiledsector.items.HullItem;
import exiledsector.items.HullUpgradeRules;
import exiledsector.items.HullUpgradeTooltip;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.ShipSection;
import exiledsector.skills.ShipSections;
import exiledsector.ui.decoration.HullPoint;
import exiledsector.ui.decoration.InspectedShip;
import exiledsector.ui.decoration.ShipAnchors;
import exiledsector.ui.section.SectionFlair;
import exiledsector.ui.section.SectionLayout;
import exiledsector.ui.socket.HullUpgradePanel;
import exiledsector.ui.socket.ItemHoverTooltip;
import exiledsector.ui.util.TextLabel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class SectionsDisplay {

    record SectionBounds(float minCentreX, float maxCentreX, float minCentreY, float maxCentreY) {
        static final SectionBounds UNBOUNDED = new SectionBounds(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);

        float clampX(float centreX) {
            return Math.max(minCentreX, Math.min(maxCentreX, centreX));
        }

        float clampY(float centreY) {
            return Math.max(minCentreY, Math.min(maxCentreY, centreY));
        }
    }

    static final int MAX_SECTIONS = HullUpgradeRules.MAX_POINTS;
    private static final float HOVER_SECONDS = 0.15f;
    private static final float LABEL_GAP = 8f;
    private static final float LABEL_FONT_SIZE = SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE;
    static final float VERTICAL_REACH = SectionLayout.SECTION_RADIUS + LABEL_GAP + LABEL_FONT_SIZE * 2f;
    private static final float OTHER_SECTION_DIM = 0.3f;

    private final CustomPanelAPI hostPanel;
    private final SectionEditor sectionEditor;
    private final SectionLayout.Arrangement arrangement;
    private final ItemHoverTooltip hoverTooltip;
    private final TextLabel[] typeLabels = new TextLabel[MAX_SECTIONS];
    private final TextLabel[] stateLabels = new TextLabel[MAX_SECTIONS];
    private final float[] hoverLevels = new float[MAX_SECTIONS];
    private final float[] sectionCentreX = new float[MAX_SECTIONS];
    private final float[] sectionCentreY = new float[MAX_SECTIONS];
    private CustomPanelAPI tooltipAnchor;
    private int tooltipSection = -1;
    private float elapsedSeconds;
    private int shownRevision = Integer.MIN_VALUE;
    private ItemStore upgradeStore;
    private int shownUpgradeRevision = Integer.MIN_VALUE;
    private Map<HullSize, Integer> shownUpgradeCounts = Map.of();
    private List<ShipSections.UnlockedSection> unlockedSections = List.of();
    private List<SectionLayout.Placement> placements = List.of();
    private int renderedSections;

    SectionsDisplay(CustomPanelAPI hostPanel, SectionEditor sectionEditor, SectionLayout.Arrangement arrangement) {
        this.hostPanel = hostPanel;
        this.sectionEditor = sectionEditor;
        this.arrangement = arrangement;
        this.hoverTooltip = new ItemHoverTooltip(hostPanel);
        for (int i = 0; i < MAX_SECTIONS; i++) {
            typeLabels[i] = new TextLabel(LABEL_FONT_SIZE, Color.WHITE);
            stateLabels[i] = new TextLabel(LABEL_FONT_SIZE, Misc.getGrayColor());
        }
    }

    List<ShipSections.UnlockedSection> sections() {
        return unlockedSections;
    }

    boolean refreshIfChanged() {
        int revision = sectionEditor.revision();
        ItemStore store = upgradeStore();
        int upgradeRevision = store == null ? 0 : store.upgradeRevision();
        if (revision == shownRevision && upgradeRevision == shownUpgradeRevision) {
            return false;
        }
        shownRevision = revision;
        shownUpgradeRevision = upgradeRevision;
        shownUpgradeCounts = store == null ? Map.of() : store.upgradeCounts();
        unlockedSections = sectionEditor.sections();
        if (unlockedSections.size() > MAX_SECTIONS) {
            unlockedSections = unlockedSections.subList(0, MAX_SECTIONS);
        }
        placements = List.of();
        refreshLabels();
        hideTooltip();
        return true;
    }

    void absorbUpgradesFromCargo() {
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        ItemStore store = upgradeStore();
        if (playerFleet != null && store != null) {
            store.absorbFrom(playerFleet.getCargo());
            shownUpgradeRevision = store.upgradeRevision();
            shownUpgradeCounts = store.upgradeCounts();
        }
    }

    void advance(float amount, float mouseX, float mouseY, boolean sectionsShown) {
        elapsedSeconds += amount;
        float hoverStep = amount / HOVER_SECONDS;
        int hovered = sectionsShown ? sectionAt(mouseX, mouseY) : -1;
        for (int i = 0; i < MAX_SECTIONS; i++) {
            hoverLevels[i] = Fade.step(hoverLevels[i], i == hovered, hoverStep);
        }
        updateTooltip(hovered);
    }

    int sectionAt(float x, float y) {
        float radius = SectionLayout.SECTION_RADIUS;
        for (int i = 0; i < renderedSections; i++) {
            if (Math.hypot(x - sectionCentreX[i], y - sectionCentreY[i]) <= radius) {
                return i;
            }
        }
        return -1;
    }

    void forgetRenderedSections() {
        renderedSections = 0;
    }

    private ItemStore upgradeStore() {
        if (upgradeStore == null && Global.getSector() != null) {
            upgradeStore = ItemStore.get();
        }
        return upgradeStore;
    }

    private void refreshLabels() {
        for (int i = 0; i < unlockedSections.size(); i++) {
            ShipSections.UnlockedSection unlocked = unlockedSections.get(i);
            typeLabels[i].set(unlocked.section().displayName());
            typeLabels[i].setColor(SectionFlair.colorOf(unlocked.section()));
            if (!unlocked.active()) {
                stateLabels[i].set(Translation.msg("framework.socket.inactive")
                        .arg("requirement", HullUpgradeTooltip.requirementText(unlocked.unmetRequirement())).text());
                stateLabels[i].setColor(SectionFlair.inactiveColor());
            } else if (unlocked.item() == null) {
                stateLabels[i].set(Translation.text("framework.socket.empty"));
                stateLabels[i].setColor(Misc.getGrayColor());
            } else {
                stateLabels[i].set(unlocked.item().name());
                stateLabels[i].setColor(unlocked.item().rarity().color());
            }
        }
    }

    private void updateTooltip(int hovered) {
        ShipSections.UnlockedSection hoveredSection = hovered < 0 || hovered >= unlockedSections.size() ? null : unlockedSections.get(hovered);
        if (hoveredSection == null || hoveredSection.item() == null) {
            hideTooltip();
            return;
        }
        if (hovered == tooltipSection) {
            hoverTooltip.refreshIfExpansionChanged();
            return;
        }
        hideTooltip();
        float radius = SectionLayout.SECTION_RADIUS;
        PositionAPI hostPosition = hostPanel.getPosition();
        tooltipAnchor = Global.getSettings().createCustom(radius * 2f, radius * 2f, null);
        hostPanel.addComponent(tooltipAnchor).inTL(sectionCentreX[hovered] - radius - hostPosition.getX(),
                hostPosition.getY() + hostPosition.getHeight() - sectionCentreY[hovered] - radius);
        tooltipSection = hovered;
        hoverTooltip.show(hoveredSection, hoveredSection.item(), sectionEditor.hullSize(), expanded -> tooltipFooter(hoveredSection), tooltipAnchor.getPosition());
    }

    private static List<StyledText> tooltipFooter(ShipSections.UnlockedSection unlocked) {
        List<StyledText> footer = new ArrayList<>();
        if (!unlocked.active()) {
            footer.add(Translation.msg("framework.socket.inactive")
                    .arg("requirement", HullUpgradeTooltip.requirementText(unlocked.unmetRequirement())).styled());
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
        tooltipSection = -1;
    }

    HullUpgradePanel.Listener panelListener(Runnable closed) {
        return new HullUpgradePanel.Listener() {
            @Override
            public void installUpgrade() {
                sectionEditor.installUpgrade();
            }

            @Override
            public void toggleSection(ShipSection section) {
                if (sectionEditor.isSectionUnlocked(section)) {
                    sectionEditor.lockSection(section);
                } else {
                    sectionEditor.unlockSection(section);
                }
            }

            @Override
            public void closed() {
                closed.run();
            }
        };
    }

    HullUpgradePanel.View panelView() {
        HullSize hullSize = sectionEditor.hullSize();
        int upgradesInStorage = shownUpgradeCounts.getOrDefault(hullSize, 0);
        HullUpgradeRules.UpgradeBlock upgradeBlock = null;
        if (sectionEditor.hullUpgradePoints() >= HullUpgradeRules.MAX_POINTS) {
            upgradeBlock = HullUpgradeRules.UpgradeBlock.AT_MAX_POINTS;
        } else if (upgradesInStorage <= 0) {
            upgradeBlock = HullUpgradeRules.UpgradeBlock.NONE_IN_STORAGE;
        }
        List<HullUpgradePanel.SectionEntry> sectionEntries = new ArrayList<>();
        for (ShipSection section : ShipSection.all()) {
            sectionEntries.add(sectionEntry(section));
        }
        return new HullUpgradePanel.View(hullSize, sectionEditor.hullUpgradePoints(), sectionEditor.unspentHullUpgradePoints(), upgradesInStorage,
                upgradeBlock, List.copyOf(sectionEntries));
    }

    private HullUpgradePanel.SectionEntry sectionEntry(ShipSection section) {
        if (sectionEditor.isSectionUnlocked(section)) {
            String itemIcon = null;
            for (ShipSections.UnlockedSection unlocked : unlockedSections) {
                if (unlocked.section() == section && unlocked.item() != null) {
                    itemIcon = unlocked.item().iconPath();
                }
            }
            return new HullUpgradePanel.SectionEntry(section, HullUpgradePanel.SectionState.UNLOCKED, null, itemIcon);
        }
        String unmetRequirement = sectionEditor.unmetRequirement(section);
        if (unmetRequirement != null) {
            return new HullUpgradePanel.SectionEntry(section, HullUpgradePanel.SectionState.RESTRICTED,
                    HullUpgradeTooltip.requirementText(unmetRequirement), null);
        }
        HullUpgradePanel.SectionState state = sectionEditor.unspentHullUpgradePoints() > 0 ? HullUpgradePanel.SectionState.AVAILABLE
                : HullUpgradePanel.SectionState.NO_POINTS;
        return new HullUpgradePanel.SectionEntry(section, state, null, null);
    }

    void render(InspectedShip ship, ShipAnchors sectionAnchors, SectionLayout.ShipExtent extent, float alpha, SectionBounds bounds,
                Item placing, ShipSection targetedSection) {
        renderedSections = 0;
        if (ship == null || sectionAnchors == null || unlockedSections.isEmpty() || alpha <= 0f) {
            return;
        }
        if (placements.size() != unlockedSections.size()) {
            List<HullPoint> sectionPoints = new ArrayList<>(unlockedSections.size());
            unlockedSections.forEach(unlocked -> sectionPoints.add(sectionAnchors.forSection(unlocked.section())));
            placements = SectionLayout.place(sectionPoints, arrangement);
        }
        for (int i = 0; i < unlockedSections.size(); i++) {
            ShipSections.UnlockedSection unlocked = unlockedSections.get(i);
            SectionLayout.Placement placement = placements.get(i);
            float sectionX = bounds.clampX(SectionLayout.sectionX(placement, ship.screenX(), extent));
            float sectionY = bounds.clampY(SectionLayout.sectionY(placement, ship.screenY(), extent));
            sectionCentreX[i] = sectionX;
            sectionCentreY[i] = sectionY;
            HullPoint anchor = sectionAnchors.forSection(unlocked.section());
            Color accent = unlocked.active() ? SectionFlair.colorOf(unlocked.section()) : SectionFlair.inactiveColor();
            SectionFlair.LeaderPath path = new SectionFlair.LeaderPath(SectionLayout.edgeX(placement, sectionX),
                    SectionLayout.edgeY(placement, sectionY), SectionLayout.elbowX(placement, sectionX),
                    SectionLayout.elbowY(placement, sectionY), ship.pointScreenX(anchor), ship.pointScreenY(anchor));
            SectionFlair.leader(path, accent, elapsedSeconds + i * 0.37f,
                    alpha * 0.9f * placingDim(placing, unlocked.section()));
        }
        renderedSections = unlockedSections.size();
        float radius = SectionLayout.SECTION_RADIUS;
        for (int i = 0; i < renderedSections; i++) {
            ShipSections.UnlockedSection unlocked = unlockedSections.get(i);
            boolean selected = placing != null ? HullItem.fits(placing, unlocked.section()) : targetedSection == unlocked.section();
            SectionFlair.SectionLook look = new SectionFlair.SectionLook(hoverLevels[i], selected, unlocked.active(),
                    unlocked.item() == null ? null : unlocked.item().iconPath());
            float sectionAlpha = alpha * placingDim(placing, unlocked.section());
            SectionFlair.render(sectionCentreX[i], sectionCentreY[i], radius, SectionFlair.colorOf(unlocked.section()), look,
                    elapsedSeconds + i * 0.53f, sectionAlpha);
            drawCentred(typeLabels[i], sectionCentreX[i], sectionCentreY[i] + radius + LABEL_GAP + LABEL_FONT_SIZE, sectionAlpha);
            drawCentred(stateLabels[i], sectionCentreX[i], sectionCentreY[i] - radius - LABEL_GAP, sectionAlpha);
        }
    }

    private static float placingDim(Item placing, ShipSection section) {
        return placing == null || HullItem.fits(placing, section) ? 1f : OTHER_SECTION_DIM;
    }

    private static void drawCentred(TextLabel label, float centreX, float top, float alpha) {
        label.setAlpha(alpha);
        label.draw(centreX - label.width() / 2f, top);
    }
}
