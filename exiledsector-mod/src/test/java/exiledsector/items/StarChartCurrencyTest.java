package exiledsector.items;

import exiledsector.charts.loot.ChartDrops;
import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierCategory;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.modifiers.ScaleContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StarChartCurrencyTest {

    private static final String SINGLE = "star_chart";
    private static final String GATED = StarChartFixtures.TEST_GATED;
    private static final String FLEET_SIZE = "HYBRID_CHART_FLEET_SIZE";
    private static final Predicate<ItemDefinition> ANY_UNIQUE = definition -> true;

    private final ItemStore store = ItemStore.forStarCharts();
    private final CargoAPI cargo = mock(CargoAPI.class);
    private CraftingMaterials materials;

    @BeforeEach
    void setUp() throws Exception {
        ItemFixtures.registerShippedItems();
        ItemImplicits.register(HullItemRows.shippedRows(ItemImplicits.dataPath()));
        StarChartFixtures.registerShipped();
        StarChartCurrency.registerAll();
        when(cargo.getQuantity(eq(CargoAPI.CargoItemType.SPECIAL), any())).thenReturn(1000f);
        materials = CraftingMaterials.cargoOnly(cargo);
    }

    @AfterEach
    void tearDown() {
        StarChartCurrency.unregisterAll();
        ItemCraftingCosts.clear();
        StarChartFixtures.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
        ItemImplicits.clear();
    }

    private StarChart stored(String definitionId, long seed, int tier) {
        return (StarChart) store.add(ItemData.rolledAtTier(ItemDefinitions.get(definitionId), seed, tier));
    }

    private StarChart storedWith(String definitionId, int tier, Predicate<StarChart> wanted) {
        for (long seed = 0; seed < 2000; seed++) {
            StarChart candidate = StarChartFixtures.chart(definitionId, seed, tier);
            if (wanted.test(candidate)) {
                return stored(definitionId, seed, tier);
            }
        }
        throw new AssertionError("no seed rolls the wanted chart");
    }

    private static boolean has(StarChart chart, String modifierId) {
        return chart.explicits().stream().anyMatch(rolled -> rolled.modifierId().equals(modifierId));
    }

    private Item use(ItemCurrency currency, Item chart, long seed) {
        return ItemCrafting.use(currency, chart, materials, store, new Random(seed), ANY_UNIQUE);
    }

    private static long side(Item chart, ModifierSlot slot) {
        return chart.explicits().stream().filter(rolled -> rolled.slot() == slot).count();
    }

    @Test
    void theChartRowListsTheExtendedKernelsThenTheChartCurrenciesAndShipTypesStayAsTheyWere() {
        assertEquals(List.of(CraftingCurrency.AUGMENTATION, CraftingCurrency.CORRUPTION, CraftingCurrency.RECALIBRATION,
                        StarChartCurrency.REFORGE, StarChartCurrency.ANNULMENT, StarChartCurrency.CARTOGRAPHY),
                ItemCurrencies.forType(ItemType.STAR_CHART));
        assertEquals(Arrays.asList(CraftingCurrency.values()), ItemCurrencies.forType(ItemType.SUBROUTINE));
        assertEquals(Arrays.asList(CraftingCurrency.values()), ItemCurrencies.forType(ItemType.HULL_ITEM));
        for (StarChartCurrency currency : StarChartCurrency.values()) {
            assertFalse(currency.dropsFromShipLoot(), currency.name());
            assertSame(currency, ItemCurrencies.byItemId(currency.itemId()));
            assertTrue(MaterialItems.isMaterial(currency.itemId()));
        }
        for (CraftingCurrency kernel : CraftingCurrency.values()) {
            assertTrue(kernel.dropsFromShipLoot(), kernel.name());
            assertFalse(kernel.appliesTo(ItemType.STAR_CHART), kernel.name());
        }

        StarChartCurrency.registerAll();
        assertEquals(6, ItemCurrencies.forType(ItemType.STAR_CHART).size());
        StarChartCurrency.unregisterAll();
        assertEquals(List.of(), ItemCurrencies.forType(ItemType.STAR_CHART));
        assertEquals(Arrays.asList(CraftingCurrency.values()), ItemCurrencies.all());
        assertThrows(IllegalArgumentException.class, () -> ItemCurrencies.extend(StarChartCurrency.REFORGE, ItemType.HULL_ITEM));
        assertFalse(ItemCurrencies.extend(CraftingCurrency.AUGMENTATION, ItemType.HULL_ITEM));
        assertFalse(ItemCurrencies.appliesTo(null, ItemType.STAR_CHART));
        assertFalse(ItemCurrencies.appliesTo(CraftingCurrency.AUGMENTATION, null));
    }

    @Test
    void chartCurrenciesNeverTouchShipItems() {
        ItemStore shipStore = new ItemStore();
        Item hullItem = shipStore.add(ItemDefinitions.all().stream().filter(definition -> !definition.unique()).findFirst().orElseThrow(), 4L);
        for (StarChartCurrency currency : StarChartCurrency.values()) {
            assertFalse(ItemCrafting.canUse(currency, hullItem, ANY_UNIQUE), currency.name());
            assertNull(ItemCrafting.use(currency, hullItem, materials, shipStore, new Random(1L), ANY_UNIQUE), currency.name());
        }
        assertEquals(ItemCrafting.canAugment(hullItem), ItemCrafting.canUse(CraftingCurrency.AUGMENTATION, hullItem, ANY_UNIQUE));
        assertFalse(ItemCrafting.canUse(CraftingCurrency.REPLICATION, stored(SINGLE, 3L, 4), ANY_UNIQUE));
    }

    @Test
    void theKernelsOnlyReachChartsWhileExtended() {
        StarChart chart = storedWith(SINGLE, 6, candidate -> candidate.explicits().size() == 1);
        assertTrue(ItemCrafting.canUse(CraftingCurrency.AUGMENTATION, chart, ANY_UNIQUE));
        assertFalse(ItemCrafting.canUse(CraftingCurrency.TRANSPOSITION, chart, ANY_UNIQUE));

        StarChartCurrency.unregisterAll();

        for (CraftingCurrency kernel : StarChartCurrency.KERNELS_ON_CHARTS) {
            assertFalse(ItemCrafting.canUse(kernel, chart, ANY_UNIQUE), kernel.name());
        }
    }

    @Test
    void augmentationFillsAChartToSixModifiersWithThreeOnEachSide() {
        StarChart chart = storedWith(SINGLE, 12, candidate -> candidate.explicits().size() == 1);
        long seed = 0;
        while (ItemCrafting.canUse(CraftingCurrency.AUGMENTATION, chart, ANY_UNIQUE)) {
            assertSame(chart, use(CraftingCurrency.AUGMENTATION, chart, seed++));
            assertTrue(side(chart, ModifierSlot.PREFIX) <= 3 && side(chart, ModifierSlot.SUFFIX) <= 3, chart.explicits().toString());
            assertEquals(chart.explicits().size() <= 2 ? ItemRarity.COMMON : ItemRarity.RARE, chart.rarity());
        }
        assertEquals(6, chart.explicits().size());
        assertEquals(3, side(chart, ModifierSlot.PREFIX));
        assertEquals(12, chart.tier());
    }

    @Test
    void augmentationOnALowTierChartOnlyAddsModifiersThatRollAtThatTier() {
        for (long seed = 0; seed < 60; seed++) {
            StarChart chart = stored(SINGLE, seed, 1);
            while (ItemCrafting.canUse(CraftingCurrency.AUGMENTATION, chart, ANY_UNIQUE)) {
                use(CraftingCurrency.AUGMENTATION, chart, seed);
            }
            assertFalse(has(chart, "CHART_COLONY_ITEM_PERCENT"), "seed " + seed);
        }
    }

    @Test
    void recalibrationRerollsTheRollPositionsAndKeepsTheModifiersAndTier() {
        StarChart chart = storedWith(SINGLE, 7, candidate -> has(candidate, FLEET_SIZE));
        List<String> idsBefore = chart.explicits().stream().map(RolledModifier::modifierId).toList();
        List<RolledModifier> before = List.copyOf(chart.explicits());

        assertSame(chart, use(CraftingCurrency.RECALIBRATION, chart, 9L));

        assertNotEquals(before, chart.explicits());
        assertEquals(idsBefore, chart.explicits().stream().map(RolledModifier::modifierId).toList());
        assertEquals(7, chart.tier());
        for (SlottedRoll roll : chart.rolls(ModifierCategory.EXPLICIT)) {
            if (roll.line() != null && roll.line().scalesWithTier() && roll.line().canVary()) {
                float position = roll.roll().magnitude();
                assertTrue(position >= 0f && position <= ModifierLine.TIER_ROLL_POSITIONS, "position " + position);
            }
        }
    }

    @Test
    void corruptionOnAChartReachesEveryOutcomeButTheImplicitOne() {
        Set<CorruptionOutcome> outcomes = EnumSet.noneOf(CorruptionOutcome.class);
        Set<String> chartImplicitIds = new java.util.HashSet<>(StarChartImplicits.corruptionPool().stream().map(Modifier::id).toList());
        for (long seed = 0; seed < 80; seed++) {
            StarChart chart = stored(SINGLE, seed, 8);
            List<RolledModifier> implicitsBefore = List.copyOf(chart.implicits());
            Item corrupted = use(CraftingCurrency.CORRUPTION, chart, seed * 0x9E3779B97F4A7C15L);
            assertNotNull(corrupted);
            assertTrue(corrupted.corrupted());
            assertEquals(8, corrupted.tier());
            outcomes.add(corrupted.corruption());
            if (corrupted.corruption() == CorruptionOutcome.IMPLICIT_REPLACED) {
                assertNotEquals(implicitsBefore, corrupted.implicits());
                corrupted.implicits().forEach(implicit -> assertTrue(chartImplicitIds.contains(implicit.modifierId()), implicit.modifierId()));
            }
            for (ItemCurrency currency : ItemCurrencies.forType(ItemType.STAR_CHART)) {
                assertFalse(ItemCrafting.canUse(currency, corrupted, ANY_UNIQUE), currency.key());
            }
        }
        Set<CorruptionOutcome> reachable = EnumSet.copyOf(CorruptionOutcome.NANOSWARM_OUTCOMES);
        reachable.remove(CorruptionOutcome.IMPLICIT_REPLACED);
        assertEquals(reachable, outcomes);
    }

    @Test
    void corruptedValuesStretchUpToThirtyFivePercentPastTheTierBand() {
        boolean beyondBand = false;
        for (long seed = 0; seed < 300; seed++) {
            StarChart chart = StarChartFixtures.chart(SINGLE, seed, 6);
            if (!has(chart, FLEET_SIZE)) {
                continue;
            }
            ItemCrafting.randomiseValues(chart, new Random(seed));
            ScaleContext context = chart.scaleContext(null);
            for (SlottedRoll roll : chart.rolls()) {
                ModifierLine line = roll.line();
                if (line == null || !line.scalesWithTier() || !line.canVary()) {
                    continue;
                }
                float position = roll.roll().magnitude();
                assertTrue(position >= -35f && position <= 135f, "position " + position);
                ModifierLine band = line.rangeFor(context);
                float value = line.valueFor(position, context);
                float lowest = Math.round(band.min() - Math.abs(band.min()) * 0.35f) - 1f;
                float highest = Math.round(band.max() + Math.abs(band.max()) * 0.35f) + 1f;
                assertTrue(value >= lowest && value <= highest, line.effectName() + " " + value + " outside " + band);
                beyondBand |= value < band.min() || value > band.max();
            }
        }
        assertTrue(beyondBand);
    }

    @Test
    void hullItemCorruptionStretchesExactlyAsBefore() {
        ModifierLine range = ModifierLine.range("HULL_MULT", 4f, 6f);
        ModifierLine perHullSize = ModifierLine.parse("ARMOR_FLAT:5/10/15/20").line();
        ModifierLine fixed = ModifierLine.range("HULL_MULT", 4f, 4f);

        assertEquals(range.widened(ItemCrafting.CORRUPTION_RANGE_STRETCH), ItemCrafting.stretchedForCorruption(range));
        assertNull(ItemCrafting.stretchedForCorruption(perHullSize));
        assertNull(ItemCrafting.stretchedForCorruption(fixed));
        assertNull(ItemCrafting.stretchedForCorruption(null));
        ModifierLine tierBand = ModifierLine.parse("FLEET_POWER:t1=10-15").line();
        assertEquals(-35f, ItemCrafting.stretchedForCorruption(tierBand).min());
        assertEquals(135f, ItemCrafting.stretchedForCorruption(tierBand).max());
    }

    @Test
    void reforgeRerollsEveryModifierAtTheChartsTier() {
        StarChart chart = storedWith(SINGLE, 3, candidate -> candidate.explicits().size() >= 2);
        Map<Integer, Integer> counts = new HashMap<>();
        for (long seed = 0; seed < 120; seed++) {
            List<RolledModifier> before = List.copyOf(chart.explicits());
            assertSame(chart, use(StarChartCurrency.REFORGE, chart, seed));
            assertNotEquals(before, chart.explicits());
            assertFalse(has(chart, "CHART_COLONY_ITEM_PERCENT"));
            assertEquals(chart.explicits().size() <= 2 ? ItemRarity.COMMON : ItemRarity.RARE, chart.rarity());
            assertEquals(3, chart.tier());
            counts.merge(chart.explicits().size(), 1, Integer::sum);
        }
        assertTrue(counts.keySet().size() >= 4, counts.toString());
    }

    @Test
    void reforgeAndAnnulmentRefuseCorruptedAndMirroredCharts() {
        StarChart chart = storedWith(SINGLE, 4, candidate -> candidate.explicits().size() >= 2);
        Item mirror = store.addMirrorOf(chart);
        StarChart corrupted = stored(SINGLE, 5L, 4);
        corrupted.corrupt(CorruptionOutcome.NOTHING);
        for (StarChartCurrency currency : List.of(StarChartCurrency.REFORGE, StarChartCurrency.ANNULMENT)) {
            assertFalse(ItemCrafting.canUse(currency, mirror, ANY_UNIQUE), currency.name());
            assertFalse(ItemCrafting.canUse(currency, corrupted, ANY_UNIQUE), currency.name());
            assertTrue(ItemCrafting.canUse(currency, chart, ANY_UNIQUE), currency.name());
        }
        assertFalse(ItemCrafting.canUse(StarChartCurrency.CARTOGRAPHY, mirror, ANY_UNIQUE));
        assertFalse(ItemCrafting.canUse(StarChartCurrency.CARTOGRAPHY, corrupted, ANY_UNIQUE));
    }

    @Test
    void annulmentRemovesEveryModifierAndTheChartTurnsCommon() {
        StarChart chart = storedWith(SINGLE, 9, candidate -> candidate.explicits().size() >= 3);
        assertEquals(ItemRarity.RARE, chart.rarity());
        List<RolledModifier> implicitsBefore = List.copyOf(chart.implicits());

        assertSame(chart, use(StarChartCurrency.ANNULMENT, chart, 2L));

        assertTrue(chart.explicits().isEmpty());
        assertEquals(implicitsBefore, chart.implicits());
        assertEquals(ItemRarity.COMMON, chart.rarity());
        assertFalse(ItemCrafting.canUse(StarChartCurrency.ANNULMENT, chart, ANY_UNIQUE));
        assertNull(use(StarChartCurrency.ANNULMENT, chart, 5L));
        assertEquals(9, chart.tier());
    }

    @Test
    void anInhibitorKeepsItsSideThroughAnnulmentAndIsUsedUp() {
        StarChart chart = storedWith(SINGLE, 9, candidate -> side(candidate, ModifierSlot.PREFIX) >= 1 && side(candidate, ModifierSlot.SUFFIX) >= 1);
        List<RolledModifier> prefixes = chart.explicits().stream().filter(rolled -> rolled.slot() == ModifierSlot.PREFIX).toList();
        List<CraftingWard> prefixWard = List.of(CraftingWard.PREFIX);

        assertEquals(Set.of(CraftingWard.PREFIX), ItemCrafting.wardsSpentBy(StarChartCurrency.ANNULMENT, chart, prefixWard));
        assertSame(chart, ItemCrafting.use(StarChartCurrency.ANNULMENT, chart, materials, store, new Random(2L), ANY_UNIQUE, prefixWard));

        assertEquals(prefixes, chart.explicits());
        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(CraftingWard.PREFIX.itemId()), 1);
        assertFalse(ItemCrafting.canUse(StarChartCurrency.ANNULMENT, chart, ANY_UNIQUE, prefixWard));
        assertFalse(ItemCrafting.canUse(StarChartCurrency.ANNULMENT, chart, ANY_UNIQUE, List.of(CraftingWard.PREFIX, CraftingWard.SUFFIX)));
        assertTrue(ItemCrafting.canUse(StarChartCurrency.ANNULMENT, chart, ANY_UNIQUE));
    }

    @Test
    void anInhibitorKeepsItsSideThroughAReforgeAndOnlyTheOtherSideIsRerolled() {
        StarChart chart = storedWith(SINGLE, 6, candidate -> side(candidate, ModifierSlot.SUFFIX) >= 1);
        List<CraftingWard> suffixWard = List.of(CraftingWard.SUFFIX);
        boolean prefixesChanged = false;
        for (long seed = 0; seed < 40; seed++) {
            List<RolledModifier> suffixes = chart.explicits().stream().filter(rolled -> rolled.slot() == ModifierSlot.SUFFIX).toList();
            List<RolledModifier> prefixes = chart.explicits().stream().filter(rolled -> rolled.slot() == ModifierSlot.PREFIX).toList();

            assertSame(chart, ItemCrafting.use(StarChartCurrency.REFORGE, chart, materials, store, new Random(seed), ANY_UNIQUE, suffixWard));

            assertEquals(suffixes, chart.explicits().stream().filter(rolled -> rolled.slot() == ModifierSlot.SUFFIX).toList());
            prefixesChanged |= !prefixes.equals(chart.explicits().stream().filter(rolled -> rolled.slot() == ModifierSlot.PREFIX).toList());
            assertTrue(side(chart, ModifierSlot.PREFIX) <= AffixLayout.limits(ItemType.STAR_CHART).maxPerSide());
            assertEquals(chart.explicits().size() <= 2 ? ItemRarity.COMMON : ItemRarity.RARE, chart.rarity());
        }
        assertTrue(prefixesChanged);
        assertFalse(ItemCrafting.canUse(StarChartCurrency.REFORGE, chart, ANY_UNIQUE, List.of(CraftingWard.PREFIX, CraftingWard.SUFFIX)));
    }

    @Test
    void cartographyIgnoresInhibitors() {
        StarChart chart = storedWith(SINGLE, 4, candidate -> !candidate.explicits().isEmpty());

        assertTrue(ItemCrafting.wardsSpentBy(StarChartCurrency.CARTOGRAPHY, chart, List.of(CraftingWard.PREFIX, CraftingWard.SUFFIX)).isEmpty());
        assertTrue(ItemCrafting.canUse(StarChartCurrency.CARTOGRAPHY, chart, ANY_UNIQUE, List.of(CraftingWard.PREFIX, CraftingWard.SUFFIX)));
    }

    @Test
    void cartographyRaisesTheTierAndTheRollPositionsTakeTheNewBand() {
        StarChart chart = storedWith(SINGLE, 5, candidate -> has(candidate, FLEET_SIZE));
        RolledModifier fleetSize = chart.explicits().stream().filter(rolled -> rolled.modifierId().equals(FLEET_SIZE)).findFirst().orElseThrow();
        ModifierLine line = ItemDefinitions.get(SINGLE).poolEntryFor(FLEET_SIZE).modifier().line(fleetSize.lines().get(0).effectName());
        float position = fleetSize.lines().get(0).magnitude();
        float tierFiveValue = line.valueFor(position, chart.scaleContext(null));
        assertTrue(tierFiveValue >= 15f && tierFiveValue <= 20f);

        assertSame(chart, use(StarChartCurrency.CARTOGRAPHY, chart, 1L));

        assertEquals(6, chart.tier());
        RolledModifier after = chart.explicits().stream().filter(rolled -> rolled.modifierId().equals(FLEET_SIZE)).findFirst().orElseThrow();
        assertEquals(position, after.lines().get(0).magnitude());
        float tierSixValue = line.valueFor(position, chart.scaleContext(null));
        assertTrue(tierSixValue >= 50f && tierSixValue <= 70f, "value " + tierSixValue);
        StarChart reloaded = (StarChart) ItemData.of(chart).preview();
        assertEquals(6, reloaded.tier());
    }

    @Test
    void cartographyStopsAtTheHighestTierAndStartsAGatedChartFromItsMinimumTier() {
        StarChart top = stored(SINGLE, 1L, 14);
        assertSame(top, use(StarChartCurrency.CARTOGRAPHY, top, 1L));
        assertEquals(StarChart.HIGHEST_TIER, top.tier());
        assertFalse(ItemCrafting.canUse(StarChartCurrency.CARTOGRAPHY, top, ANY_UNIQUE));
        assertNull(use(StarChartCurrency.CARTOGRAPHY, top, 2L));

        StarChart gated = stored(GATED, 3L, 1);
        assertEquals(4, gated.tier());
        use(StarChartCurrency.CARTOGRAPHY, gated, 1L);
        assertEquals(5, gated.tier());
    }

    @Test
    void usingAChartCurrencyTakesOneFromTheMaterials() {
        StarChart chart = stored(SINGLE, 2L, 2);

        assertSame(chart, use(StarChartCurrency.CARTOGRAPHY, chart, 1L));

        verify(cargo, times(1)).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(StarChartCurrency.CARTOGRAPHY.itemId()), 1);
        verify(cargo, never()).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(StarChartCurrency.REFORGE.itemId()), 1);
    }

    @Test
    void theShippedCostsMatchTheDefaultsAndTheCurrenciesCanBeGrown() throws Exception {
        assertEquals(List.of(30, 40, 120), Arrays.stream(StarChartCurrency.values()).map(StarChartCurrency::partsCost).toList());
        ItemCraftingCosts.register(HullItemRows.shippedRows(ItemCraftingCosts.dataPath()));
        assertEquals(List.of(30, 40, 120), Arrays.stream(StarChartCurrency.values()).map(StarChartCurrency::partsCost).toList());
        for (StarChartCurrency currency : StarChartCurrency.values()) {
            assertTrue(currency.synthesisable());
        }
    }

    @Test
    void shipKernelDropsPickExactlyAsTheOldFixedListDidWithChartCurrenciesRegistered() {
        record Drop(String itemId, float weight) {
        }
        List<Drop> legacyDrops = new ArrayList<>();
        for (CraftingCurrency currency : CraftingCurrency.values()) {
            legacyDrops.add(new Drop(currency.itemId(), currency.dropWeight()));
        }
        for (CraftingWard ward : CraftingWard.values()) {
            legacyDrops.add(new Drop(ward.itemId(), ward.dropWeight()));
        }
        Random legacyRandom = new Random(17L);
        Random registryRandom = new Random(17L);
        for (int pick = 0; pick < 5000; pick++) {
            assertEquals(WeightedPick.pick(legacyDrops, Drop::weight, legacyRandom).itemId(), ItemDrops.pickKernel(registryRandom), "pick " + pick);
        }
    }

    @Test
    void chartCurrencyDropsOnlyPickTheChartOnlyCurrenciesByWeight() {
        Map<String, Integer> picks = new HashMap<>();
        Random random = new Random(3L);
        int trials = 12000;
        for (int pick = 0; pick < trials; pick++) {
            picks.merge(ChartDrops.pickChartCurrency(random), 1, Integer::sum);
        }
        float totalWeight = 0f;
        for (StarChartCurrency currency : StarChartCurrency.values()) {
            totalWeight += currency.dropWeight();
        }
        assertEquals(Set.of(StarChartCurrency.REFORGE.itemId(), StarChartCurrency.ANNULMENT.itemId(), StarChartCurrency.CARTOGRAPHY.itemId()),
                picks.keySet());
        for (StarChartCurrency currency : StarChartCurrency.values()) {
            assertEquals(currency.dropWeight() / totalWeight, picks.get(currency.itemId()) / (double) trials, 0.02, currency.name());
        }

        StarChartCurrency.unregisterAll();
        assertNull(ChartDrops.pickChartCurrency(new Random(1L)));
    }
}
