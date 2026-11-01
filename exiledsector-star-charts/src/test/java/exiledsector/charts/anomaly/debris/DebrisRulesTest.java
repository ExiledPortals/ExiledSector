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
    void theFleetRevealsOnceThePlayerIsWithinTheMarginOfTheField() {
        float radius = 400f;
        assertTrue(DebrisRules.reveals(radius + DebrisRules.REVEAL_MARGIN, 0f, radius));
        assertTrue(DebrisRules.reveals(radius + DebrisRules.REVEAL_MARGIN + 50f, 60f, radius));
        assertFalse(DebrisRules.reveals(radius + DebrisRules.REVEAL_MARGIN + 100f, 0f, radius));
    }

    @Test
    void aDormantFleetOutgunsTheBossOnEveryTier() {
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.CORRUPTED_TIER; tier++) {
            ChartTier chartTier = ChartGenContext.none().adjust(ChartTiers.forTier(tier));
            PlannedFleet boss = ChartContentPlanner.plannedBoss(chartTier, ChartContentPlanner.fleetQuality(chartTier), ChartContentPlanner.NO_POI);

            float points = DebrisRules.fleetPoints(chartTier, false);
            assertTrue(points >= boss.combatPoints(), "tier " + tier);
            assertTrue(points > boss.combatPoints() || DebrisRules.strengthBonus(chartTier, false) > boss.strengthBonus(), "tier " + tier);
            assertTrue(DebrisRules.quality(chartTier, false) >= Math.min(1f, boss.quality()), "tier " + tier);
        }
    }

    @Test
    void corruptionMakesADormantFleetLargerStrongerAndBetterKept() {
        ChartTier tier = ChartTier.fallback(5);
        assertEquals(DebrisRules.fleetPoints(tier, false) * DebrisRules.CORRUPTED_SIZE_MULT, DebrisRules.fleetPoints(tier, true), 0.01f);
        assertTrue(DebrisRules.strengthBonus(tier, true) >= DebrisRules.strengthBonus(tier, false) + DebrisRules.CORRUPTED_STRENGTH_BONUS_PERCENT);
        assertTrue(DebrisRules.quality(tier, true) >= DebrisRules.quality(tier, false));
        assertTrue(DebrisRules.quality(ChartTier.fallback(15), true) <= 1f);
    }

    @Test
    void coreLevelsRiseWithTierAndCorruptionStepsEachCoreUpOnce() {
        Random random = new Random(3L);
        Set<String> lowTier = new HashSet<>();
        Set<String> highTier = new HashSet<>();
        for (int draw = 0; draw < 200; draw++) {
            lowTier.add(DebrisRules.coreId(DebrisRules.coreLevel(1, random), false));
            highTier.add(DebrisRules.coreId(DebrisRules.coreLevel(15, random), false));
        }
        assertEquals(Set.of(Commodities.GAMMA_CORE, Commodities.BETA_CORE), lowTier);
        assertEquals(Set.of(Commodities.BETA_CORE, Commodities.ALPHA_CORE), highTier);
        assertEquals(Commodities.BETA_CORE, DebrisRules.coreId(0, true));
        assertEquals(Commodities.ALPHA_CORE, DebrisRules.coreId(2, true));
    }
}
