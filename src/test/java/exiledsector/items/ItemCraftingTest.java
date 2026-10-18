package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemCraftingTest {

    private static final String PARTS = ItemDisassembly.PARTS_COMMODITY_ID;
    private final ItemStore store = new ItemStore();
    private final CargoAPI cargo = mock(CargoAPI.class);

    @BeforeEach
    void setUp() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(ItemFixtures.row(ItemFixtures.MILITARY, "subroutine", ItemFixtures.MILITARY_PREFIXES,
                        ItemFixtures.MILITARY_SUFFIXES))
                .put(ItemFixtures.row("relic_a", "reactor", "HULL_MULT:4:6; ARMOR_PERCENT:6:9").put("unique", "TRUE"))
                .put(ItemFixtures.row("relic_b", "crew_quarters", "FLUX_CAPACITY_MULT:4:6").put("unique", "TRUE")));
        ItemNames.registerWords(new JSONObject().put(ItemFixtures.MILITARY, new JSONObject().put("style", "codename")
                .put("first", new JSONArray(List.of("Thule"))).put("second", new JSONArray(List.of("Doctrine")))));
    }

    @AfterEach
    void tearDown() {
        ItemCraftingCosts.clear();
        ItemDefinitions.clear();
        ItemNames.clear();
    }

    private Item common() {
        List<RolledEffect> effects = List.of(new RolledEffect("FLUX_CAPACITY_MULT", 5f), new RolledEffect("ARMOR_PERCENT", 7f));
        return store.add(ItemFixtures.data(ItemFixtures.MILITARY, 1L, effects, null));
    }

    private void owns(String commodityId, float quantity) {
        when(cargo.getCommodityQuantity(commodityId)).thenReturn(quantity);
    }

    @Test
    void augmentingFillsUpToTwoPrefixesAndTwoSuffixesWithoutRepeats() {
        Item item = common();
        Random random = new Random(3L);

        assertNotNull(ItemCrafting.augment(item, random));
        assertNotNull(ItemCrafting.augment(item, random));

        ItemDefinition definition = item.definition();
        Set<String> names = new HashSet<>();
        item.effects().forEach(effect -> names.add(effect.effectName()));
        assertEquals(4, names.size());
        assertEquals(2, names.stream().filter(definition::isPrefix).count());
        assertEquals(2, names.stream().filter(definition::isSuffix).count());
        assertTrue(definition.isPrefix(item.effects().get(1).effectName()), "prefixes stay ahead of suffixes");
        assertTrue(definition.isSuffix(item.effects().get(2).effectName()), "suffixes follow the prefixes");
        for (RolledEffect effect : item.effects()) {
            PoolEntry range = definition.poolEntry(effect.effectName());
            assertTrue(effect.magnitude() >= range.min() && effect.magnitude() <= range.max(), effect.effectName());
        }
        assertFalse(ItemCrafting.canUse(CraftingCurrency.AUGMENTATION, item, definitionAllowed -> true));
        assertNull(ItemCrafting.augment(item, random));
    }

    @Test
    void aCommonThatGainsAThirdEffectBecomesRareWithARareName() {
        Item item = common();
        assertEquals(ItemRarity.COMMON, item.rarity());

        ItemCrafting.augment(item, new Random(1L));

        assertEquals(ItemRarity.RARE, item.rarity());
        assertEquals("Thule Doctrine", item.name());
    }

    @Test
    void uniquesCannotBeAugmentedButCanBeRecalibratedAndTransposed() {
        Item unique = store.add(ItemDefinitions.get("relic_a"), 5L);

        assertFalse(ItemCrafting.canUse(CraftingCurrency.AUGMENTATION, unique, definition -> true));
        assertTrue(ItemCrafting.canUse(CraftingCurrency.RECALIBRATION, unique, definition -> true));
        assertTrue(ItemCrafting.canUse(CraftingCurrency.TRANSPOSITION, unique, definition -> true));
        assertFalse(ItemCrafting.canUse(CraftingCurrency.TRANSPOSITION, common(), definition -> true));
    }

    @Test
    void recalibratingKeepsEachEffectAndRerollsItWithinItsRange() {
        Item item = common();
        List<String> before = item.effects().stream().map(RolledEffect::effectName).toList();

        for (long seed = 0; seed < 20; seed++) {
            ItemCrafting.recalibrate(item, new Random(seed));
            assertEquals(before, item.effects().stream().map(RolledEffect::effectName).toList());
            for (RolledEffect effect : item.effects()) {
                PoolEntry range = item.definition().poolEntry(effect.effectName());
                assertTrue(effect.magnitude() >= range.min() && effect.magnitude() <= range.max());
            }
        }
    }

    @Test
    void everyRecalibrationChangesAValueSoTheKernelIsNeverWasted() throws Exception {
        ItemDefinitions.register(new JSONArray().put(ItemFixtures.row("narrow", "subroutine", "HULL_MULT:4:5", "ARMOR_PERCENT:1:1")));
        Item item = store.add(ItemFixtures.data("narrow", 1L,
                List.of(new RolledEffect("HULL_MULT", 4f), new RolledEffect("ARMOR_PERCENT", 1f)), null));

        for (long seed = 0; seed < 50; seed++) {
            List<RolledEffect> before = List.copyOf(item.effects());
            assertSame(item, ItemCrafting.recalibrate(item, new Random(seed)));
            assertNotEquals(before, item.effects());
            assertEquals(1f, item.effects().get(1).magnitude());
        }
    }

    @Test
    void decimalRangesThatStayBelowOneCanBeRecalibrated() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(ItemFixtures.row("decimal", "subroutine", "HULL_MULT:0.15:0.25", "ARMOR_PERCENT:0.01:0.03"))
                .put(ItemFixtures.row("fixed", "subroutine", "HULL_MULT:1:1", "ARMOR_PERCENT:2.5:2.5")));
        Item decimal = store.add(ItemFixtures.data("decimal", 1L,
                List.of(new RolledEffect("HULL_MULT", 0.2f), new RolledEffect("ARMOR_PERCENT", 0.02f)), null));
        Item fixed = store.add(ItemFixtures.data("fixed", 1L,
                List.of(new RolledEffect("HULL_MULT", 1f), new RolledEffect("ARMOR_PERCENT", 2.5f)), null));

        assertTrue(ItemCrafting.canUse(CraftingCurrency.RECALIBRATION, decimal, definition -> true));
        assertFalse(ItemCrafting.canUse(CraftingCurrency.RECALIBRATION, fixed, definition -> true));
        owns(CraftingCurrency.RECALIBRATION.commodityId(), 1f);
        assertNull(ItemCrafting.use(CraftingCurrency.RECALIBRATION, fixed, CraftingMaterials.cargoOnly(cargo), store, new Random(1L), definition -> true));
        verify(cargo, never()).removeCommodity(anyString(), anyFloat());
    }

    @Test
    void transposingSwapsAUniqueForADifferentOneInTheSameStorageSlot() {
        Item first = common();
        Item unique = store.add(ItemDefinitions.get("relic_a"), 5L);

        Item replacement = ItemCrafting.transpose(store, unique, new Random(2L), definition -> true);

        assertNotNull(replacement);
        assertEquals("relic_b", replacement.definitionId());
        assertEquals(ItemKind.CREW_QUARTERS, replacement.kind());
        assertEquals(unique.id(), replacement.id());
        assertEquals(List.of(first, replacement), store.owned());
        assertSame(replacement, store.find(unique.id()));
    }

    @Test
    void transposingNeedsAnotherAllowedUnique() {
        Item unique = store.add(ItemDefinitions.get("relic_a"), 5L);

        assertFalse(ItemCrafting.canUse(CraftingCurrency.TRANSPOSITION, unique, definition -> !definition.id().equals("relic_b")));
        assertNull(ItemCrafting.transpose(store, unique, new Random(2L), definition -> false));
    }

    @Test
    void synthesisingACommonSpendsPartsAndStoresAOnePrefixOneSuffixSubroutine() {
        owns(PARTS, 12f);

        Item created = ItemCrafting.synthesiseCommon(CraftingMaterials.cargoOnly(cargo), store, new Random(4L));

        assertNotNull(created);
        assertEquals(ItemRarity.COMMON, created.rarity());
        assertEquals(2, created.effects().size());
        assertTrue(created.definition().isPrefix(created.effects().get(0).effectName()));
        assertTrue(created.definition().isSuffix(created.effects().get(1).effectName()));
        assertTrue(store.owned().contains(created));
        verify(cargo).removeCommodity(PARTS, ItemCrafting.commonSynthesisParts());
    }

    @Test
    void synthesisNeedsEnoughParts() {
        owns(PARTS, 2f);

        assertNull(ItemCrafting.synthesiseCommon(CraftingMaterials.cargoOnly(cargo), store, new Random(4L)));
        assertFalse(ItemCrafting.synthesise(CraftingCurrency.AUGMENTATION, CraftingMaterials.cargoOnly(cargo)));
        assertTrue(store.owned().isEmpty());
        verify(cargo, never()).removeCommodity(anyString(), anyFloat());
        verify(cargo, never()).addCommodity(anyString(), anyFloat());
    }

    @Test
    void synthesisingACurrencyTradesItsPartsCostForOneCurrency() {
        owns(PARTS, 10f);

        assertTrue(ItemCrafting.synthesise(CraftingCurrency.AUGMENTATION, CraftingMaterials.cargoOnly(cargo)));

        verify(cargo).removeCommodity(PARTS, CraftingCurrency.AUGMENTATION.partsCost());
        verify(cargo).addCommodity(CraftingCurrency.AUGMENTATION.commodityId(), 1);
    }

    @Test
    void usingACurrencySpendsOneOnlyWhenItWorks() {
        Item item = common();
        String recalibration = CraftingCurrency.RECALIBRATION.commodityId();
        owns(recalibration, 0f);
        assertNull(ItemCrafting.use(CraftingCurrency.RECALIBRATION, item, CraftingMaterials.cargoOnly(cargo), store, new Random(1L), definition -> true));
        verify(cargo, never()).removeCommodity(anyString(), anyFloat());

        owns(recalibration, 2f);
        assertSame(item, ItemCrafting.use(CraftingCurrency.RECALIBRATION, item, CraftingMaterials.cargoOnly(cargo), store, new Random(1L),
                definition -> true));
        verify(cargo).removeCommodity(recalibration, 1);

        owns(CraftingCurrency.TRANSPOSITION.commodityId(), 1f);
        assertNull(ItemCrafting.use(CraftingCurrency.TRANSPOSITION, item, CraftingMaterials.cargoOnly(cargo), store, new Random(1L), definition -> true));
        assertNotEquals(0, item.effects().size());
    }

    @Test
    void costsComeFromTheCraftingCsvAndFallBackToTheDefaults() throws Exception {
        assertEquals(10, ItemCrafting.commonSynthesisParts());
        assertEquals(100, CraftingCurrency.TRANSPOSITION.partsCost());

        ItemCraftingCosts.register(new JSONArray()
                .put(new JSONObject().put("item", ItemCraftingCosts.COMMON_SUBROUTINE).put("partsCost", "7"))
                .put(new JSONObject().put("item", CraftingCurrency.TRANSPOSITION.commodityId()).put("partsCost", "55"))
                .put(new JSONObject().put("item", CraftingCurrency.RECALIBRATION.commodityId()).put("partsCost", "lots")));

        assertEquals(7, ItemCrafting.commonSynthesisParts());
        assertEquals(55, CraftingCurrency.TRANSPOSITION.partsCost());
        assertEquals(20, CraftingCurrency.RECALIBRATION.partsCost());
    }
}
