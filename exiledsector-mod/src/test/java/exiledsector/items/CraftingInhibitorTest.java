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

class CraftingInhibitorTest {

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
        List<RolledLine> effects = List.of(new RolledLine("FLUX_CAPACITY_MULT", 5f), new RolledLine("FLUX_DISSIPATION_MULT", 5f),
                new RolledLine("ARMOR_PERCENT", 7f), new RolledLine("HULL_MULT", 5f));
        return store.add(HullItemRows.data(HullItemRows.MILITARY, 1L, effects, null));
    }

    private static List<RolledModifier> side(Item item, ModifierSlot slot) {
        return item.explicits().stream().filter(rolled -> rolled.slot() == slot).toList();
    }

    private void owns(String itemId, float quantity) {
        when(cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(itemId))).thenReturn(quantity);
    }

    @Test
    void aPrefixInhibitorKeepsTuningOffEveryPrefix() {
        Item item = rare();
        List<RolledModifier> prefixes = side(item, ModifierSlot.PREFIX);

        for (long seed = 0; seed < 20; seed++) {
            assertNotNull(ItemCrafting.tune(item, new Random(seed), Set.of(ModifierSlot.PREFIX)));
            assertEquals(prefixes, side(item, ModifierSlot.PREFIX));
        }
    }

    @Test
    void aSuffixInhibitorKeepsReshapingOffEverySuffix() {
        Item item = rare();
        List<RolledModifier> suffixes = side(item, ModifierSlot.SUFFIX);

        for (long seed = 0; seed < 20; seed++) {
            ItemCrafting.reshape(item, new Random(seed), Set.of(ModifierSlot.SUFFIX));
            assertEquals(suffixes, side(item, ModifierSlot.SUFFIX));
        }
    }

    @Test
    void anInhibitorPicksWhichSideImmutableLands() {
        for (long seed = 0; seed < 20; seed++) {
            Item item = rare();
            ItemCrafting.makeImmutable(item, new Random(seed), Set.of(ModifierSlot.PREFIX));
            assertTrue(side(item, ModifierSlot.SUFFIX).stream().anyMatch(RolledModifier::immutable));
            assertFalse(side(item, ModifierSlot.PREFIX).stream().anyMatch(RolledModifier::immutable));
        }
    }

    @Test
    void inhibitorsBlockMetamorphButLeaveCorruptionAndReplicationAlone() {
        Item unique = store.add(ItemDefinitions.get("relic_a"), 5L);
        List<CraftingInhibitor> prefixInhibitor = List.of(CraftingInhibitor.PREFIX);

        assertTrue(ItemCrafting.canUse(Nanoswarm.METAMORPH, unique, definition -> true));
        assertFalse(ItemCrafting.canUse(Nanoswarm.METAMORPH, unique, definition -> true, prefixInhibitor));
        assertTrue(ItemCrafting.canUse(Nanoswarm.CORRUPTION, unique, definition -> true, prefixInhibitor));
        assertTrue(ItemCrafting.canUse(Nanoswarm.REPLICATION, unique, definition -> true, prefixInhibitor));
        assertTrue(ItemCrafting.inhibitorsSpentBy(Nanoswarm.CORRUPTION, unique, prefixInhibitor).isEmpty());
        assertTrue(ItemCrafting.inhibitorsSpentBy(Nanoswarm.REPLICATION, unique, prefixInhibitor).isEmpty());
        assertTrue(ItemCrafting.inhibitorsSpentBy(Nanoswarm.AUGMENTATION, rare(), prefixInhibitor).isEmpty());
    }

    @Test
    void aWardIsSpentOnlyWhenItProtectsAnEffect() {
        Item item = rare();
        owns(Nanoswarm.TUNING.itemId(), 1f);
        owns(CraftingInhibitor.PREFIX.itemId(), 1f);

        assertEquals(Set.of(CraftingInhibitor.PREFIX), ItemCrafting.inhibitorsSpentBy(Nanoswarm.TUNING, item, List.of(CraftingInhibitor.PREFIX)));
        assertNotNull(ItemCrafting.use(Nanoswarm.TUNING, item, CraftingMaterials.cargoOnly(cargo), store, new Random(3L),
                definition -> true, List.of(CraftingInhibitor.PREFIX)));

        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(CraftingInhibitor.PREFIX.itemId()), 1);
        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(Nanoswarm.TUNING.itemId()), 1);
    }

    @Test
    void aWardGuardingOnlyAFixedEffectIsNotSpent() throws Exception {
        ItemDefinitions.register(new JSONArray().put(HullItemRows.row("lean", "bridge", "HULL_MULT:4:6", "ARMOR_PERCENT:6:9")));
        Item item = store.add(HullItemRows.data("lean", 1L, List.of(new RolledLine("HULL_MULT", 5f), new RolledLine("ARMOR_PERCENT", 7f)), null));
        ItemCrafting.makeImmutable(item, new Random(1L), Set.of(ModifierSlot.SUFFIX));

        assertTrue(side(item, ModifierSlot.PREFIX).get(0).immutable());
        assertTrue(ItemCrafting.inhibitorsSpentBy(Nanoswarm.TUNING, item, List.of(CraftingInhibitor.PREFIX)).isEmpty());
        assertTrue(ItemCrafting.inhibitorsSpentBy(Nanoswarm.RESHAPING, item, List.of(CraftingInhibitor.PREFIX)).isEmpty());
        assertEquals(Set.of(CraftingInhibitor.SUFFIX), ItemCrafting.inhibitorsSpentBy(Nanoswarm.TUNING, item, List.of(CraftingInhibitor.SUFFIX)));
    }

    @Test
    void aWardYouDoNotOwnStopsTheModification() {
        Item item = rare();
        owns(Nanoswarm.TUNING.itemId(), 1f);
        owns(CraftingInhibitor.SUFFIX.itemId(), 0f);

        assertEquals(null, ItemCrafting.use(Nanoswarm.TUNING, item, CraftingMaterials.cargoOnly(cargo), store, new Random(3L),
                definition -> true, List.of(CraftingInhibitor.SUFFIX)));
        verify(cargo, never()).removeItems(Mockito.any(), Mockito.any(), anyFloat());
    }

    @Test
    void wardsAreRareFoundMaterialsThatCannotBeSynthesised() {
        for (CraftingInhibitor inhibitor : CraftingInhibitor.values()) {
            assertTrue(MaterialItems.ids().contains(inhibitor.itemId()));
            assertEquals(MaterialTier.EXOTIC, MaterialItems.tier(inhibitor.itemId()));
            assertTrue(inhibitor.dropWeight() < 1f);
        }
    }
}
