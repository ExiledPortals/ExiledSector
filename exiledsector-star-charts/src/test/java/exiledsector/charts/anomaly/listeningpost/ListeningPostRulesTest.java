package exiledsector.charts.anomaly.listeningpost;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.loot.ChartLootContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListeningPostRulesTest {

    private static final float EPSILON = 1e-5f;

    @Test
    void suspicionBuildsInContactDrainsOutOfItAndStaysBetweenNoneAndFull() {
        assertEquals(ListeningPostRules.SUSPICION_PER_DAY, ListeningPostRules.suspicionAfter(0f, true, 1f), EPSILON);
        assertEquals(0.5f - ListeningPostRules.SUSPICION_DRAIN_PER_DAY, ListeningPostRules.suspicionAfter(0.5f, false, 1f), EPSILON);
        assertEquals(1f, ListeningPostRules.suspicionAfter(0.9f, true, 10f));
        assertEquals(0f, ListeningPostRules.suspicionAfter(0.1f, false, 10f));
    }

    @Test
    void theVaultPurgesAShareADayDownToItsFloor() {
        assertEquals(1f - ListeningPostRules.PURGE_PER_DAY, ListeningPostRules.vaultAfter(1f, 1f, 0f), EPSILON);
        assertEquals(ListeningPostRules.VAULT_FLOOR, ListeningPostRules.vaultAfter(1f, 100f, 0f), EPSILON);
        assertEquals(0.1f, ListeningPostRules.vaultAfter(0.1f, 1f, 0f), EPSILON);
    }

    @Test
    void thePurgeLineSlowsThePurgeAndNeverReversesIt() {
        assertEquals(1f - ListeningPostRules.PURGE_PER_DAY / 2f, ListeningPostRules.vaultAfter(1f, 1f, 50f), EPSILON);
        assertEquals(1f, ListeningPostRules.vaultAfter(1f, 1f, 150f), EPSILON);
        assertEquals(ListeningPostRules.PURGE_PER_DAY * 1.5f, ListeningPostRules.purgeRate(-50f), EPSILON);
    }

    @Test
    void picketCountsStayInRangeAndTheExtraLineIsCapped() {
        Random random = new Random(5L);
        for (int roll = 0; roll < 200; roll++) {
            int count = ListeningPostRules.picketCount(random, 0);
            assertTrue(count >= ListeningPostRules.MIN_PICKETS && count <= ListeningPostRules.MAX_PICKETS, String.valueOf(count));
        }
        assertTrue(ListeningPostRules.picketCount(random, 100) <= ListeningPostRules.MAX_PICKETS + ListeningPostRules.MAX_EXTRA_PICKETS);
        assertTrue(ListeningPostRules.picketCount(random, -4) >= ListeningPostRules.MIN_PICKETS);
    }

    @Test
    void stolenChartsNumberOneToTheLineAndSitWithinTwoTiersOfTheRun() {
        Random random = new Random(5L);
        boolean sawLowest = false;
        boolean sawHighest = false;
        for (int roll = 0; roll < 200; roll++) {
            int count = ListeningPostRules.stolenChartCount(random, 3);
            assertTrue(count >= 1 && count <= 3, String.valueOf(count));
            int tier = ListeningPostRules.stolenChartTier(random, 8);
            assertTrue(tier >= 6 && tier <= 10, String.valueOf(tier));
            sawLowest |= tier == 6;
            sawHighest |= tier == 10;
            int lowTier = ListeningPostRules.stolenChartTier(random, 1);
            assertTrue(lowTier >= ChartTiers.LOWEST_TIER && lowTier <= 3, String.valueOf(lowTier));
            int topTier = ListeningPostRules.stolenChartTier(random, ChartTiers.HIGHEST_DROP_TIER);
            assertTrue(topTier >= ChartTiers.HIGHEST_DROP_TIER - 2 && topTier <= ChartTiers.HIGHEST_DROP_TIER, String.valueOf(topTier));
        }
        assertTrue(sawLowest && sawHighest);
        assertEquals(0, ListeningPostRules.stolenChartCount(random, 0));
    }

    @Test
    void theVaultGrowsWithTierAndItsQuantityJoinsTheChartsItemQuantity() {
        assertEquals(ListeningPostRules.LOOT_STEPS, VaultLoot.LOOT.steps(1), EPSILON);
        assertTrue(VaultLoot.LOOT.steps(10) > VaultLoot.LOOT.steps(1));
        ChartGenContext context = ChartGenContext.none().withEffects(List.of(
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_QUANTITY_PERCENT, 20f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_LISTENING_POST_LOOT_QUANTITY_PERCENT, 30f)));
        ChartLootContext vaultLoot = VaultLoot.LOOT.withQuantityAndLeans(new ChartLootContext(context, 1), List.of());
        assertEquals(1.5f, vaultLoot.itemChances().general(), EPSILON);
        assertEquals(0f, VaultLoot.LOOT.quantityPercent(ChartGenContext.none()), EPSILON);
    }
}
