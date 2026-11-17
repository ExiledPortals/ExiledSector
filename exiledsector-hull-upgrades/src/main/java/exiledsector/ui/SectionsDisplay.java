package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.render.Fade;
import exiledsector.items.hull.HullItem;
import exiledsector.items.hull.HullUpgradePoints;
import exiledsector.items.hull.HullUpgradeRules;
import exiledsector.items.schematic.RefitSchematicTooltip;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.schematic.RefitSchematic;
import exiledsector.items.hull.ShipSection;
import exiledsector.items.pattern.HullPatterns;
import exiledsector.skills.ShipSections;
import exiledsector.ui.decoration.HullPoint;
import exiledsector.ui.decoration.InspectedShip;
import exiledsector.ui.decoration.ShipAnchors;
import exiledsector.ui.section.SectionFlair;
import exiledsector.ui.section.SectionLayout;
import exiledsector.ui.socket.HullUpgradePanel;
import exiledsector.ui.socket.ItemHoverTooltip;
import exiledsector.ui.util.TextFont;
import exiledsector.ui.util.TextLabel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

final class SectionsDisplay {

    record SectionBounds(float minCentreX, float maxCentreX, float minCentreY, float maxCentreY) {

        float clampX(float centreX) {
            return Math.max(minCentreX, Math.min(maxCentreX, centreX));
        }

        float clampY(float centreY) {
            return Math.max(minCentreY, Math.min(maxCentreY, centreY));
        }
    }

    record PatternLook(Set<ShipSection> litSections, Function<ShipSection, StyledText> hint) {
    }

    static final int MAX_SECTIONS = HullUpgradePoints.MOST_POINTS;
    private static final float PATTERN_DULL = 0.3f;
    private static final float PATTERN_HOVER_LIFT = 0.35f;
    private static final float HOVER_SECONDS = 0.15f;
    private static final float LABEL_GAP = 8f;
    private static final TextFont LABEL_FONT = UiFonts.TITLE;
    private static final float LABEL_FONT_SIZE = LABEL_FONT.size();
    static final float VERTICAL_REACH = SectionLayout.SECTION_RADIUS + LABEL_GAP + LABEL_FONT_SIZE * 2f;
    private static final float OTHER_SECTION_DIM = 0.3f;

    private final CustomPanelAPI hostPanel;
    private final SectionEditor sectionEditor;
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
    private int shownPointsRevision = Integer.MIN_VALUE;
    private int shownPatternRevision = Integer.MIN_VALUE;
    private ItemStore upgradeStore;
    private int shownSchematicRevision = Integer.MIN_VALUE;
    private Map<RefitSchematic, Integer> shownSchematicCounts = Map.of();
    private int shownMasterSchematicCount;
    private List<ShipSections.UnlockedSection> unlockedSections = List.of();
    private List<SectionLayout.Placement> placements = List.of();
    private int renderedSections;
    private PatternLook patternLook;

    SectionsDisplay(CustomPanelAPI hostPanel, SectionEditor sectionEditor) {
        this.hostPanel = hostPanel;
        this.sectionEditor = sectionEditor;
        this.hoverTooltip = new ItemHoverTooltip(hostPanel);
        for (int i = 0; i < MAX_SECTIONS; i++) {
            typeLabels[i] = new TextLabel(LABEL_FONT, Color.WHITE);
            stateLabels[i] = new TextLabel(LABEL_FONT, Misc.getGrayColor());
        }
    }

    List<ShipSections.UnlockedSection> sections() {
        return unlockedSections;
    }

    boolean refreshIfChanged() {
        sectionEditor.syncOutsideChanges();
        int revision = sectionEditor.revision();
        int pointsRevision = HullUpgradePoints.revision();
        ItemStore store = upgradeStore();
        int schematicRevision = store == null ? 0 : store.shipSchematicRevision();
        int patternRevision = HullPatterns.revision();
        if (revision == shownRevision && pointsRevision == shownPointsRevision && schematicRevision == shownSchematicRevision
                && patternRevision == shownPatternRevision) {
            return false;
        }
        shownPatternRevision = patternRevision;
        shownRevision = revision;
        shownPointsRevision = pointsRevision;
        shownSchematicRevision = schematicRevision;
        shownSchematicCounts = store == null ? Map.of() : store.schematicCounts();
        shownMasterSchematicCount = store == null ? 0 : store.masterSchematicCount();
        unlockedSections = sectionEditor.sections();
        if (unlockedSections.size() > MAX_SECTIONS) {
            unlockedSections = unlockedSections.subList(0, MAX_SECTIONS);
        }
        placements = List.of();
        refreshLabels();
        hideTooltip();
        return true;
    }

    void showPattern(PatternLook look) {
        if (look != patternLook) {
            patternLook = look;
            hideTooltip();
        }
    }

    void absorbUpgradesFromCargo() {
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        ItemStore store = upgradeStore();
        if (playerFleet != null && store != null) {
            store.absorbFrom(playerFleet.getCargo());
            shownSchematicRevision = store.shipSchematicRevision();
            shownSchematicCounts = store.schematicCounts();
            shownMasterSchematicCount = store.masterSchematicCount();
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
                stateLabels[i].set(Translation.msg("framework.section.inactive")
                        .arg("requirement", RefitSchematicTooltip.requirementText(unlocked.unmetRequirement())).text());
                stateLabels[i].setColor(SectionFlair.inactiveColor());
            } else if (unlocked.item() == null) {
                stateLabels[i].set(Translation.text("framework.section.empty"));
                stateLabels[i].setColor(Misc.getGrayColor());
            } else if (unlocked.cloned()) {
                stateLabels[i].set(Translation.msg("pattern.section.clonedLabel").arg("item", unlocked.item().name()).text());
                stateLabels[i].setColor(unlocked.item().rarity().color());
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
        PatternLook shownLook = patternLook;
        hoverTooltip.show(hoveredSection, hoveredSection.item(), sectionEditor.hullSize(), expanded -> tooltipFooter(hoveredSection, shownLook),
                tooltipAnchor.getPosition());
    }

    private List<StyledText> tooltipFooter(ShipSections.UnlockedSection unlocked, PatternLook shownLook) {
        List<StyledText> footer = new ArrayList<>(AffinityItemLines.current(unlocked.item(), sectionEditor.hullSize(), sectionEditor::affinityTotals));
        if (!unlocked.active()) {
            footer.add(Translation.msg("framework.section.inactive")
                    .arg("requirement", RefitSchematicTooltip.requirementText(unlocked.unmetRequirement())).styled());
        }
        StyledText patternHint = shownLook == null ? null : shownLook.hint().apply(unlocked.section());
        if (unlocked.cloned()) {
            footer.add(Translation.msg("pattern.section.clonedHint").arg("ship", sectionEditor.patternShipName()).styled());
        } else if (patternHint != null) {
            footer.add(patternHint);
        } else if (shownLook == null) {
            footer.add(Translation.styled("framework.section.removeHint"));
        }
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
            public void applySchematic(RefitSchematic schematic) {
                sectionEditor.applySchematic(schematic);
            }

            @Override
            public void applyMasterSchematic() {
                sectionEditor.applyMasterSchematic();
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
        List<HullUpgradePanel.SchematicEntry> schematicEntries = new ArrayList<>();
        for (RefitSchematic schematic : RefitSchematic.values()) {
            int schematicsInStorage = shownSchematicCounts.getOrDefault(schematic, 0);
            schematicEntries.add(new HullUpgradePanel.SchematicEntry(schematic, schematicsInStorage,
                    sectionEditor.schematicBlock(schematic, schematicsInStorage)));
        }
        List<HullUpgradePanel.SectionEntry> sectionEntries = new ArrayList<>();
        for (ShipSection section : ShipSection.all()) {
            sectionEntries.add(sectionEntry(section));
        }
        SectorAPI sector = Global.getSector();
        HullUpgradePanel.ChartProgress chartProgress = HullUpgradePoints.schematicsInUse() ? null
                : new HullUpgradePanel.ChartProgress(HullUpgradePoints.chartsEnabled(), HullUpgradePoints.nextChartTier(sector));
        return new HullUpgradePanel.View(sectionEditor.hullUpgradePoints(), sectionEditor.unspentHullUpgradePoints(), HullUpgradePoints.maxPoints(),
                List.copyOf(schematicEntries), chartProgress, List.copyOf(sectionEntries), masterSchematicEntry());
    }

    private HullUpgradePanel.MasterSchematicEntry masterSchematicEntry() {
        HullUpgradeRules.MasterSchematicBlock block = sectionEditor.masterSchematicBlock(shownMasterSchematicCount);
        if (block == HullUpgradeRules.MasterSchematicBlock.NOT_REQUIRED || block == HullUpgradeRules.MasterSchematicBlock.ALREADY_UNLOCKED) {
            return null;
        }
        return new HullUpgradePanel.MasterSchematicEntry(shownMasterSchematicCount, block);
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
                    RefitSchematicTooltip.requirementText(unmetRequirement), null);
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
            placements = SectionLayout.place(sectionPoints);
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
                    alpha * 0.9f * sectionDim(i, placing, unlocked.section()));
        }
        renderedSections = unlockedSections.size();
        float radius = SectionLayout.SECTION_RADIUS;
        for (int i = 0; i < renderedSections; i++) {
            ShipSections.UnlockedSection unlocked = unlockedSections.get(i);
            boolean selected = isSelected(unlocked.section(), placing, targetedSection);
            SectionFlair.SectionLook look = new SectionFlair.SectionLook(hoverLevels[i], selected, unlocked.active(),
                    unlocked.item() == null ? null : unlocked.item().iconPath());
            float sectionAlpha = alpha * sectionDim(i, placing, unlocked.section());
            SectionFlair.render(sectionCentreX[i], sectionCentreY[i], radius, SectionFlair.colorOf(unlocked.section()), look,
                    elapsedSeconds + i * 0.53f, sectionAlpha);
            drawCentred(typeLabels[i], sectionCentreX[i], sectionCentreY[i] + radius + LABEL_GAP + LABEL_FONT_SIZE, sectionAlpha);
            drawCentred(stateLabels[i], sectionCentreX[i], sectionCentreY[i] - radius - LABEL_GAP, sectionAlpha);
        }
    }

    private boolean isSelected(ShipSection section, Item placing, ShipSection targetedSection) {
        if (patternLook != null) {
            return patternLook.litSections().contains(section);
        }
        return placing != null ? HullItem.fits(placing, section) : targetedSection == section;
    }

    private float sectionDim(int index, Item placing, ShipSection section) {
        if (patternLook == null) {
            return placingDim(placing, section);
        }
        if (patternLook.litSections().contains(section)) {
            return 1f;
        }
        return PATTERN_DULL + PATTERN_HOVER_LIFT * hoverLevels[index];
    }

    private static float placingDim(Item placing, ShipSection section) {
        return placing == null || HullItem.fits(placing, section) ? 1f : OTHER_SECTION_DIM;
    }

    private static void drawCentred(TextLabel label, float centreX, float top, float alpha) {
        label.setAlpha(alpha);
        label.draw(centreX - label.width() / 2f, top);
    }
}
