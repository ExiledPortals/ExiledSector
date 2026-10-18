package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private LocationAPI hyperspace;
    private StarSystemAPI pocket;
    private CampaignFleetAPI playerFleet;
    private CustomCampaignEntityAPI outer;

    @BeforeEach
    void setUp() {
        Map<String, Object> persistentData = new HashMap<>();
        sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        hyperspace = mock(LocationAPI.class);
        pocket = mock(StarSystemAPI.class);
        playerFleet = mock(CampaignFleetAPI.class);
        outer = mock(CustomCampaignEntityAPI.class);
        when(hyperspace.isHyperspace()).thenReturn(true);
        when(sector.getHyperspace()).thenReturn(hyperspace);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        when(sector.createStarSystem(anyString())).thenReturn(pocket);
        when(pocket.getLocation()).thenReturn(new Vector2f());
        when(pocket.getMemoryWithoutUpdate()).thenReturn(mock(MemoryAPI.class));
        when(pocket.initStar(anyString(), anyString(), anyFloat(), anyFloat())).thenReturn(mock(PlanetAPI.class));
        when(pocket.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(mock(CustomCampaignEntityAPI.class));
        when(hyperspace.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(outer);
        when(playerFleet.getContainingLocation()).thenReturn(hyperspace);
        when(playerFleet.getLocation()).thenReturn(new Vector2f(500f, 600f));
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
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
        verify(pocket).setOptionalUniqueId(run.pocketSystemId());
    }

    @Test
    void refusesWhileARunIsOpen() {
        ChartRunActions.openBesidePlayer(sector, 1, new Random(1));

        assertEquals(ChartRunActions.OpenResult.RUN_EXISTS, ChartRunActions.openBesidePlayer(sector, 1, new Random(2)));
        verify(sector, Mockito.times(1)).createStarSystem(anyString());
    }

    @Test
    void refusesMidJumpOrFromInsideAPocket() {
        when(playerFleet.isInHyperspaceTransition()).thenReturn(true);
        assertEquals(ChartRunActions.OpenResult.PLAYER_IN_TRANSITION, ChartRunActions.openBesidePlayer(sector, 1, new Random(1)));

        when(playerFleet.isInHyperspaceTransition()).thenReturn(false);
        StarSystemAPI otherPocket = mock(StarSystemAPI.class);
        when(otherPocket.hasTag(ChartIds.POCKET_TAG)).thenReturn(true);
        when(playerFleet.getContainingLocation()).thenReturn(otherPocket);
        assertEquals(ChartRunActions.OpenResult.PLAYER_IN_POCKET, ChartRunActions.openBesidePlayer(sector, 1, new Random(1)));

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
    void aFailedOpenCleansUpAndLeavesNoRecord() {
        when(sector.createStarSystem(anyString())).thenThrow(new IllegalStateException("engine changed"));

        assertEquals(ChartRunActions.OpenResult.FAILED, ChartRunActions.openBesidePlayer(sector, 1, new Random(1)));
        assertNull(ChartRuns.currentOrNull());
    }

    @Test
    void enteringJumpsToThePocketThroughTheOuterWormhole() {
        ChartRun run = new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        when(sector.getStarSystem(run.pocketSystemId())).thenReturn(pocket);
        SectorEntityToken entry = mock(SectorEntityToken.class);
        when(pocket.createToken(anyFloat(), anyFloat())).thenReturn(entry);

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
        when(hyperspace.createToken(100f + ChartPocketFactory.ENTRY_OFFSET, 100f)).thenReturn(arrival);
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
