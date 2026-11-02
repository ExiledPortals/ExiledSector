package exiledsector.ui.socket;

import exiledsector.items.Nanoswarm;
import exiledsector.items.CraftingMaterialFamily;
import exiledsector.items.ItemNature;
import exiledsector.items.ItemType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HullCraftingBenchCurrenciesTest {

    private static CraftingMaterialFamily extra(String itemId, ItemType type) {
        CraftingMaterialFamily currency = mock(CraftingMaterialFamily.class);
        when(currency.itemId()).thenReturn(itemId);
        when(currency.appliesTo(type)).thenReturn(true);
        when(currency.appliesToShipItems()).thenCallRealMethod();
        when(currency.usedNoticeKey(ItemNature.HARDWARE)).thenReturn("notice." + itemId);
        return currency;
    }

    @Test
    void anExtraHullItemCurrencyShowsOnlyWhileOwned() {
        CraftingMaterialFamily engram = extra("engram", ItemType.HULL_ITEM);
        CraftingMaterialFamily chartOnly = extra("cipher", ItemType.STAR_CHART);
        List<CraftingMaterialFamily> registered = List.of(Nanoswarm.AUGMENTATION, engram, chartOnly);

        List<CraftingMaterialFamily> owned = HullCraftingBench.hullCraftingBenchCurrencies(registered, itemId -> 1);
        List<CraftingMaterialFamily> none = HullCraftingBench.hullCraftingBenchCurrencies(registered, itemId -> 0);

        assertEquals(Nanoswarm.values().length + 1, owned.size());
        assertTrue(owned.contains(engram));
        assertFalse(owned.contains(chartOnly));
        assertEquals(List.of(Nanoswarm.values()), none);
    }

    @Test
    void extraCurrenciesLeaveTheOrbitForACentredColumn() {
        CraftingMaterialFamily engram = extra("engram", ItemType.HULL_ITEM);
        List<CraftingMaterialFamily> shown = HullCraftingBench.hullCraftingBenchCurrencies(List.of(engram), itemId -> 1);

        assertEquals(List.of(Nanoswarm.values()), shown.stream().filter(HullCraftingBench.INSTANCE::inOrbit).toList());
        assertEquals(80f, CraftingBenchPanel.columnTileTop(0, 1, 100f, 40f, 10f, 300f));
        assertEquals(55f, CraftingBenchPanel.columnTileTop(0, 2, 100f, 40f, 10f, 300f));
        assertEquals(105f, CraftingBenchPanel.columnTileTop(1, 2, 100f, 40f, 10f, 300f));
        assertEquals(0f, CraftingBenchPanel.columnTileTop(0, 5, 60f, 40f, 10f, 300f), "a tall column stays inside the bench");
    }

    @Test
    void aBenchModeNoDomainServesFallsBackToTheHullCraftingBench() {
        assertEquals(HullCraftingBench.INSTANCE, BenchModes.of(BenchMode.HULL_ITEMS));
        assertEquals(HullCraftingBench.INSTANCE, BenchModes.of(BenchMode.FORGE));
        assertEquals(BenchMode.HULL_ITEMS, BenchModes.modeFor(ItemType.STAR_CHART));
    }

    @Test
    void theUsedNoticeComesFromTheCurrencyUnlessACorruptionOutcomeNamesIt() {
        assertEquals("ui.craftingBench.notice.used.augmentation.hardware",
                HullCraftingBench.usedNoticeKey(Nanoswarm.AUGMENTATION, null, ItemNature.HARDWARE));
        assertEquals("notice.engram", HullCraftingBench.usedNoticeKey(extra("engram", ItemType.HULL_ITEM), null, ItemNature.HARDWARE));
    }
}
