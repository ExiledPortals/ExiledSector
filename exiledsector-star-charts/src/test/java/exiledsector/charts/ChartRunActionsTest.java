package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledlib.MockSector;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.content.ChartPois;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.layout.ChartLayoutGenerator;
import exiledsector.charts.layout.ChartLayoutRequest;
import exiledsector.charts.layout.FakeChartLayoutEngine;
import exiledsector.charts.objective.ChartObjectiveIntel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartRunActionsTest {

    private MockSector mockSector;
    private SectorAPI sector;
    private LocationAPI hyperspace;
    private StarSystemAPI system;
    private CampaignFleetAPI playerFleet;
    private CustomCampaignEntityAPI outer;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        sector = mockSector.sector;
        hyperspace = mock(LocationAPI.class);
        system = mock(StarSystemAPI.class);
        playerFleet = mockSector.playerFleet();
        outer = mock(CustomCampaignEntityAPI.class);
        when(hyperspace.isHyperspace()).thenReturn(true);
        when(sector.getHyperspace()).thenReturn(hyperspace);
        when(sector.createStarSystem(anyString())).thenReturn(system);
        when(system.getLocation()).thenReturn(new Vector2f());
        when(system.getMemoryWithoutUpdate()).thenReturn(mock(MemoryAPI.class));
        when(system.initStar(anyString(), anyString(), anyFloat(), anyFloat())).thenReturn(mock(PlanetAPI.class));
        when(system.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(mock(CustomCampaignEntityAPI.class));
        when(hyperspace.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(outer);
        when(playerFleet.getContainingLocation()).thenReturn(hyperspace);
        when(playerFleet.getLocation()).thenReturn(new Vector2f(500f, 600f));
        when(system.getStar()).thenReturn(mock(PlanetAPI.class));
        FakeChartLayoutEngine.install(FakeChartLayoutEngine.doingNothing());
        FakeChartContentEngine.install(FakeChartContentEngine.offeringNothing());
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        ChartLayoutGenerator.resetForTests();
        ChartContentGenerator.resetForTests();
    }

    @Test
    void opensARunBesideThePlayerAndStoresOnlyItsIds() {
        assertEquals(ChartRunActions.OpenResult.OPENED, ChartRunActions.openBesidePlayer(sector, 3, new Random(1)));

        ChartRun run = ChartRuns.currentOrNull();
        assertNotNull(run);
        assertEquals(ChartRunState.OPEN, run.runState());
        assertEquals(3, run.entriesAllowed());
        assertEquals(ChartIds.HYPERSPACE_LOCATION_ID, run.originLocationId());
        assertEquals(500f, run.originX());
        assertEquals(600f, run.originY());
        verify(hyperspace).addCustomEntity(eq(run.outerWormholeId()), anyString(), eq(ChartIds.WORMHOLE_TYPE), anyString());
        verify(system).setOptionalUniqueId(run.systemId());
    }

    @Test
    void refusesWhileARunIsOpen() {
        ChartRunActions.openBesidePlayer(sector, 1, new Random(1));

        assertEquals(ChartRunActions.OpenResult.RUN_EXISTS, ChartRunActions.openBesidePlayer(sector, 1, new Random(2)));
        verify(sector, Mockito.times(1)).createStarSystem(anyString());
    }

    @Test
    void refusesMidJumpOrFromInsideASystem() {
        when(playerFleet.isInHyperspaceTransition()).thenReturn(true);
        assertEquals(ChartRunActions.OpenResult.PLAYER_IN_TRANSITION, ChartRunActions.openBesidePlayer(sector, 1, new Random(1)));

        when(playerFleet.isInHyperspaceTransition()).thenReturn(false);
        StarSystemAPI otherSystem = mock(StarSystemAPI.class);
        when(otherSystem.hasTag(ChartIds.SYSTEM_TAG)).thenReturn(true);
        when(playerFleet.getContainingLocation()).thenReturn(otherSystem);
        assertEquals(ChartRunActions.OpenResult.PLAYER_IN_SYSTEM, ChartRunActions.openBesidePlayer(sector, 1, new Random(1)));

        assertNull(ChartRuns.currentOrNull());
        verify(sector, never()).createStarSystem(anyString());
    }

    @Test
    void opensFromAStarSystemAndRemembersItById() {
        StarSystemAPI realSystem = mock(StarSystemAPI.class);
        when(realSystem.getId()).thenReturn("corvus");
        when(realSystem.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(outer);
        when(playerFleet.getContainingLocation()).thenReturn(realSystem);

        ChartRunActions.openBesidePlayer(sector, 1, new Random(1));

        assertEquals("corvus", ChartRuns.currentOrNull().originLocationId());
        when(sector.getStarSystem("corvus")).thenReturn(realSystem);
        assertSame(realSystem, ChartIds.resolveLocation(sector, "corvus"));
    }

    @Test
    void openingRecordsTheTiersContentAndPostsTheObjectiveIntel() throws Exception {
        ChartTables.loadShipped();
        FakeChartContentEngine content = FakeChartContentEngine.install(new FakeChartContentEngine().offering(-4000f, 0f, 0f, 5000f));
        when(sector.getFaction(anyString())).thenReturn(mock(FactionAPI.class));
        IntelManagerAPI intelManager = mock(IntelManagerAPI.class);
        when(sector.getIntelManager()).thenReturn(intelManager);
        try {
            assertEquals(ChartRunActions.OpenResult.OPENED, ChartRunActions.openBesidePlayer(sector, 1, new Random(1),
                    new ChartLayoutRequest(99L, null, 9)));

            ChartRun run = ChartRuns.currentOrNull();
            assertEquals(9, run.tier());
            assertNotNull(run.objective());
            assertEquals(content.fleets.size(), run.contentFleets());
            ArgumentCaptor<IntelInfoPlugin> posted =
                    ArgumentCaptor.forClass(IntelInfoPlugin.class);
            verify(intelManager).addIntel(posted.capture(), Mockito.eq(true));
            assertTrue(((ChartObjectiveIntel) posted.getValue()).belongsToRun(run.runId()));
        } finally {
            ChartTiers.resetForTests();
            ChartPois.resetForTests();
            ChartFactions.resetForTests();
        }
    }

    @Test
    void aFailedOpenCleansUpAndLeavesNoRecord() {
        when(sector.createStarSystem(anyString())).thenThrow(new IllegalStateException("engine changed"));

        assertEquals(ChartRunActions.OpenResult.FAILED, ChartRunActions.openBesidePlayer(sector, 1, new Random(1)));
        assertNull(ChartRuns.currentOrNull());
    }

    @Test
    void enteringJumpsToTheSystemThroughTheOuterWormhole() {
        ChartRun run = new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        when(sector.getStarSystem(run.systemId())).thenReturn(system);
        SectorEntityToken entry = mock(SectorEntityToken.class);
        when(system.createToken(anyFloat(), anyFloat())).thenReturn(entry);

        assertTrue(ChartRunActions.enter(sector, run, outer));

        ArgumentCaptor<JumpDestination> destination = ArgumentCaptor.forClass(JumpDestination.class);
        verify(sector).doHyperspaceTransition(eq(playerFleet), eq(outer), destination.capture());
        assertSame(entry, destination.getValue().getDestination());
        assertEquals(0, run.entriesUsed(), "the entry is counted when the fleet arrives, so an aborted jump costs nothing");
    }

    @Test
    void cannotEnterWithoutEntriesLeft() {
        ChartRun run = new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.collapseOuter();

        assertFalse(ChartRunActions.enter(sector, run, outer));
        verify(sector, never()).doHyperspaceTransition(any(), any(), any());
    }

    @Test
    void returningLandsBesideTheOuterWormholeWhileItIsOpen() {
        ChartRun run = new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 2);
        run.recordArrival();
        when(sector.getEntityById(run.outerWormholeId())).thenReturn(outer);
        when(outer.getContainingLocation()).thenReturn(hyperspace);
        when(outer.getLocation()).thenReturn(new Vector2f(100f, 100f));
        SectorEntityToken arrival = mock(SectorEntityToken.class);
        when(arrival.getContainingLocation()).thenReturn(hyperspace);
        when(hyperspace.createToken(100f + ChartSystemFactory.ENTRY_OFFSET, 100f)).thenReturn(arrival);
        SectorEntityToken inner = mock(SectorEntityToken.class);

        assertTrue(ChartRunActions.returnToOrigin(sector, run, inner));

        assertTrue(run.isReturnRequested());
        verify(sector).doHyperspaceTransition(eq(playerFleet), eq(inner), any(JumpDestination.class));
    }

    @Test
    void returningAfterTheOuterCollapsedLandsAtTheOrigin() {
        ChartRun run = new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 40f, 50f, 1);
        run.recordArrival();
        SectorEntityToken arrival = mock(SectorEntityToken.class);
        when(hyperspace.createToken(40f, 50f)).thenReturn(arrival);

        assertSame(arrival, ChartRunActions.returnPoint(sector, run));
    }

    @Test
    void returningAfterTheOuterCollapsedLandsBesideTheMarketWhereverItHasOrbitedTo() {
        ChartRun run = new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 40f, 50f, 1);
        run.recordOriginEntity("gateway_planet");
        run.recordArrival();
        PlanetAPI market = mock(PlanetAPI.class);
        when(sector.getEntityById("gateway_planet")).thenReturn(market);
        when(market.getContainingLocation()).thenReturn(hyperspace);
        when(market.getLocation()).thenReturn(new Vector2f(3000f, -800f));
        when(market.getRadius()).thenReturn(120f);
        SectorEntityToken arrival = mock(SectorEntityToken.class);
        when(hyperspace.createToken(3000f + 120f + ChartSystemFactory.ENTRY_OFFSET, -800f)).thenReturn(arrival);

        assertSame(arrival, ChartRunActions.returnPoint(sector, run));
    }

    @Test
    void theNearestMarketBesideThePlayerAnchorsTheWormholeAndBareSpaceAnchorsNothing() {
        LocationAPI system = mock(LocationAPI.class);
        PlanetAPI near = marketAt(new Vector2f(100f, 0f), 200f, false);
        PlanetAPI far = marketAt(new Vector2f(5000f, 0f), 200f, false);
        PlanetAPI barren = marketAt(new Vector2f(0f, 50f), 200f, true);
        SectorEntityToken rock = mock(SectorEntityToken.class);
        when(rock.getLocation()).thenReturn(new Vector2f(0f, 0f));
        when(system.getAllEntities()).thenReturn(List.of(far, barren, rock, near));

        assertSame(near, ChartRunActions.marketBeside(system, new Vector2f(0f, 0f)));
        assertNull(ChartRunActions.marketBeside(system, new Vector2f(2500f, 2500f)));
    }

    private static PlanetAPI marketAt(Vector2f location, float radius, boolean conditionsOnly) {
        PlanetAPI planet = mock(PlanetAPI.class);
        MarketAPI market = mock(MarketAPI.class);
        when(market.isPlanetConditionMarketOnly()).thenReturn(conditionsOnly);
        when(planet.getMarket()).thenReturn(market);
        when(planet.getLocation()).thenReturn(location);
        when(planet.getRadius()).thenReturn(radius);
        return planet;
    }

    @Test
    void returningToAVanishedOriginFallsBackToTheRespawnLocation() {
        ChartRun run = new ChartRun("abc", "gone_system", 40f, 50f, 1);
        run.recordArrival();
        StarSystemAPI respawn = mock(StarSystemAPI.class);
        SectorEntityToken arrival = mock(SectorEntityToken.class);
        Vector2f respawnCoordinates = new Vector2f(7f, 8f);
        when(sector.getRespawnLocation()).thenReturn(respawn);
        when(sector.getRespawnCoordinates()).thenReturn(respawnCoordinates);
        when(respawn.createToken(respawnCoordinates)).thenReturn(arrival);

        assertSame(arrival, ChartRunActions.returnPoint(sector, run));
    }

    @Test
    void runIdsAreShortAndDistinct() {
        Random random = new Random(5);
        String first = ChartRunActions.newRunId(random);
        String second = ChartRunActions.newRunId(random);

        assertFalse(first.equals(second));
        assertTrue(first.matches("[0-9a-z]+"), first);
        assertTrue(List.of(first, second).stream().allMatch(id -> id.length() <= 13));
    }
}
