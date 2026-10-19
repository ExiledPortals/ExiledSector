package exiledsector.items;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ModifierLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class StarChart extends Item {

    public static final int LOWEST_TIER = 1;
    public static final int HIGHEST_TIER = 15;
    public static final int LOWEST_QUALITY = 0;
    public static final int HIGHEST_QUALITY = 20;

    private int tier = LOWEST_TIER;
    private int quality;

    StarChart(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        super(id, definitionId, seed, explicitModifiers);
    }

    StarChart(Item source) {
        super(source);
        if (source instanceof StarChart chart) {
            tier = chart.tier;
            quality = chart.quality;
        } else if (source instanceof UnresolvedItem unresolved) {
            tier = unresolved.chartTier(LOWEST_TIER);
            quality = unresolved.chartQuality(LOWEST_QUALITY);
        }
    }

    public static int clampTier(int tier) {
        return Math.max(LOWEST_TIER, Math.min(HIGHEST_TIER, tier));
    }

    public static int clampQuality(int quality) {
        return Math.max(LOWEST_QUALITY, Math.min(HIGHEST_QUALITY, quality));
    }

    @Override
    public ItemType type() {
        return ItemType.STAR_CHART;
    }

    @Override
    public int tier() {
        return StarChartDefinitions.tierAtLeastMinimum(definitionId(), tier);
    }

    public int quality() {
        return clampQuality(quality);
    }

    public String systemType() {
        return StarChartDefinitions.systemType(definitionId());
    }

    @Override
    void applyTraits(ItemTraits traits) {
        super.applyTraits(traits);
        tier = StarChartDefinitions.tierAtLeastMinimum(definitionId(), traits.tier());
        quality = clampQuality(traits.quality());
        if (traits.implicits() != null) {
            replaceImplicits(traits.implicits());
        }
    }

    @Override
    void copyTraitsFrom(Item source) {
        if (source instanceof StarChart chart) {
            tier = chart.tier();
            quality = chart.quality();
        }
    }

    @Override
    public ItemTraits traits() {
        List<RolledModifier> currentImplicits = implicits();
        return new ItemTraits(tier(), quality(), corruption(), currentImplicits.isEmpty() ? null : currentImplicits, mirrored());
    }

    @Override
    public List<RolledModifier> implicits() {
        List<RolledModifier> stored = storedImplicits();
        if (stored != null && !(StarChartImplicits.isLoaded() && noneResolve(stored))) {
            return Collections.unmodifiableList(stored);
        }
        List<RolledModifier> rolled = StarChartImplicits.roll(systemType(), seed());
        if (StarChartImplicits.isLoaded()) {
            replaceImplicits(rolled);
        }
        return rolled;
    }

    private boolean noneResolve(List<RolledModifier> stored) {
        return stored.stream().noneMatch(implicit -> implicitModifier(implicit.modifierId()) != null);
    }

    @Override
    Modifier implicitModifier(String modifierId) {
        for (Modifier implicit : StarChartImplicits.forSystemType(systemType())) {
            if (implicit.id().equals(modifierId)) {
                return implicit;
            }
        }
        Modifier corruptionImplicit = StarChartImplicits.anyById(modifierId);
        return corruptionImplicit != null ? corruptionImplicit : catalogueModifier(modifierId);
    }

    @Override
    Modifier explicitModifier(String modifierId) {
        Modifier poolModifier = super.explicitModifier(modifierId);
        return poolModifier != null ? poolModifier : catalogueModifier(modifierId);
    }

    private static Modifier catalogueModifier(String modifierId) {
        return ItemModifiers.itemTypes(modifierId).contains(ItemType.STAR_CHART) ? ItemModifiers.get(modifierId) : null;
    }

    @Override
    StyledText unresolvedLine(ModifierEffects effects, RolledEffect effect) {
        return effect.effect(effects) == null ? null : Translation.styled("chart.item.unresolvedModifier");
    }

    @Override
    ModifierLine implicitLine(String effectName) {
        return StarChartImplicits.line(systemType(), effectName);
    }

    @Override
    List<Modifier> implicitCorruptionPool() {
        return StarChartImplicits.corruptionPool();
    }

    @Override
    boolean implicitCanVary(RolledModifier implicit) {
        if (StarChartImplicits.sharedChoice(implicit.modifierId()) != null) {
            return StarChartImplicits.sharedChoiceCanVary(implicit.modifierId());
        }
        return super.implicitCanVary(implicit);
    }

    @Override
    RolledModifier rerolledImplicit(RolledModifier implicit, Random random) {
        if (StarChartImplicits.sharedChoice(implicit.modifierId()) != null) {
            RolledModifier rerolled = StarChartImplicits.rollSharedChoice(random);
            return rerolled == null ? implicit : rerolled;
        }
        return super.rerolledImplicit(implicit, random);
    }

    @Override
    public List<StyledText> headerLines() {
        List<StyledText> lines = new ArrayList<>();
        lines.add(Translation.msg("chart.item.tier").arg("tier", tier()).arg("systemType", systemTypeName(systemType())).styled());
        if (quality() > 0) {
            lines.add(Translation.msg("chart.item.quality").arg("quality", quality()).styled());
        }
        lines.addAll(super.headerLines());
        return lines;
    }

    public static String systemTypeName(String systemType) {
        String key = systemType == null ? StarChartDefinitions.ANY_SYSTEM_TYPE : systemType.toLowerCase(Locale.ROOT);
        return Translation.text("chart.systemType." + key);
    }
}
