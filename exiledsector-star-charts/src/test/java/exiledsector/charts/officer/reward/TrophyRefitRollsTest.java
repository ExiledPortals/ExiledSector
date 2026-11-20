package exiledsector.charts.officer.reward;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrophyRefitRollsTest {

    private static final CarriedItem UNIQUE = CarriedItem.of(RewardKind.UNIQUE, "unique_a");
    private static final CarriedItem BIOME_HULL_ITEM = CarriedItem.of(RewardKind.BIOME_HULL_ITEM, "biome_a");
    private static final CarriedItem ENGRAM_A = CarriedItem.of(RewardKind.ENGRAM, "engram_a");
    private static final CarriedItem ENGRAM_B = CarriedItem.of(RewardKind.ENGRAM, "engram_b");

    @AfterEach
    void tearDown() {
        OfficerRewardTable.clear();
    }

    private static JSONObject craftingRow(String id, String tier) throws Exception {
        return new JSONObject().put("id", id).put("category", "crafting_item").put("share", "").put("tier", tier);
    }

    private static RewardCandidate candidate(RewardKind kind, String id) {
        return new RewardCandidate(new CarriedItem(kind, id, null, 3), 1f);
    }

    @Test
    void uniquesAndBiomeHullItemsAreKept() {
        Map<RewardKind, List<RewardCandidate>> candidates = Map.of(RewardKind.UNIQUE, List.of(candidate(RewardKind.UNIQUE, "unique_b")));

        assertEquals(List.of(UNIQUE, BIOME_HULL_ITEM), TrophyRefitRolls.refitted(List.of(UNIQUE, BIOME_HULL_ITEM), candidates, 65f, new Random(1L)));
    }

    @Test
    void eachItemOfAStackRerollsOnItsOwnAndLikeResultsStackAgain() {
        Map<RewardKind, List<RewardCandidate>> candidates = Map.of(RewardKind.ENGRAM,
                List.of(candidate(RewardKind.ENGRAM, "engram_a"), candidate(RewardKind.ENGRAM, "engram_b")));
        Set<List<CarriedItem>> outcomes = new HashSet<>();
        Random random = new Random(3L);
        for (int refit = 0; refit < 80; refit++) {
            List<CarriedItem> refitted = TrophyRefitRolls.refitted(List.of(ENGRAM_A.withCount(4)), candidates, 25f, random);
            assertEquals(4, refitted.stream().mapToInt(CarriedItem::baseCount).sum());
            assertTrue(refitted.stream().allMatch(item -> item.kind() == RewardKind.ENGRAM));
            outcomes.add(refitted);
        }
        assertTrue(outcomes.contains(List.of(ENGRAM_A.withCount(4))));
        assertTrue(outcomes.contains(List.of(ENGRAM_B.withCount(4))));
        assertTrue(outcomes.stream().anyMatch(outcome -> outcome.size() == 2));
    }

    @Test
    void aCraftingItemRerollsOnlyIntoItsTierOrHigher() throws Exception {
        OfficerRewardTable.register(new JSONArray().put(craftingRow("low_item", "low")).put(craftingRow("mid_item", "mid"))
                .put(craftingRow("high_item", "high")));
        Map<RewardKind, List<RewardCandidate>> candidates = Map.of(RewardKind.CRAFTING_ITEM, List.of(candidate(RewardKind.CRAFTING_ITEM, "low_item"),
                candidate(RewardKind.CRAFTING_ITEM, "mid_item"), candidate(RewardKind.CRAFTING_ITEM, "high_item")));
        Set<String> rerolledIds = new HashSet<>();
        Random random = new Random(9L);
        for (int refit = 0; refit < 200; refit++) {
            rerolledIds.add(TrophyRefitRolls.rerolled(CarriedItem.of(RewardKind.CRAFTING_ITEM, "mid_item"), candidates, 0f, random).id());
        }
        assertEquals(Set.of("mid_item", "high_item"), rerolledIds);
    }

    @Test
    void aFinalCountItemStaysAFinalCountItem() {
        CarriedItem feedstock = CarriedItem.withFinalCount(RewardKind.FEEDSTOCK, "threat_feedstock", 1);
        Map<RewardKind, List<RewardCandidate>> candidates = Map.of(RewardKind.FEEDSTOCK, List.of(candidate(RewardKind.FEEDSTOCK, "shroud_feedstock")));

        CarriedItem rerolled = TrophyRefitRolls.rerolled(feedstock, candidates, 0f, new Random(1L));

        assertEquals(CarriedItem.withFinalCount(RewardKind.FEEDSTOCK, "shroud_feedstock", 1), rerolled);
    }

    @Test
    void anItemWithNothingToRerollIntoIsKept() {
        assertEquals(ENGRAM_A, TrophyRefitRolls.rerolled(ENGRAM_A, Map.of(), 0f, new Random(1L)));
        assertEquals(CarriedItem.of(RewardKind.HULL_ITEM, "unknown_hull_item"),
                TrophyRefitRolls.rerolled(CarriedItem.of(RewardKind.HULL_ITEM, "unknown_hull_item"), Map.of(), 1f, new Random(1L)));
    }
}
