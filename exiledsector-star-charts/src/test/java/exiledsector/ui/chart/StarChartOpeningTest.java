package exiledsector.ui.chart;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import exiledlib.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunActions.OpenResult;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.wormhole.WormholeGateway;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemStore;
import exiledsector.items.StarChartFixtures;
import exiledsector.ui.chart.WormholeOpenCheck.Blocker;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StarChartOpeningTest {

    private MockSector mockSector;
    private MockedStatic<LunaSettings> lunaMock;
    private SectorAPI sector;
    private ItemStore store;
    private Item chart;
    private final List<Item> openedCharts = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        mockSector = new MockSector();
        sector = mockSector.sector;
        when(mockSector.playerFleet().getContainingLocation()).thenReturn(mock(LocationAPI.class));
        lunaMock = Mockito.mockStatic(LunaSettings.class);
        lunaMock.when(() -> LunaSettings.getBoolean("exiledSector", StarChartSettings.OPEN_ANYWHERE_FIELD_ID)).thenReturn(true);
        store = ItemStore.starCharts();
        chart = store.add(ItemData.rolledAtTier(ItemDefinitions.get("star_chart"), 12L, 4));
    }

    @AfterEach
    void tearDown() {
        lunaMock.close();
        mockSector.close();
        StarChartFixtures.clear();
    }

    private StarChartOpening.Opener answering(OpenResult result) {
        return (openSector, entries, random, openStore, openChart) -> {
            openedCharts.add(openChart);
            if (result == OpenResult.OPENED) {
                openStore.remove(openChart);
            }
            return result;
        };
    }

    @Test
    void anOpenedWormholeUsesUpTheChartAndClosesTheScreen() {
        StarChartOpening.Outcome outcome = StarChartOpening.open(answering(OpenResult.OPENED), sector, store, chart, 1, new Random(1L));

        assertTrue(outcome.opened());
        assertEquals(List.of(chart), openedCharts);
        assertTrue(store.owned().isEmpty());
        assertEquals("chart.ui.open.result.opened", outcome.messageKey());
    }

    @Test
    void aFailedOpeningKeepsTheChartAndExplainsWhy() {
        StarChartOpening.Outcome outcome = StarChartOpening.open(answering(OpenResult.FAILED), sector, store, chart, 1, new Random(1L));

        assertFalse(outcome.opened());
        assertEquals(List.of(chart), store.owned());
        assertEquals("chart.ui.open.result.failed", outcome.messageKey());
        assertEquals(OpenResult.FAILED, StarChartOpening.open((s, e, r, st, c) -> null, sector, store, chart, 1, new Random(1L)).result());
    }

    @Test
    void anOpenRunStopsTheOpenerBeingCalledAndKeepsTheChart() {
        ChartRuns.store(new ChartRun("busy", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1));

        StarChartOpening.Outcome outcome = StarChartOpening.open(answering(OpenResult.OPENED), sector, store, chart, 1, new Random(1L));

        assertFalse(outcome.opened());
        assertTrue(openedCharts.isEmpty());
        assertEquals(Blocker.RUN_OPEN, outcome.blocker());
        assertEquals("chart.ui.open.blocked.run_open", outcome.messageKey());
        assertEquals(List.of(chart), store.owned());
    }

    @Test
    void theRealOpenerRefusesWhileARunExistsAndKeepsTheChart() {
        ChartRuns.store(new ChartRun("busy", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1));

        StarChartOpening.Outcome outcome = StarChartOpening.open(StarChartOpening.FROM_STORE, Blocker.NONE, sector, store, chart, 1,
                new Random(1L));

        assertEquals(OpenResult.RUN_EXISTS, outcome.result());
        assertEquals("chart.ui.open.result.run_exists", outcome.messageKey());
        assertSame(chart, store.owned().get(0));
    }

    @Test
    void withoutTheDevSwitchOpeningNeedsAWorkingGatewayWhereTheFleetIsDocked() {
        lunaMock.when(() -> LunaSettings.getBoolean("exiledSector", StarChartSettings.OPEN_ANYWHERE_FIELD_ID))
                .thenReturn(null);

        StarChartOpening.Outcome outcome = StarChartOpening.open(answering(OpenResult.OPENED), sector, store, chart, 1, new Random(1L));

        assertEquals(Blocker.NO_GATEWAY, outcome.blocker());
        assertEquals("chart.ui.open.blocked.no_gateway", outcome.messageKey());
        assertTrue(openedCharts.isEmpty());
        assertEquals(List.of(chart), store.owned());

        dockAtColonyWithWorkingGateway();

        assertTrue(StarChartOpening.open(answering(OpenResult.OPENED), sector, store, chart, 1, new Random(1L)).opened());
    }

    private void dockAtColonyWithWorkingGateway() {
        CampaignUIAPI campaignUi = mock(CampaignUIAPI.class);
        InteractionDialogAPI dialog = mock(InteractionDialogAPI.class);
        SectorEntityToken colony = mock(SectorEntityToken.class);
        MarketAPI market = mock(MarketAPI.class);
        Industry gateway = mock(Industry.class);
        when(sector.getCampaignUI()).thenReturn(campaignUi);
        when(campaignUi.getCurrentInteractionDialog()).thenReturn(dialog);
        when(dialog.getInteractionTarget()).thenReturn(colony);
        when(colony.getMarket()).thenReturn(market);
        when(market.isPlayerOwned()).thenReturn(true);
        when(market.getIndustry(WormholeGateway.INDUSTRY_ID)).thenReturn(gateway);
        when(gateway.isFunctional()).thenReturn(true);
    }
}
