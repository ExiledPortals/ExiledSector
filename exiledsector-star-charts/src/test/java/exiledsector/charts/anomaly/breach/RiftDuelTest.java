package exiledsector.charts.anomaly.breach;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiftDuelTest {

    private static final List<CarriedItem> CARRIED = List.of(CarriedItem.of(RewardKind.STAR_CHART, "chart_a"),
            CarriedItem.of(RewardKind.UNIQUE, "unique_b"), CarriedItem.of(RewardKind.STAR_CHART, "chart_c"));

    @Test
    void theDuelReadsHowManyRewardsAreDuplicatedAndHowManyExtraOffersAreMade() {
        ChartGenContext context = ChartGenContext.of(List.of(
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_BREACH_OFFICER_DUPLICATED_REWARDS, 2f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_BREACH_OFFICER_EXTRA_OFFERS, 1f)));

        assertEquals(new RiftDuel(2, 1), RiftDuel.of(context));
        assertEquals(RiftDuel.NONE, RiftDuel.of(ChartGenContext.none()));
        assertEquals(RiftDuel.NONE, RiftDuel.at(null, null));
    }

    @Test
    void duplicatedRewardsAreDistinctCardEntriesPickedTheSameWayEveryTime() {
        for (long seed = 0L; seed < 30L; seed++) {
            List<CarriedItem> picked = RiftDuel.duplicated(CARRIED, 2, new Random(seed));
            assertEquals(2, picked.size());
            assertEquals(2, new HashSet<>(picked).size());
            assertTrue(CARRIED.containsAll(picked));
            assertEquals(picked, RiftDuel.duplicated(CARRIED, 2, new Random(seed)));
        }
        assertEquals(CARRIED.size(), RiftDuel.duplicated(CARRIED, 5, new Random(1L)).size());
        assertTrue(RiftDuel.duplicated(null, 2, new Random(1L)).isEmpty());
        assertSame(exiledsector.loot.Loot.NONE, RiftDuel.NONE.duplicatedLoot(null, null));
    }
}
