package exiledsector.socketables;

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

class SocketableCraftingTest {

    private static final String PARTS = SocketableDisassembly.PARTS_COMMODITY_ID;
    private final SocketableStore store = new SocketableStore();
    private final CargoAPI cargo = mock(CargoAPI.class);

    @BeforeEach
    void setUp() throws Exception {
        SocketableDefinitions.register(new JSONArray()
                .put(SocketableFixtures.row(SocketableFixtures.MILITARY, "subroutine", SocketableFixtures.MILITARY_PREFIXES,
                        SocketableFixtures.MILITARY_SUFFIXES))
                .put(SocketableFixtures.row("relic_a", "ai_core", "HULL_MULT:4:6; ARMOR_PERCENT:6:9").put("unique", "TRUE"))
                .put(SocketableFixtures.row("relic_b", "team", "FLUX_CAPACITY_MULT:4:6").put("unique", "TRUE")));
        SocketableNames.registerWords(new JSONObject().put(SocketableFixtures.MILITARY, new JSONObject().put("style", "codename")
                .put("first", new JSONArray(List.of("Thule"))).put("second", new JSONArray(List.of("Doctrine")))));
    }

    @AfterEach
    void tearDown() {
        SocketCraftingCosts.clear();
        SocketableDefinitions.clear();
        SocketableNames.clear();
    }

    private Socketable common() {
        List<RolledEffect> effects = List.of(new RolledEffect("FLUX_CAPACITY_MULT", 5f), new RolledEffect("ARMOR_PERCENT", 7f));
        return store.add(new SocketableItemData(SocketableFixtures.MILITARY, 1L, effects, null));
    }

    private void owns(String commodityId, float quantity) {
        when(cargo.getCommodityQuantity(commodityId)).thenReturn(quantity);
    }

    @Test
    void augmentingFillsUpToTwoPrefixesAndTwoSuffixesWithoutRepeats() {
        Socketable socketable = common();
        Random random = new Random(3L);

        assertNotNull(SocketableCrafting.augment(socketable, random));
        assertNotNull(SocketableCrafting.augment(socketable, random));

        SocketableDefinition definition = socketable.definition();
        Set<String> names = new HashSet<>();
        socketable.effects().forEach(effect -> names.add(effect.effectName()));
        assertEquals(4, names.size());
        assertEquals(2, names.stream().filter(definition::isPrefix).count());
        assertEquals(2, names.stream().filter(definition::isSuffix).count());
        assertTrue(definition.isPrefix(socketable.effects().get(1).effectName()), "prefixes stay ahead of suffixes");
        assertTrue(definition.isSuffix(socketable.effects().get(2).effectName()), "suffixes follow the prefixes");
        for (RolledEffect effect : socketable.effects()) {
            PoolEntry range = definition.poolEntry(effect.effectName());
            assertTrue(effect.magnitude() >= range.min() && effect.magnitude() <= range.max(), effect.effectName());
        }
        assertFalse(SocketableCrafting.canUse(SocketCurrency.AUGMENTATION, socketable, definitionAllowed -> true));
        assertNull(SocketableCrafting.augment(socketable, random));
    }

    @Test
    void aCommonThatGainsAThirdEffectBecomesRareWithARareName() {
        Socketable socketable = common();
        assertEquals(SocketableRarity.COMMON, socketable.rarity());

        SocketableCrafting.augment(socketable, new Random(1L));

        assertEquals(SocketableRarity.RARE, socketable.rarity());
        assertEquals("Thule Doctrine", socketable.name());
    }

    @Test
    void uniquesCannotBeAugmentedButCanBeRecalibratedAndTransposed() {
        Socketable unique = store.add(SocketableDefinitions.get("relic_a"), 5L);

        assertFalse(SocketableCrafting.canUse(SocketCurrency.AUGMENTATION, unique, definition -> true));
        assertTrue(SocketableCrafting.canUse(SocketCurrency.RECALIBRATION, unique, definition -> true));
        assertTrue(SocketableCrafting.canUse(SocketCurrency.TRANSPOSITION, unique, definition -> true));
        assertFalse(SocketableCrafting.canUse(SocketCurrency.TRANSPOSITION, common(), definition -> true));
    }

    @Test
    void recalibratingKeepsEachEffectAndRerollsItWithinItsRange() {
        Socketable socketable = common();
        List<String> before = socketable.effects().stream().map(RolledEffect::effectName).toList();

        for (long seed = 0; seed < 20; seed++) {
            SocketableCrafting.recalibrate(socketable, new Random(seed));
            assertEquals(before, socketable.effects().stream().map(RolledEffect::effectName).toList());
            for (RolledEffect effect : socketable.effects()) {
                PoolEntry range = socketable.definition().poolEntry(effect.effectName());
                assertTrue(effect.magnitude() >= range.min() && effect.magnitude() <= range.max());
            }
        }
    }

    @Test
    void everyRecalibrationChangesAValueSoTheKernelIsNeverWasted() throws Exception {
        SocketableDefinitions.register(new JSONArray().put(SocketableFixtures.row("narrow", "subroutine", "HULL_MULT:4:5", "ARMOR_PERCENT:1:1")));
        Socketable socketable = store.add(new SocketableItemData("narrow", 1L,
                List.of(new RolledEffect("HULL_MULT", 4f), new RolledEffect("ARMOR_PERCENT", 1f)), null));

        for (long seed = 0; seed < 50; seed++) {
            List<RolledEffect> before = List.copyOf(socketable.effects());
            assertSame(socketable, SocketableCrafting.recalibrate(socketable, new Random(seed)));
            assertNotEquals(before, socketable.effects());
            assertEquals(1f, socketable.effects().get(1).magnitude());
        }
    }

    @Test
    void decimalRangesThatStayBelowOneCanBeRecalibrated() throws Exception {
        SocketableDefinitions.register(new JSONArray()
                .put(SocketableFixtures.row("decimal", "subroutine", "HULL_MULT:0.15:0.25", "ARMOR_PERCENT:0.01:0.03"))
                .put(SocketableFixtures.row("fixed", "subroutine", "HULL_MULT:1:1", "ARMOR_PERCENT:2.5:2.5")));
        Socketable decimal = store.add(new SocketableItemData("decimal", 1L,
                List.of(new RolledEffect("HULL_MULT", 0.2f), new RolledEffect("ARMOR_PERCENT", 0.02f)), null));
        Socketable fixed = store.add(new SocketableItemData("fixed", 1L,
                List.of(new RolledEffect("HULL_MULT", 1f), new RolledEffect("ARMOR_PERCENT", 2.5f)), null));

        assertTrue(SocketableCrafting.canUse(SocketCurrency.RECALIBRATION, decimal, definition -> true));
        assertFalse(SocketableCrafting.canUse(SocketCurrency.RECALIBRATION, fixed, definition -> true));
        owns(SocketCurrency.RECALIBRATION.commodityId(), 1f);
        assertNull(SocketableCrafting.use(SocketCurrency.RECALIBRATION, fixed, cargo, store, new Random(1L), definition -> true));
        verify(cargo, never()).removeCommodity(anyString(), anyFloat());
    }

    @Test
    void transposingSwapsAUniqueForADifferentOneInTheSameStorageSlot() {
        Socketable first = common();
        Socketable unique = store.add(SocketableDefinitions.get("relic_a"), 5L);

        Socketable replacement = SocketableCrafting.transpose(store, unique, new Random(2L), definition -> true);

        assertNotNull(replacement);
        assertEquals("relic_b", replacement.definitionId());
        assertEquals(SocketableKind.TEAM, replacement.kind());
        assertEquals(unique.id(), replacement.id());
        assertEquals(List.of(first, replacement), store.owned());
        assertSame(replacement, store.find(unique.id()));
    }

    @Test
    void transposingNeedsAnotherAllowedUnique() {
        Socketable unique = store.add(SocketableDefinitions.get("relic_a"), 5L);

        assertFalse(SocketableCrafting.canUse(SocketCurrency.TRANSPOSITION, unique, definition -> !definition.id().equals("relic_b")));
        assertNull(SocketableCrafting.transpose(store, unique, new Random(2L), definition -> false));
    }

    @Test
    void synthesisingACommonSpendsPartsAndStoresAOnePrefixOneSuffixSubroutine() {
        owns(PARTS, 12f);

        Socketable created = SocketableCrafting.synthesiseCommon(cargo, store, new Random(4L));

        assertNotNull(created);
        assertEquals(SocketableRarity.COMMON, created.rarity());
        assertEquals(2, created.effects().size());
        assertTrue(created.definition().isPrefix(created.effects().get(0).effectName()));
        assertTrue(created.definition().isSuffix(created.effects().get(1).effectName()));
        assertTrue(store.owned().contains(created));
        verify(cargo).removeCommodity(PARTS, SocketableCrafting.commonSynthesisParts());
    }

    @Test
    void synthesisNeedsEnoughParts() {
        owns(PARTS, 2f);

        assertNull(SocketableCrafting.synthesiseCommon(cargo, store, new Random(4L)));
        assertFalse(SocketableCrafting.synthesise(SocketCurrency.AUGMENTATION, cargo));
        assertTrue(store.owned().isEmpty());
        verify(cargo, never()).removeCommodity(anyString(), anyFloat());
        verify(cargo, never()).addCommodity(anyString(), anyFloat());
    }

    @Test
    void synthesisingACurrencyTradesItsPartsCostForOneCurrency() {
        owns(PARTS, 10f);

        assertTrue(SocketableCrafting.synthesise(SocketCurrency.AUGMENTATION, cargo));

        verify(cargo).removeCommodity(PARTS, SocketCurrency.AUGMENTATION.partsCost());
        verify(cargo).addCommodity(SocketCurrency.AUGMENTATION.commodityId(), 1);
    }

    @Test
    void usingACurrencySpendsOneOnlyWhenItWorks() {
        Socketable socketable = common();
        String recalibration = SocketCurrency.RECALIBRATION.commodityId();
        owns(recalibration, 0f);
        assertNull(SocketableCrafting.use(SocketCurrency.RECALIBRATION, socketable, cargo, store, new Random(1L), definition -> true));
        verify(cargo, never()).removeCommodity(anyString(), anyFloat());

        owns(recalibration, 2f);
        assertSame(socketable, SocketableCrafting.use(SocketCurrency.RECALIBRATION, socketable, cargo, store, new Random(1L),
                definition -> true));
        verify(cargo).removeCommodity(recalibration, 1);

        owns(SocketCurrency.TRANSPOSITION.commodityId(), 1f);
        assertNull(SocketableCrafting.use(SocketCurrency.TRANSPOSITION, socketable, cargo, store, new Random(1L), definition -> true));
        assertNotEquals(0, socketable.effects().size());
    }

    @Test
    void costsComeFromTheCraftingCsvAndFallBackToTheDefaults() throws Exception {
        assertEquals(10, SocketableCrafting.commonSynthesisParts());
        assertEquals(100, SocketCurrency.TRANSPOSITION.partsCost());

        SocketCraftingCosts.register(new JSONArray()
                .put(new JSONObject().put("item", SocketCraftingCosts.COMMON_SUBROUTINE).put("partsCost", "7"))
                .put(new JSONObject().put("item", SocketCurrency.TRANSPOSITION.commodityId()).put("partsCost", "55"))
                .put(new JSONObject().put("item", SocketCurrency.RECALIBRATION.commodityId()).put("partsCost", "lots")));

        assertEquals(7, SocketableCrafting.commonSynthesisParts());
        assertEquals(55, SocketCurrency.TRANSPOSITION.partsCost());
        assertEquals(20, SocketCurrency.RECALIBRATION.partsCost());
    }
}
