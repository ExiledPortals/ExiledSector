package exiledsector.ui.socket;

import exiledsector.items.CraftingCurrency;
import exiledsector.items.ItemCurrency;
import exiledsector.items.ItemNature;
import exiledsector.items.ItemType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HullBenchCurrenciesTest {

    private static ItemCurrency extra(String itemId, ItemType type) {
        ItemCurrency currency = mock(ItemCurrency.class);
        when(currency.itemId()).thenReturn(itemId);
        when(currency.appliesTo(type)).thenReturn(true);
        when(currency.appliesToShipItems()).thenCallRealMethod();
        when(currency.usedNoticeKey(ItemNature.HARDWARE)).thenReturn("notice." + itemId);
        return currency;
    }

    @Test
    void anExtraHullItemCurrencyShowsOnlyWhileOwned() {
        ItemCurrency engram = extra("engram", ItemType.HULL_ITEM);
        ItemCurrency chartOnly = extra("cipher", ItemType.STAR_CHART);
        List<ItemCurrency> registered = List.of(CraftingCurrency.AUGMENTATION, engram, chartOnly);

        List<ItemCurrency> owned = HullBench.hullBenchCurrencies(registered, itemId -> 1);
        List<ItemCurrency> none = HullBench.hullBenchCurrencies(registered, itemId -> 0);

        assertEquals(CraftingCurrency.values().length + 1, owned.size());
        assertTrue(owned.contains(engram));
        assertFalse(owned.contains(chartOnly));
        assertEquals(List.of(CraftingCurrency.values()), none);
    }

    @Test
    void extraCurrenciesLeaveTheOrbitForACentredColumn() {
        ItemCurrency engram = extra("engram", ItemType.HULL_ITEM);
        List<ItemCurrency> shown = HullBench.hullBenchCurrencies(List.of(engram), itemId -> 1);

        assertEquals(List.of(CraftingCurrency.values()), shown.stream().filter(HullBench.INSTANCE::inOrbit).toList());
        assertEquals(80f, SocketWorkbenchPanel.columnTileTop(0, 1, 100f, 40f, 10f, 300f));
        assertEquals(55f, SocketWorkbenchPanel.columnTileTop(0, 2, 100f, 40f, 10f, 300f));
        assertEquals(105f, SocketWorkbenchPanel.columnTileTop(1, 2, 100f, 40f, 10f, 300f));
        assertEquals(0f, SocketWorkbenchPanel.columnTileTop(0, 5, 60f, 40f, 10f, 300f), "a tall column stays inside the bench");
    }

    @Test
    void aBenchModeNoDomainServesFallsBackToTheHullBench() {
        assertEquals(HullBench.INSTANCE, BenchModes.of(BenchMode.HULL_ITEMS));
        assertEquals(HullBench.INSTANCE, BenchModes.of(BenchMode.FORGE));
        assertEquals(BenchMode.HULL_ITEMS, BenchModes.modeFor(ItemType.STAR_CHART));
    }

    @Test
    void theUsedNoticeComesFromTheCurrencyUnlessACorruptionOutcomeNamesIt() {
        assertEquals("ui.workbench.notice.used.augmentation.hardware",
                HullBench.usedNoticeKey(CraftingCurrency.AUGMENTATION, null, ItemNature.HARDWARE));
        assertEquals("notice.engram", HullBench.usedNoticeKey(extra("engram", ItemType.HULL_ITEM), null, ItemNature.HARDWARE));
    }
}
