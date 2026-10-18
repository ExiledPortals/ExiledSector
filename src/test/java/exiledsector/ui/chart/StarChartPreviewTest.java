package exiledsector.ui.chart;

import exiledsector.charts.ChartEffect;
import exiledsector.charts.ChartGenContext;
import exiledsector.charts.ChartGenContext.AppliedEffect;
import exiledsector.charts.ChartPreview;
import exiledsector.charts.ChartPreview.Stat;
import exiledsector.charts.ChartTier;
import exiledsector.charts.ChartTiers;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarChartPreviewTest {

    private static final float TOLERANCE = 0.001f;

    private static ChartGenContext with(int quality, AppliedEffect... effects) {
        return ChartGenContext.of(List.of(effects), quality);
    }

    private static AppliedEffect effect(ChartEffect effect, float value) {
        return new AppliedEffect(effect, value);
    }

    private static List<Stat> stats(ChartGenContext context) {
        return ChartPreview.of(context).stream().map(ChartPreview.Line::stat).toList();
    }

    @Test
    void aChartWithoutModifiersPreviewsNothing() {
        assertTrue(ChartPreview.of(ChartGenContext.none()).isEmpty());
        assertTrue(ChartPreview.of(with(0)).isEmpty());
        assertTrue(ChartPreview.of(null).isEmpty());
        assertTrue(StarChartPreviewText.modifierLines(ChartGenContext.none()).isEmpty());
    }

    @Test
    void onlyTheValuesAModifierMovesAreShown() {
        ChartGenContext context = with(0, effect(ChartEffect.CHART_FLEET_POWER_PERCENT, 25f), effect(ChartEffect.CHART_EXTRA_FLEETS, 2f));

        List<ChartPreview.Line> lines = ChartPreview.of(context);

        assertEquals(List.of(Stat.FLEET_POWER, Stat.EXTRA_FLEETS), lines.stream().map(ChartPreview.Line::stat).toList());
        assertEquals(1.25f, lines.get(0).value(), TOLERANCE);
        assertEquals(2f, lines.get(1).value(), TOLERANCE);
        assertFalse(lines.get(0).favourable());
        assertFalse(lines.get(1).favourable());
    }

    @Test
    void modifiersThatCancelOutPreviewNothing() {
        ChartGenContext context = with(0, effect(ChartEffect.CHART_FLEET_POWER_PERCENT, 10f), effect(ChartEffect.CHART_FLEET_POWER_PERCENT, -10f));

        assertTrue(ChartPreview.of(context).isEmpty());
    }

    @Test
    void rewardsAreFavourableAndAShorterSensorRangeIsNot() {
        ChartGenContext context = with(0, effect(ChartEffect.CHART_LOOT_BUDGET_PERCENT, 40f), effect(ChartEffect.CHART_SENSOR_RANGE_PERCENT, -30f),
                effect(ChartEffect.CHART_VAULT_RICHNESS_PERCENT, 30f), effect(ChartEffect.CHART_EXTRA_POIS, 1f));

        List<ChartPreview.Line> lines = ChartPreview.of(context);

        assertEquals(List.of(Stat.SENSOR_RANGE, Stat.LOOT, Stat.EXTRA_POIS, Stat.VAULT_RICHNESS), lines.stream().map(ChartPreview.Line::stat).toList());
        assertEquals(0.7f, lines.get(0).value(), TOLERANCE);
        assertFalse(lines.get(0).favourable());
        assertTrue(lines.get(1).favourable());
        assertTrue(lines.get(2).favourable());
        assertTrue(lines.get(3).favourable());
    }

    @Test
    void everyEnemyStatIsReadFromItsOwnEffect() {
        ChartGenContext context = with(0, effect(ChartEffect.CHART_OFFICER_LEVEL_BONUS, 2f), effect(ChartEffect.CHART_EXTRA_OFFICERS, 1f),
                effect(ChartEffect.CHART_AVERAGE_SMODS_BONUS, 1f), effect(ChartEffect.CHART_BOSS_POWER_PERCENT, 20f),
                effect(ChartEffect.CHART_ITEM_DROP_CHANCE_PERCENT, 50f));

        List<ChartPreview.Line> lines = ChartPreview.of(context);

        assertEquals(List.of(Stat.OFFICER_LEVELS, Stat.EXTRA_OFFICERS, Stat.AVERAGE_SMODS, Stat.BOSS_POWER, Stat.ITEM_CHANCE),
                lines.stream().map(ChartPreview.Line::stat).toList());
        assertEquals(List.of(2f, 1f, 1f, 1.2f, 1.5f), lines.stream().map(line -> Math.round(line.value() * 100f) / 100f).toList());
    }

    @Test
    void chartQualityRaisesLootAndRewardToo() {
        assertEquals(List.of(Stat.CHART_QUALITY, Stat.LOOT, Stat.REWARD), stats(with(5)));
    }

    @Test
    void previewLinesReadInEnglishWithTheValueColouredByWhetherItHelps() {
        List<StyledText> lines = StarChartPreviewText.modifierLines(with(0, effect(ChartEffect.CHART_FLEET_POWER_PERCENT, 25f),
                effect(ChartEffect.CHART_EXTRA_FLEETS, 2f), effect(ChartEffect.CHART_FLEET_QUALITY_PERCENT, 15f),
                effect(ChartEffect.CHART_REWARD_PERCENT, 50f)));

        assertEquals(List.of("Enemy fleet power x1.25", "Enemy fleets +2", "Enemy fleet quality +15%", "Completion reward x1.5"),
                lines.stream().map(StyledText::plain).toList());
        assertEquals(Style.BAD, lines.get(0).spans().get(0).style());
        assertEquals(Style.GOOD, lines.get(3).spans().get(0).style());
    }

    @Test
    void theTierBaselineNamesTheWardenOnlyWhenTheTierHasOne() {
        ChartTier withoutWarden = ChartTiers.forTier(1);
        ChartTier withWarden = ChartTiers.forTier(ChartTiers.HIGHEST_TIER);

        assertFalse(withoutWarden.boss());
        assertTrue(withWarden.boss());
        String early = StarChartPreviewText.tierLine(1).plain();
        String late = StarChartPreviewText.tierLine(ChartTiers.HIGHEST_TIER).plain();
        assertTrue(early.startsWith("Tier 1 baseline: " + withoutWarden.fleetsMin() + " to " + withoutWarden.fleetsMax()), early);
        assertFalse(early.contains("warden"), early);
        assertTrue(late.contains("warden fleet"), late);
    }
}
