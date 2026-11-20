package exiledsector.charts.officer.turn;

import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class OfficerBoonsTest {

    private static final CarriedItem KERNELS = new CarriedItem(RewardKind.CRAFTING_ITEM, "kernel", null, 4);
    private static final TurnRules RULES = new TurnRules(4, 6f, 50f, 6.25f, 10f, 1, 3, 2, 0, 50, 6, 2, 3, Map.of());

    private static OfficerBoons duplicating(float trophyPercent, float scavengePercent) {
        return new OfficerBoons(trophyPercent, scavengePercent, false, 0f, 0f, false);
    }

    @Test
    void aCertainDuplicationDoublesEveryItemOfAStack() {
        OfficerBoons boons = duplicating(100f, 100f);

        assertEquals(List.of(KERNELS.withCount(8)), boons.withTrophyDuplicates(List.of(KERNELS), new Random(1L)));
        assertEquals(KERNELS.withCount(8), boons.withScavengeDuplicates(KERNELS, new Random(1L)));
    }

    @Test
    void aStackRollsEachOfItsItemsOnItsOwn() {
        Random random = new Random(7L);
        int lowest = Integer.MAX_VALUE;
        int highest = 0;
        for (int roll = 0; roll < 400; roll++) {
            int count = OfficerBoons.duplicated(KERNELS, 50f, random).baseCount();
            lowest = Math.min(lowest, count);
            highest = Math.max(highest, count);
        }
        assertEquals(4, lowest);
        assertEquals(8, highest);
    }

    @Test
    void noDuplicationLeavesItemsAlone() {
        List<CarriedItem> taken = List.of(KERNELS);

        assertSame(taken, OfficerBoons.NONE.withTrophyDuplicates(taken, new Random(1L)));
        assertSame(KERNELS, OfficerBoons.NONE.withScavengeDuplicates(KERNELS, new Random(1L)));
        assertNull(duplicating(0f, 100f).withScavengeDuplicates(null, new Random(1L)));
    }

    @Test
    void increasedFightsAndScavengersScaleTheTurnsCounts() {
        TurnRules boosted = new OfficerBoons(0f, 0f, false, 100f, 100f, false).applyTo(RULES);

        assertEquals(12, boosted.maxConflictsPerTurn());
        assertEquals(4, boosted.scavengersMin());
        assertEquals(6, boosted.scavengersMax());
        assertEquals(RULES.intentWeights(), boosted.intentWeights());
        assertEquals(2f, new OfficerBoons(0f, 0f, false, 100f, 0f, false).fightWeightMult());
    }

    @Test
    void noBoonsKeepTheRules() {
        assertEquals(RULES, OfficerBoons.NONE.applyTo(RULES));
        assertEquals(1f, OfficerBoons.NONE.fightWeightMult());
    }
}
