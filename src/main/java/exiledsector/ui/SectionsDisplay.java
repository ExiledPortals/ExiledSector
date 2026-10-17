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
import exiledsector.skills.ShipSections;
import exiledsector.hullitems.HullUpgradeRules;
import exiledsector.hullitems.HullUpgradeTooltip;
import exiledsector.hullitems.ShipSection;
import exiledsector.hullitems.HullItem;
import exiledsector.hullitems.HullItemStore;
import exiledsector.ui.decoration.ShipAnchors;
import exiledsector.ui.decoration.SkillTreeFleetRenderer;
import exiledsector.ui.section.SectionFlair;
import exiledsector.ui.section.SectionLayout;
import exiledsector.ui.node.TreeAllocationSession;
import exiledsector.ui.socket.HullUpgradePanel;
import exiledsector.ui.socket.HullItemHoverTooltip;
import exiledsector.ui.util.Fade;
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
    private final TreeAllocationSession treeSession;
    private final SectionLayout.Arrangement arrangement;
    private final HullItemHoverTooltip hoverTooltip;
    private final TextLabel[] typeLabels = new TextLabel[MAX_SECTIONS];
    private final TextLabel[] stateLabels = new TextLabel[MAX_SECTIONS];
    private final float[] hoverLevels = new float[MAX_SECTIONS];
    private final float[] sectionCentreX = new float[MAX_SECTIONS];
    private final float[] sectionCentreY = new float[MAX_SECTIONS];
    private CustomPanelAPI tooltipAnchor;
    private int tooltipSection = -1;
    private float elapsedSeconds;
    private int shownRevision = Integer.MIN_VALUE;
    private HullItemStore upgradeStore;
    private int shownUpgradeRevision = Integer.MIN_VALUE;
    private Map<HullSize, Integer> shownUpgradeCounts = Map.of();
    private List<ShipSections.UnlockedSection> unlockedSections = List.of();
    private List<SectionLayout.Placement> placements = List.of();
    private int renderedSections;

    SectionsDisplay(CustomPanelAPI hostPanel, TreeAllocationSession treeSession, SectionLayout.Arrangement arrangement) {
        this.hostPanel = hostPanel;
        this.treeSession = treeSession;
        this.arrangement = arrangement;
        this.hoverTooltip = new HullItemHoverTooltip(hostPanel);
        for (int i = 0; i < MAX_SECTIONS; i++) {
            typeLabels[i] = new TextLabel(LABEL_FONT_SIZE, Color.WHITE);
            stateLabels[i] = new TextLabel(LABEL_FONT_SIZE, Misc.getGrayColor());
        }
    }

    List<ShipSections.UnlockedSection> sections() {
        return unlockedSections;
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
        unlockedSections = treeSession.sections();
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
        HullItemStore store = upgradeStore();
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

    private HullItemStore upgradeStore() {
        if (upgradeStore == null && Global.getSector() != null) {
            upgradeStore = HullItemStore.get();
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
        hoverTooltip.show(hoveredSection, hoveredSection.item(), treeSession.hullSize(), () -> tooltipFooter(hoveredSection), tooltipAnchor.getPosition());
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
                treeSession.installUpgrade();
            }

            @Override
            public void toggleSection(ShipSection section) {
                if (treeSession.isSectionUnlocked(section)) {
                    treeSession.lockSection(section);
                } else {
                    treeSession.unlockSection(section);
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
        HullUpgradeRules.UpgradeBlock upgradeBlock = null;
        if (treeSession.hullUpgradePoints() >= HullUpgradeRules.MAX_POINTS) {
            upgradeBlock = HullUpgradeRules.UpgradeBlock.AT_MAX_POINTS;
        } else if (upgradesInStorage <= 0) {
            upgradeBlock = HullUpgradeRules.UpgradeBlock.NONE_IN_STORAGE;
        }
        List<HullUpgradePanel.SectionEntry> sectionEntries = new ArrayList<>();
        for (ShipSection section : ShipSection.all()) {
            sectionEntries.add(sectionEntry(section));
        }
        return new HullUpgradePanel.View(hullSize, treeSession.hullUpgradePoints(), treeSession.unspentHullUpgradePoints(), upgradesInStorage,
                upgradeBlock, List.copyOf(sectionEntries));
    }

    private HullUpgradePanel.SectionEntry sectionEntry(ShipSection section) {
        if (treeSession.isSectionUnlocked(section)) {
            String itemIcon = null;
            for (ShipSections.UnlockedSection unlocked : unlockedSections) {
                if (unlocked.section() == section && unlocked.item() != null) {
                    itemIcon = unlocked.item().iconPath();
                }
            }
            return new HullUpgradePanel.SectionEntry(section, HullUpgradePanel.SectionState.UNLOCKED, null, itemIcon);
        }
        String unmetRequirement = treeSession.unmetRequirement(section);
        if (unmetRequirement != null) {
            return new HullUpgradePanel.SectionEntry(section, HullUpgradePanel.SectionState.RESTRICTED,
                    HullUpgradeTooltip.requirementText(unmetRequirement), null);
        }
        HullUpgradePanel.SectionState state = treeSession.unspentHullUpgradePoints() > 0 ? HullUpgradePanel.SectionState.AVAILABLE
                : HullUpgradePanel.SectionState.NO_POINTS;
        return new HullUpgradePanel.SectionEntry(section, state, null, null);
    }

    void render(SkillTreeFleetRenderer.InspectedShip ship, SectionLayout.ShipExtent extent, float alpha, SectionBounds bounds,
                HullItem placing, ShipSection targetedSection) {
        renderedSections = 0;
        if (ship == null || unlockedSections.isEmpty() || alpha <= 0f) {
            return;
        }
        if (placements.size() != unlockedSections.size()) {
            List<ShipAnchors.Anchor> anchors = new ArrayList<>(unlockedSections.size());
            unlockedSections.forEach(unlocked -> anchors.add(ship.anchors().forSection(unlocked.section())));
            placements = SectionLayout.place(anchors, arrangement);
        }
        for (int i = 0; i < unlockedSections.size(); i++) {
            ShipSections.UnlockedSection unlocked = unlockedSections.get(i);
            SectionLayout.Placement placement = placements.get(i);
            float sectionX = bounds.clampX(SectionLayout.sectionX(placement, ship.screenX(), extent));
            float sectionY = bounds.clampY(SectionLayout.sectionY(placement, ship.screenY(), extent));
            sectionCentreX[i] = sectionX;
            sectionCentreY[i] = sectionY;
            ShipAnchors.Anchor anchor = ship.anchors().forSection(unlocked.section());
            Color accent = unlocked.active() ? SectionFlair.colorOf(unlocked.section()) : SectionFlair.inactiveColor();
            SectionFlair.LeaderPath path = new SectionFlair.LeaderPath(SectionLayout.edgeX(placement, sectionX),
                    SectionLayout.edgeY(placement, sectionY), SectionLayout.elbowX(placement, sectionX),
                    SectionLayout.elbowY(placement, sectionY), ship.anchorScreenX(anchor), ship.anchorScreenY(anchor));
            SectionFlair.leader(path, accent, elapsedSeconds + i * 0.37f,
                    alpha * 0.9f * placingDim(placing, unlocked.section()));
        }
        renderedSections = unlockedSections.size();
        float radius = SectionLayout.SECTION_RADIUS;
        for (int i = 0; i < renderedSections; i++) {
            ShipSections.UnlockedSection unlocked = unlockedSections.get(i);
            boolean selected = placing != null ? placing.canSocketInto(unlocked.section()) : targetedSection == unlocked.section();
            SectionFlair.SectionLook look = new SectionFlair.SectionLook(hoverLevels[i], selected, unlocked.active(),
                    unlocked.item() == null ? null : unlocked.item().iconPath());
            float sectionAlpha = alpha * placingDim(placing, unlocked.section());
            SectionFlair.render(sectionCentreX[i], sectionCentreY[i], radius, SectionFlair.colorOf(unlocked.section()), look,
                    elapsedSeconds + i * 0.53f, sectionAlpha);
            drawCentred(typeLabels[i], sectionCentreX[i], sectionCentreY[i] + radius + LABEL_GAP + LABEL_FONT_SIZE, sectionAlpha);
            drawCentred(stateLabels[i], sectionCentreX[i], sectionCentreY[i] - radius - LABEL_GAP, sectionAlpha);
        }
    }

    private static float placingDim(HullItem placing, ShipSection section) {
        return placing == null || placing.canSocketInto(section) ? 1f : OTHER_SECTION_DIM;
    }

    private static void drawCentred(TextLabel label, float centreX, float top, float alpha) {
        label.setAlpha(alpha);
        label.draw(centreX - label.width() / 2f, top);
    }
}
