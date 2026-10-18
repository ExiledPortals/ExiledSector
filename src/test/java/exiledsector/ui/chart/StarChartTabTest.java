package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.i18n.I18n;
import exiledsector.i18n.LocaleChain;
import exiledsector.i18n.RealCatalogue;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemStore;
import exiledsector.items.StarChartFixtures;
import exiledsector.ui.socket.SocketPanelPlacement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StarChartTabTest {

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
    void openingTheTabMovesChartsFromCargoIntoStorageAndShowsThemByTier() {
        CargoStackAPI threes = chartStack(3, 2f);
        CargoStackAPI eight = chartStack(8, 1f);
        when(cargo.getStacksCopy()).thenReturn(new ArrayList<>(List.of(threes, eight)));
        StarChartBrowser browser = new StarChartBrowser();

        int moved = StarChartTab.prepare(sector, browser);

        assertEquals(3, moved);
        verify(cargo).removeStack(threes);
        verify(cargo).removeStack(eight);
        assertEquals(3, ItemStore.starChartsIfCreated().owned().size());
        assertEquals(2, browser.count(3));
        assertEquals(1, browser.count(8));
        assertEquals(3, browser.selectedTier());
        assertEquals("3 Star Charts moved from your cargo into storage.", StarChartTab.movedNotice(moved));
        assertEquals("1 Star Chart moved from your cargo into storage.", StarChartTab.movedNotice(1));
        assertNull(StarChartTab.movedNotice(0));
    }

    @Test
    void openingWithNoChartsAnywhereCreatesNoStorage() {
        when(cargo.getStacksCopy()).thenReturn(new ArrayList<>());
        StarChartBrowser browser = new StarChartBrowser();

        assertEquals(0, StarChartTab.prepare(sector, browser));

        assertNull(ItemStore.starChartsIfCreated());
        assertEquals(0, browser.storedCount());
        verify(cargo, never()).removeStack(Mockito.any());
    }

    @Test
    void theMovedNoticeUsesTheGameTextLanguageBecauseVanillaDrawsIt() {
        I18n.install(RealCatalogue.of(LocaleChain.SIMPLIFIED_CHINESE), RealCatalogue.english());

        assertEquals("2 Star Charts moved from your cargo into storage.", StarChartTab.movedNotice(2));
    }

    @Test
    void theDetailPanelSitsBesideTheStorageAtItsHeightAndFillsTheRoomUpToItsCap() {
        SocketPanelPlacement storage = new SocketPanelPlacement(16f, 80f, 500f, 700f, true);

        SocketPanelPlacement capped = StarChartTab.detailBeside(storage, 1900f, StarChartTab.DETAIL_WIDTH);
        SocketPanelPlacement filled = StarChartTab.detailBeside(storage, 1300f, Float.MAX_VALUE);
        SocketPanelPlacement cramped = StarChartTab.detailBeside(storage, 700f, StarChartTab.DETAIL_WIDTH);

        assertEquals(16f + 500f + StarChartTab.PANEL_GAP, capped.panelLeft(), 0.01f);
        assertEquals(StarChartTab.DETAIL_WIDTH, capped.panelWidth(), 0.01f);
        assertEquals(80f, capped.panelTop(), 0.01f);
        assertEquals(700f, capped.panelHeight(), 0.01f);
        assertTrue(capped.instantly());
        assertEquals(1300f, filled.panelLeft() + filled.panelWidth(), 0.01f);
        assertEquals(StarChartTab.MIN_DETAIL_WIDTH, cramped.panelWidth(), 0.01f);
    }

    @Test
    void theWormholeIsReportedOnceAndClosingIsReportedOnce() {
        RecordingListener listener = new RecordingListener();
        StarChartTab chartTab = new StarChartTab(listener);

        chartTab.wormholeOpened("Opened.");
        chartTab.wormholeOpened("Opened again.");
        chartTab.storageClosed();
        chartTab.storageClosed();

        assertEquals(List.of("Opened."), listener.wormholeMessages);
        assertEquals(1, listener.closings);
        assertTrue(chartTab.hasOpenedWormhole());
    }

    @Test
    void aTabThatHasNotOpenedItsPanelsIgnoresInputSafely() {
        StarChartTab chartTab = new StarChartTab(new RecordingListener());

        chartTab.chartClicked(null);
        chartTab.close();
        chartTab.swapOut();
        chartTab.hideTooltips();

        assertFalse(chartTab.escape());
        assertFalse(chartTab.contains(0f, 0f));
        assertFalse(chartTab.isVisible());
        assertFalse(chartTab.hasOpenedWormhole());
    }

    private static final class RecordingListener implements StarChartTab.Listener {

        private final List<String> wormholeMessages = new ArrayList<>();
        private int closings;

        @Override
        public void closed() {
            closings++;
        }

        @Override
        public void wormholeOpened(String message) {
            wormholeMessages.add(message);
        }

        @Override
        public void pressedInside() {
        }
    }
}
