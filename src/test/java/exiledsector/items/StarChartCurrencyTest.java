package exiledsector.items;

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
import static org.junit.jupiter.api.Assertions.assertNotSame;
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

    private static final String SINGLE = "star_chart_single";
    private static final String SILENT_REACH = "unique_chart_silent_reach";
    private static final String WARDENS_VIGIL = "unique_chart_wardens_vigil";
    private static final String FLEET_SIZE = "HYBRID_CHART_FLEET_SIZE";
    private static final Predicate<ItemDefinition> ANY_UNIQUE = definition -> true;

    private final ItemStore store = ItemStore.forStarCharts();
    private final CargoAPI cargo = mock(CargoAPI.class);
    private CraftingMaterials materials;

    @BeforeEach
    void setUp() throws Exception {
        ItemFixtures.registerShippedItems();
        ItemImplicits.register(ItemFixtures.shippedRows(ItemImplicits.DATA_PATH));
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
        assertEquals(List.of(CraftingCurrency.AUGMENTATION, CraftingCurrency.ATTUNEMENT, CraftingCurrency.CORRUPTION, CraftingCurrency.TRANSPOSITION,
                        CraftingCurrency.RECALIBRATION, StarChartCurrency.REFORGE, StarChartCurrency.ANNULMENT, StarChartCurrency.CARTOGRAPHY),
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
        assertEquals(8, ItemCurrencies.forType(ItemType.STAR_CHART).size());
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
            assertFalse(has(chart, "HYBRID_CHART_WARDEN_SIZE"), "seed " + seed);
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
    void transpositionOnlyPicksAUniqueThatRollsAtTheChartsTier() {
        StarChart lowTier = stored(SILENT_REACH, 5L, 2);
        assertFalse(ItemCrafting.canUse(CraftingCurrency.TRANSPOSITION, lowTier, ANY_UNIQUE));
        assertNull(use(CraftingCurrency.TRANSPOSITION, lowTier, 1L));

        StarChart highTier = stored(SILENT_REACH, 5L, 6);
        Item transposed = use(CraftingCurrency.TRANSPOSITION, highTier, 1L);

        assertNotNull(transposed);
        assertEquals(WARDENS_VIGIL, transposed.definitionId());
        assertEquals(6, transposed.tier());
        assertSame(transposed, store.find(transposed.id()));
        assertNotSame(highTier, store.find(highTier.id()));
        assertFalse(ItemCrafting.canUse(CraftingCurrency.TRANSPOSITION, stored(SINGLE, 2L, 6), ANY_UNIQUE));
    }

    @Test
    void attunementRerollsTheChartsFactionAndLeavesItsModifiersAlone() {
        Set<String> factionsSeen = new java.util.HashSet<>();
        for (long seed = 0; seed < 40; seed++) {
            StarChart chart = stored(SINGLE, seed, 8);
            String factionBefore = chart.implicits().get(0).modifierId();
            List<RolledModifier> explicitsBefore = List.copyOf(chart.explicits());
            assertTrue(ItemCrafting.canUse(CraftingCurrency.ATTUNEMENT, chart, ANY_UNIQUE));

            assertSame(chart, use(CraftingCurrency.ATTUNEMENT, chart, seed * 0x9E3779B97F4A7C15L));

            String factionAfter = chart.implicits().get(0).modifierId();
            assertEquals(1, chart.implicits().size());
            assertNotEquals(factionBefore, factionAfter);
            assertTrue(factionAfter.startsWith("CHART_FACTION_"), factionAfter);
            assertEquals(explicitsBefore, chart.explicits());
            assertEquals(chart.implicits(), ((StarChart) ItemData.parse(ItemData.of(chart).encode()).preview()).implicits());
            factionsSeen.add(factionAfter);
        }
        assertTrue(factionsSeen.size() > 5, factionsSeen.toString());
    }

    @Test
    void attunementCannotChangeTheFactionWhenOnlyThatFactionIsListed() throws Exception {
        StarChart chart = stored(SINGLE, 3L, 8);
        String faction = chart.implicits().get(0).modifierId().substring("CHART_FACTION_".length()).toLowerCase(java.util.Locale.ROOT);
        exiledsector.charts.ChartFactions.register(new org.json.JSONArray("[{\"faction\": \"" + faction + "\", \"weight\": \"1\"}]"));

        assertFalse(ItemCrafting.canUse(CraftingCurrency.ATTUNEMENT, chart, ANY_UNIQUE));
    }

    @Test
    void corruptionOnAChartReachesEveryOutcomeAndUsesTheChartImplicitPool() {
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
        assertEquals(EnumSet.copyOf(CorruptionOutcome.NANOSWARM_OUTCOMES), outcomes);
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
            assertFalse(has(chart, "HYBRID_CHART_WARDEN_SIZE"));
            assertEquals(chart.explicits().size() <= 2 ? ItemRarity.COMMON : ItemRarity.RARE, chart.rarity());
            assertEquals(3, chart.tier());
            counts.merge(chart.explicits().size(), 1, Integer::sum);
        }
        assertTrue(counts.keySet().size() >= 4, counts.toString());
    }

    @Test
    void reforgeAndAnnulmentRefuseUniqueCorruptedAndMirroredCharts() {
        StarChart unique = stored(SILENT_REACH, 4L, 3);
        StarChart chart = storedWith(SINGLE, 4, candidate -> candidate.explicits().size() >= 2);
        Item mirror = store.addMirrorOf(chart);
        StarChart corrupted = stored(SINGLE, 5L, 4);
        corrupted.corrupt(CorruptionOutcome.NOTHING);
        for (StarChartCurrency currency : List.of(StarChartCurrency.REFORGE, StarChartCurrency.ANNULMENT)) {
            assertFalse(ItemCrafting.canUse(currency, unique, ANY_UNIQUE), currency.name());
            assertFalse(ItemCrafting.canUse(currency, mirror, ANY_UNIQUE), currency.name());
            assertFalse(ItemCrafting.canUse(currency, corrupted, ANY_UNIQUE), currency.name());
            assertTrue(ItemCrafting.canUse(currency, chart, ANY_UNIQUE), currency.name());
        }
        assertFalse(ItemCrafting.canUse(StarChartCurrency.CARTOGRAPHY, mirror, ANY_UNIQUE));
        assertFalse(ItemCrafting.canUse(StarChartCurrency.CARTOGRAPHY, corrupted, ANY_UNIQUE));
    }

    @Test
    void annulmentRemovesOneModifierAtATimeAndARareChartCanTurnCommon() {
        StarChart chart = storedWith(SINGLE, 9, candidate -> candidate.explicits().size() == 3);
        assertEquals(ItemRarity.RARE, chart.rarity());
        List<RolledModifier> before = List.copyOf(chart.explicits());

        assertSame(chart, use(StarChartCurrency.ANNULMENT, chart, 2L));

        assertEquals(2, chart.explicits().size());
        assertTrue(before.containsAll(chart.explicits()));
        assertEquals(ItemRarity.COMMON, chart.rarity());
        use(StarChartCurrency.ANNULMENT, chart, 3L);
        use(StarChartCurrency.ANNULMENT, chart, 4L);
        assertTrue(chart.explicits().isEmpty());
        assertFalse(ItemCrafting.canUse(StarChartCurrency.ANNULMENT, chart, ANY_UNIQUE));
        assertNull(use(StarChartCurrency.ANNULMENT, chart, 5L));
        assertEquals(9, chart.tier());
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
    void cartographyStopsAtTheHighestTierAndStartsAUniqueFromItsMinimumTier() {
        StarChart top = stored(SINGLE, 1L, 14);
        assertSame(top, use(StarChartCurrency.CARTOGRAPHY, top, 1L));
        assertEquals(StarChart.HIGHEST_TIER, top.tier());
        assertFalse(ItemCrafting.canUse(StarChartCurrency.CARTOGRAPHY, top, ANY_UNIQUE));
        assertNull(use(StarChartCurrency.CARTOGRAPHY, top, 2L));

        StarChart vigil = stored(WARDENS_VIGIL, 3L, 1);
        assertEquals(4, vigil.tier());
        use(StarChartCurrency.CARTOGRAPHY, vigil, 1L);
        assertEquals(5, vigil.tier());
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
        ItemCraftingCosts.register(ItemFixtures.shippedRows(ItemCraftingCosts.DATA_PATH));
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
            picks.merge(ItemDrops.pickChartCurrency(random), 1, Integer::sum);
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
        assertNull(ItemDrops.pickChartCurrency(new Random(1L)));
    }
}
