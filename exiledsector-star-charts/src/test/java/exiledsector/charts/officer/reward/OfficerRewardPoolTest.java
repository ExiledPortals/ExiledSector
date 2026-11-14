package exiledsector.charts.officer.reward;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import exiledsector.charts.ChartTables;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.charts.loot.ChartMaterials;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemModifiers;
import exiledsector.items.ItemUnlock;
import exiledsector.items.ModifierSources;
import exiledsector.items.PoolEntry;
import exiledsector.items.StarChartCipher;
import exiledsector.items.StarChartFixtures;
import exiledsector.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OfficerRewardPoolTest {

    private static final String RIFT = "abyssal_rift";
    private static final String BELT = "storm_belt";
    private static final String CHRONAL = "TIMEFLOW_PERCENT_ABYSSAL_RIFT";
    private static final String CHARGED = "ARMOR_PERCENT_STORM_BELT";
    private static final String BRIDGE = "test_bridge";
    private static final String EMITTER = "test_emitter";
    private static final int DRAWS = 20000;
    private static final double TOLERANCE = 0.02;

    @AfterEach
    void tearDown() {
        OfficerRewardTable.clear();
        BiomeModifiers.unregisterAll();
        ModifierSources.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
        ChartMaterials.clear();
        StarChartFixtures.unregisterCraftingItems();
    }

    private static RewardCandidate candidate(RewardKind kind, String id, float weight) {
        return new RewardCandidate(CarriedItem.of(kind, id), weight);
    }

    @Test
    void offersNeverRepeatAnItem() {
        Map<RewardKind, List<RewardCandidate>> candidates = Map.of(
                RewardKind.HULL_ITEM, List.of(candidate(RewardKind.HULL_ITEM, "a", 5f), candidate(RewardKind.HULL_ITEM, "b", 1f)),
                RewardKind.CRAFTING_ITEM, List.of(candidate(RewardKind.CRAFTING_ITEM, "c", 9f), candidate(RewardKind.CRAFTING_ITEM, "d", 1f)),
                RewardKind.UNIQUE, List.of(candidate(RewardKind.UNIQUE, "e", 1f)));
        Map<RewardKind, Float> shares = Map.of(RewardKind.HULL_ITEM, 50f, RewardKind.CRAFTING_ITEM, 45f, RewardKind.UNIQUE, 5f);
        Random random = new Random(3L);
        for (int draw = 0; draw < 500; draw++) {
            List<CarriedItem> offers = OfficerRewardPool.offers(candidates, shares, OfficerRewardPool.OFFER_COUNT, random);

            assertEquals(OfficerRewardPool.OFFER_COUNT, offers.size());
            assertEquals(OfficerRewardPool.OFFER_COUNT, new HashSet<>(offers).size());
        }
    }

    @Test
    void theFirstOfferFollowsTheKindSharesThenTheWeightsWithinAKind() {
        Map<RewardKind, List<RewardCandidate>> candidates = Map.of(
                RewardKind.HULL_ITEM, List.of(candidate(RewardKind.HULL_ITEM, "a", 3f), candidate(RewardKind.HULL_ITEM, "b", 1f)),
                RewardKind.CRAFTING_ITEM, List.of(candidate(RewardKind.CRAFTING_ITEM, "c", 100f)));
        Map<RewardKind, Float> shares = Map.of(RewardKind.HULL_ITEM, 75f, RewardKind.CRAFTING_ITEM, 25f);
        Map<String, Integer> picks = new HashMap<>();
        Random random = new Random(7L);
        for (int draw = 0; draw < DRAWS; draw++) {
            picks.merge(OfficerRewardPool.offers(candidates, shares, 1, random).get(0).id(), 1, Integer::sum);
        }

        assertEquals(0.5625, picks.get("a") / (double) DRAWS, TOLERANCE);
        assertEquals(0.1875, picks.get("b") / (double) DRAWS, TOLERANCE);
        assertEquals(0.25, picks.get("c") / (double) DRAWS, TOLERANCE);
    }

    @Test
    void kindsWithoutAShareAndCandidatesWithoutWeightAreNeverOffered() {
        Map<RewardKind, List<RewardCandidate>> candidates = Map.of(
                RewardKind.HULL_ITEM, List.of(candidate(RewardKind.HULL_ITEM, "a", 1f), candidate(RewardKind.HULL_ITEM, "weightless", 0f)),
                RewardKind.ENGRAM, List.of(candidate(RewardKind.ENGRAM, "unshared", 1f)),
                RewardKind.UNIQUE, List.of());
        Map<RewardKind, Float> shares = Map.of(RewardKind.HULL_ITEM, 10f, RewardKind.UNIQUE, 90f);

        for (long seed = 0L; seed < 50L; seed++) {
            assertEquals(List.of(CarriedItem.of(RewardKind.HULL_ITEM, "a")),
                    OfficerRewardPool.offers(candidates, shares, OfficerRewardPool.OFFER_COUNT, new Random(seed)));
        }
    }

    private static void registerCraftingTiers() throws Exception {
        OfficerRewardTable.register(new JSONArray()
                .put(new JSONObject().put("id", "low_a").put("category", "crafting_item").put("tier", "low"))
                .put(new JSONObject().put("id", "low_b").put("category", "crafting_item").put("tier", "low"))
                .put(new JSONObject().put("id", "mid_a").put("category", "crafting_item").put("tier", "mid"))
                .put(new JSONObject().put("id", "high_a").put("category", "crafting_item").put("tier", "high")));
    }

    @Test
    void aCategorysShareSplitsOverItsGroupsByTheirItemWeights() throws Exception {
        registerCraftingTiers();
        Map<RewardKind, List<RewardCandidate>> candidates = Map.of(
                RewardKind.CRAFTING_ITEM, List.of(candidate(RewardKind.CRAFTING_ITEM, "low_a", 4f), candidate(RewardKind.CRAFTING_ITEM, "low_b", 2f),
                        candidate(RewardKind.CRAFTING_ITEM, "mid_a", 3f), candidate(RewardKind.CRAFTING_ITEM, "high_a", 1f)),
                RewardKind.ENGRAM, List.of(candidate(RewardKind.ENGRAM, "engram_a", 50f)));

        Map<RewardGroup, Float> shares = OfficerRewardPool.groupShares(OfficerRewardPool.grouped(candidates),
                Map.of(RewardKind.CRAFTING_ITEM, 30f, RewardKind.ENGRAM, 8f));

        assertEquals(Map.of(RewardGroup.LOW_CRAFTING, 18f, RewardGroup.MID_CRAFTING, 9f, RewardGroup.HIGH_CRAFTING, 3f, RewardGroup.ENGRAM, 8f),
                shares);
    }

    @Test
    void theThreeOffersAreThreeDifferentGroups() throws Exception {
        registerCraftingTiers();
        Map<RewardKind, List<RewardCandidate>> candidates = Map.of(
                RewardKind.CRAFTING_ITEM, List.of(candidate(RewardKind.CRAFTING_ITEM, "low_a", 90f), candidate(RewardKind.CRAFTING_ITEM, "low_b", 90f),
                        candidate(RewardKind.CRAFTING_ITEM, "mid_a", 5f), candidate(RewardKind.CRAFTING_ITEM, "high_a", 5f)),
                RewardKind.HULL_ITEM, List.of(candidate(RewardKind.HULL_ITEM, "a", 1f), candidate(RewardKind.HULL_ITEM, "b", 1f)));
        Map<RewardKind, Float> shares = Map.of(RewardKind.CRAFTING_ITEM, 90f, RewardKind.HULL_ITEM, 10f);
        Random random = new Random(11L);
        for (int draw = 0; draw < 500; draw++) {
            List<CarriedItem> offers = OfficerRewardPool.offers(candidates, shares, OfficerRewardPool.OFFER_COUNT, random);

            assertEquals(OfficerRewardPool.OFFER_COUNT, offers.stream().map(RewardGroup::of).distinct().count(), offers.toString());
        }
    }

    @Test
    void theShippedTablesOfferRarerCraftingTiersLessOften() throws Exception {
        OfficerRewardTable.register(ChartTables.csvRows(OfficerRewardTable.PATH));
        StarChartCipher.registerAll();
        ChartMaterials.register(ChartTables.csvRows(ChartMaterials.PATH));
        for (int tier : new int[]{1, 8, 15}) {
            RewardContext context = new RewardContext(tier, null, null, biomeId -> false, definition -> true, itemId -> true);

            Map<RewardGroup, Float> shares = OfficerRewardPool.groupShares(OfficerRewardPool.grouped(OfficerRewardPool.candidates(context,
                    new Random(1L))), OfficerRewardTable.shares());

            float low = shares.get(RewardGroup.LOW_CRAFTING);
            float mid = shares.get(RewardGroup.MID_CRAFTING);
            float high = shares.get(RewardGroup.HIGH_CRAFTING);
            assertTrue(low > mid && mid > high && high > 0f, tier + ": " + shares);
            assertEquals(30f, low + mid + high, 0.001f);
            assertEquals(8f, shares.get(RewardGroup.FEEDSTOCK), 0.001f);
            assertEquals(7f, shares.get(RewardGroup.INHIBITOR), 0.001f);
        }
    }

    @Test
    void pickingAFeedstockRollsItsCountFromItsRow() throws Exception {
        OfficerRewardTable.register(ChartTables.csvRows(OfficerRewardTable.PATH));
        Random random = new Random(23L);
        TreeSet<Integer> counts = new TreeSet<>();
        for (int pick = 0; pick < 2000; pick++) {
            counts.add(OfficerRewardPool.picked(CarriedItem.of(RewardKind.FEEDSTOCK, "exiledSector_nanite_feedstock"), random).baseCount());
        }

        assertEquals(30, counts.first());
        assertEquals(90, counts.last());
        assertEquals(1, OfficerRewardPool.picked(CarriedItem.of(RewardKind.STAR_CHART, "some_chart").withCount(3), random).baseCount());
    }

    @Test
    void theShownPoolLeavesOutUniquesTheSectorHasNotUnlocked() {
        ItemDefinitions.register(List.of(definition(BRIDGE, ItemKind.BRIDGE), unique("test_planetkiller_core", "found_planetkiller"),
                unique("test_open_unique", "")));
        MemoryAPI memory = mock(MemoryAPI.class);
        MutableCharacterStatsAPI playerStats = mock(MutableCharacterStatsAPI.class);
        when(playerStats.getLevel()).thenReturn(30);
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getMemoryWithoutUpdate()).thenReturn(memory);
        when(sector.getPlayerStats()).thenReturn(playerStats);
        Predicate<ItemDefinition> canDrop = definition -> ItemUnlock.canDrop(definition, sector);

        assertEquals(List.of("test_open_unique"), ids(OfficerRewardPool.possible(RewardGroup.UNIQUE, canDrop, itemId -> true)));
        assertEquals(List.of(BRIDGE), ids(OfficerRewardPool.possible(RewardGroup.HULL_ITEM, canDrop, itemId -> true)));
        assertFalse(RewardContext.itemAllowedIn(sector).test(ItemDefinitions.get("test_planetkiller_core")));

        when(memory.getBoolean("$pk_recovered")).thenReturn(true);
        assertEquals(List.of("test_planetkiller_core", "test_open_unique"),
                ids(OfficerRewardPool.possible(RewardGroup.UNIQUE, canDrop, itemId -> true)));
    }

    @Test
    void theShownCraftingPoolLeavesOutCraftingItemsThatCannotBeUsed() throws Exception {
        registerCraftingTiers();

        assertEquals(List.of("low_b"), ids(OfficerRewardPool.possible(RewardGroup.LOW_CRAFTING, definition -> true, "low_b"::equals)));
    }

    @Test
    void aCorruptedTierRunOffersAndDropsStarChartsOfTheHighestNormalTier() throws Exception {
        StarChartFixtures.registerShipped();
        try {
            int corrupted = ChartTiers.CORRUPTED_TIER;
            List<CarriedItem> atCorrupted = OfficerRewardPool.candidates(new RewardContext(corrupted, null, null, biomeId -> false,
                    definition -> true, itemId -> true), new Random(1L)).get(RewardKind.STAR_CHART).stream().map(RewardCandidate::item).toList();
            List<CarriedItem> atHighest = OfficerRewardPool.candidates(new RewardContext(ChartTiers.HIGHEST_TIER, null, null, biomeId -> false,
                    definition -> true, itemId -> true), new Random(1L)).get(RewardKind.STAR_CHART).stream().map(RewardCandidate::item).toList();

            assertFalse(atHighest.isEmpty());
            assertEquals(atHighest, atCorrupted);
            assertEquals(ChartTiers.HIGHEST_TIER, OfficerRewardPool.chartTier(corrupted));
            assertEquals(4, OfficerRewardPool.chartTier(4));
        } finally {
            StarChartFixtures.clear();
        }
    }

    private static ItemDefinition unique(String id, String unlock) {
        return new ItemDefinition(id, ItemKind.BRIDGE, id, ItemDefinition.FALLBACK_ICON, 1f, true, "",
                List.of(PoolEntry.of(ModifierSlot.PREFIX, "HULL_PERCENT", 4f, 6f, 1f)), List.of(), unlock);
    }

    private static List<String> ids(List<CarriedItem> items) {
        return items.stream().map(CarriedItem::id).toList();
    }

    @Test
    void anEmptyPoolOffersNothing() {
        assertTrue(OfficerRewardPool.offers(Map.of(), Map.of(RewardKind.HULL_ITEM, 35f), OfficerRewardPool.OFFER_COUNT, new Random(1L)).isEmpty());
    }

    @Test
    void pickingAnItemRollsItsHiddenCountByTier() throws Exception {
        OfficerRewardTable.register(ChartTables.csvRows(OfficerRewardTable.PATH));
        Set<Integer> lowCounts = new TreeSet<>();
        Set<Integer> inhibitorCounts = new TreeSet<>();
        Set<Integer> highCounts = new TreeSet<>();
        Random random = new Random(19L);
        for (int pick = 0; pick < 1000; pick++) {
            lowCounts.add(OfficerRewardPool.picked(CarriedItem.of(RewardKind.CRAFTING_ITEM, "exiledSector_augmentation_nanoswarm"), random).baseCount());
            inhibitorCounts.add(OfficerRewardPool.picked(CarriedItem.of(RewardKind.INHIBITOR, "exiledSector_prefix_inhibitor"), random).baseCount());
            highCounts.add(OfficerRewardPool.picked(CarriedItem.of(RewardKind.CRAFTING_ITEM, "exiledSector_replicator_nanoseed"), random).baseCount());
        }

        assertEquals(Set.of(1, 2, 3, 4, 5, 6, 7), lowCounts);
        assertEquals(Set.of(1, 2, 3), inhibitorCounts);
        assertEquals(Set.of(1), highCounts);
    }

    @Test
    void pickedItemsOutsideTheCountedCategoriesAreSingleCopies() throws Exception {
        OfficerRewardTable.register(ChartTables.csvRows(OfficerRewardTable.PATH));
        Random random = new Random(2L);
        CarriedItem biomeItem = new CarriedItem(RewardKind.BIOME_HULL_ITEM, BRIDGE, RIFT, 4);

        assertEquals(biomeItem.withCount(1), OfficerRewardPool.picked(biomeItem, random));
        assertEquals(1, OfficerRewardPool.picked(CarriedItem.of(RewardKind.ENGRAM, "some_engram").withCount(5), random).baseCount());
        assertEquals(1, OfficerRewardPool.picked(CarriedItem.of(RewardKind.CRAFTING_ITEM, "not_in_the_table"), random).baseCount());
    }

    private void registerBiomeFixture() throws Exception {
        ItemModifiers.register(new JSONArray()
                .put(modifierRow(CHRONAL, "TIMEFLOW_PERCENT:3:5", "Chronal"))
                .put(modifierRow(CHARGED, "ARMOR_PERCENT:3:5", "Charged")));
        ItemDefinitions.register(List.of(definition(BRIDGE, ItemKind.BRIDGE), definition(EMITTER, ItemKind.SHIELD_GENERATOR)));
        BiomeModifiers.register(new JSONArray()
                .put(biomeRow(CHRONAL, RIFT, "bridge"))
                .put(biomeRow(CHARGED, BELT, "bridge")));
    }

    private static JSONObject modifierRow(String id, String lines, String name) throws Exception {
        return new JSONObject().put("id", id).put("affix", "prefix").put("scopes", "hull_item").put("lines", lines).put("name", name);
    }

    private static JSONObject biomeRow(String modifierId, String biomeId, String item) throws Exception {
        return new JSONObject().put("modifier", modifierId).put("biome", biomeId).put("item", item);
    }

    private static ItemDefinition definition(String id, ItemKind kind) {
        return new ItemDefinition(id, kind, id, ItemDefinition.FALLBACK_ICON, 1f, false, "",
                List.of(PoolEntry.of(ModifierSlot.PREFIX, "HULL_PERCENT", 4f, 6f, 1f)), List.of(), "");
    }

    private static RewardContext context(String centreBiomeId, Predicate<String> biomeOpen) {
        return new RewardContext(1, null, centreBiomeId, biomeOpen, definition -> true, itemId -> true);
    }

    @Test
    void aBiomeItemTakesTheCommandCentresBiomeWhenItHasAModifierForThePart() throws Exception {
        registerBiomeFixture();
        Random random = new Random(4L);
        for (int roll = 0; roll < 20; roll++) {
            assertEquals(BELT, OfficerRewardPool.biomeFor(ItemDefinitions.get(BRIDGE), context(BELT, biomeId -> false), random));
        }
    }

    @Test
    void otherwiseABiomeItemTakesARandomOpenBiome() throws Exception {
        registerBiomeFixture();
        Set<String> biomes = new HashSet<>();
        Random random = new Random(4L);
        for (int roll = 0; roll < 40; roll++) {
            biomes.add(OfficerRewardPool.biomeFor(ItemDefinitions.get(BRIDGE), context("normal", biomeId -> true), random));
        }

        assertEquals(Set.of(RIFT, BELT), biomes);
        assertEquals(RIFT, OfficerRewardPool.biomeFor(ItemDefinitions.get(BRIDGE), context(null, RIFT::equals), random));
    }

    @Test
    void aPartWithNoOpenBiomeModifierHasNoBiomeItem() throws Exception {
        registerBiomeFixture();
        Random random = new Random(4L);

        assertNull(OfficerRewardPool.biomeFor(ItemDefinitions.get(EMITTER), context(RIFT, biomeId -> true), random));
        assertNull(OfficerRewardPool.biomeFor(ItemDefinitions.get(BRIDGE), context(null, biomeId -> false), random));
    }
}
