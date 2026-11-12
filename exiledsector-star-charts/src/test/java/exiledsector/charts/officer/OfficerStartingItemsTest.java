package exiledsector.charts.officer;

import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerStartingItemsTest {

    private static AtlasOfficer officer(String rankId) {
        return AtlasOfficer.waiting(4, "hegemony", rankId, "steady", new AtlasOfficer.OfficerSeeds(1L, 2L, 3L, 4L), null);
    }

    @Test
    void aDealtOfficerCarriesTheRolledItem() {
        CarriedItem engram = CarriedItem.of(RewardKind.ENGRAM, "engram_a");

        AtlasOfficer dealt = OfficerStartingItems.dealt(officer("captain"), (officer, random) -> engram, new Random(1L));

        assertEquals(List.of(engram), dealt.carried());
    }

    @Test
    void aRolledItemStacksOntoAMatchingCarriedOne() {
        CarriedItem kernel = CarriedItem.of(RewardKind.CRAFTING_ITEM, "kernel");
        AtlasOfficer carrying = officer("captain").withCarried(List.of(kernel));

        AtlasOfficer dealt = OfficerStartingItems.dealt(carrying, (officer, random) -> kernel, new Random(1L));

        assertEquals(List.of(kernel.withCount(2)), dealt.carried());
    }

    @Test
    void anEmptyRollLeavesTheOfficerUnchanged() {
        AtlasOfficer officer = officer("lieutenant");

        assertSame(officer, OfficerStartingItems.dealt(officer, StartingItemRoll.NONE, new Random(1L)));
    }

    @Test
    void theRewardTierRunsFromTheLowestForLieutenantsToTheHighestForAdmirals() {
        assertEquals(ChartTiers.LOWEST_TIER, OfficerStartingItems.tierFor("lieutenant"));
        assertEquals(ChartTiers.HIGHEST_TIER, OfficerStartingItems.tierFor("admiral"));
        int previousTier = 0;
        for (OfficerRank rank : OfficerRanks.all()) {
            int tier = OfficerStartingItems.tierFor(rank.id());
            assertTrue(tier > previousTier, rank.id());
            previousTier = tier;
        }
    }
}
