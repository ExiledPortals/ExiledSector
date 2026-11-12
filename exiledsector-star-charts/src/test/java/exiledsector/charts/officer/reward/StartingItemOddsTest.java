package exiledsector.charts.officer.reward;

import exiledsector.charts.ChartTables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StartingItemOddsTest {

    private static final String LOW_SWARM = "exiledSector_augmentation_nanoswarm";
    private static final String OTHER_LOW_SWARM = "exiledSector_reshaping_nanoswarm";
    private static final String MID_SWARM = "exiledSector_fundamental_nanoswarm";
    private static final String HIGH_SEED = "exiledSector_replicator_nanoseed";
    private static final float EPSILON = 1e-4f;
    private static final Map<RewardKind, Float> POOL_SHARES = Map.of(RewardKind.HULL_ITEM, 35f, RewardKind.BIOME_HULL_ITEM, 15f,
            RewardKind.CRAFTING_ITEM, 30f, RewardKind.INHIBITOR, 7f, RewardKind.ENGRAM, 8f, RewardKind.UNIQUE, 5f);

    @BeforeEach
    void setUp() throws Exception {
        OfficerRewardTable.register(ChartTables.csvRows(OfficerRewardTable.PATH));
    }

    @AfterEach
    void tearDown() {
        OfficerRewardTable.clear();
    }

    private static RewardCandidate candidate(RewardKind kind, String id, float weight) {
        return new RewardCandidate(CarriedItem.of(kind, id), weight);
    }

    private static Map<RewardKind, List<RewardCandidate>> pool() {
        return Map.of(
                RewardKind.HULL_ITEM, List.of(candidate(RewardKind.HULL_ITEM, "plain_bridge", 1f)),
                RewardKind.CRAFTING_ITEM, List.of(candidate(RewardKind.CRAFTING_ITEM, LOW_SWARM, 2f), candidate(RewardKind.CRAFTING_ITEM, OTHER_LOW_SWARM, 6f),
                        candidate(RewardKind.CRAFTING_ITEM, MID_SWARM, 1f), candidate(RewardKind.CRAFTING_ITEM, HIGH_SEED, 4f)),
                RewardKind.UNIQUE, List.of(candidate(RewardKind.UNIQUE, "relic", 1f)));
    }

    @Test
    void aRanksUniqueShareReplacesThePoolsAndTheOtherKindsKeepTheirProportions() {
        Map<RewardKind, Float> shares = new StartingItemOdds(20f, 1f, 1f, 1f).shares(POOL_SHARES);

        assertEquals(20f, shares.get(RewardKind.UNIQUE), EPSILON);
        assertEquals(35f * 80f / 95f, shares.get(RewardKind.HULL_ITEM), EPSILON);
        assertEquals(8f * 80f / 95f, shares.get(RewardKind.ENGRAM), EPSILON);
        assertEquals(100f, (float) shares.values().stream().mapToDouble(Float::doubleValue).sum(), EPSILON);
    }

    @Test
    void theUniqueShareIsClampedToAPercent() {
        assertEquals(100f, new StartingItemOdds(250f, 1f, 1f, 1f).uniqueSharePercent(), EPSILON);
        assertEquals(0f, new StartingItemOdds(-5f, 1f, 1f, 1f).uniqueSharePercent(), EPSILON);
    }

    @Test
    void craftingItemsAreReweightedByTheirTierAndKeepTheirShareWithinIt() {
        List<RewardCandidate> reweighted = new StartingItemOdds(0f, 1f, 2f, 0f).reweighted(pool()).get(RewardKind.CRAFTING_ITEM);

        assertEquals(List.of(0.25f, 0.75f, 2f, 0f), reweighted.stream().map(RewardCandidate::weight).toList());
        assertEquals(pool().get(RewardKind.HULL_ITEM), new StartingItemOdds(0f, 1f, 2f, 0f).reweighted(pool()).get(RewardKind.HULL_ITEM));
    }

    @Test
    void aFullUniqueShareAlwaysStartsTheOfficerWithAUnique() {
        StartingItemOdds odds = new StartingItemOdds(100f, 1f, 1f, 1f);
        Random random = new Random(8L);
        for (int roll = 0; roll < 200; roll++) {
            assertEquals(CarriedItem.of(RewardKind.UNIQUE, "relic"), OfficerRewardPool.startingItem(pool(), POOL_SHARES, odds, random));
        }
    }

    @Test
    void aRankWithOnlyHighTierWeightsStartsWithOnlyHighTierCraftingItems() {
        StartingItemOdds odds = new StartingItemOdds(0f, 0f, 0f, 1f);
        Map<RewardKind, Float> craftingOnly = Map.of(RewardKind.CRAFTING_ITEM, 30f);
        Random random = new Random(8L);
        for (int roll = 0; roll < 200; roll++) {
            assertEquals(CarriedItem.of(RewardKind.CRAFTING_ITEM, HIGH_SEED), OfficerRewardPool.startingItem(pool(), craftingOnly, odds, random));
        }
    }

    @Test
    void aStartingCraftingItemRollsItsHiddenCount() {
        StartingItemOdds odds = new StartingItemOdds(0f, 1f, 0f, 0f);
        Map<RewardKind, Float> craftingOnly = Map.of(RewardKind.CRAFTING_ITEM, 30f);
        Set<Integer> counts = new TreeSet<>();
        Random random = new Random(8L);
        for (int roll = 0; roll < 1000; roll++) {
            counts.add(OfficerRewardPool.startingItem(pool(), craftingOnly, odds, random).baseCount());
        }

        assertEquals(Set.of(1, 2, 3, 4, 5, 6, 7), counts);
    }

    @Test
    void aRankWithNothingToDrawStartsEmptyHanded() {
        assertNull(OfficerRewardPool.startingItem(pool(), Map.of(RewardKind.CRAFTING_ITEM, 30f), new StartingItemOdds(0f, 0f, 0f, 0f), new Random(1L)));
    }
}
