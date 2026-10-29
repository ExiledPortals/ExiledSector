package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarChartCorruptionTest {

    private static final String CHART = "star_chart";

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
    }

    @AfterEach
    void tearDown() {
        StarChartFixtures.clear();
    }

    @Test
    void chartsRollTheTierAndExpandedOutcomesBesideTheNanoswarmOnes() {
        StarChart chart = StarChartFixtures.chart(CHART, 1L, 5);

        assertEquals(CorruptionOutcome.STAR_CHART_OUTCOMES, ItemCrafting.corruptionOutcomes(chart));
        assertTrue(CorruptionOutcome.STAR_CHART_OUTCOMES.containsAll(CorruptionOutcome.NANOSWARM_OUTCOMES));
    }

    @Test
    void expandingRerollsEightModifiersFourPerSide() {
        for (long seed = 0; seed < 20; seed++) {
            StarChart chart = StarChartFixtures.chart(CHART, seed, 1 + (int) (seed % StarChart.CORRUPTED_TIER));

            assertSame(chart, StarChartCorruption.expandModifiers(chart, new Random(seed)));

            List<RolledModifier> explicits = chart.explicits();
            assertEquals(StarChartCorruption.EXPANDED_MODIFIERS, explicits.size());
            assertEquals(StarChartCorruption.EXPANDED_PER_SIDE, explicits.stream().filter(rolled -> rolled.slot() == ModifierSlot.PREFIX).count());
            assertEquals(StarChartCorruption.EXPANDED_PER_SIDE, explicits.stream().filter(rolled -> rolled.slot() == ModifierSlot.SUFFIX).count());
            assertEquals(explicits.size(), explicits.stream().map(RolledModifier::modifierId).distinct().count());
        }
    }

    @Test
    void raisingATierReachesTheCorruptedTierAndStopsThere() {
        StarChart chart = StarChartFixtures.chart(CHART, 3L, StarChart.HIGHEST_TIER);

        assertSame(chart, StarChartCorruption.raiseTier(chart));
        assertEquals(StarChart.CORRUPTED_TIER, chart.tier());
        assertNull(StarChartCorruption.raiseTier(chart));
        assertEquals(StarChart.CORRUPTED_TIER, chart.tier());
    }

    @Test
    void corruptingTopTierChartsCanRaiseThemOrExpandThem() {
        Set<CorruptionOutcome> seen = EnumSet.noneOf(CorruptionOutcome.class);
        for (long seed = 0; seed < 120; seed++) {
            StarChart chart = StarChartFixtures.chart(CHART, seed, StarChart.HIGHEST_TIER);
            Item corrupted = ItemCrafting.corrupt(new ItemStore(), chart, new Random(seed), definition -> true);

            assertTrue(corrupted.corrupted());
            seen.add(corrupted.corruption());
            if (corrupted.corruption() == CorruptionOutcome.TIER_RAISED) {
                assertEquals(StarChart.CORRUPTED_TIER, corrupted.tier());
            } else {
                assertEquals(StarChart.HIGHEST_TIER, corrupted.tier());
            }
            if (corrupted.corruption() == CorruptionOutcome.MODIFIERS_EXPANDED) {
                assertEquals(StarChartCorruption.EXPANDED_MODIFIERS, corrupted.explicits().size());
            }
        }
        assertTrue(seen.contains(CorruptionOutcome.TIER_RAISED));
        assertTrue(seen.contains(CorruptionOutcome.MODIFIERS_EXPANDED));
    }

    @Test
    void aCorruptedTierChartRaisesNoFurther() {
        StarChart chart = StarChartFixtures.chart(CHART, 4L, StarChart.CORRUPTED_TIER);

        assertEquals(StarChart.CORRUPTED_TIER, chart.tier());
        assertNull(StarChartCorruption.corrupt(CorruptionOutcome.TIER_RAISED, chart, new Random(4L)));
    }
}
