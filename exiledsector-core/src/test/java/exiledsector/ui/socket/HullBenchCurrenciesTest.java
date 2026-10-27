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
        when(currency.usedNoticeKey(ItemNature.HARDWARE)).thenReturn("notice." + itemId);
        return currency;
    }

    @Test
    void anExtraHullItemCurrencyShowsOnlyWhileOwned() {
        ItemCurrency engram = extra("engram", ItemType.HULL_ITEM);
        ItemCurrency chartOnly = extra("cipher", ItemType.STAR_CHART);
        List<ItemCurrency> registered = List.of(CraftingCurrency.AUGMENTATION, engram, chartOnly);

        List<ItemCurrency> owned = SocketWorkbenchPanel.hullBenchCurrencies(registered, itemId -> 1);
        List<ItemCurrency> none = SocketWorkbenchPanel.hullBenchCurrencies(registered, itemId -> 0);

        assertEquals(CraftingCurrency.values().length + 1, owned.size());
        assertTrue(owned.contains(engram));
        assertFalse(owned.contains(chartOnly));
        assertEquals(List.of(CraftingCurrency.values()), none);
    }

    @Test
    void theUsedNoticeComesFromTheCurrencyUnlessACorruptionOutcomeNamesIt() {
        assertEquals("ui.workbench.notice.used.augmentation.hardware",
                SocketWorkbenchPanel.usedNoticeKey(CraftingCurrency.AUGMENTATION, null, ItemNature.HARDWARE));
        assertEquals("notice.engram", SocketWorkbenchPanel.usedNoticeKey(extra("engram", ItemType.HULL_ITEM), null, ItemNature.HARDWARE));
    }
}
