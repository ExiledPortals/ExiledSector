package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CraftingWardTest {

    private final ItemStore store = new ItemStore();
    private final CargoAPI cargo = mock(CargoAPI.class);

    @BeforeEach
    void setUp() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(HullItemRows.row(HullItemRows.MILITARY, "bridge", HullItemRows.MILITARY_PREFIXES, HullItemRows.MILITARY_SUFFIXES))
                .put(HullItemRows.row("relic_a", "reactor", "HULL_MULT:4:6; ARMOR_PERCENT:6:9").put("unique", "TRUE"))
                .put(HullItemRows.row("relic_b", "crew_quarters", "FLUX_CAPACITY_MULT:4:6").put("unique", "TRUE")));
        ItemNames.registerWords(new JSONObject().put(HullItemRows.MILITARY, new JSONObject().put("style", "codename")
                .put("first", new JSONArray(List.of("Thule"))).put("second", new JSONArray(List.of("Doctrine")))));
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
        ItemNames.clear();
    }

    private Item rare() {
        List<RolledEffect> effects = List.of(new RolledEffect("FLUX_CAPACITY_MULT", 5f), new RolledEffect("FLUX_DISSIPATION_MULT", 5f),
                new RolledEffect("ARMOR_PERCENT", 7f), new RolledEffect("HULL_MULT", 5f));
        return store.add(HullItemRows.data(HullItemRows.MILITARY, 1L, effects, null));
    }

    private static List<RolledModifier> side(Item item, ModifierSlot slot) {
        return item.explicits().stream().filter(rolled -> rolled.slot() == slot).toList();
    }

    private void owns(String itemId, float quantity) {
        when(cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(itemId))).thenReturn(quantity);
    }

    @Test
    void aPrefixWardKeepsRecalibrationOffEveryPrefix() {
        Item item = rare();
        List<RolledModifier> prefixes = side(item, ModifierSlot.PREFIX);

        for (long seed = 0; seed < 20; seed++) {
            assertNotNull(ItemCrafting.recalibrate(item, new Random(seed), Set.of(ModifierSlot.PREFIX)));
            assertEquals(prefixes, side(item, ModifierSlot.PREFIX));
        }
    }

    @Test
    void aSuffixWardKeepsSubstitutionOffEverySuffix() {
        Item item = rare();
        List<RolledModifier> suffixes = side(item, ModifierSlot.SUFFIX);

        for (long seed = 0; seed < 20; seed++) {
            ItemCrafting.substitute(item, new Random(seed), Set.of(ModifierSlot.SUFFIX));
            assertEquals(suffixes, side(item, ModifierSlot.SUFFIX));
        }
    }

    @Test
    void aWardPicksWhichSideFixationLands() {
        for (long seed = 0; seed < 20; seed++) {
            Item item = rare();
            ItemCrafting.fixate(item, new Random(seed), Set.of(ModifierSlot.PREFIX));
            assertTrue(side(item, ModifierSlot.SUFFIX).stream().anyMatch(RolledModifier::locked));
            assertFalse(side(item, ModifierSlot.PREFIX).stream().anyMatch(RolledModifier::locked));
        }
    }

    @Test
    void wardsBlockTranspositionButLeaveCorruptionAndReplicationAlone() {
        Item unique = store.add(ItemDefinitions.get("relic_a"), 5L);
        List<CraftingWard> prefixWard = List.of(CraftingWard.PREFIX);

        assertTrue(ItemCrafting.canUse(CraftingCurrency.TRANSPOSITION, unique, definition -> true));
        assertFalse(ItemCrafting.canUse(CraftingCurrency.TRANSPOSITION, unique, definition -> true, prefixWard));
        assertTrue(ItemCrafting.canUse(CraftingCurrency.CORRUPTION, unique, definition -> true, prefixWard));
        assertTrue(ItemCrafting.canUse(CraftingCurrency.REPLICATION, unique, definition -> true, prefixWard));
        assertTrue(ItemCrafting.wardsSpentBy(CraftingCurrency.CORRUPTION, unique, prefixWard).isEmpty());
        assertTrue(ItemCrafting.wardsSpentBy(CraftingCurrency.REPLICATION, unique, prefixWard).isEmpty());
        assertTrue(ItemCrafting.wardsSpentBy(CraftingCurrency.AUGMENTATION, rare(), prefixWard).isEmpty());
    }

    @Test
    void aWardIsSpentOnlyWhenItProtectsAnEffect() {
        Item item = rare();
        owns(CraftingCurrency.RECALIBRATION.itemId(), 1f);
        owns(CraftingWard.PREFIX.itemId(), 1f);

        assertEquals(Set.of(CraftingWard.PREFIX), ItemCrafting.wardsSpentBy(CraftingCurrency.RECALIBRATION, item, List.of(CraftingWard.PREFIX)));
        assertNotNull(ItemCrafting.use(CraftingCurrency.RECALIBRATION, item, CraftingMaterials.cargoOnly(cargo), store, new Random(3L),
                definition -> true, List.of(CraftingWard.PREFIX)));

        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(CraftingWard.PREFIX.itemId()), 1);
        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(CraftingCurrency.RECALIBRATION.itemId()), 1);
    }

    @Test
    void aWardGuardingOnlyAFixedEffectIsNotSpent() throws Exception {
        ItemDefinitions.register(new JSONArray().put(HullItemRows.row("lean", "bridge", "HULL_MULT:4:6", "ARMOR_PERCENT:6:9")));
        Item item = store.add(HullItemRows.data("lean", 1L, List.of(new RolledEffect("HULL_MULT", 5f), new RolledEffect("ARMOR_PERCENT", 7f)), null));
        ItemCrafting.fixate(item, new Random(1L), Set.of(ModifierSlot.SUFFIX));

        assertTrue(side(item, ModifierSlot.PREFIX).get(0).locked());
        assertTrue(ItemCrafting.wardsSpentBy(CraftingCurrency.RECALIBRATION, item, List.of(CraftingWard.PREFIX)).isEmpty());
        assertTrue(ItemCrafting.wardsSpentBy(CraftingCurrency.SUBSTITUTION, item, List.of(CraftingWard.PREFIX)).isEmpty());
        assertEquals(Set.of(CraftingWard.SUFFIX), ItemCrafting.wardsSpentBy(CraftingCurrency.RECALIBRATION, item, List.of(CraftingWard.SUFFIX)));
    }

    @Test
    void aWardYouDoNotOwnStopsTheModification() {
        Item item = rare();
        owns(CraftingCurrency.RECALIBRATION.itemId(), 1f);
        owns(CraftingWard.SUFFIX.itemId(), 0f);

        assertEquals(null, ItemCrafting.use(CraftingCurrency.RECALIBRATION, item, CraftingMaterials.cargoOnly(cargo), store, new Random(3L),
                definition -> true, List.of(CraftingWard.SUFFIX)));
        verify(cargo, never()).removeItems(Mockito.any(), Mockito.any(), anyFloat());
    }

    @Test
    void wardsAreRareFoundMaterialsThatCannotBeSynthesised() {
        for (CraftingWard ward : CraftingWard.values()) {
            assertTrue(MaterialItems.ids().contains(ward.itemId()));
            assertEquals(MaterialTier.EXOTIC, MaterialItems.tier(ward.itemId()));
            assertTrue(ward.dropWeight() < 1f);
        }
    }
}
