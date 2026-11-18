package exiledsector.items.drop;

import exiledsector.ProjectFiles;
import exiledsector.data.RandomRounding;
import exiledsector.data.WeightedPick;
import exiledsector.items.crafting.material.CraftingInhibitor;
import exiledsector.items.crafting.material.Nanoswarm;
import exiledsector.loot.BattleLootRates;
import exiledsector.loot.DropType;
import exiledsector.loot.LootMultipliers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingDropsTest {

    private static final String AUGMENTATION = Nanoswarm.AUGMENTATION.itemId();
    private static final String CORRUPTION = Nanoswarm.CORRUPTION.itemId();
    private static final String IMMUTABLE = Nanoswarm.IMMUTABLE.itemId();
    private static final String BEAM_INHIBITOR = CraftingInhibitor.BEAM.itemId();
    private static final String PREFIX_INHIBITOR = CraftingInhibitor.PREFIX.itemId();
    private static final int MANY_DROPS = 40000;

    @AfterEach
    void tearDown() {
        CraftingDrops.clear();
        BattleLootRates.clear();
    }

    private static JSONObject row(String itemId, String weight, String chartOnly) throws Exception {
        return new JSONObject().put("item", itemId).put("weight", weight).put("chart_only", chartOnly);
    }

    private static void registerSmallTable() throws Exception {
        CraftingDrops.register(new JSONArray()
                .put(row(AUGMENTATION, "6", "FALSE"))
                .put(row(CORRUPTION, "3", "FALSE"))
                .put(row(IMMUTABLE, "1", "FALSE"))
                .put(row(BEAM_INHIBITOR, "2", "TRUE")));
    }

    private static int total(Map<String, Integer> drops) {
        return drops.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static LootMultipliers atRarity(float rarity) {
        return LootMultipliers.atTierCraftingRarity(rarity);
    }

    private static float curveWeight(float weight, float tableWeight, float rarity) {
        return (float) (tableWeight * Math.pow(weight / tableWeight, Math.pow(rarity, -CraftingDrops.RARITY_FLATTENING)));
    }

    private static float share(String itemId, float rarity, String... poolIds) {
        float pooledWeight = 0f;
        for (String poolId : poolIds) {
            pooledWeight += CraftingDrops.weightAtRarity(poolId, rarity);
        }
        return CraftingDrops.weightAtRarity(itemId, rarity) / pooledWeight;
    }

    private static Map<String, Integer> singlePoolRoll(float expectedCount, Predicate<String> allowedItems, float rarity, Random random) {
        Map<String, Integer> dropped = new LinkedHashMap<>();
        List<CraftingDrops.Drop> candidates = CraftingDrops.all().stream()
                .filter(drop -> drop.weight() > 0f && allowedItems.test(drop.itemId())).toList();
        if (!(expectedCount > 0f) || candidates.isEmpty()) {
            return dropped;
        }
        int count = RandomRounding.round(expectedCount, random);
        for (int dropIndex = 0; dropIndex < count; dropIndex++) {
            CraftingDrops.Drop picked = WeightedPick.pick(candidates, candidate -> CraftingDrops.weightAtRarity(candidate.itemId(), rarity), random);
            dropped.merge(picked.itemId(), 1, Integer::sum);
        }
        return dropped;
    }

    @Test
    void theTableReadsEachItemsWeightAndWhetherItDropsOnlyInCharts() throws Exception {
        registerSmallTable();

        assertEquals(6f, CraftingDrops.weight(AUGMENTATION), 1e-6f);
        assertEquals(0f, CraftingDrops.weight("not_in_the_table"), 1e-6f);
        assertTrue(CraftingDrops.chartOnly(BEAM_INHIBITOR));
        assertFalse(CraftingDrops.chartOnly(AUGMENTATION));
        assertTrue(CraftingDrops.covers(IMMUTABLE));
        assertFalse(CraftingDrops.covers(null));
    }

    @Test
    void atBaseRarityEveryItemKeepsItsWeight() throws Exception {
        registerSmallTable();

        assertEquals(6f, CraftingDrops.weightAtRarity(AUGMENTATION, LootMultipliers.NONE), 1e-6f);
        assertEquals(3f, CraftingDrops.weightAtRarity(CORRUPTION, CraftingDrops.BASE_RARITY), 1e-6f);
        assertEquals(1f, CraftingDrops.weightAtRarity(IMMUTABLE, CraftingDrops.BASE_RARITY), 1e-6f);
        assertEquals(0f, CraftingDrops.weightAtRarity("not_in_the_table", 3f), 1e-6f);
    }

    @Test
    void rarityRaisesEachWeightAsItsShareOfTheTableToAShrinkingPower() throws Exception {
        registerSmallTable();

        assertEquals(curveWeight(6f, 12f, 2.5f), CraftingDrops.weightAtRarity(AUGMENTATION, atRarity(2.5f)), 1e-5f);
        assertEquals(curveWeight(3f, 12f, 2.5f), CraftingDrops.weightAtRarity(CORRUPTION, atRarity(2.5f)), 1e-5f);
        assertEquals(curveWeight(1f, 12f, 2.5f), CraftingDrops.weightAtRarity(IMMUTABLE, atRarity(2.5f)), 1e-5f);
    }

    @Test
    void moreRarityKeepsGivingTheRarestItemMoreWithoutEverPassingTheCommonest() throws Exception {
        registerSmallTable();
        String[] pool = {AUGMENTATION, CORRUPTION, IMMUTABLE};

        float previousImmutable = share(IMMUTABLE, 1f, pool);
        float previousAugmentation = share(AUGMENTATION, 1f, pool);
        for (float rarity : new float[] {1.5f, 2f, 4f, 8f, 16f, 64f}) {
            float immutable = share(IMMUTABLE, rarity, pool);
            float corruption = share(CORRUPTION, rarity, pool);
            float augmentation = share(AUGMENTATION, rarity, pool);
            assertTrue(immutable > previousImmutable, "rarity " + rarity);
            assertTrue(augmentation < previousAugmentation, "rarity " + rarity);
            assertTrue(immutable < corruption && corruption < augmentation, "rarity " + rarity);
            previousImmutable = immutable;
            previousAugmentation = augmentation;
        }
    }

    @Test
    void rarityAtOrBelowZeroStillDropsTheCommonestItemsMost() throws Exception {
        registerSmallTable();

        Map<String, Integer> drops = CraftingDrops.roll(1000f, CraftingDrops.OUTSIDE_CHARTS, atRarity(0f), new Random(9L));

        assertEquals(1000, total(drops));
        assertTrue(drops.getOrDefault(AUGMENTATION, 0) > 900);
    }

    @Test
    void atBaseRarityEachItemDropsByItsShareOfTheWeights() throws Exception {
        registerSmallTable();

        Map<String, Integer> drops = CraftingDrops.roll(MANY_DROPS, CraftingDrops.OUTSIDE_CHARTS, LootMultipliers.NONE, new Random(7L));

        assertEquals(MANY_DROPS, total(drops));
        assertEquals(0.6f, drops.get(AUGMENTATION) / (float) MANY_DROPS, 0.01f);
        assertEquals(0.3f, drops.get(CORRUPTION) / (float) MANY_DROPS, 0.01f);
        assertEquals(0.1f, drops.get(IMMUTABLE) / (float) MANY_DROPS, 0.01f);
    }

    @Test
    void higherRarityMovesDropsTowardRarerItemsWithoutChangingTheCount() throws Exception {
        registerSmallTable();
        float pooledWeight = curveWeight(6f, 12f, 2f) + curveWeight(3f, 12f, 2f) + curveWeight(1f, 12f, 2f);

        Map<String, Integer> base = CraftingDrops.roll(MANY_DROPS, CraftingDrops.OUTSIDE_CHARTS, atRarity(1f), new Random(3L));
        Map<String, Integer> rarer = CraftingDrops.roll(MANY_DROPS, CraftingDrops.OUTSIDE_CHARTS, atRarity(2f), new Random(3L));

        assertEquals(total(base), total(rarer));
        assertEquals(curveWeight(6f, 12f, 2f) / pooledWeight, rarer.get(AUGMENTATION) / (float) MANY_DROPS, 0.01f);
        assertEquals(curveWeight(1f, 12f, 2f) / pooledWeight, rarer.get(IMMUTABLE) / (float) MANY_DROPS, 0.01f);
        assertTrue(rarer.get(AUGMENTATION) < base.get(AUGMENTATION));
        assertTrue(rarer.get(IMMUTABLE) > base.get(IMMUTABLE));
    }

    @Test
    void chartOnlyItemsStayOutOfOtherSourcesAndASourceGivesOnlyItsItems() throws Exception {
        registerSmallTable();

        Map<String, Integer> outside = CraftingDrops.roll(MANY_DROPS, CraftingDrops.OUTSIDE_CHARTS, atRarity(1f), new Random(5L));
        Map<String, Integer> inside = CraftingDrops.roll(MANY_DROPS, CraftingDrops.ANY_ITEM, atRarity(1f), new Random(5L));
        Map<String, Integer> inhibitorsOnly = CraftingDrops.roll(30, BEAM_INHIBITOR::equals, atRarity(3f), new Random(5L));

        assertFalse(outside.containsKey(BEAM_INHIBITOR));
        assertTrue(inside.containsKey(BEAM_INHIBITOR));
        assertEquals(Map.of(BEAM_INHIBITOR, 30), inhibitorsOnly);
    }

    @Test
    void nothingDropsWithoutACountOrAnAllowedItem() throws Exception {
        registerSmallTable();

        assertTrue(CraftingDrops.roll(0f, CraftingDrops.ANY_ITEM, atRarity(1f), new Random(1L)).isEmpty());
        assertTrue(CraftingDrops.roll(5f, itemId -> false, atRarity(1f), new Random(1L)).isEmpty());
    }

    @Test
    void withoutSubcategoryPercentsTheDropsMatchOneCraftingItemPoolDrawForDraw() throws Exception {
        registerSmallTable();
        LootMultipliers multipliers = new LootMultipliers(ItemChances.increasedBy(DropType.CRAFTING_ITEM, 50f)
                .plusPercent(DropType.ITEM, 20f).plusRarityPercent(DropType.ITEM, 30f), 2f);
        float craftingQuantity = multipliers.quantity(DropType.CRAFTING_ITEM);
        float rarity = 2f * multipliers.rarity(DropType.ITEM);

        for (long seed = 0L; seed < 200L; seed++) {
            float baseCount = 0.37f * seed;
            assertEquals(singlePoolRoll(baseCount * craftingQuantity, CraftingDrops.ANY_ITEM, rarity, new Random(seed)),
                    CraftingDrops.roll(baseCount, CraftingDrops.ANY_ITEM, multipliers, new Random(seed)), "seed " + seed);
        }
        assertEquals(craftingQuantity, CraftingDrops.split(CraftingDrops.ANY_ITEM, multipliers).meanQuantity());
    }

    @Test
    void eachSubcategoryScalesItsOwnShareOfTheCount() throws Exception {
        registerSmallTable();
        LootMultipliers doubledInhibitors = new LootMultipliers(ItemChances.increasedBy(DropType.INHIBITOR, 100f), CraftingDrops.BASE_RARITY);
        CraftingDrops.Split split = CraftingDrops.split(CraftingDrops.ANY_ITEM, doubledInhibitors);

        assertEquals((10f + 2f * 2f) / 12f, split.meanQuantity(), 1e-6f);
        Map<String, Integer> drops = CraftingDrops.roll(MANY_DROPS, CraftingDrops.ANY_ITEM, doubledInhibitors, new Random(11L));
        assertEquals(MANY_DROPS * 10f / 12f, drops.get(AUGMENTATION) + drops.get(CORRUPTION) + drops.get(IMMUTABLE), MANY_DROPS * 0.01f);
        assertEquals(MANY_DROPS * 4f / 12f, drops.get(BEAM_INHIBITOR), MANY_DROPS * 0.01f);
        assertEquals(MANY_DROPS * 6f / 12f, drops.get(AUGMENTATION), MANY_DROPS * 0.01f);
    }

    @Test
    void aSubcategoryAtNoQuantityDropsNoneOfItsItems() throws Exception {
        registerSmallTable();
        LootMultipliers noBasics = new LootMultipliers(ItemChances.increasedBy(DropType.BASIC_CRAFTING_ITEM, -100f), CraftingDrops.BASE_RARITY);

        Map<String, Integer> drops = CraftingDrops.roll(120f, CraftingDrops.ANY_ITEM, noBasics, new Random(2L));

        assertEquals(Map.of(BEAM_INHIBITOR, 20), drops);
        assertTrue(CraftingDrops.roll(120f, CraftingDrops.OUTSIDE_CHARTS, noBasics, new Random(2L)).isEmpty());
    }

    @Test
    void aSplitWithNoQuantityLeftDropsNothingEvenWhenGivenACount() throws Exception {
        registerSmallTable();
        LootMultipliers noBasics = new LootMultipliers(ItemChances.increasedBy(DropType.BASIC_CRAFTING_ITEM, -100f), CraftingDrops.BASE_RARITY);
        CraftingDrops.Split basicsOnly = CraftingDrops.split(CraftingDrops.OUTSIDE_CHARTS, noBasics);

        assertTrue(basicsOnly.roll(5f, new Random(3L)).isEmpty());
    }

    @Test
    void eachSubcategorysRarityReshapesOnlyItsOwnItems() throws Exception {
        CraftingDrops.register(new JSONArray()
                .put(row(CORRUPTION, "3", "FALSE"))
                .put(row(PREFIX_INHIBITOR, "3", "FALSE")));
        LootMultipliers rarerInhibitors = new LootMultipliers(ItemChances.BASE.plusRarityPercent(DropType.INHIBITOR, 100f), 1.5f);
        float corruptionWeight = curveWeight(3f, 6f, 1.5f);
        float inhibitorWeight = curveWeight(3f, 6f, 3f);

        assertEquals(corruptionWeight, CraftingDrops.weightAtRarity(CORRUPTION, rarerInhibitors), 1e-5f);
        assertEquals(inhibitorWeight, CraftingDrops.weightAtRarity(PREFIX_INHIBITOR, rarerInhibitors), 1e-5f);
        Map<String, Integer> drops = CraftingDrops.roll(MANY_DROPS, CraftingDrops.ANY_ITEM, rarerInhibitors, new Random(4L));
        assertEquals(MANY_DROPS, total(drops));
        assertEquals(MANY_DROPS * inhibitorWeight / (corruptionWeight + inhibitorWeight), drops.get(PREFIX_INHIBITOR), MANY_DROPS * 0.01f);
    }

    @Test
    void theCountIsTheCraftingItemRatePerHundredDeploymentPoints() throws Exception {
        BattleLootRates.register(new JSONArray().put(new JSONObject().put("drop", BattleLootRates.CRAFTING_ITEM_RATE).put("per_100_dp", "1.5")
                .put("curve", "linear").put("difficulty_scaling", "0")));
        BattleLootRates.Rate craftingRate = BattleLootRates.rate(BattleLootRates.CRAFTING_ITEM_RATE);

        assertEquals(6f, craftingRate.expectedAtDifficulty(400f, 400f, 3f), 1e-5f);
        assertEquals(0f, craftingRate.expectedAtDifficulty(-10f, 400f, 3f), 1e-6f);
    }

    @Test
    void theShippedTableCoversEveryCoreCraftingItemAndMarksTagInhibitorsChartOnly() throws Exception {
        CraftingDrops.register(ProjectFiles.csvRows(CraftingDrops.DATA_PATH));

        for (Nanoswarm nanoswarm : Nanoswarm.values()) {
            assertTrue(CraftingDrops.weight(nanoswarm.itemId()) > 0f, nanoswarm.name());
            assertFalse(CraftingDrops.chartOnly(nanoswarm.itemId()), nanoswarm.name());
        }
        for (CraftingInhibitor inhibitor : CraftingInhibitor.values()) {
            assertTrue(CraftingDrops.weight(inhibitor.itemId()) > 0f, inhibitor.name());
            assertEquals(inhibitor.inhibitedSlot() == null, CraftingDrops.chartOnly(inhibitor.itemId()), inhibitor.name());
        }
        assertTrue(CraftingDrops.weight(Nanoswarm.AUGMENTATION.itemId()) > CraftingDrops.weight(Nanoswarm.IMMUTABLE.itemId()) * 10f);
    }
}
