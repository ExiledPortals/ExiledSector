package exiledsector.charts.run.diagnostics;

import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledlib.MockSector;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.ChartIds;
import exiledsector.charts.layout.ChartLayoutGenerator;
import exiledsector.charts.content.ChartPois;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunScript;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.ChartTables;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.layout.FakeChartLayoutEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
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

    private MockSector mockSector;
    private SectorAPI sector;
    private CampaignFleetAPI playerFleet;
    private final List<StarSystemAPI> starSystems = new ArrayList<>();
    private final List<StarSystemAPI> removeNextFrame = new ArrayList<>();
    private final List<String> reports = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        sector = mockSector.sector;
        LocationAPI hyperspace = mock(LocationAPI.class);
        when(hyperspace.isHyperspace()).thenReturn(true);
        when(hyperspace.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(mock(CustomCampaignEntityAPI.class));
        playerFleet = mockSector.playerFleet();
        when(playerFleet.getContainingLocation()).thenReturn(hyperspace);
        when(playerFleet.getLocation()).thenReturn(new Vector2f());
        when(sector.getHyperspace()).thenReturn(hyperspace);
        when(sector.getStarSystems()).thenAnswer(invocation -> new ArrayList<>(starSystems));
        when(sector.createStarSystem(anyString())).thenAnswer(invocation -> newSystem());
        when(sector.getStarSystem(anyString())).thenAnswer(invocation -> starSystems.stream()
                .filter(system -> invocation.getArgument(0).equals(system.getOptionalUniqueId())).findFirst().orElse(null));
        doAnswer(invocation -> removeNextFrame.add(invocation.getArgument(0))).when(sector).removeStarSystemNextFrame(Mockito.any());
        FakeChartLayoutEngine.install(FakeChartLayoutEngine.doingNothing());
        FakeChartContentEngine.install(FakeChartContentEngine.offeringNothing());
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        ChartLayoutGenerator.resetForTests();
        ChartContentGenerator.resetForTests();
    }

    private StarSystemAPI newSystem() {
        StarSystemAPI system = mock(StarSystemAPI.class);
        String[] uniqueId = new String[1];
        doAnswer(invocation -> uniqueId[0] = invocation.getArgument(0)).when(system).setOptionalUniqueId(anyString());
        when(system.getOptionalUniqueId()).thenAnswer(invocation -> uniqueId[0]);
        when(system.getLocation()).thenReturn(new Vector2f());
        when(system.getMemoryWithoutUpdate()).thenReturn(mock(MemoryAPI.class));
        when(system.initStar(anyString(), anyString(), anyFloat(), anyFloat())).thenReturn(mock(PlanetAPI.class));
        when(system.getStar()).thenReturn(mock(PlanetAPI.class));
        when(system.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(mock(CustomCampaignEntityAPI.class));
        List<SectorEntityToken> added = new ArrayList<>();
        doAnswer(invocation -> added.add(invocation.getArgument(0))).when(system).addEntity(Mockito.any());
        doAnswer(invocation -> added.remove(invocation.<Object>getArgument(0))).when(system).removeEntity(Mockito.any());
        when(system.getFleets()).thenAnswer(invocation -> added.stream().filter(CampaignFleetAPI.class::isInstance)
                .map(CampaignFleetAPI.class::cast).toList());
        when(system.getAllEntities()).thenAnswer(invocation -> new ArrayList<>(added));
        starSystems.add(system);
        return system;
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
    void soaksTheRequestedSystemsWithoutMovingThePlayerAndReportsBeforeAndAfter() {
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
        verify(sector, Mockito.times(3)).createStarSystem(ChartIds.SYSTEM_BASE_NAME);
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
    void theCensusAlsoNamesLeakedFleetsScriptsAndListeners() {
        ChartCensus before = new ChartCensus(10, 0, 100, 20, 0, 0, 500, 12, 40, 30, 7, 60, 0, 25, 9);
        ChartCensus leaked = new ChartCensus(10, 0, 100, 20, 0, 0, 500, 12, 40, 30, 7, 62, 2, 26, 10);

        assertEquals(List.of("fleets in the sector 60 -> 62", "chart fleets anywhere 0 -> 2", "sector scripts 25 -> 26",
                "sector listeners 9 -> 10"), leaked.changesSince(before));
    }

    @Test
    void soaksFullContentSystemsAcrossEveryTierAndStillLeavesEveryCountUnchanged() throws Exception {
        ChartTables.loadShipped();
        FakeChartContentEngine content = FakeChartContentEngine.install(new FakeChartContentEngine().offering(-4000f, 0f, 0f, 5000f));
        when(sector.getFaction(anyString())).thenReturn(mock(FactionAPI.class));
        try {
            ChartCensus before = ChartCensus.take(sector);
            ChartSoakScript soak = new ChartSoakScript(15, before, reports::add, new Random(11));

            runFrames(soak, new ChartRunScript(), 400);

            assertTrue(reports.get(0).contains("finished, 15 of 15"), reports.get(0));
            assertTrue(reports.get(0).contains("tiers [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15]"), reports.get(0));
            assertTrue(reports.get(0).contains("Unchanged before vs after"), reports.get(0));
            assertTrue(content.fleets.size() > 15);
            assertTrue(content.salvageEntities.size() > 15);
            for (CampaignFleetAPI fleet : content.fleets) {
                verify(fleet).despawn(FleetDespawnReason.PLAYER_FAR_AWAY, null);
            }
        } finally {
            ChartContentGenerator.resetForTests();
            ChartTiers.resetForTests();
            ChartPois.resetForTests();
            ChartFactions.resetForTests();
        }
    }

    @Test
    void soaksSystemsOpenedFromRolledChartsAndStillLeavesEveryCountUnchanged() throws Exception {
        ChartTables.loadShipped();
        exiledsector.items.StarChartFixtures.registerShipped();
        FakeChartContentEngine.install(new FakeChartContentEngine().offering(-4000f, 0f, 0f, 5000f));
        when(sector.getFaction(anyString())).thenReturn(mock(FactionAPI.class));
        try {
            ChartCensus before = ChartCensus.take(sector);
            ChartSoakScript soak = new ChartSoakScript(16, before, reports::add, new Random(13));

            runFrames(soak, new ChartRunScript(), 400);

            assertTrue(reports.get(0).contains("finished, 16 of 16"), reports.get(0));
            assertTrue(reports.get(0).contains("8 system(s) opened from rolled Star Charts"), reports.get(0));
            assertTrue(reports.get(0).contains("Unchanged before vs after"), reports.get(0));
            assertTrue(starSystems.isEmpty());
            assertNull(ChartRuns.currentOrNull());
            assertTrue(ChartSoakScript.chartForIteration(1, new Random(1)) != null);
            assertNull(ChartSoakScript.chartForIteration(2, new Random(1)));
        } finally {
            exiledsector.items.StarChartFixtures.clear();
            ChartContentGenerator.resetForTests();
            ChartTiers.resetForTests();
            ChartPois.resetForTests();
            ChartFactions.resetForTests();
        }
    }

    @Test
    void aChartSystemThatFailsToOpenIsNotCountedAsOpened() throws Exception {
        ChartTables.loadShipped();
        exiledsector.items.StarChartFixtures.registerShipped();
        FakeChartContentEngine.install(new FakeChartContentEngine().offering(-4000f, 0f, 0f, 5000f));
        when(sector.getFaction(anyString())).thenReturn(mock(FactionAPI.class));
        try {
            ChartSoakScript soak = new ChartSoakScript(2, ChartCensus.take(sector), reports::add, new Random(13));
            ChartRunScript watchdog = new ChartRunScript();
            for (int frame = 0; frame < 200 && soak.systemsDone() < 1; frame++) {
                runFrames(soak, watchdog, 1);
            }
            assertEquals(1, soak.systemsDone());
            assertNull(ChartRuns.currentOrNull());
            ChartRuns.store(new ChartRun("busy", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1));

            soak.advance(0f);

            assertTrue(soak.isDone());
            assertEquals(0, soak.chartsOpened());
            assertTrue(reports.get(0).contains("returned RUN_EXISTS"), reports.get(0));
            assertTrue(reports.get(0).contains("0 system(s) opened from rolled Star Charts"), reports.get(0));
        } finally {
            exiledsector.items.StarChartFixtures.clear();
            ChartContentGenerator.resetForTests();
            ChartTiers.resetForTests();
            ChartPois.resetForTests();
            ChartFactions.resetForTests();
        }
    }

    @Test
    void tiersCycleThroughOneToFifteen() {
        assertEquals(1, ChartSoakScript.tierForIteration(0));
        assertEquals(15, ChartSoakScript.tierForIteration(14));
        assertEquals(1, ChartSoakScript.tierForIteration(15));
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
