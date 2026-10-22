package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledlib.i18n.I18n;
import exiledlib.i18n.LocaleChain;
import exiledlib.i18n.RealCatalogue;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemStore;
import exiledsector.items.StarChartFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StarChartCargoTest {

    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private CargoAPI cargo;

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(new HashMap<>());
        CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
        cargo = mock(CargoAPI.class);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        when(playerFleet.getCargo()).thenReturn(cargo);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        StarChartFixtures.clear();
    }

    private CargoStackAPI chartStack(int tier, float size) {
        ItemData chart = ItemData.rolledAtTier(ItemDefinitions.get("star_chart_single"), 40L + tier, tier);
        CargoStackAPI stack = mock(CargoStackAPI.class);
        when(stack.getSpecialDataIfSpecial()).thenReturn(new SpecialItemData(ItemData.STAR_CHART_ITEM_ID, chart.encode()));
        when(stack.getSize()).thenReturn(size);
        return stack;
    }

    @Test
    void preparingMovesChartsFromCargoIntoStorageAndShowsThemByTier() {
        CargoStackAPI threes = chartStack(3, 2f);
        CargoStackAPI eight = chartStack(8, 1f);
        when(cargo.getStacksCopy()).thenReturn(new ArrayList<>(List.of(threes, eight)));
        StarChartBrowser browser = new StarChartBrowser();

        int moved = StarChartBrowser.prepare(sector, browser);

        assertEquals(3, moved);
        verify(cargo).removeStack(threes);
        verify(cargo).removeStack(eight);
        assertEquals(3, ItemStore.starChartsIfCreated().owned().size());
        assertEquals(2, browser.count(3));
        assertEquals(1, browser.count(8));
        assertEquals(3, browser.selectedTier());
        assertEquals("3 Star Charts moved from your cargo into storage.", StarChartBrowser.movedNotice(moved));
        assertEquals("1 Star Chart moved from your cargo into storage.", StarChartBrowser.movedNotice(1));
        assertNull(StarChartBrowser.movedNotice(0));
    }

    @Test
    void preparingWithNoChartsAnywhereCreatesNoStorage() {
        when(cargo.getStacksCopy()).thenReturn(new ArrayList<>());
        StarChartBrowser browser = new StarChartBrowser();

        assertEquals(0, StarChartBrowser.prepare(sector, browser));

        assertNull(ItemStore.starChartsIfCreated());
        assertEquals(0, browser.storedCount());
        verify(cargo, never()).removeStack(Mockito.any());
    }

    @Test
    void theMovedNoticeUsesTheGameTextLanguageBecauseVanillaDrawsIt() {
        I18n.install(RealCatalogue.of(LocaleChain.SIMPLIFIED_CHINESE), RealCatalogue.english());

        assertEquals("2 Star Charts moved from your cargo into storage.", StarChartBrowser.movedNotice(2));
    }
}
