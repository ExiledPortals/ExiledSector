package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.i18n.Style;
import exiledsector.modifiers.ModifierSlot;
import org.mockito.Mockito;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
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

    private static final String FEEDSTOCK = ItemDisassembly.FEEDSTOCK_ITEM_ID;
    private final ItemStore store = new ItemStore();
    private final CargoAPI cargo = mock(CargoAPI.class);

    @BeforeEach
    void setUp() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(HullItemRows.row(HullItemRows.MILITARY, "bridge", HullItemRows.MILITARY_PREFIXES,
                        HullItemRows.MILITARY_SUFFIXES))
                .put(HullItemRows.row("relic_a", "reactor", "HULL_MULT:4:6; ARMOR_PERCENT:6:9").put("unique", "TRUE"))
                .put(HullItemRows.row("relic_b", "crew_quarters", "FLUX_CAPACITY_MULT:4:6").put("unique", "TRUE")));
        ItemNames.registerWords(new JSONObject().put(HullItemRows.MILITARY, new JSONObject().put("style", "codename")
                .put("first", new JSONArray(List.of("Thule"))).put("second", new JSONArray(List.of("Doctrine")))));
    }

    @AfterEach
    void tearDown() {
        ItemCraftingCosts.clear();
        ItemDefinitions.clear();
        ItemNames.clear();
    }

    private Item common() {
        List<RolledLine> effects = List.of(new RolledLine("FLUX_CAPACITY_MULT", 5f), new RolledLine("ARMOR_PERCENT", 7f));
        return store.add(HullItemRows.data(HullItemRows.MILITARY, 1L, effects, null));
    }

    private void owns(String commodityId, float quantity) {
        when(cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(commodityId))).thenReturn(quantity);
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
        for (RolledLine effect : item.effects()) {
            PoolEntry range = definition.poolEntry(effect.effectName());
            assertTrue(effect.magnitude() >= range.min() && effect.magnitude() <= range.max(), effect.effectName());
        }
        assertFalse(ItemCrafting.canUse(Nanoswarm.AUGMENTATION, item, definitionAllowed -> true));
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
    void uniquesCannotBeAugmentedButCanBeTunedAndMetamorphosed() {
        Item unique = store.add(ItemDefinitions.get("relic_a"), 5L);

        assertFalse(ItemCrafting.canUse(Nanoswarm.AUGMENTATION, unique, definition -> true));
        assertTrue(ItemCrafting.canUse(Nanoswarm.TUNING, unique, definition -> true));
        assertTrue(ItemCrafting.canUse(Nanoswarm.METAMORPH, unique, definition -> true));
        assertFalse(ItemCrafting.canUse(Nanoswarm.METAMORPH, common(), definition -> true));
    }

    @Test
    void tuningKeepsEachModifierAndRerollsItWithinItsRange() {
        Item item = common();
        List<String> before = item.effects().stream().map(RolledLine::effectName).toList();

        for (long seed = 0; seed < 20; seed++) {
            ItemCrafting.tune(item, new Random(seed));
            assertEquals(before, item.effects().stream().map(RolledLine::effectName).toList());
            for (RolledLine effect : item.effects()) {
                PoolEntry range = item.definition().poolEntry(effect.effectName());
                assertTrue(effect.magnitude() >= range.min() && effect.magnitude() <= range.max());
            }
        }
    }

    @Test
    void everyTuningChangesAValueSoTheNanoswarmIsNeverWasted() throws Exception {
        ItemDefinitions.register(new JSONArray().put(HullItemRows.row("narrow", "bridge", "HULL_MULT:4:5", "ARMOR_PERCENT:1:1")));
        Item item = store.add(HullItemRows.data("narrow", 1L,
                List.of(new RolledLine("HULL_MULT", 4f), new RolledLine("ARMOR_PERCENT", 1f)), null));

        for (long seed = 0; seed < 50; seed++) {
            List<RolledLine> before = List.copyOf(item.effects());
            assertSame(item, ItemCrafting.tune(item, new Random(seed)));
            assertNotEquals(before, item.effects());
            assertEquals(1f, item.effects().get(1).magnitude());
        }
    }

    @Test
    void decimalRangesThatStayBelowOneCanBeTuned() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(HullItemRows.row("decimal", "bridge", "HULL_MULT:0.15:0.25", "ARMOR_PERCENT:0.01:0.03"))
                .put(HullItemRows.row("fixed", "bridge", "HULL_MULT:1:1", "ARMOR_PERCENT:2.5:2.5")));
        Item decimal = store.add(HullItemRows.data("decimal", 1L,
                List.of(new RolledLine("HULL_MULT", 0.2f), new RolledLine("ARMOR_PERCENT", 0.02f)), null));
        Item fixed = store.add(HullItemRows.data("fixed", 1L,
                List.of(new RolledLine("HULL_MULT", 1f), new RolledLine("ARMOR_PERCENT", 2.5f)), null));

        assertTrue(ItemCrafting.canUse(Nanoswarm.TUNING, decimal, definition -> true));
        assertFalse(ItemCrafting.canUse(Nanoswarm.TUNING, fixed, definition -> true));
        owns(Nanoswarm.TUNING.itemId(), 1f);
        assertNull(ItemCrafting.use(Nanoswarm.TUNING, fixed, CraftingMaterials.cargoOnly(cargo), store, new Random(1L), definition -> true));
        verify(cargo, never()).removeItems(Mockito.any(), Mockito.any(), anyFloat());
    }

    @Test
    void metamorphSwapsAUniqueForADifferentOneInTheSameStorageSlot() {
        Item first = common();
        Item unique = store.add(ItemDefinitions.get("relic_a"), 5L);

        Item replacement = ItemCrafting.metamorph(store, unique, new Random(2L), definition -> true);

        assertNotNull(replacement);
        assertEquals("relic_b", replacement.definitionId());
        assertEquals(ItemKind.CREW_QUARTERS, replacement.kind());
        assertEquals(unique.id(), replacement.id());
        assertEquals(List.of(first, replacement), store.owned());
        assertSame(replacement, store.find(unique.id()));
    }

    @Test
    void metamorphNeedsAnotherAllowedUnique() {
        Item unique = store.add(ItemDefinitions.get("relic_a"), 5L);

        assertFalse(ItemCrafting.canUse(Nanoswarm.METAMORPH, unique, definition -> !definition.id().equals("relic_b")));
        assertNull(ItemCrafting.metamorph(store, unique, new Random(2L), definition -> false));
    }

    @Test
    void synthesisNeedsEnoughParts() {
        owns(FEEDSTOCK, 2f);

        assertFalse(ItemCrafting.grow(Nanoswarm.AUGMENTATION, CraftingMaterials.cargoOnly(cargo)));
        assertTrue(store.owned().isEmpty());
        verify(cargo, never()).removeItems(Mockito.any(), Mockito.any(), anyFloat());
        verify(cargo, never()).addSpecial(Mockito.any(), anyFloat());
    }

    @Test
    void synthesisingACurrencyTradesItsPartsCostForOneCurrency() {
        owns(FEEDSTOCK, 10f);

        assertTrue(ItemCrafting.grow(Nanoswarm.AUGMENTATION, CraftingMaterials.cargoOnly(cargo)));

        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(FEEDSTOCK), Nanoswarm.AUGMENTATION.feedstockCost());
        verify(cargo).addSpecial(MaterialItems.data(Nanoswarm.AUGMENTATION.itemId()), 1);
    }

    @Test
    void anInhibitorGrowsFromItsPartsCost() {
        owns(FEEDSTOCK, 100f);

        assertTrue(CraftingInhibitor.PREFIX.growable());
        assertTrue(ItemCrafting.grow(CraftingInhibitor.PREFIX, CraftingMaterials.cargoOnly(cargo)));

        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(FEEDSTOCK), 100);
        verify(cargo).addSpecial(MaterialItems.data(CraftingInhibitor.PREFIX.itemId()), 1);
    }

    @Test
    void usingACurrencySpendsOneOnlyWhenItWorks() {
        Item item = common();
        String recalibration = Nanoswarm.TUNING.itemId();
        owns(recalibration, 0f);
        assertNull(ItemCrafting.use(Nanoswarm.TUNING, item, CraftingMaterials.cargoOnly(cargo), store, new Random(1L), definition -> true));
        verify(cargo, never()).removeItems(Mockito.any(), Mockito.any(), anyFloat());

        owns(recalibration, 2f);
        assertSame(item, ItemCrafting.use(Nanoswarm.TUNING, item, CraftingMaterials.cargoOnly(cargo), store, new Random(1L),
                definition -> true));
        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(recalibration), 1);

        owns(Nanoswarm.METAMORPH.itemId(), 1f);
        assertNull(ItemCrafting.use(Nanoswarm.METAMORPH, item, CraftingMaterials.cargoOnly(cargo), store, new Random(1L), definition -> true));
        assertNotEquals(0, item.effects().size());
    }

    @Test
    void costsComeFromTheCraftingCsvAndFallBackToTheDefaults() throws Exception {
        assertEquals(50, Nanoswarm.CORRUPTION.feedstockCost());

        ItemCraftingCosts.register(new JSONArray()
                .put(new JSONObject().put("item", Nanoswarm.CORRUPTION.itemId()).put("partsCost", "55"))
                .put(new JSONObject().put("item", Nanoswarm.RESHAPING.itemId()).put("partsCost", "lots")));

        assertEquals(55, Nanoswarm.CORRUPTION.feedstockCost());
        assertEquals(20, Nanoswarm.RESHAPING.feedstockCost());
    }

    @Test
    void immutableFreezesOneModifierOnceAndTuningLeavesItAlone() {
        Item item = common();

        assertTrue(ItemCrafting.canUse(Nanoswarm.IMMUTABLE, item, definition -> true));
        assertSame(item, ItemCrafting.makeImmutable(item, new Random(4L)));
        RolledModifier locked = item.explicits().stream().filter(RolledModifier::immutable).findFirst().orElseThrow();
        assertEquals(1, item.explicits().stream().filter(RolledModifier::immutable).count());
        assertFalse(ItemCrafting.canUse(Nanoswarm.IMMUTABLE, item, definition -> true));
        assertNull(ItemCrafting.makeImmutable(item, new Random(5L)));
        for (long seed = 0; seed < 20; seed++) {
            ItemCrafting.tune(item, new Random(seed));
            assertTrue(item.explicits().contains(locked), "the fixed effect keeps its rolled value");
        }
        assertTrue(item.effectLines().stream().anyMatch(line -> line.spans().stream().anyMatch(span -> span.style() == Style.IMMUTABLE)));
        assertTrue(item.effectLines().stream().noneMatch(line -> line.plain().contains("(Immutable)")));
    }

    @Test
    void aFixedEffectStaysFixedWhenItsSavedIdIsRemapped() {
        Item item = common();
        ItemCrafting.makeImmutable(item, new Random(4L));
        RolledModifier locked = item.explicits().stream().filter(RolledModifier::immutable).findFirst().orElseThrow();
        RolledModifier legacy = new RolledModifier("RETIRED_" + locked.modifierId(), locked.slot(), locked.lines(), true);

        List<RolledModifier> resolved = item.definition().withCurrentIds(List.of(legacy));

        assertEquals(locked.modifierId(), resolved.get(0).modifierId());
        assertTrue(resolved.get(0).immutable());
    }

    @Test
    void fixingNeedsAtLeastTwoEffects() throws Exception {
        ItemDefinitions.register(new JSONArray().put(HullItemRows.row("single", "bridge", "HULL_MULT:4:6", "")));
        Item single = store.add(HullItemRows.data("single", 1L, List.of(new RolledLine("HULL_MULT", 5f)), null));

        assertFalse(ItemCrafting.canUse(Nanoswarm.IMMUTABLE, single, definition -> true));
    }

    @Test
    void reshapingSwapsAMutableModifierAndKeepsTheModifierCount() {
        Item item = common();
        ItemCrafting.makeImmutable(item, new Random(4L));
        RolledModifier locked = item.explicits().stream().filter(RolledModifier::immutable).findFirst().orElseThrow();

        for (long seed = 0; seed < 20; seed++) {
            List<String> before = item.explicits().stream().map(RolledModifier::modifierId).toList();
            List<ModifierSlot> slotsBefore = item.explicits().stream().map(RolledModifier::slot).toList();
            assertSame(item, ItemCrafting.reshape(item, new Random(seed)));
            List<String> after = item.explicits().stream().map(RolledModifier::modifierId).toList();
            assertEquals(2, after.size());
            assertNotEquals(before, after);
            assertEquals(slotsBefore, item.explicits().stream().map(RolledModifier::slot).toList(), "the new effect takes the removed one's slot");
            assertTrue(item.explicits().contains(locked), "fixed effects are never removed");
        }
    }

    @Test
    void reshapingSkipsUniques() {
        Item unique = store.add(ItemDefinitions.get("relic_a"), 5L);

        assertFalse(ItemCrafting.canUse(Nanoswarm.RESHAPING, unique, definition -> true));
    }

    @Test
    void annullingRemovesOneUnfixedEffectUntilOnlyTheFixedOneIsLeft() {
        Item item = common();
        ItemCrafting.makeImmutable(item, new Random(4L));
        RolledModifier locked = item.explicits().stream().filter(RolledModifier::immutable).findFirst().orElseThrow();

        assertTrue(ItemCrafting.canUse(Nanoswarm.ANNULMENT, item, definition -> true));
        assertSame(item, ItemCrafting.annul(item, new Random(1L)));
        assertEquals(List.of(locked), item.explicits(), "fixed effects are never removed");
        assertFalse(ItemCrafting.canUse(Nanoswarm.ANNULMENT, item, definition -> true));
        assertNull(ItemCrafting.annul(item, new Random(1L)));
    }

    @Test
    void annulmentSkipsUniquesSubroutinesAndGuardedSlots() {
        Item unique = store.add(ItemDefinitions.get("relic_a"), 5L);
        Item item = common();
        Set<ModifierSlot> bothInhibited = EnumSet.allOf(ModifierSlot.class);

        assertFalse(ItemCrafting.canUse(Nanoswarm.ANNULMENT, unique, definition -> true));
        assertFalse(Nanoswarm.ANNULMENT.appliesTo(ItemType.SUBROUTINE));
        assertTrue(Nanoswarm.ANNULMENT.appliesTo(ItemType.HULL_ITEM));
        assertFalse(ItemCrafting.canAnnul(item, bothInhibited));
        assertNull(ItemCrafting.annul(item, new Random(1L), bothInhibited));
        assertEquals(2, item.explicits().size());
    }

    @Test
    void attuningRerollsImplicitValuesWithinTheirRange() throws Exception {
        ItemDefinitions.register(new JSONArray().put(HullItemRows.row("plate", "armor_plating", "HULL_MULT:4:6", "")));
        ItemImplicits.register(new JSONArray().put(new JSONObject().put("section", "armor_plating").put("implicits", "ARMOR_PERCENT:10:20")));
        try {
            Item plate = store.add(ItemDefinitions.get("plate"), 3L);
            List<RolledLine> explicitsBefore = RolledModifier.lines(plate.explicits());

            assertTrue(ItemCrafting.canUse(Nanoswarm.FUNDAMENTAL, plate, definition -> true));
            for (long seed = 0; seed < 20; seed++) {
                float before = plate.implicits().get(0).lines().get(0).magnitude();
                assertSame(plate, ItemCrafting.rerollFundamentals(plate, new Random(seed)));
                float after = plate.implicits().get(0).lines().get(0).magnitude();
                assertNotEquals(before, after);
                assertTrue(after >= 10f && after <= 20f);
            }
            assertEquals(explicitsBefore, RolledModifier.lines(plate.explicits()));
            assertFalse(ItemCrafting.canUse(Nanoswarm.FUNDAMENTAL, common(), definition -> true));
        } finally {
            ItemImplicits.clear();
        }
    }

    @Test
    void replicatingAddsAMirroredCopyThatNoCurrencyCanModify() {
        Item original = common();
        owns(Nanoswarm.REPLICATION.itemId(), 1f);

        Item copy = ItemCrafting.use(Nanoswarm.REPLICATION, original, CraftingMaterials.cargoOnly(cargo), store, new Random(1L),
                definition -> true);

        assertNotNull(copy);
        assertNotEquals(original.id(), copy.id());
        assertEquals(List.of(original, copy), store.owned());
        assertTrue(copy.mirrored());
        assertFalse(original.mirrored());
        assertEquals(original.explicits(), copy.explicits());
        assertEquals(original.implicits(), copy.implicits());
        assertEquals(original.name(), copy.name());
        for (Nanoswarm currency : Nanoswarm.values()) {
            assertFalse(ItemCrafting.canUse(currency, copy, definition -> true), currency + " must not modify a mirrored item");
        }
        assertTrue(ItemCrafting.canUse(Nanoswarm.REPLICATION, original, definition -> true));
        assertTrue(copy.headerLines().stream().anyMatch(line -> line.plain().equals("Mirrored")));
        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(Nanoswarm.REPLICATION.itemId()), 1);
    }

    @Test
    void replicatorNanoseedsCannotBeGrown() {
        owns(FEEDSTOCK, 10_000f);

        assertFalse(Nanoswarm.REPLICATION.growable());
        assertFalse(ItemCrafting.grow(Nanoswarm.REPLICATION, CraftingMaterials.cargoOnly(cargo)));
        verify(cargo, never()).removeItems(Mockito.any(), Mockito.any(), anyFloat());
    }
}
