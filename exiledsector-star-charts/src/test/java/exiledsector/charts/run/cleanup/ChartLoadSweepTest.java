package exiledsector.charts.run.cleanup;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.IntelDataAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import exiledlib.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunState;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.objective.ChartObjectiveIntel;
import exiledsector.charts.run.cleanup.ChartLoadSweep.SystemDecision;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartLoadSweepTest {

    private static final String RUN_ID = "abc";

    private MockSector mockSector;
    private SectorAPI sector;
    private StarSystemAPI system;
    private LocationAPI hyperspace;
    private CampaignFleetAPI playerFleet;
    private SectorEntityToken systemstar;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        sector = mockSector.sector;
        system = systemFor(RUN_ID);
        hyperspace = mock(LocationAPI.class);
        playerFleet = mockSector.playerFleet();
        systemstar = taggedEntity(RUN_ID, system);
        when(system.getEntitiesWithTag(ChartIds.ENTITY_TAG)).thenReturn(List.of(systemstar));
        when(system.getAllEntities()).thenReturn(List.of(systemstar));
        when(sector.getStarSystems()).thenReturn(List.of(system));
        when(sector.getStarSystem(ChartIds.systemId(RUN_ID))).thenReturn(system);
        when(sector.getHyperspace()).thenReturn(hyperspace);
        when(playerFleet.getContainingLocation()).thenReturn(hyperspace);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private static StarSystemAPI systemFor(String runId) {
        StarSystemAPI system = mock(StarSystemAPI.class);
        when(system.hasTag(ChartIds.SYSTEM_TAG)).thenReturn(true);
        when(system.getOptionalUniqueId()).thenReturn(ChartIds.systemId(runId));
        when(system.getTags()).thenReturn(List.of(ChartIds.SYSTEM_TAG, ChartIds.runTag(runId)));
        return system;
    }

    private static SectorEntityToken taggedEntity(String runId, LocationAPI location) {
        SectorEntityToken entity = mock(SectorEntityToken.class);
        when(entity.hasTag(ChartIds.ENTITY_TAG)).thenReturn(true);
        when(entity.hasTag(ChartIds.runTag(runId))).thenReturn(true);
        when(entity.getContainingLocation()).thenReturn(location);
        return entity;
    }

    private static ChartRun runInState(ChartRunState wanted) {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        if (wanted == ChartRunState.INSIDE) {
            run.recordArrival();
        } else if (wanted == ChartRunState.CLOSING) {
            run.collapseOuter();
        } else if (wanted == ChartRunState.CLOSED) {
            run.markClosed();
        }
        return run;
    }

    @Test
    void decidesToKeepALiveRunsSystemAndPurgeEverythingElse() {
        assertEquals(SystemDecision.KEEP, ChartLoadSweep.decide(system, runInState(ChartRunState.OPEN), false));
        assertEquals(SystemDecision.KEEP, ChartLoadSweep.decide(system, runInState(ChartRunState.INSIDE), true));
        assertEquals(SystemDecision.PURGE, ChartLoadSweep.decide(system, null, false));
        assertEquals(SystemDecision.PURGE, ChartLoadSweep.decide(system, runInState(ChartRunState.CLOSING), false));
        assertEquals(SystemDecision.PURGE, ChartLoadSweep.decide(systemFor("other"), runInState(ChartRunState.OPEN), false));
    }

    @Test
    void decidesToAdoptASystemThePlayerIsInsideWithoutALiveRun() {
        assertEquals(SystemDecision.ADOPT, ChartLoadSweep.decide(system, null, true));
        assertEquals(SystemDecision.ADOPT, ChartLoadSweep.decide(system, runInState(ChartRunState.CLOSING), true));
        assertEquals(SystemDecision.LEAVE_FOR_NOW, ChartLoadSweep.decide(systemFor("other"), runInState(ChartRunState.OPEN), true));
    }

    @Test
    void aSaveMadeInsideKeepsItsRunAndSystem() {
        ChartRun run = runInState(ChartRunState.INSIDE);
        ChartRuns.store(run);
        when(playerFleet.getContainingLocation()).thenReturn(system);

        ChartLoadSweep.sweep(sector);

        assertSame(run, ChartRuns.currentOrNull());
        assertEquals(ChartRunState.INSIDE, run.runState());
        verify(sector, never()).removeStarSystemNextFrame(any());
        verify(system, never()).removeEntity(any());
    }

    @Test
    void anOpenRunKeepsItsOuterWormholeInHyperspace() {
        ChartRuns.store(runInState(ChartRunState.OPEN));
        SectorEntityToken outer = taggedEntity(RUN_ID, hyperspace);
        when(hyperspace.getEntitiesWithTag(ChartIds.ENTITY_TAG)).thenReturn(List.of(outer));
        when(sector.getEntityById(ChartIds.outerWormholeId(RUN_ID))).thenReturn(outer);

        ChartLoadSweep.sweep(sector);

        verify(hyperspace, never()).removeEntity(any());
        assertNotNull(ChartRuns.currentOrNull());
    }

    @Test
    void anOrphanSystemIsPurged() {
        ChartLoadSweep.sweep(sector);

        verify(system, Mockito.atLeastOnce()).removeEntity(systemstar);
        verify(sector).removeStarSystemNextFrame(system);
    }

    @Test
    void aClosingRunIsFinishedOnLoad() {
        ChartRuns.store(runInState(ChartRunState.CLOSING));

        ChartLoadSweep.sweep(sector);

        verify(sector).removeStarSystemNextFrame(system);
        assertNull(ChartRuns.currentOrNull());
    }

    @Test
    void aRunWhoseSystemIsGoneIsDeletedWithItsWormhole() {
        ChartRuns.store(runInState(ChartRunState.OPEN));
        when(sector.getStarSystem(ChartIds.systemId(RUN_ID))).thenReturn(null);
        when(sector.getStarSystems()).thenReturn(List.of());
        SectorEntityToken outer = taggedEntity(RUN_ID, hyperspace);
        when(sector.getEntityById(ChartIds.outerWormholeId(RUN_ID))).thenReturn(outer);

        ChartLoadSweep.sweep(sector);

        verify(hyperspace).removeEntity(outer);
        assertNull(ChartRuns.currentOrNull());
    }

    @Test
    void leftoverEntitiesOfDeadRunsAreRemovedFromHyperspace() {
        SectorEntityToken leftover = taggedEntity("dead", hyperspace);
        when(hyperspace.getEntitiesWithTag(ChartIds.ENTITY_TAG)).thenReturn(List.of(leftover));
        when(sector.getStarSystems()).thenReturn(List.of());

        ChartLoadSweep.sweep(sector);

        verify(hyperspace).removeEntity(leftover);
    }

    @Test
    void aSystemThePlayerIsStuckInWithNoRecordIsAdoptedSoTheyCanLeave() {
        when(playerFleet.getContainingLocation()).thenReturn(system);
        when(sector.getRespawnLocation()).thenReturn(hyperspace);
        when(sector.getRespawnCoordinates()).thenReturn(new Vector2f(10f, 20f));
        when(hyperspace.isHyperspace()).thenReturn(true);
        CustomCampaignEntityAPI inner = mock(CustomCampaignEntityAPI.class);
        when(system.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(inner);

        ChartLoadSweep.sweep(sector);

        ChartRun adopted = ChartRuns.currentOrNull();
        assertNotNull(adopted);
        assertEquals(RUN_ID, adopted.runId());
        assertEquals(ChartRunState.INSIDE, adopted.runState());
        assertEquals(ChartIds.HYPERSPACE_LOCATION_ID, adopted.originLocationId());
        verify(system).addCustomEntity(Mockito.eq(ChartIds.innerWormholeId(RUN_ID)), anyString(), anyString(), anyString());
        verify(sector, never()).removeStarSystemNextFrame(any());
    }

    @Test
    void aLiveRunMissingItsExitGetsItBackSoThePlayerCanLeave() {
        ChartRuns.store(runInState(ChartRunState.INSIDE));
        when(playerFleet.getContainingLocation()).thenReturn(system);
        when(system.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(mock(CustomCampaignEntityAPI.class));

        ChartLoadSweep.sweep(sector);

        verify(system).addCustomEntity(Mockito.eq(ChartIds.innerWormholeId(RUN_ID)), anyString(), anyString(), anyString());
        verify(sector, never()).removeStarSystemNextFrame(any());
    }

    @Test
    void aClosingRunWithThePlayerStillInsideResumes() {
        ChartRun run = runInState(ChartRunState.INSIDE);
        run.beginClosing();
        ChartRuns.store(run);
        when(playerFleet.getContainingLocation()).thenReturn(system);
        when(system.getEntityById(ChartIds.innerWormholeId(RUN_ID))).thenReturn(mock(CustomCampaignEntityAPI.class));

        ChartLoadSweep.sweep(sector);

        assertEquals(ChartRunState.INSIDE, run.runState());
        verify(sector, never()).removeStarSystemNextFrame(any());
    }

    @Test
    void anOpenRunThatLostItsOuterWormholeIsClosedInsteadOfBlockingNewRuns() {
        ChartRuns.store(runInState(ChartRunState.OPEN));

        ChartLoadSweep.sweep(sector);

        verify(sector).removeStarSystemNextFrame(system);
        assertNull(ChartRuns.currentOrNull());
    }

    @Test
    void aSystemThePlayerIsInsideKeepsItsEntitiesEvenWhenAnotherRunIsLive() {
        ChartRuns.store(new ChartRun("other", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1));
        when(sector.getStarSystem(ChartIds.systemId("other"))).thenReturn(mock(StarSystemAPI.class));
        when(sector.getEntityById(ChartIds.outerWormholeId("other"))).thenReturn(mock(SectorEntityToken.class));
        when(playerFleet.getContainingLocation()).thenReturn(system);

        ChartLoadSweep.sweep(sector);

        verify(system, never()).removeEntity(any());
        verify(sector, never()).removeStarSystemNextFrame(system);
    }

    @Test
    void aBrokenSectorNeverThrowsOutOfTheSweep() {
        when(sector.getStarSystems()).thenThrow(new IllegalStateException("not loaded"));

        assertDoesNotThrow(() -> ChartLoadSweep.sweep(sector));
    }

    @Test
    void objectiveIntelOfADeadRunIsRemovedAndUnselectedButTheLiveRunsStays() {
        ChartRuns.store(new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1));
        ChartObjectiveIntel live = new ChartObjectiveIntel(RUN_ID);
        ChartObjectiveIntel orphan = new ChartObjectiveIntel("long_gone");
        IntelManagerAPI intelManager = mock(IntelManagerAPI.class);
        when(intelManager.getIntel(ChartObjectiveIntel.class)).thenReturn(List.of(live, orphan));
        when(sector.getIntelManager()).thenReturn(intelManager);
        IntelDataAPI intelTab = mock(IntelDataAPI.class);
        when(intelTab.getSelectedIntel()).thenReturn(orphan);
        when(sector.getIntel()).thenReturn(intelTab);

        assertEquals(1, ChartLoadSweep.removeOrphanIntel(sector));

        verify(intelManager).removeIntel(orphan);
        verify(intelManager, never()).removeIntel(live);
        verify(intelTab).setSelectedIntel(null);
    }
}
