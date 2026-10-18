package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StarChartScreenTest {

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
    void openingMovesChartsFromCargoIntoStorageAndShowsThemByTier() {
        CargoStackAPI threes = chartStack(3, 2f);
        CargoStackAPI eight = chartStack(8, 1f);
        when(cargo.getStacksCopy()).thenReturn(new ArrayList<>(List.of(threes, eight)));
        StarChartBrowser browser = new StarChartBrowser();

        int moved = StarChartScreen.prepare(sector, browser);

        assertEquals(3, moved);
        verify(cargo).removeStack(threes);
        verify(cargo).removeStack(eight);
        assertEquals(3, ItemStore.starChartsIfCreated().owned().size());
        assertEquals(2, browser.count(3));
        assertEquals(1, browser.count(8));
        assertEquals(3, browser.selectedTier());
        assertEquals("3 Star Charts moved from your cargo into storage.", StarChartScreen.movedNotice(moved));
        assertEquals("1 Star Chart moved from your cargo into storage.", StarChartScreen.movedNotice(1));
        assertNull(StarChartScreen.movedNotice(0));
    }

    @Test
    void openingWithNoChartsAnywhereCreatesNoStorage() {
        when(cargo.getStacksCopy()).thenReturn(new ArrayList<>());
        StarChartBrowser browser = new StarChartBrowser();

        assertEquals(0, StarChartScreen.prepare(sector, browser));

        assertNull(ItemStore.starChartsIfCreated());
        assertEquals(0, browser.storedCount());
        verify(cargo, never()).removeStack(Mockito.any());
    }

    @Test
    void theScreenSplitsIntoAStorageColumnAndADetailColumn() {
        StarChartScreen.Layout layout = StarChartScreen.Layout.within(1200f, 800f);

        assertEquals(StarChartScreen.DETAIL_WIDTH, layout.detailWidth());
        assertEquals(1200f, layout.detailLeft() + layout.detailWidth(), 0.01f);
        assertEquals(layout.storageWidth() + StarChartScreen.PANEL_GAP, layout.detailLeft(), 0.01f);
        assertEquals(800f, layout.panelHeight());
        assertTrue(StarChartScreen.width(1920f) <= StarChartScreen.MAX_WIDTH);
        assertEquals(1280f - StarChartScreen.SCREEN_MARGIN * 2f, StarChartScreen.width(1280f));
        assertTrue(StarChartScreen.height(768f) < 768f);
    }
}
