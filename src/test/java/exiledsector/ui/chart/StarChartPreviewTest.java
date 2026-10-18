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
import exiledsector.items.Item;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarChartPreviewTest {

    private static final float TOLERANCE = 0.001f;
    private static final ChartTier BASE_TIER = new ChartTier(5, 185f, 2, 4, 1, 1, 50f, 90, 120, 14f, true, 350f, 1f, 1f, 1f);

    private static ChartGenContext with(int quality, AppliedEffect... effects) {
        return ChartGenContext.of(List.of(effects), quality);
    }

    private static AppliedEffect effect(ChartEffect effect, float value) {
        return new AppliedEffect(effect, value);
    }

    private static List<ChartPreview.Line> preview(ChartGenContext context) {
        return ChartPreview.of(context, BASE_TIER);
    }

    private static List<Stat> stats(ChartGenContext context) {
        return preview(context).stream().map(ChartPreview.Line::stat).toList();
    }

    private static List<String> texts(ChartGenContext context) {
        return StarChartPreviewText.modifierLines(context, BASE_TIER).stream().map(StyledText::plain).toList();
    }

    @Test
    void aChartWithoutModifiersPreviewsNothing() {
        assertTrue(preview(ChartGenContext.none()).isEmpty());
        assertTrue(preview(with(0)).isEmpty());
        assertTrue(preview(null).isEmpty());
        assertTrue(ChartPreview.of(with(5), null).isEmpty());
        assertTrue(StarChartPreviewText.modifierLines(ChartGenContext.none(), BASE_TIER).isEmpty());
        assertTrue(StarChartPreviewText.modifierLines((Item) null).isEmpty());
    }

    @Test
    void onlyTheValuesAModifierMovesAreShown() {
        ChartGenContext context = with(0, effect(ChartEffect.CHART_FLEET_SIZE_PERCENT, 25f), effect(ChartEffect.CHART_EXTRA_FLEETS, 2f));

        List<ChartPreview.Line> lines = preview(context);

        assertEquals(List.of(Stat.FLEET_SIZE, Stat.EXTRA_FLEETS), lines.stream().map(ChartPreview.Line::stat).toList());
        assertEquals(25f, lines.get(0).value(), TOLERANCE);
        assertEquals(2f, lines.get(1).value(), TOLERANCE);
        assertFalse(lines.get(1).isRange());
        assertFalse(lines.get(0).favourable());
        assertFalse(lines.get(1).favourable());
    }

    @Test
    void extraFleetsAddFullSizeFleetsWithoutChangingTheFleetSize() {
        ChartGenContext context = with(0, effect(ChartEffect.CHART_EXTRA_FLEETS, 3f));

        List<ChartPreview.Line> lines = preview(context);

        assertEquals(List.of(Stat.EXTRA_FLEETS), lines.stream().map(ChartPreview.Line::stat).toList());
        assertEquals(BASE_TIER.fleetSize(), context.adjust(BASE_TIER).fleetSize(), TOLERANCE);
        assertEquals(List.of("Enemy fleets +3"), texts(context));
    }

    @Test
    void fleetCountsClampedAtTheFloorPreviewTheChangeThatActuallySpawns() {
        ChartGenContext context = with(0, effect(ChartEffect.CHART_EXTRA_FLEETS, -3f));
        ChartTier adjusted = context.adjust(BASE_TIER);

        ChartPreview.Line fleets = preview(context).stream().filter(line -> line.stat() == Stat.EXTRA_FLEETS).findFirst().orElseThrow();

        assertEquals(1, adjusted.fleetsMin());
        assertEquals(1, adjusted.fleetsMax());
        assertTrue(fleets.isRange());
        assertEquals(adjusted.fleetsMax() - BASE_TIER.fleetsMax(), fleets.value(), TOLERANCE);
        assertEquals(adjusted.fleetsMin() - BASE_TIER.fleetsMin(), fleets.upperValue(), TOLERANCE);
        assertTrue(fleets.favourable());
        assertTrue(texts(context).contains("Enemy fleets -3 to -1"), texts(context).toString());
    }

    @Test
    void enemyBonusesBelowZeroPreviewOnlyWhatTheTierCanLose() {
        ChartGenContext context = with(0, effect(ChartEffect.CHART_OFFICER_LEVEL_BONUS, -3f), effect(ChartEffect.CHART_EXTRA_OFFICERS, -2f));

        List<ChartPreview.Line> lines = preview(context);

        assertEquals(List.of(Stat.OFFICER_LEVELS, Stat.EXTRA_OFFICERS), lines.stream().map(ChartPreview.Line::stat).toList());
        assertEquals(-1f, lines.get(0).value(), TOLERANCE);
        assertEquals(-1f, lines.get(1).value(), TOLERANCE);
        assertTrue(lines.get(0).favourable());
    }

    @Test
    void modifiersThatCancelOutPreviewNothing() {
        ChartGenContext context = with(0, effect(ChartEffect.CHART_FLEET_SIZE_PERCENT, 10f), effect(ChartEffect.CHART_FLEET_SIZE_PERCENT, -10f));

        assertTrue(preview(context).isEmpty());
    }

    @Test
    void valuesThatRoundToNeutralAreLeftOutAndTheRestShowTheirRoundedChange() {
        ChartGenContext tiny = with(0, effect(ChartEffect.CHART_FLEET_SIZE_PERCENT, 0.004f), effect(ChartEffect.CHART_FLEET_QUALITY_PERCENT, 0.004f),
                effect(ChartEffect.CHART_VAULT_RICHNESS_PERCENT, 0.003f), effect(ChartEffect.CHART_SENSOR_RANGE_PERCENT, -0.4f));
        ChartGenContext justVisible = with(0, effect(ChartEffect.CHART_FLEET_SIZE_PERCENT, 0.6f));

        assertTrue(preview(tiny).isEmpty(), preview(tiny).toString());
        assertEquals(List.of("Enemy fleet size +0.6%"), texts(justVisible));
    }

    @Test
    void rewardsAreFavourableAndAShorterSensorRangeIsNot() {
        ChartGenContext context = with(0, effect(ChartEffect.CHART_LOOT_BUDGET_PERCENT, 40f), effect(ChartEffect.CHART_SENSOR_RANGE_PERCENT, -30f),
                effect(ChartEffect.CHART_VAULT_RICHNESS_PERCENT, 30f), effect(ChartEffect.CHART_EXTRA_POIS, 1f));

        List<ChartPreview.Line> lines = preview(context);

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
                effect(ChartEffect.CHART_WARDEN_SIZE_PERCENT, 20f),
                effect(ChartEffect.CHART_ITEM_DROP_CHANCE_PERCENT, 50f));

        List<ChartPreview.Line> lines = preview(context);

        assertEquals(List.of(Stat.OFFICER_LEVELS, Stat.EXTRA_OFFICERS, Stat.WARDEN_SIZE, Stat.ITEM_CHANCE),
                lines.stream().map(ChartPreview.Line::stat).toList());
        assertEquals(List.of(2f, 1f, 20f, 1.5f), lines.stream().map(line -> ChartPreview.rounded(line.value())).toList());
    }

    @Test
    void chartQualityRaisesLootAndRewardToo() {
        assertEquals(List.of(Stat.CHART_QUALITY, Stat.LOOT, Stat.REWARD), stats(with(5)));
    }

    @Test
    void previewLinesReadInEnglishWithTheValueColouredByWhetherItHelps() {
        List<StyledText> lines = StarChartPreviewText.modifierLines(with(0, effect(ChartEffect.CHART_FLEET_SIZE_PERCENT, 25f),
                effect(ChartEffect.CHART_FLEET_QUALITY_PERCENT, 15f), effect(ChartEffect.CHART_REWARD_PERCENT, 50f)), BASE_TIER);

        assertEquals(List.of("Enemy fleet size +25%", "Enemy fleet quality +15%", "Completion reward x1.5"),
                lines.stream().map(StyledText::plain).toList());
        assertEquals(Style.BAD, lines.get(0).spans().get(0).style());
        assertEquals(Style.GOOD, lines.get(2).spans().get(0).style());
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
        assertTrue(early.contains(Math.round(withoutWarden.fleetSize()) + "% fleet size"), early);
        assertTrue(early.contains(Math.round(withoutWarden.fleetQuality()) + "% fleet quality"), early);
        assertTrue(early.contains(withoutWarden.fleetStrengthMin() + "% to " + withoutWarden.fleetStrengthMax() + "% fleet strength"), early);
        assertTrue(late.contains("warden fleet at " + Math.round(withWarden.wardenSize()) + "% fleet size"), late);
    }
}
