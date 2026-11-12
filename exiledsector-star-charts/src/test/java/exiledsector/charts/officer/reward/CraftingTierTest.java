package exiledsector.charts.officer.reward;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CraftingTierTest {

    @Test
    void eachTierRollsEveryCountFromOneToItsMaximum() {
        assertEquals(Set.of(1, 2, 3, 4, 5, 6, 7), rolledCounts(CraftingTier.LOW));
        assertEquals(Set.of(1, 2, 3), rolledCounts(CraftingTier.MID));
        assertEquals(Set.of(1), rolledCounts(CraftingTier.HIGH));
    }

    private static Set<Integer> rolledCounts(CraftingTier tier) {
        Random random = new Random(11L);
        Set<Integer> counts = new TreeSet<>();
        for (int roll = 0; roll < 2000; roll++) {
            counts.add(tier.rollCount(random));
        }
        return counts;
    }
}
