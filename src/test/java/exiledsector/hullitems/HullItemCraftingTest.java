package exiledsector.hullitems;

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

class HullItemCraftingTest {

    private static final String PARTS = HullItemDisassembly.PARTS_COMMODITY_ID;
    private final HullItemStore store = new HullItemStore();
    private final CargoAPI cargo = mock(CargoAPI.class);

    @BeforeEach
    void setUp() throws Exception {
        HullItemDefinitions.register(new JSONArray()
                .put(HullItemFixtures.row(HullItemFixtures.MILITARY, "subroutine", HullItemFixtures.MILITARY_PREFIXES,
                        HullItemFixtures.MILITARY_SUFFIXES))
                .put(HullItemFixtures.row("relic_a", "reactor", "HULL_MULT:4:6; ARMOR_PERCENT:6:9").put("unique", "TRUE"))
                .put(HullItemFixtures.row("relic_b", "crew_quarters", "FLUX_CAPACITY_MULT:4:6").put("unique", "TRUE")));
        HullItemNames.registerWords(new JSONObject().put(HullItemFixtures.MILITARY, new JSONObject().put("style", "codename")
                .put("first", new JSONArray(List.of("Thule"))).put("second", new JSONArray(List.of("Doctrine")))));
    }

    @AfterEach
    void tearDown() {
        SocketCraftingCosts.clear();
        HullItemDefinitions.clear();
        HullItemNames.clear();
    }

    private HullItem common() {
        List<RolledEffect> effects = List.of(new RolledEffect("FLUX_CAPACITY_MULT", 5f), new RolledEffect("ARMOR_PERCENT", 7f));
        return store.add(new HullItemData(HullItemFixtures.MILITARY, 1L, effects, null));
    }

    private void owns(String commodityId, float quantity) {
        when(cargo.getCommodityQuantity(commodityId)).thenReturn(quantity);
    }

    @Test
    void augmentingFillsUpToTwoPrefixesAndTwoSuffixesWithoutRepeats() {
        HullItem hullItem = common();
        Random random = new Random(3L);

        assertNotNull(HullItemCrafting.augment(hullItem, random));
        assertNotNull(HullItemCrafting.augment(hullItem, random));

        HullItemDefinition definition = hullItem.definition();
        Set<String> names = new HashSet<>();
        hullItem.effects().forEach(effect -> names.add(effect.effectName()));
        assertEquals(4, names.size());
        assertEquals(2, names.stream().filter(definition::isPrefix).count());
        assertEquals(2, names.stream().filter(definition::isSuffix).count());
        assertTrue(definition.isPrefix(hullItem.effects().get(1).effectName()), "prefixes stay ahead of suffixes");
        assertTrue(definition.isSuffix(hullItem.effects().get(2).effectName()), "suffixes follow the prefixes");
        for (RolledEffect effect : hullItem.effects()) {
            PoolEntry range = definition.poolEntry(effect.effectName());
            assertTrue(effect.magnitude() >= range.min() && effect.magnitude() <= range.max(), effect.effectName());
        }
        assertFalse(HullItemCrafting.canUse(SocketCurrency.AUGMENTATION, hullItem, definitionAllowed -> true));
        assertNull(HullItemCrafting.augment(hullItem, random));
    }

    @Test
    void aCommonThatGainsAThirdEffectBecomesRareWithARareName() {
        HullItem hullItem = common();
        assertEquals(HullItemRarity.COMMON, hullItem.rarity());

        HullItemCrafting.augment(hullItem, new Random(1L));

        assertEquals(HullItemRarity.RARE, hullItem.rarity());
        assertEquals("Thule Doctrine", hullItem.name());
    }

    @Test
    void uniquesCannotBeAugmentedButCanBeRecalibratedAndTransposed() {
        HullItem unique = store.add(HullItemDefinitions.get("relic_a"), 5L);

        assertFalse(HullItemCrafting.canUse(SocketCurrency.AUGMENTATION, unique, definition -> true));
        assertTrue(HullItemCrafting.canUse(SocketCurrency.RECALIBRATION, unique, definition -> true));
        assertTrue(HullItemCrafting.canUse(SocketCurrency.TRANSPOSITION, unique, definition -> true));
        assertFalse(HullItemCrafting.canUse(SocketCurrency.TRANSPOSITION, common(), definition -> true));
    }

    @Test
    void recalibratingKeepsEachEffectAndRerollsItWithinItsRange() {
        HullItem hullItem = common();
        List<String> before = hullItem.effects().stream().map(RolledEffect::effectName).toList();

        for (long seed = 0; seed < 20; seed++) {
            HullItemCrafting.recalibrate(hullItem, new Random(seed));
            assertEquals(before, hullItem.effects().stream().map(RolledEffect::effectName).toList());
            for (RolledEffect effect : hullItem.effects()) {
                PoolEntry range = hullItem.definition().poolEntry(effect.effectName());
                assertTrue(effect.magnitude() >= range.min() && effect.magnitude() <= range.max());
            }
        }
    }

    @Test
    void everyRecalibrationChangesAValueSoTheKernelIsNeverWasted() throws Exception {
        HullItemDefinitions.register(new JSONArray().put(HullItemFixtures.row("narrow", "subroutine", "HULL_MULT:4:5", "ARMOR_PERCENT:1:1")));
        HullItem hullItem = store.add(new HullItemData("narrow", 1L,
                List.of(new RolledEffect("HULL_MULT", 4f), new RolledEffect("ARMOR_PERCENT", 1f)), null));

        for (long seed = 0; seed < 50; seed++) {
            List<RolledEffect> before = List.copyOf(hullItem.effects());
            assertSame(hullItem, HullItemCrafting.recalibrate(hullItem, new Random(seed)));
            assertNotEquals(before, hullItem.effects());
            assertEquals(1f, hullItem.effects().get(1).magnitude());
        }
    }

    @Test
    void decimalRangesThatStayBelowOneCanBeRecalibrated() throws Exception {
        HullItemDefinitions.register(new JSONArray()
                .put(HullItemFixtures.row("decimal", "subroutine", "HULL_MULT:0.15:0.25", "ARMOR_PERCENT:0.01:0.03"))
                .put(HullItemFixtures.row("fixed", "subroutine", "HULL_MULT:1:1", "ARMOR_PERCENT:2.5:2.5")));
        HullItem decimal = store.add(new HullItemData("decimal", 1L,
                List.of(new RolledEffect("HULL_MULT", 0.2f), new RolledEffect("ARMOR_PERCENT", 0.02f)), null));
        HullItem fixed = store.add(new HullItemData("fixed", 1L,
                List.of(new RolledEffect("HULL_MULT", 1f), new RolledEffect("ARMOR_PERCENT", 2.5f)), null));

        assertTrue(HullItemCrafting.canUse(SocketCurrency.RECALIBRATION, decimal, definition -> true));
        assertFalse(HullItemCrafting.canUse(SocketCurrency.RECALIBRATION, fixed, definition -> true));
        owns(SocketCurrency.RECALIBRATION.commodityId(), 1f);
        assertNull(HullItemCrafting.use(SocketCurrency.RECALIBRATION, fixed, SocketMaterials.cargoOnly(cargo), store, new Random(1L), definition -> true));
        verify(cargo, never()).removeCommodity(anyString(), anyFloat());
    }

    @Test
    void transposingSwapsAUniqueForADifferentOneInTheSameStorageSlot() {
        HullItem first = common();
        HullItem unique = store.add(HullItemDefinitions.get("relic_a"), 5L);

        HullItem replacement = HullItemCrafting.transpose(store, unique, new Random(2L), definition -> true);

        assertNotNull(replacement);
        assertEquals("relic_b", replacement.definitionId());
        assertEquals(HullItemType.CREW_QUARTERS, replacement.kind());
        assertEquals(unique.id(), replacement.id());
        assertEquals(List.of(first, replacement), store.owned());
        assertSame(replacement, store.find(unique.id()));
    }

    @Test
    void transposingNeedsAnotherAllowedUnique() {
        HullItem unique = store.add(HullItemDefinitions.get("relic_a"), 5L);

        assertFalse(HullItemCrafting.canUse(SocketCurrency.TRANSPOSITION, unique, definition -> !definition.id().equals("relic_b")));
        assertNull(HullItemCrafting.transpose(store, unique, new Random(2L), definition -> false));
    }

    @Test
    void synthesisingACommonSpendsPartsAndStoresAOnePrefixOneSuffixSubroutine() {
        owns(PARTS, 12f);

        HullItem created = HullItemCrafting.synthesiseCommon(SocketMaterials.cargoOnly(cargo), store, new Random(4L));

        assertNotNull(created);
        assertEquals(HullItemRarity.COMMON, created.rarity());
        assertEquals(2, created.effects().size());
        assertTrue(created.definition().isPrefix(created.effects().get(0).effectName()));
        assertTrue(created.definition().isSuffix(created.effects().get(1).effectName()));
        assertTrue(store.owned().contains(created));
        verify(cargo).removeCommodity(PARTS, HullItemCrafting.commonSynthesisParts());
    }

    @Test
    void synthesisNeedsEnoughParts() {
        owns(PARTS, 2f);

        assertNull(HullItemCrafting.synthesiseCommon(SocketMaterials.cargoOnly(cargo), store, new Random(4L)));
        assertFalse(HullItemCrafting.synthesise(SocketCurrency.AUGMENTATION, SocketMaterials.cargoOnly(cargo)));
        assertTrue(store.owned().isEmpty());
        verify(cargo, never()).removeCommodity(anyString(), anyFloat());
        verify(cargo, never()).addCommodity(anyString(), anyFloat());
    }

    @Test
    void synthesisingACurrencyTradesItsPartsCostForOneCurrency() {
        owns(PARTS, 10f);

        assertTrue(HullItemCrafting.synthesise(SocketCurrency.AUGMENTATION, SocketMaterials.cargoOnly(cargo)));

        verify(cargo).removeCommodity(PARTS, SocketCurrency.AUGMENTATION.partsCost());
        verify(cargo).addCommodity(SocketCurrency.AUGMENTATION.commodityId(), 1);
    }

    @Test
    void usingACurrencySpendsOneOnlyWhenItWorks() {
        HullItem hullItem = common();
        String recalibration = SocketCurrency.RECALIBRATION.commodityId();
        owns(recalibration, 0f);
        assertNull(HullItemCrafting.use(SocketCurrency.RECALIBRATION, hullItem, SocketMaterials.cargoOnly(cargo), store, new Random(1L), definition -> true));
        verify(cargo, never()).removeCommodity(anyString(), anyFloat());

        owns(recalibration, 2f);
        assertSame(hullItem, HullItemCrafting.use(SocketCurrency.RECALIBRATION, hullItem, SocketMaterials.cargoOnly(cargo), store, new Random(1L),
                definition -> true));
        verify(cargo).removeCommodity(recalibration, 1);

        owns(SocketCurrency.TRANSPOSITION.commodityId(), 1f);
        assertNull(HullItemCrafting.use(SocketCurrency.TRANSPOSITION, hullItem, SocketMaterials.cargoOnly(cargo), store, new Random(1L), definition -> true));
        assertNotEquals(0, hullItem.effects().size());
    }

    @Test
    void costsComeFromTheCraftingCsvAndFallBackToTheDefaults() throws Exception {
        assertEquals(10, HullItemCrafting.commonSynthesisParts());
        assertEquals(100, SocketCurrency.TRANSPOSITION.partsCost());

        SocketCraftingCosts.register(new JSONArray()
                .put(new JSONObject().put("item", SocketCraftingCosts.COMMON_SUBROUTINE).put("partsCost", "7"))
                .put(new JSONObject().put("item", SocketCurrency.TRANSPOSITION.commodityId()).put("partsCost", "55"))
                .put(new JSONObject().put("item", SocketCurrency.RECALIBRATION.commodityId()).put("partsCost", "lots")));

        assertEquals(7, HullItemCrafting.commonSynthesisParts());
        assertEquals(55, SocketCurrency.TRANSPOSITION.partsCost());
        assertEquals(20, SocketCurrency.RECALIBRATION.partsCost());
    }
}
