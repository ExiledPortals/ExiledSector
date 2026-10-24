package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledlib.MockSector;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemStore;
import exiledsector.items.StarChart;
import exiledsector.items.StarChartFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChartOpenFromChartTest {

    private MockSector mockSector;
    private SectorAPI sector;
    private ItemStore store;
    private Item firstChart;
    private Item secondChart;

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        mockSector = new MockSector();
        sector = mockSector.sector;
        LocationAPI hyperspace = mock(LocationAPI.class);
        StarSystemAPI system = mock(StarSystemAPI.class);
        CampaignFleetAPI playerFleet = mockSector.playerFleet();
        when(hyperspace.isHyperspace()).thenReturn(true);
        when(sector.getHyperspace()).thenReturn(hyperspace);
        when(sector.createStarSystem(anyString())).thenReturn(system);
        when(system.getLocation()).thenReturn(new Vector2f());
        when(system.getMemoryWithoutUpdate()).thenReturn(mock(MemoryAPI.class));
        when(system.initStar(anyString(), anyString(), anyFloat(), anyFloat())).thenReturn(mock(PlanetAPI.class));
        when(system.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(mock(CustomCampaignEntityAPI.class));
        when(hyperspace.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(mock(CustomCampaignEntityAPI.class));
        when(playerFleet.getContainingLocation()).thenReturn(hyperspace);
        when(playerFleet.getLocation()).thenReturn(new Vector2f(500f, 600f));
        when(system.getStar()).thenReturn(mock(PlanetAPI.class));
        FakeChartLayoutEngine.install(FakeChartLayoutEngine.doingNothing());
        FakeChartContentEngine.install(FakeChartContentEngine.offeringNothing());
        store = ItemStore.starCharts();
        firstChart = store.add(ItemData.rolledAtTier(ItemDefinitions.get("star_chart"), 31L, 9));
        secondChart = store.add(ItemData.rolledAtTier(ItemDefinitions.get("star_chart"), 32L, 2));
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        ChartLayoutGenerator.resetForTests();
        ChartContentGenerator.resetForTests();
        StarChartFixtures.clear();
    }

    @Test
    void openingFromAStoredChartUsesUpExactlyThatChartAndTheRunCarriesIt() {
        assertEquals(ChartRunActions.OpenResult.OPENED, ChartRunActions.openFromStore(sector, 1, new Random(1), store, firstChart));

        assertEquals(1, store.owned().size());
        assertSame(secondChart, store.owned().get(0));
        ChartRun run = ChartRuns.currentOrNull();
        assertNotNull(run);
        assertTrue(run.hasChart());
        assertEquals(9, run.tier());
        assertEquals(firstChart.seed(), run.layoutSeed());
        assertEquals(ChartLayoutGenerator.typeFor(ChartLayoutRequest.forChart((StarChart) firstChart)).name(), run.layoutType());
        StarChart carried = ChartRunEffects.chartOf(run);
        assertEquals(firstChart.definitionId(), carried.definitionId());
        assertEquals(firstChart.explicits(), carried.explicits());
        assertEquals(firstChart.implicits(), carried.implicits());
        assertEquals(9, carried.tier());
    }

    @Test
    void aChartIsKeptWhenTheWormholeCannotOpen() {
        ChartRuns.store(new ChartRun("busy", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1));
        assertEquals(ChartRunActions.OpenResult.RUN_EXISTS, ChartRunActions.openFromStore(sector, 1, new Random(1), store, firstChart));
        assertEquals(2, store.owned().size());
        ChartRuns.delete("busy");

        when(sector.createStarSystem(anyString())).thenThrow(new IllegalStateException("engine changed"));
        assertEquals(ChartRunActions.OpenResult.FAILED, ChartRunActions.openFromStore(sector, 1, new Random(1), store, firstChart));
        assertEquals(2, store.owned().size());
        assertNull(ChartRuns.currentOrNull());
    }

    @Test
    void onlyAChartThatIsReallyInTheChartStoreCanBeOpened() {
        Item looseChart = ItemData.rolledAtTier(ItemDefinitions.get("star_chart"), 33L, 2).preview();

        assertEquals(ChartRunActions.OpenResult.NO_CHART, ChartRunActions.openFromStore(sector, 1, new Random(1), store, looseChart));
        assertEquals(ChartRunActions.OpenResult.NO_CHART, ChartRunActions.openFromStore(sector, 1, new Random(1), new ItemStore(), firstChart));
        assertEquals(ChartRunActions.OpenResult.NO_CHART, ChartRunActions.openFromStore(sector, 1, new Random(1), store, null));
        assertNull(ChartRuns.currentOrNull());
        assertEquals(2, store.owned().size());
    }

    @Test
    void theConsoleOpenPathStillCarriesNoChart() {
        assertEquals(ChartRunActions.OpenResult.OPENED, ChartRunActions.openBesidePlayer(sector, 1, new Random(1), new ChartLayoutRequest(5L, null, 3)));

        ChartRun run = ChartRuns.currentOrNull();
        assertFalse(run.hasChart());
        assertNull(ChartRunEffects.chartOf(run));
        assertEquals(2, store.owned().size());
    }

    @Test
    void theStoredChartsLiveUnderTheirOwnSaveKey() {
        assertSame(store, ItemStore.starCharts());
        assertSame(store, ItemStore.starChartsIfCreated());
        assertTrue(ItemStore.get().owned().isEmpty());
        assertFalse(ItemStore.get().holdsStarCharts());
    }
}
