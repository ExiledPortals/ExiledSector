package exiledsector.ui.cargo;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import exiledsector.items.CraftingCurrency;
import exiledsector.items.ItemDisassembly;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

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

    private final SectorAPI sector = mock(SectorAPI.class);
    private MockedStatic<Global> global;

    @BeforeEach
    void setUp() {
        global = Mockito.mockStatic(Global.class);
        global.when(Global::getSector).thenReturn(sector);
        LootAlertListener.resetForTests();
    }

    @AfterEach
    void tearDown() {
        LootAlertListener.resetForTests();
        global.close();
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

        verify(sector).addTransientScript(any(EveryFrameScript.class));
        assertSame(shown, LootAlertListener.activeWatcher().watchedCargo());
    }

    @Test
    void ignoresStorageUpdatesOnceTheWatcherHasStarted() {
        LootAlertListener listener = new LootAlertListener();
        CargoAPI loot = cargoWith(CraftingCurrency.FIXATION.itemId());
        listener.reportAboutToShowLootToPlayer(loot, null);
        LootAlertListener.activeWatcher().advance(0f);

        listener.reportSubmarketCargoAndShipsUpdated(submarket(LootAlertListener.LOOT_MARKET_ID, mock(CargoAPI.class)));

        assertNotNull(LootAlertListener.activeWatcher());
        assertSame(loot, LootAlertListener.activeWatcher().watchedCargo());
    }

    @Test
    void lootWithoutAlertsAddsNoScript() {
        new LootAlertListener().reportAboutToShowLootToPlayer(cargoWith(ItemDisassembly.PARTS_ITEM_ID), null);

        verify(sector, never()).addTransientScript(any(EveryFrameScript.class));
        assertNull(LootAlertListener.activeWatcher());
    }
}
