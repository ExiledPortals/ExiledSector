package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartSoakScriptTest {

    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private CampaignFleetAPI playerFleet;
    private final List<StarSystemAPI> starSystems = new ArrayList<>();
    private final List<StarSystemAPI> removeNextFrame = new ArrayList<>();
    private final List<String> reports = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Map<String, Object> persistentData = new HashMap<>();
        sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        LocationAPI hyperspace = mock(LocationAPI.class);
        when(hyperspace.isHyperspace()).thenReturn(true);
        when(hyperspace.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(mock(CustomCampaignEntityAPI.class));
        playerFleet = mock(CampaignFleetAPI.class);
        when(playerFleet.getContainingLocation()).thenReturn(hyperspace);
        when(playerFleet.getLocation()).thenReturn(new Vector2f());
        when(sector.getHyperspace()).thenReturn(hyperspace);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        when(sector.getStarSystems()).thenAnswer(invocation -> new ArrayList<>(starSystems));
        when(sector.createStarSystem(anyString())).thenAnswer(invocation -> newPocket());
        when(sector.getStarSystem(anyString())).thenAnswer(invocation -> starSystems.stream()
                .filter(system -> invocation.getArgument(0).equals(system.getOptionalUniqueId())).findFirst().orElse(null));
        doAnswer(invocation -> removeNextFrame.add(invocation.getArgument(0))).when(sector).removeStarSystemNextFrame(Mockito.any());
        FakeChartLayoutEngine.install(FakeChartLayoutEngine.doingNothing());
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        ChartLayoutGenerator.resetForTests();
    }

    private StarSystemAPI newPocket() {
        StarSystemAPI pocket = mock(StarSystemAPI.class);
        String[] uniqueId = new String[1];
        doAnswer(invocation -> uniqueId[0] = invocation.getArgument(0)).when(pocket).setOptionalUniqueId(anyString());
        when(pocket.getOptionalUniqueId()).thenAnswer(invocation -> uniqueId[0]);
        when(pocket.getLocation()).thenReturn(new Vector2f());
        when(pocket.getMemoryWithoutUpdate()).thenReturn(mock(MemoryAPI.class));
        when(pocket.initStar(anyString(), anyString(), anyFloat(), anyFloat())).thenReturn(mock(PlanetAPI.class));
        when(pocket.getStar()).thenReturn(mock(PlanetAPI.class));
        when(pocket.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(mock(CustomCampaignEntityAPI.class));
        starSystems.add(pocket);
        return pocket;
    }

    private void runFrames(ChartSoakScript soak, ChartRunScript watchdog, int frames) {
        for (int frame = 0; frame < frames && !soak.isDone(); frame++) {
            starSystems.removeAll(removeNextFrame);
            removeNextFrame.clear();
            soak.advance(0f);
            watchdog.advance(0f);
        }
    }

    @Test
    void soaksTheRequestedPocketsWithoutMovingThePlayerAndReportsBeforeAndAfter() {
        ChartCensus before = ChartCensus.take(sector);
        ChartSoakScript soak = new ChartSoakScript(3, before, reports::add, new Random(7));

        runFrames(soak, new ChartRunScript(), 100);

        assertTrue(soak.isDone());
        assertEquals(1, reports.size());
        assertTrue(reports.get(0).contains("finished, 3 of 3"), reports.get(0));
        assertTrue(reports.get(0).contains("Layouts: 3 generated, 0 fell back"), reports.get(0));
        assertTrue(reports.get(0).contains("Unchanged before vs after"), reports.get(0));
        assertTrue(reports.get(0).contains("[BINARY_CLOSE, BINARY_FAR, SINGLE]"), reports.get(0));
        assertTrue(starSystems.isEmpty());
        assertNull(ChartRuns.currentOrNull());
        verify(sector, Mockito.times(3)).createStarSystem(ChartIds.POCKET_BASE_NAME);
        verify(sector, never()).doHyperspaceTransition(Mockito.any(), Mockito.any(), Mockito.any());
        verify(playerFleet, never()).setLocation(anyFloat(), anyFloat());
    }

    @Test
    void everyAllowedTypeAndARandomPickAreCycledThrough() {
        List<StarSystemType> cycle = new ArrayList<>();
        for (int iteration = 0; iteration <= ChartLayoutGenerator.ALLOWED_TYPES.size(); iteration++) {
            cycle.add(ChartSoakScript.typeForIteration(iteration));
        }

        List<StarSystemType> expected = new ArrayList<>(ChartLayoutGenerator.ALLOWED_TYPES);
        expected.add(null);
        assertEquals(expected, cycle);
        assertEquals(StarSystemType.SINGLE, ChartSoakScript.typeForIteration(expected.size()));
    }

    @Test
    void theCensusNamesEveryCountThatChanged() {
        ChartCensus before = new ChartCensus(10, 0, 100, 20, 0, 0, 500, 12, 40, 30, 7);
        ChartCensus fleetsMoved = new ChartCensus(10, 0, 104, 24, 0, 0, 500, 12, 40, 30, 7);
        ChartCensus leaked = new ChartCensus(11, 1, 101, 20, 1, 3, 502, 15, 41, 31, 8);

        assertEquals(List.of(), fleetsMoved.changesSince(before));
        assertEquals(List.of("used procgen names 500 -> 502", "Hyperspace Topography scans 12 -> 15", "star systems 10 -> 11",
                "hyperspace entities other than fleets 80 -> 81",
                "chart-tagged entities 0 -> 3", "Economy markets 40 -> 41", "intel 30 -> 31"), leaked.changesSince(before));
    }

    @Test
    void stopsAndReportsWhenARunIsAlreadyOpen() {
        ChartRuns.store(new ChartRun("busy", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1));
        ChartSoakScript soak = new ChartSoakScript(2, ChartCensus.take(sector), reports::add, new Random(7));

        soak.advance(0f);

        assertTrue(soak.isDone());
        assertTrue(reports.get(0).contains("RUN_EXISTS"), reports.get(0));
    }
}
