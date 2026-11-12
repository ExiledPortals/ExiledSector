package exiledsector.charts.officer.reward;

import exiledsector.charts.ChartTables;
import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemModifiers;
import exiledsector.items.ModifierSources;
import exiledsector.items.PoolEntry;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
