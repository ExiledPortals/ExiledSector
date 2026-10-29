package exiledsector.ui.cargo;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import exiledsector.MockSector;
import exiledsector.items.CraftingCurrency;
import exiledsector.items.ItemDisassembly;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LootAlertListenerTest {

    private MockSector mockSector;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private static CargoAPI cargoWith(String itemId) {
        CargoStackAPI stack = mock(CargoStackAPI.class);
        when(stack.getSpecialDataIfSpecial()).thenReturn(new SpecialItemData(itemId, null));
        CargoAPI cargo = mock(CargoAPI.class);
        when(cargo.getStacksCopy()).thenReturn(List.of(stack));
        return cargo;
    }

    private static SubmarketAPI submarket(String marketId, CargoAPI cargo) {
        MarketAPI market = mock(MarketAPI.class);
        when(market.getId()).thenReturn(marketId);
        SubmarketAPI submarket = mock(SubmarketAPI.class);
        when(submarket.getMarket()).thenReturn(market);
        when(submarket.getCargo()).thenReturn(cargo);
        return submarket;
    }

    @Test
    void watchesTheLootScreensStorageInsteadOfTheReportedLoot() {
        LootAlertListener listener = new LootAlertListener();
        CargoAPI loot = cargoWith(CraftingCurrency.FIXATION.itemId());
        CargoAPI shown = mock(CargoAPI.class);

        listener.reportAboutToShowLootToPlayer(loot, null);
        listener.reportSubmarketCargoAndShipsUpdated(submarket("some_market", mock(CargoAPI.class)));
        listener.reportSubmarketCargoAndShipsUpdated(submarket(LootAlertListener.LOOT_MARKET_ID, shown));

        verify(mockSector.sector).addTransientScript(any(EveryFrameScript.class));
        assertSame(shown, listener.activeWatcher().watchedCargo());
    }

    @Test
    void ignoresStorageUpdatesOnceTheWatcherHasStarted() {
        LootAlertListener listener = new LootAlertListener();
        CargoAPI loot = cargoWith(CraftingCurrency.FIXATION.itemId());
        listener.reportAboutToShowLootToPlayer(loot, null);
        listener.activeWatcher().advance(0f);

        listener.reportSubmarketCargoAndShipsUpdated(submarket(LootAlertListener.LOOT_MARKET_ID, mock(CargoAPI.class)));

        assertNotNull(listener.activeWatcher());
        assertSame(loot, listener.activeWatcher().watchedCargo());
    }

    @Test
    void lootWithoutAlertsAddsNoScript() {
        LootAlertListener listener = new LootAlertListener();
        listener.reportAboutToShowLootToPlayer(cargoWith(ItemDisassembly.PARTS_ITEM_ID), null);

        verify(mockSector.sector, never()).addTransientScript(any(EveryFrameScript.class));
        assertNull(listener.activeWatcher());
    }
}
