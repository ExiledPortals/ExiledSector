package exiledsector.ui;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.Translation;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.AllocatedSkillEffects;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.progression.ShipOpBudget;
import exiledsector.skills.skilleffect.ShieldSkillEffect;
import exiledsector.ui.util.FallbackSupport;
import exiledsector.ui.util.HoloFrame;
import exiledsector.ui.util.Rects;
import exiledsector.ui.util.TextLabel;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static exiledsector.ui.SkillTreePanelStyle.FONT_LINE_HEIGHT_FACTOR;
import static exiledsector.ui.SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE;
import static exiledsector.ui.SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE;

final class SkillTreeStatPanel {

    private static final float STAT_PANEL_FONT_SIZE = TOOLTIP_BODY_FONT_SIZE;
    private static final float STAT_PANEL_PADDING = 26f;
    private static final float STAT_PANEL_MARGIN = 16f;
    private static final float STAT_PANEL_GROUP_GAP = 8f;
    private static final float STAT_PANEL_COLUMN_GAP = 20f;
    private static final float STAT_PANEL_HEADER_GAP = 10f;
    private static final float STAT_PANEL_HEADER_FONT_SIZE = TOOLTIP_TITLE_FONT_SIZE;

    private static final Color STAT_PANEL_LABEL_COLOR = new Color(0xCB, 0xF5, 0xFF);
    private static final Color STAT_PANEL_VALUE_COLOR = new Color(0xFF, 0xD2, 0x00);
    private static final Color STAT_PANEL_HEADER_TEXT_COLOR = new Color(0xCB, 0xF5, 0xFF);
    private static final Color STAT_DECREASED_COLOR = SkillTreePanelStyle.NEGATIVE_STAT_COLOR;
    private static final Color STAT_INCREASED_COLOR = SkillTreePanelStyle.POSITIVE_STAT_COLOR;
    private static final float STAT_COMPARISON_EPSILON = 0.001f;

    private static final float TOGGLE_HEIGHT = 40f;
    private static final float TOGGLE_MIN_WIDTH = 150f;
    private static final float TOGGLE_GAP = 8f;

    private final FleetMemberAPI member;
    private final HoloFrame panelFrame = new HoloFrame(SkillTreeStatPanel.class, HoloFrame.Look.PANEL);
    private final SkillTreeUiButton toggleButton = new SkillTreeUiButton("");
    private final String showLabel = Translation.text("ui.stats.show");
    private final String hideLabel = Translation.text("ui.stats.hide");
    private final Map<String, GroupLabels> groupLabels = new HashMap<>();

    private List<StatGroup> statGroups;
    private int statGroupsRevision;
    private PanelLayout cachedLayout;
    private List<StatGroup> cachedLayoutGroups;
    private float cachedLayoutX;
    private float cachedLayoutY;
    private float cachedLayoutWidth;
    private float cachedLayoutHeight;

    private boolean panelOpen = true;
    private float toggleWidth;
    private PanelLayout drawnLayout;
    private float placedToggleRight = Float.NaN;
    private float placedToggleTop = Float.NaN;

    SkillTreeStatPanel(FleetMemberAPI member) {
        this.member = member;
        panelFrame.openInstantly();
        toggleButton.setLabel(showLabel);
        float showWidth = toggleButton.preferredWidth();
        toggleButton.setLabel(hideLabel);
        toggleWidth = Math.max(TOGGLE_MIN_WIDTH, Math.max(showWidth, toggleButton.preferredWidth()));
    }

    void refresh(ShipOpBudget budget, int revision) {
        if (statGroups != null && revision == statGroupsRevision) return;
        statGroups = buildStatGroups(budget);
        statGroupsRevision = revision;
    }

    boolean isOpen() {
        return panelOpen;
    }

    void toggle() {
        if (panelOpen) {
            close();
        } else {
            open(true);
        }
    }

    void open(boolean withSound) {
        if (panelOpen) return;
        panelOpen = true;
        panelFrame.open();
        toggleButton.setLabel(hideLabel);
        if (withSound) {
            SkillTreeSounds.panelOpened();
        }
    }

    void close() {
        if (!panelOpen) return;
        panelOpen = false;
        panelFrame.close();
        toggleButton.setLabel(showLabel);
    }

    void advance(float amount) {
        panelFrame.advance(amount);
    }

    boolean isToggleHit(float x, float y) {
        return toggleButton.isClickable(x, y);
    }

    boolean contains(float x, float y) {
        if (toggleButton.contains(x, y)) return true;
        PanelLayout layout = drawnLayout;
        return layout != null && Rects.contains(layout.x, layout.topY - layout.fullHeight, layout.width, layout.fullHeight, x, y);
    }

    void render(PositionAPI canvasPosition, float mouseX, float mouseY, float alphaMult, float toggleAlpha, boolean toggleLive) {
        drawnLayout = null;
        toggleButton.setEnabled(toggleLive);
        placeToggle(canvasPosition, toggleAlpha);
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null) return;

        PanelLayout layout = layoutPanel(canvasPosition);
        if (layout != null && panelFrame.isVisible()) {
            drawnLayout = layout;
            panelFrame.render(layout.x, layout.topY - layout.fullHeight, layout.width, layout.fullHeight, SkillTreePanelStyle.GLOW_COLOR,
                    alphaMult, (frameLeft, frameBottom, frameWidth, frameHeight, frameAlpha) -> renderGroups(layout, frameAlpha));
        }
        if (toggleAlpha > 0f) {
            toggleButton.render(mouseX, mouseY, alphaMult * toggleAlpha);
        }
    }

    private void placeToggle(PositionAPI canvasPosition, float toggleAlpha) {
        if (toggleAlpha <= 0f) {
            toggleButton.hide();
            placedToggleRight = Float.NaN;
            return;
        }
        float toggleRight = canvasPosition.getX() + canvasPosition.getWidth() - STAT_PANEL_MARGIN;
        float toggleTop = canvasPosition.getY() + canvasPosition.getHeight() - STAT_PANEL_MARGIN;
        if (toggleRight == placedToggleRight && toggleTop == placedToggleTop) return;
        toggleButton.place(toggleRight - toggleWidth, toggleTop - TOGGLE_HEIGHT, toggleWidth, TOGGLE_HEIGHT);
        placedToggleRight = toggleRight;
        placedToggleTop = toggleTop;
    }

    private static void renderGroups(PanelLayout layout, float alphaMult) {
        float rowStep = STAT_PANEL_FONT_SIZE * FONT_LINE_HEIGHT_FACTOR;
        for (GroupContent group : layout.groupContents) {
            GroupLabels labels = group.labels;
            labels.header.setAlpha(alphaMult).draw(layout.x + layout.width / 2f, group.headerTextY);
            labels.statNames.setAlpha(alphaMult).draw(layout.x + STAT_PANEL_PADDING, group.bodyTextY);

            float rowY = group.bodyTextY;
            for (TextLabel valueLine : labels.values) {
                valueLine.setAlpha(alphaMult).draw(layout.x + layout.width - STAT_PANEL_PADDING, rowY);
                rowY -= rowStep;
            }
        }
    }

    private PanelLayout layoutPanel(PositionAPI canvasPosition) {
        if (statGroups == null || statGroups.isEmpty()) return null;

        if (cachedLayout != null && statGroups.equals(cachedLayoutGroups)
                && canvasPosition.getX() == cachedLayoutX && canvasPosition.getY() == cachedLayoutY
                && canvasPosition.getWidth() == cachedLayoutWidth && canvasPosition.getHeight() == cachedLayoutHeight) {
            return cachedLayout;
        }

        List<GroupLabels> labelsByGroup = new ArrayList<>();
        float headerHeight = STAT_PANEL_HEADER_FONT_SIZE * FONT_LINE_HEIGHT_FACTOR;
        float panelWidth = 0f;

        for (StatGroup group : statGroups) {
            GroupLabels labels = groupLabels.computeIfAbsent(group.name, GroupLabels::new).update(group);
            labelsByGroup.add(labels);
            float headerMinWidth = labels.header.width() + STAT_PANEL_PADDING * 2f;
            float bodyWidth = labels.statNames.width() + STAT_PANEL_COLUMN_GAP + labels.valueWidth() + STAT_PANEL_PADDING * 2f;
            panelWidth = Math.max(panelWidth, Math.max(headerMinWidth, bodyWidth));
        }

        float contentHeight = 0f;
        for (int i = 0; i < labelsByGroup.size(); i++) {
            contentHeight += headerHeight + STAT_PANEL_HEADER_GAP + labelsByGroup.get(i).statNames.height();
            if (i < labelsByGroup.size() - 1) contentHeight += STAT_PANEL_GROUP_GAP;
        }
        float fullHeight = STAT_PANEL_PADDING * 2f + contentHeight;

        float topY = canvasPosition.getY() + canvasPosition.getHeight() - STAT_PANEL_MARGIN - TOGGLE_HEIGHT - TOGGLE_GAP;
        float panelX = canvasPosition.getX() + canvasPosition.getWidth() - panelWidth - STAT_PANEL_MARGIN;

        List<GroupContent> groupContents = new ArrayList<>();
        float cursorY = topY - STAT_PANEL_PADDING;
        for (GroupLabels labels : labelsByGroup) {
            float headerTextY = cursorY;
            float bodyTextY = headerTextY - headerHeight - STAT_PANEL_HEADER_GAP;
            groupContents.add(new GroupContent(labels, headerTextY, bodyTextY));
            float groupHeight = headerHeight + STAT_PANEL_HEADER_GAP + labels.statNames.height();
            cursorY = headerTextY - groupHeight - STAT_PANEL_GROUP_GAP;
        }

        cachedLayout = new PanelLayout(panelX, topY, panelWidth, fullHeight, groupContents);
        cachedLayoutGroups = statGroups;
        cachedLayoutX = canvasPosition.getX();
        cachedLayoutY = canvasPosition.getY();
        cachedLayoutWidth = canvasPosition.getWidth();
        cachedLayoutHeight = canvasPosition.getHeight();
        return cachedLayout;
    }

    private List<StatGroup> buildStatGroups(ShipOpBudget budget) {
        List<StatGroup> builtGroups = new ArrayList<>();
        MutableShipStatsAPI stats = member.getStats();
        ShipHullSpecAPI hullSpec = member.getHullSpec();

        List<StatLine> general = new ArrayList<>();
        addComparedStat(general, Translation.text("ui.stats.hullPoints"), stats.getHullBonus().computeEffective(hullSpec.getHitpoints()), hullSpec.getHitpoints());
        addComparedStat(general, Translation.text("ui.stats.armor"), stats.getArmorBonus().computeEffective(hullSpec.getArmorRating()), hullSpec.getArmorRating());
        addComparedStat(general, Translation.text("ui.stats.maxFlux"), stats.getFluxCapacity().getModifiedValue(), stats.getFluxCapacity().getBaseValue());
        addComparedStat(general, Translation.text("ui.stats.fluxDissipation"), stats.getFluxDissipation().getModifiedValue(), stats.getFluxDissipation().getBaseValue());
        builtGroups.add(new StatGroup(Translation.text("ui.stats.group.general"), general));

        List<StatLine> mobility = new ArrayList<>();
        addComparedStat(mobility, Translation.text("ui.stats.topSpeed"), stats.getMaxSpeed().getModifiedValue(), stats.getMaxSpeed().getBaseValue());
        addComparedStat(mobility, Translation.text("ui.stats.maxTurnRate"), stats.getMaxTurnRate().getModifiedValue(), stats.getMaxTurnRate().getBaseValue());
        addComparedStat(mobility, Translation.text("ui.stats.acceleration"), stats.getAcceleration().getModifiedValue(), stats.getAcceleration().getBaseValue());
        builtGroups.add(new StatGroup(Translation.text("ui.stats.group.mobility"), mobility));

        ShieldAPI.ShieldType shieldType = ShieldSkillEffect.resolveDisplayShieldType(hullSpec.getShieldType(), AllocatedSkillEffects.forMember(member));
        if (shieldType != ShieldAPI.ShieldType.NONE) {
            List<StatLine> defense = new ArrayList<>();
            defense.add(new StatLine(Translation.text("ui.stats.shieldType"), Translation.text("shieldType." + shieldType.name())));
            ShipHullSpecAPI.ShieldSpecAPI shieldSpec = getShieldSpecOrNull(hullSpec);
            if (shieldSpec != null && hullSpec.getShieldType() != ShieldAPI.ShieldType.NONE) {
                addComparedStat(defense, Translation.text("ui.stats.shieldArc"), stats.getShieldArcBonus().computeEffective(shieldSpec.getArc()), shieldSpec.getArc());
                addShieldFluxPerDamage(defense, hullSpec.getBaseShieldFluxPerDamageAbsorbed() * shieldFluxPerDamageMult(stats),
                        hullSpec.getBaseShieldFluxPerDamageAbsorbed());
                addComparedStatLowerIsBetter(defense, Translation.text("ui.stats.shieldUpkeep"),
                        shieldSpec.getUpkeepCost() * stats.getShieldUpkeepMult().getModifiedValue(),
                        shieldSpec.getUpkeepCost() * stats.getShieldUpkeepMult().getBaseValue());
            } else {
                addStat(defense, Translation.text("ui.stats.shieldArc"), stats.getShieldArcBonus().computeEffective(ShieldSkillEffect.MAKESHIFT_SHIELD_ARC));
                addShieldFluxPerDamage(defense, ShieldSkillEffect.MAKESHIFT_SHIELD_EFFICIENCY * shieldFluxPerDamageMult(stats),
                        ShieldSkillEffect.MAKESHIFT_SHIELD_EFFICIENCY);
                addStat(defense, Translation.text("ui.stats.shieldUpkeep"),
                        ShieldSkillEffect.MAKESHIFT_SHIELD_UPKEEP * stats.getShieldUpkeepMult().getModifiedValue());
            }
            builtGroups.add(new StatGroup(Translation.text("ui.stats.group.defense"), defense));
        }

        List<StatLine> logistics = new ArrayList<>();
        logistics.add(new StatLine(Translation.text("ui.stats.crew"), Math.round(member.getMinCrew()) + "-" + Math.round(member.getMaxCrew())));
        addComparedStat(logistics, Translation.text("ui.stats.cargoCapacity"), member.getCargoCapacity(), hullSpec.getCargo());
        addComparedStat(logistics, Translation.text("ui.stats.fuelCapacity"), member.getFuelCapacity(), hullSpec.getFuel());
        addStat(logistics, Translation.text("ui.stats.fuelUse"), member.getFuelUse());
        addComparedStat(logistics, Translation.text("ui.stats.burnLevel"), stats.getMaxBurnLevel().getModifiedValue(), stats.getMaxBurnLevel().getBaseValue());
        addComparedStatLowerIsBetter(logistics, Translation.text("ui.stats.sensorProfile"), stats.getSensorProfile().getModifiedValue(), stats.getSensorProfile().getBaseValue());
        addComparedStat(logistics, Translation.text("ui.stats.sensorStrength"), stats.getSensorStrength().getModifiedValue(), stats.getSensorStrength().getBaseValue());
        logistics.add(new StatLine(Translation.text("ui.stats.ordnancePoints"), budget.usedOp + "/" + budget.totalOp));
        ShipSkillData skillData = ShipSkillDataManager.get(member.getId());
        logistics.add(new StatLine(Translation.text("ui.stats.level"), Translation.msg("ui.stats.levelValue").arg("level", skillData.getLevel())
                .arg("xp", Math.round(skillData.getXp())).text()));
        if (skillData.getBankedFreeAllocations() > 0) {
            logistics.add(new StatLine(Translation.text("ui.stats.freeAllocationsBanked"), String.valueOf(skillData.getBankedFreeAllocations())));
        }
        addStat(logistics, Translation.text("ui.stats.maxCombatReadiness"), stats.getMaxCombatReadiness().getModifiedValue() * 100f, "%");
        addComparedStatLowerIsBetter(logistics, Translation.text("ui.stats.suppliesPerMonth"), stats.getSuppliesPerMonth().getModifiedValue(), stats.getSuppliesPerMonth().getBaseValue());
        builtGroups.add(new StatGroup(Translation.text("ui.stats.group.logistics"), logistics));

        return builtGroups;
    }

    private static float shieldFluxPerDamageMult(MutableShipStatsAPI stats) {
        return stats.getShieldAbsorptionMult().getModifiedValue() * stats.getShieldDamageTakenMult().getModifiedValue();
    }

    private static ShipHullSpecAPI.ShieldSpecAPI getShieldSpecOrNull(ShipHullSpecAPI hullSpec) {
        return FallbackSupport.getOrFallback(hullSpec::getShieldSpec, null,
                Logger.getLogger(SkillTreeStatPanel.class), "Failed to read shield spec");
    }

    private static final class StatGroup {
        final String name;
        final List<StatLine> statLines;

        StatGroup(String name, List<StatLine> statLines) {
            this.name = name;
            this.statLines = statLines;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StatGroup other)) return false;
            return name.equals(other.name) && statLines.equals(other.statLines);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, statLines);
        }
    }

    private static final class StatLine {
        final String label;
        final String value;
        final Color valueColor;

        StatLine(String label, String value) {
            this(label, value, STAT_PANEL_VALUE_COLOR);
        }

        StatLine(String label, String value, Color valueColor) {
            this.label = label;
            this.value = value;
            this.valueColor = valueColor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StatLine other)) return false;
            return label.equals(other.label) && value.equals(other.value) && valueColor.equals(other.valueColor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(label, value, valueColor);
        }
    }

    private static final class PanelLayout {
        final float x;
        final float topY;
        final float width;
        final float fullHeight;
        final List<GroupContent> groupContents;

        PanelLayout(float x, float topY, float width, float fullHeight, List<GroupContent> groupContents) {
            this.x = x;
            this.topY = topY;
            this.width = width;
            this.fullHeight = fullHeight;
            this.groupContents = groupContents;
        }
    }

    private static final class GroupLabels {
        final TextLabel header;
        final TextLabel statNames = new TextLabel(STAT_PANEL_FONT_SIZE, STAT_PANEL_LABEL_COLOR);
        final List<TextLabel> values = new ArrayList<>();
        private List<StatLine> shownLines = List.of();

        GroupLabels(String groupName) {
            header = new TextLabel(STAT_PANEL_HEADER_FONT_SIZE, STAT_PANEL_HEADER_TEXT_COLOR, LazyFont.TextAnchor.TOP_CENTER).set(groupName);
        }

        GroupLabels update(StatGroup group) {
            if (group.statLines.equals(shownLines)) {
                return this;
            }
            shownLines = group.statLines;
            List<String> names = new ArrayList<>(shownLines.size());
            for (int i = 0; i < shownLines.size(); i++) {
                StatLine line = shownLines.get(i);
                names.add(line.label);
                if (i == values.size()) {
                    values.add(new TextLabel(STAT_PANEL_FONT_SIZE, line.valueColor, LazyFont.TextAnchor.TOP_RIGHT));
                }
                values.get(i).setColor(line.valueColor).set(line.value);
            }
            while (values.size() > shownLines.size()) {
                values.remove(values.size() - 1).dispose();
            }
            statNames.set(String.join("\n", names));
            return this;
        }

        float valueWidth() {
            float widest = 0f;
            for (TextLabel value : values) {
                widest = Math.max(widest, value.width());
            }
            return widest;
        }
    }

    private static final class GroupContent {
        final GroupLabels labels;
        final float headerTextY;
        final float bodyTextY;

        GroupContent(GroupLabels labels, float headerTextY, float bodyTextY) {
            this.labels = labels;
            this.headerTextY = headerTextY;
            this.bodyTextY = bodyTextY;
        }
    }

    private static void addStat(List<StatLine> lines, String label, float value) {
        addStat(lines, label, value, "");
    }

    private static void addStat(List<StatLine> lines, String label, float value, String suffix) {
        lines.add(new StatLine(label, formatStat(value) + suffix));
    }

    private static void addComparedStat(List<StatLine> lines, String label, float current, float base) {
        addComparedStat(lines, label, current, base, "");
    }

    private static void addComparedStat(List<StatLine> lines, String label, float current, float base, String suffix) {
        lines.add(new StatLine(label, formatStat(current) + suffix, colorForComparison(current, base, false)));
    }

    private static void addComparedStatLowerIsBetter(List<StatLine> lines, String label, float current, float base) {
        addComparedStatLowerIsBetter(lines, label, current, base, "");
    }

    private static void addComparedStatLowerIsBetter(List<StatLine> lines, String label, float current, float base, String suffix) {
        lines.add(new StatLine(label, formatStat(current) + suffix, colorForComparison(current, base, true)));
    }

    private static void addShieldFluxPerDamage(List<StatLine> lines, float current, float base) {
        String value = BigDecimal.valueOf(Math.round(current * 100f) / 100.0).stripTrailingZeros().toPlainString();
        lines.add(new StatLine(Translation.text("ui.stats.shieldFluxPerDamage"), value, colorForComparison(current, base, true)));
    }

    private static Color colorForComparison(float current, float base, boolean lowerIsBetter) {
        if (current > base + STAT_COMPARISON_EPSILON) return lowerIsBetter ? STAT_DECREASED_COLOR : STAT_INCREASED_COLOR;
        if (current < base - STAT_COMPARISON_EPSILON) return lowerIsBetter ? STAT_INCREASED_COLOR : STAT_DECREASED_COLOR;
        return STAT_PANEL_VALUE_COLOR;
    }

    private static String formatStat(float value) {
        if (value == Math.round(value)) {
            return String.valueOf(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
