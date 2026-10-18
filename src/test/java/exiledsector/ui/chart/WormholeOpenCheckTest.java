package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartRun;
import exiledsector.charts.ChartRuns;
import exiledsector.charts.StarChartSettings;
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

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WormholeOpenCheckTest {

    private final ItemStore store = ItemStore.forStarCharts();
    private final CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
    private final LocationAPI hyperspace = mock(LocationAPI.class);
    private Item chart;

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        chart = store.add(ItemData.rolledAtTier(ItemDefinitions.get("star_chart_single"), 4L, 3));
        when(playerFleet.getContainingLocation()).thenReturn(hyperspace);
    }

    @AfterEach
    void tearDown() {
        StarChartFixtures.clear();
    }

    @Test
    void aStoredChartOpensWhenNothingStandsInTheWay() {
        Blocker blocker = WormholeOpenCheck.evaluate(chart, store, true, false, playerFleet);

        assertEquals(Blocker.NONE, blocker);
        assertTrue(blocker.allowsOpening());
    }

    @Test
    void openingNeedsAChartThatIsReallyInTheChartStore() {
        Item loose = ItemData.rolledAtTier(ItemDefinitions.get("star_chart_single"), 5L, 3).preview();

        assertEquals(Blocker.NO_CHART, WormholeOpenCheck.evaluate(null, store, true, false, playerFleet));
        assertEquals(Blocker.NO_CHART, WormholeOpenCheck.evaluate(loose, store, true, false, playerFleet));
        assertEquals(Blocker.NO_CHART, WormholeOpenCheck.evaluate(chart, new ItemStore(), true, false, playerFleet));
        assertEquals(Blocker.NO_CHART, WormholeOpenCheck.evaluate(chart, null, true, false, playerFleet));
        store.remove(chart);
        assertEquals(Blocker.NO_CHART, WormholeOpenCheck.evaluate(chart, store, true, false, playerFleet));
    }

    @Test
    void theSettingSwitchBlocksOpening() {
        assertEquals(Blocker.SETTING_OFF, WormholeOpenCheck.evaluate(chart, store, false, false, playerFleet));
        assertFalse(Blocker.SETTING_OFF.allowsOpening());
    }

    @Test
    void anOpenRunBlocksOpening() {
        assertEquals(Blocker.RUN_OPEN, WormholeOpenCheck.evaluate(chart, store, true, true, playerFleet));
    }

    @Test
    void theFleetMustBeOutsideAPocketAndNotMidJump() {
        assertEquals(Blocker.NO_FLEET, WormholeOpenCheck.evaluate(chart, store, true, false, null));
        when(playerFleet.isInHyperspaceTransition()).thenReturn(true);
        assertEquals(Blocker.IN_TRANSITION, WormholeOpenCheck.evaluate(chart, store, true, false, playerFleet));
        when(playerFleet.isInHyperspaceTransition()).thenReturn(false);
        when(hyperspace.hasTag(ChartIds.POCKET_TAG)).thenReturn(true);
        assertEquals(Blocker.IN_POCKET, WormholeOpenCheck.evaluate(chart, store, true, false, playerFleet));
        when(playerFleet.getContainingLocation()).thenReturn(null);
        assertEquals(Blocker.NO_FLEET, WormholeOpenCheck.evaluate(chart, store, true, false, playerFleet));
    }

    @Test
    void theLiveCheckReadsTheSettingTheRunAndThePlayerFleet() {
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(new HashMap<>());
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class);
             MockedStatic<LunaSettings> lunaMock = Mockito.mockStatic(LunaSettings.class)) {
            globalMock.when(Global::getSector).thenReturn(sector);
            lunaMock.when(() -> LunaSettings.getBoolean("exiledSector", StarChartSettings.OPEN_ANYWHERE_FIELD_ID)).thenReturn(null);
            assertEquals(Blocker.NONE, WormholeOpenCheck.current(sector, store, chart), "the switch defaults to on");

            ChartRuns.store(new ChartRun("busy", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1));
            assertEquals(Blocker.RUN_OPEN, WormholeOpenCheck.current(sector, store, chart));
            ChartRuns.delete("busy");

            lunaMock.when(() -> LunaSettings.getBoolean("exiledSector", StarChartSettings.OPEN_ANYWHERE_FIELD_ID)).thenReturn(false);
            assertEquals(Blocker.SETTING_OFF, WormholeOpenCheck.current(sector, store, chart));
        }
    }
}
