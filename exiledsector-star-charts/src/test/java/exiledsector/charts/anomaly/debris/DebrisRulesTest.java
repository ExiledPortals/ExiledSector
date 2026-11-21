package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import exiledsector.charts.content.ChartContentPlan.PlannedFleet;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.generation.ChartTiers;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebrisRulesTest {

    @Test
    void aChartPlacesOneToFourFieldsPlusAnyExtra() {
        Set<Integer> counts = new HashSet<>();
        Random random = new Random(5L);
        for (int draw = 0; draw < 200; draw++) {
            counts.add(DebrisRules.fieldCount(random, 0));
        }
        assertEquals(Set.of(1, 2, 3, 4), counts);
        assertTrue(DebrisRules.fieldCount(new Random(1L), 2) >= 3);
        assertTrue(DebrisRules.fieldCount(new Random(1L), -3) >= 1);
    }

    @Test
    void aSingleFieldChartPlacesExactlyOneFieldWhateverTheExtras() {
        Random random = new Random(9L);
        for (int draw = 0; draw < 50; draw++) {
            assertEquals(1, DebrisRules.fieldCount(random, 3, true));
        }
        assertTrue(DebrisRules.fieldCount(new Random(1L), 2, false) >= 3);
    }

    @Test
    void theFleetRevealsOnceThePlayerIsWithinTheMarginOfTheField() {
        float radius = 400f;
        assertTrue(DebrisRules.reveals(radius + DebrisRules.REVEAL_MARGIN, 0f, radius));
        assertTrue(DebrisRules.reveals(radius + DebrisRules.REVEAL_MARGIN + 50f, 60f, radius));
        assertFalse(DebrisRules.reveals(radius + DebrisRules.REVEAL_MARGIN + 100f, 0f, radius));
    }

    @Test
    void anUncorruptedAnomalousFleetIsHalfTheBossFleetOnEveryTier() {
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_TIER; tier++) {
            ChartTier chartTier = ChartGenContext.none().adjust(ChartTiers.forTier(tier));
            float bossSize = Math.max(chartTier.bossSize(), chartTier.fleetSize());
            assertEquals(bossSize * 0.5f, DebrisRules.fleetSize(chartTier, 0, 0f), 0.001f, "tier " + tier);
            PlannedFleet boss = ChartContentPlanner.plannedBoss(chartTier, ChartContentPlanner.NO_POI);
            assertTrue(DebrisRules.fleetPoints(chartTier, 0, 0f) < boss.strength().combatPoints(), "tier " + tier);
            assertTrue(DebrisRules.quality(chartTier, 0) >= Math.min(1f, boss.strength().quality()), "tier " + tier);
        }
    }

    @Test
    void corruptionMakesAnAnomalousFleetLargerStrongerAndBetterKept() {
        ChartTier tier = ChartTier.fallback(5);
        assertEquals(DebrisRules.fleetPoints(tier, 0, 0f) * DebrisRules.CORRUPTED_SIZE_MULT, DebrisRules.fleetPoints(tier, 1, 0f), 0.01f);
        assertTrue(DebrisRules.strengthBonus(tier, 1, 0f) >= DebrisRules.strengthBonus(tier, 0, 0f) + DebrisRules.CORRUPTED_STRENGTH_BONUS_PERCENT);
        assertTrue(DebrisRules.quality(tier, 1) >= DebrisRules.quality(tier, 0));
        assertTrue(DebrisRules.quality(ChartTier.fallback(15), 1) <= 1f);
    }

    @Test
    void aSecondCorruptionStrengthensTheFleetAgainByTheSameStep() {
        ChartTier tier = ChartTier.fallback(3);
        float once = DebrisRules.fleetSize(tier, 1, 0f);
        assertEquals(once * DebrisRules.CORRUPTED_SIZE_MULT, DebrisRules.fleetSize(tier, 2, 0f), 0.001f);
        assertEquals(DebrisRules.fleetSize(tier, 2, 0f), DebrisRules.fleetSize(tier, 5, 0f), 0.001f);
        assertTrue(DebrisRules.strengthBonus(tier, 2, 0f) >= DebrisRules.strengthBonus(tier, 1, 0f) + DebrisRules.CORRUPTED_STRENGTH_BONUS_PERCENT);
        assertTrue(DebrisRules.quality(tier, 2) >= DebrisRules.quality(tier, 1));
    }

    @Test
    void increasedFleetSizeScalesTheFleetAndNeverDropsItBelowNothing() {
        ChartTier tier = ChartTier.fallback(4);
        float base = DebrisRules.fleetSize(tier, 0, 0f);
        assertEquals(base * 2f, DebrisRules.fleetSize(tier, 0, 100f), 0.001f);
        assertEquals(base * 1.5f * 1.3f, DebrisRules.fleetSize(tier, 1, 30f), 0.001f);
        assertEquals(0f, DebrisRules.fleetSize(tier, 0, -150f), 0.001f);
    }

    @Test
    void engramCountsScaleWithQuantityAndNeverGoNegative() {
        Random random = new Random(2L);
        assertEquals(4, DebrisRules.craftingItemCount(4, 1f, random));
        assertEquals(6, DebrisRules.craftingItemCount(3, 2f, random));
        assertEquals(0, DebrisRules.craftingItemCount(-2, 1f, random));
        assertEquals(0, DebrisRules.craftingItemCount(3, -1f, random));
    }

    @Test
    void coreLevelsRiseWithTierAndCorruptionStepsEachCoreUpOnce() {
        Random random = new Random(3L);
        Set<String> lowTier = new HashSet<>();
        Set<String> highTier = new HashSet<>();
        for (int draw = 0; draw < 200; draw++) {
            lowTier.add(DebrisRules.coreId(DebrisRules.coreLevel(1, random), 0));
            highTier.add(DebrisRules.coreId(DebrisRules.coreLevel(15, random), 0));
        }
        assertEquals(Set.of(Commodities.GAMMA_CORE, Commodities.BETA_CORE), lowTier);
        assertEquals(Set.of(Commodities.BETA_CORE, Commodities.ALPHA_CORE), highTier);
        assertEquals(Commodities.BETA_CORE, DebrisRules.coreId(0, 1));
        assertEquals(Commodities.ALPHA_CORE, DebrisRules.coreId(0, 2));
        assertEquals(Commodities.ALPHA_CORE, DebrisRules.coreId(2, 1));
    }
}
