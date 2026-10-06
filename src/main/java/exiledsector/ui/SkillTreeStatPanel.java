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
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.CachedText;
import exiledsector.ui.util.FallbackSupport;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;
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

    private static final String COLLAPSE_ICON_PATH = "graphics/icons/ship_store.png";
    private static final String EXPAND_ICON_PATH = "graphics/icons/ship_take.png";
    private static final float COLLAPSE_BUTTON_SIZE = 40f;
    private static final float COLLAPSE_BUTTON_MARGIN = 8f;
    private static final float COLLAPSED_PANEL_SIZE = COLLAPSE_BUTTON_MARGIN * 2f + COLLAPSE_BUTTON_SIZE;

    private final FleetMemberAPI member;
    private final BorderedPanel borderedPanel = new BorderedPanel(SkillTreeStatPanel.class);
    private final SpriteCache spriteCache = new SpriteCache(SkillTreeStatPanel.class);
    private final CachedText<String, GroupTexts> groupTextCache = new CachedText<>();
    private final Map<String, LazyFont.DrawableString> statGroupHeaderText = new HashMap<>();

    private List<StatGroup> groups;
    private int groupsRevision;
    private PanelLayout cachedLayout;
    private List<StatGroup> cachedLayoutGroups;
    private float cachedLayoutX;
    private float cachedLayoutY;
    private float cachedLayoutWidth;
    private float cachedLayoutHeight;

    private boolean collapsed = false;
    private ScreenRect drawnBounds = ScreenRect.NONE;

    SkillTreeStatPanel(FleetMemberAPI member) {
        this.member = member;
    }

    void refresh(ShipOpBudget budget, int revision) {
        if (groups != null && revision == groupsRevision) return;
        groups = buildStatGroups(budget);
        groupsRevision = revision;
    }

    void toggleCollapsed() {
        collapsed = !collapsed;
        if (!collapsed) {
            SkillTreeSounds.panelOpened();
        }
    }

    boolean isCollapseButtonHit(PositionAPI position, float x, float y) {
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null) return false;
        PanelLayout layout = layoutPanel(position, font);
        if (layout == null) return false;

        return isWithinButton(collapseButtonCenterX(layout), collapseButtonCenterY(layout), COLLAPSE_BUTTON_SIZE, x, y);
    }

    boolean contains(float x, float y) {
        return drawnBounds.contains(x, y);
    }

    void render(PositionAPI position, float alphaMult) {
        drawnBounds = ScreenRect.NONE;
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null) return;

        PanelLayout layout = layoutPanel(position, font);
        if (layout == null) return;

        float panelWidth = collapsed ? COLLAPSED_PANEL_SIZE : layout.width;
        float panelHeight = collapsed ? COLLAPSED_PANEL_SIZE : layout.fullHeight;
        float rightEdge = layout.x + layout.width;
        float panelX = rightEdge - panelWidth;
        float bottomY = layout.topY - panelHeight;

        drawnBounds = new ScreenRect(panelX, bottomY, panelWidth, panelHeight);
        borderedPanel.draw(panelX, bottomY, panelWidth, panelHeight, alphaMult);

        if (!collapsed) {
            float rowStep = STAT_PANEL_FONT_SIZE * FONT_LINE_HEIGHT_FACTOR;
            for (GroupContent group : layout.groups) {
                group.headerText.draw(layout.x + layout.width / 2f, group.headerTextY);
                group.labelText.drawable.draw(layout.x + STAT_PANEL_PADDING, group.bodyTextY);

                float rowY = group.bodyTextY;
                for (LazyFont.DrawableString valueLine : group.valueLines) {
                    valueLine.draw(layout.x + layout.width - STAT_PANEL_PADDING, rowY);
                    rowY -= rowStep;
                }
            }
        }

        drawCollapseButton(layout, alphaMult);
    }

    private void drawCollapseButton(PanelLayout layout, float alphaMult) {
        String iconPath = collapsed ? COLLAPSE_ICON_PATH : EXPAND_ICON_PATH;
        SpriteDraw.drawAtCenter(spriteCache, iconPath,
                collapseButtonCenterX(layout), collapseButtonCenterY(layout),
                COLLAPSE_BUTTON_SIZE, COLLAPSE_BUTTON_SIZE, null, alphaMult);
    }

    private static float collapseButtonCenterX(PanelLayout layout) {
        return layout.x + layout.width - COLLAPSE_BUTTON_MARGIN - COLLAPSE_BUTTON_SIZE / 2f;
    }

    private static float collapseButtonCenterY(PanelLayout layout) {
        return layout.topY - COLLAPSE_BUTTON_MARGIN - COLLAPSE_BUTTON_SIZE / 2f;
    }

    static boolean isWithinButton(float buttonCenterX, float buttonCenterY, float buttonSize, float x, float y) {
        float half = buttonSize / 2f;
        return x >= buttonCenterX - half && x <= buttonCenterX + half
                && y >= buttonCenterY - half && y <= buttonCenterY + half;
    }

    private PanelLayout layoutPanel(PositionAPI position, LazyFont font) {
        if (groups == null || groups.isEmpty()) return null;

        if (cachedLayout != null && groups.equals(cachedLayoutGroups)
                && position.getX() == cachedLayoutX && position.getY() == cachedLayoutY
                && position.getWidth() == cachedLayoutWidth && position.getHeight() == cachedLayoutHeight) {
            return cachedLayout;
        }

        List<SkillTreePanelStyle.TooltipText> labelTexts = new ArrayList<>();
        List<List<LazyFont.DrawableString>> valueLinesList = new ArrayList<>();
        float headerHeight = STAT_PANEL_HEADER_FONT_SIZE * FONT_LINE_HEIGHT_FACTOR;
        float width = 0f;

        for (StatGroup group : groups) {
            GroupTexts texts = getGroupTexts(font, group);
            labelTexts.add(texts.labelText);
            valueLinesList.add(texts.valueLines);
            float headerMinWidth = font.calcWidth(group.name, STAT_PANEL_HEADER_FONT_SIZE) + STAT_PANEL_PADDING * 2f;
            float bodyWidth = texts.labelText.width + STAT_PANEL_COLUMN_GAP + texts.valueWidth + STAT_PANEL_PADDING * 2f;
            width = Math.max(width, Math.max(headerMinWidth, bodyWidth));
        }

        float contentHeight = 0f;
        for (int i = 0; i < groups.size(); i++) {
            contentHeight += headerHeight + STAT_PANEL_HEADER_GAP + labelTexts.get(i).height;
            if (i < groups.size() - 1) contentHeight += STAT_PANEL_GROUP_GAP;
        }
        float fullHeight = STAT_PANEL_PADDING * 2f + contentHeight;

        float topY = position.getY() + position.getHeight() - STAT_PANEL_MARGIN;
        float x = position.getX() + position.getWidth() - width - STAT_PANEL_MARGIN;

        List<GroupContent> contents = new ArrayList<>();
        float cursorY = topY - STAT_PANEL_PADDING;
        for (int i = 0; i < groups.size(); i++) {
            StatGroup group = groups.get(i);
            SkillTreePanelStyle.TooltipText labelText = labelTexts.get(i);
            List<LazyFont.DrawableString> valueLines = valueLinesList.get(i);

            float headerTextY = cursorY;
            float bodyTextY = headerTextY - headerHeight - STAT_PANEL_HEADER_GAP;

            contents.add(new GroupContent(getStatGroupHeaderText(font, group.name), labelText, valueLines, headerTextY, bodyTextY));

            float groupHeight = headerHeight + STAT_PANEL_HEADER_GAP + labelText.height;
            cursorY = headerTextY - groupHeight - STAT_PANEL_GROUP_GAP;
        }

        cachedLayout = new PanelLayout(x, topY, width, fullHeight, contents);
        cachedLayoutGroups = groups;
        cachedLayoutX = position.getX();
        cachedLayoutY = position.getY();
        cachedLayoutWidth = position.getWidth();
        cachedLayoutHeight = position.getHeight();
        return cachedLayout;
    }

    private LazyFont.DrawableString getStatGroupHeaderText(LazyFont font, String name) {
        return statGroupHeaderText.computeIfAbsent(name,
                n -> SkillTreePanelStyle.buildSimpleText(font, n, STAT_PANEL_HEADER_FONT_SIZE, STAT_PANEL_HEADER_TEXT_COLOR, LazyFont.TextAnchor.TOP_CENTER));
    }

    private GroupTexts getGroupTexts(LazyFont font, StatGroup group) {
        return groupTextCache.get(group.name, group.statLines, name -> buildGroupTexts(font, group));
    }

    private GroupTexts buildGroupTexts(LazyFont font, StatGroup group) {
        List<String> labels = new ArrayList<>();
        List<LazyFont.DrawableString> valueLines = new ArrayList<>();
        float valueWidth = 0f;
        for (StatLine line : group.statLines) {
            labels.add(line.label);
            valueLines.add(SkillTreePanelStyle.buildSimpleText(font, line.value, STAT_PANEL_FONT_SIZE, line.valueColor, LazyFont.TextAnchor.TOP_RIGHT));
            valueWidth = Math.max(valueWidth, font.calcWidth(line.value, STAT_PANEL_FONT_SIZE));
        }
        SkillTreePanelStyle.TooltipText labelText = SkillTreePanelStyle.buildJoinedText(font, labels, STAT_PANEL_FONT_SIZE, STAT_PANEL_LABEL_COLOR);
        return new GroupTexts(labelText, valueLines, valueWidth);
    }

    private List<StatGroup> buildStatGroups(ShipOpBudget budget) {
        List<StatGroup> groups = new ArrayList<>();
        MutableShipStatsAPI stats = member.getStats();
        ShipHullSpecAPI hullSpec = member.getHullSpec();

        List<StatLine> general = new ArrayList<>();
        addComparedStat(general, Translation.text("ui.stats.hullPoints"), stats.getHullBonus().computeEffective(hullSpec.getHitpoints()), hullSpec.getHitpoints());
        addComparedStat(general, Translation.text("ui.stats.armor"), stats.getArmorBonus().computeEffective(hullSpec.getArmorRating()), hullSpec.getArmorRating());
        addComparedStat(general, Translation.text("ui.stats.maxFlux"), stats.getFluxCapacity().getModifiedValue(), stats.getFluxCapacity().getBaseValue());
        addComparedStat(general, Translation.text("ui.stats.fluxDissipation"), stats.getFluxDissipation().getModifiedValue(), stats.getFluxDissipation().getBaseValue());
        groups.add(new StatGroup(Translation.text("ui.stats.group.general"), general));

        List<StatLine> mobility = new ArrayList<>();
        addComparedStat(mobility, Translation.text("ui.stats.topSpeed"), stats.getMaxSpeed().getModifiedValue(), stats.getMaxSpeed().getBaseValue());
        addComparedStat(mobility, Translation.text("ui.stats.maxTurnRate"), stats.getMaxTurnRate().getModifiedValue(), stats.getMaxTurnRate().getBaseValue());
        addComparedStat(mobility, Translation.text("ui.stats.acceleration"), stats.getAcceleration().getModifiedValue(), stats.getAcceleration().getBaseValue());
        groups.add(new StatGroup(Translation.text("ui.stats.group.mobility"), mobility));

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
            groups.add(new StatGroup(Translation.text("ui.stats.group.defense"), defense));
        }

        List<StatLine> logistics = new ArrayList<>();
        logistics.add(new StatLine(Translation.text("ui.stats.crew"), Math.round(member.getMinCrew()) + "-" + Math.round(member.getMaxCrew())));
        addComparedStat(logistics, Translation.text("ui.stats.cargoCapacity"), member.getCargoCapacity(), hullSpec.getCargo());
        addComparedStat(logistics, Translation.text("ui.stats.fuelCapacity"), member.getFuelCapacity(), hullSpec.getFuel());
        addStat(logistics, Translation.text("ui.stats.fuelUse"), member.getFuelUse());
        addComparedStat(logistics, Translation.text("ui.stats.burnLevel"), stats.getMaxBurnLevel().getModifiedValue(), stats.getMaxBurnLevel().getBaseValue());
        addComparedStatLowerIsBetter(logistics, Translation.text("ui.stats.sensorProfile"), stats.getSensorProfile().getModifiedValue(), stats.getSensorProfile().getBaseValue());
        addComparedStat(logistics, Translation.text("ui.stats.sensorStrength"), stats.getSensorStrength().getModifiedValue(), stats.getSensorStrength().getBaseValue());
        logistics.add(new StatLine(Translation.text("ui.stats.ordnancePoints"), budget.used + "/" + budget.total));
        ShipSkillData skillData = ShipSkillDataManager.get(member.getId());
        logistics.add(new StatLine(Translation.text("ui.stats.level"), Translation.msg("ui.stats.levelValue").arg("level", skillData.getLevel())
                .arg("xp", Math.round(skillData.getXp())).text()));
        if (skillData.getBankedFreeAllocations() > 0) {
            logistics.add(new StatLine(Translation.text("ui.stats.freeAllocationsBanked"), String.valueOf(skillData.getBankedFreeAllocations())));
        }
        addStat(logistics, Translation.text("ui.stats.maxCombatReadiness"), stats.getMaxCombatReadiness().getModifiedValue() * 100f, "%");
        addComparedStatLowerIsBetter(logistics, Translation.text("ui.stats.suppliesPerMonth"), stats.getSuppliesPerMonth().getModifiedValue(), stats.getSuppliesPerMonth().getBaseValue());
        groups.add(new StatGroup(Translation.text("ui.stats.group.logistics"), logistics));

        return groups;
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
        final List<GroupContent> groups;

        PanelLayout(float x, float topY, float width, float fullHeight, List<GroupContent> groups) {
            this.x = x;
            this.topY = topY;
            this.width = width;
            this.fullHeight = fullHeight;
            this.groups = groups;
        }
    }

    private static final class GroupTexts {
        final SkillTreePanelStyle.TooltipText labelText;
        final List<LazyFont.DrawableString> valueLines;
        final float valueWidth;

        GroupTexts(SkillTreePanelStyle.TooltipText labelText, List<LazyFont.DrawableString> valueLines, float valueWidth) {
            this.labelText = labelText;
            this.valueLines = valueLines;
            this.valueWidth = valueWidth;
        }
    }

    private static final class GroupContent {
        final LazyFont.DrawableString headerText;
        final SkillTreePanelStyle.TooltipText labelText;
        final List<LazyFont.DrawableString> valueLines;
        final float headerTextY;
        final float bodyTextY;

        GroupContent(LazyFont.DrawableString headerText, SkillTreePanelStyle.TooltipText labelText,
                     List<LazyFont.DrawableString> valueLines, float headerTextY, float bodyTextY) {
            this.headerText = headerText;
            this.labelText = labelText;
            this.valueLines = valueLines;
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
