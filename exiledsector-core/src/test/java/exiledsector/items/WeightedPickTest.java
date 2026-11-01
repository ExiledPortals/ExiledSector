package exiledsector.items;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeightedPickTest {

    @Test
    void anEmptyOrZeroWeightMapPicksNothingAndDrawsNoRoll() {
        Random random = new Random(1);
        long untouched = new Random(1).nextLong();

        assertNull(WeightedPick.pick(Map.of(), random));
        assertNull(WeightedPick.pick(Map.of("a", 0f, "b", -2f), random));
        assertEquals(untouched, random.nextLong());
    }

    @Test
    void mapPicksFollowTheirWeightsAndSkipNonPositiveEntries() {
        Random random = new Random(2L);
        Map<String, Integer> picks = new HashMap<>();
        for (int roll = 0; roll < 4000; roll++) {
            picks.merge(WeightedPick.pick(new TreeMap<>(Map.of("a", 1f, "b", 3f, "c", 0f)), random), 1, Integer::sum);
        }

        assertFalse(picks.containsKey("c"));
        assertTrue(picks.get("b") > picks.get("a") * 2.5f && picks.get("b") < picks.get("a") * 3.5f, picks.toString());
    }

    @Test
    void aMapPickMatchesTheListPickOverItsPositiveEntriesInOrder() {
        Map<String, Float> weights = new LinkedHashMap<>();
        weights.put("first", 2f);
        weights.put("skipped", 0f);
        weights.put("second", 5f);
        for (long seed = 0; seed < 50; seed++) {
            String fromList = WeightedPick.pick(List.of("first", "second"), weights::get, new Random(seed));

            assertEquals(fromList, WeightedPick.pick(weights, new Random(seed)));
        }
    }
}
