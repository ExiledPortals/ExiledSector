package exiledsector.charts.phasing;

import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.officer.OfficerRank;
import exiledsector.charts.officer.OfficerRanks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplacementTheoremTest {

    private static final int ROLLS = 2000;

    @AfterEach
    void restoreRanks() {
        OfficerRanks.useForTests(List.of());
    }

    private static int drops(String rankId, int tier) {
        Random random = new Random(7L);
        int dropped = 0;
        for (int roll = 0; roll < ROLLS; roll++) {
            if (DisplacementTheorem.drops(rankId, tier, random)) {
                dropped++;
            }
        }
        return dropped;
    }

    @Test
    void onlyTheTopRankCounts() {
        assertTrue(DisplacementTheorem.isTopRank("admiral"));
        assertFalse(DisplacementTheorem.isTopRank("commodore"));
        assertFalse(DisplacementTheorem.isTopRank(null));
        OfficerRanks.useForTests(List.of(new OfficerRank("ensign", 0, 0, 1, 1), new OfficerRank("grand_admiral", 0, 0, 1, 1)));
        assertTrue(DisplacementTheorem.isTopRank("grand_admiral"));
        assertFalse(DisplacementTheorem.isTopRank("admiral"));
    }

    @Test
    void admiralsDropItFromTierFifteenUpAtAboutThirtyPercent() {
        int highest = drops("admiral", ChartTiers.HIGHEST_DROP_TIER);
        int corrupted = drops("admiral", ChartTiers.HIGHEST_TIER);
        assertTrue(highest > ROLLS * 0.25f && highest < ROLLS * 0.35f, String.valueOf(highest));
        assertTrue(corrupted > ROLLS * 0.25f && corrupted < ROLLS * 0.35f, String.valueOf(corrupted));
    }

    @Test
    void lowerTiersAndLowerRanksNeverDropIt() {
        assertEquals(0, drops("admiral", ChartTiers.HIGHEST_DROP_TIER - 1));
        assertEquals(0, drops("commodore", ChartTiers.HIGHEST_TIER));
    }
}
