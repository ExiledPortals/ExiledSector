package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.IntelDataAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import exiledsector.charts.ChartLoadSweep.PocketDecision;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private StarSystemAPI pocket;
    private LocationAPI hyperspace;
    private CampaignFleetAPI playerFleet;
    private SectorEntityToken pocketStar;

    @BeforeEach
    void setUp() {
        Map<String, Object> persistentData = new HashMap<>();
        sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        pocket = pocketFor(RUN_ID);
        hyperspace = mock(LocationAPI.class);
        playerFleet = mock(CampaignFleetAPI.class);
        pocketStar = taggedEntity(RUN_ID, pocket);
        when(pocket.getEntitiesWithTag(ChartIds.ENTITY_TAG)).thenReturn(List.of(pocketStar));
        when(pocket.getAllEntities()).thenReturn(List.of(pocketStar));
        when(sector.getStarSystems()).thenReturn(List.of(pocket));
        when(sector.getStarSystem(ChartIds.pocketSystemId(RUN_ID))).thenReturn(pocket);
        when(sector.getHyperspace()).thenReturn(hyperspace);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        when(playerFleet.getContainingLocation()).thenReturn(hyperspace);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static StarSystemAPI pocketFor(String runId) {
        StarSystemAPI system = mock(StarSystemAPI.class);
        when(system.hasTag(ChartIds.POCKET_TAG)).thenReturn(true);
        when(system.getOptionalUniqueId()).thenReturn(ChartIds.pocketSystemId(runId));
        when(system.getTags()).thenReturn(List.of(ChartIds.POCKET_TAG, ChartIds.runTag(runId)));
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
    void decidesToKeepALiveRunsPocketAndPurgeEverythingElse() {
        assertEquals(PocketDecision.KEEP, ChartLoadSweep.decide(pocket, runInState(ChartRunState.OPEN), false));
        assertEquals(PocketDecision.KEEP, ChartLoadSweep.decide(pocket, runInState(ChartRunState.INSIDE), true));
        assertEquals(PocketDecision.PURGE, ChartLoadSweep.decide(pocket, null, false));
        assertEquals(PocketDecision.PURGE, ChartLoadSweep.decide(pocket, runInState(ChartRunState.CLOSING), false));
        assertEquals(PocketDecision.PURGE, ChartLoadSweep.decide(pocketFor("other"), runInState(ChartRunState.OPEN), false));
    }

    @Test
    void decidesToAdoptAPocketThePlayerIsInsideWithoutALiveRun() {
        assertEquals(PocketDecision.ADOPT, ChartLoadSweep.decide(pocket, null, true));
        assertEquals(PocketDecision.ADOPT, ChartLoadSweep.decide(pocket, runInState(ChartRunState.CLOSING), true));
        assertEquals(PocketDecision.LEAVE_FOR_NOW, ChartLoadSweep.decide(pocketFor("other"), runInState(ChartRunState.OPEN), true));
    }

    @Test
    void aSaveMadeInsideKeepsItsRunAndPocket() {
        ChartRun run = runInState(ChartRunState.INSIDE);
        ChartRuns.store(run);
        when(playerFleet.getContainingLocation()).thenReturn(pocket);

        ChartLoadSweep.sweep(sector);

        assertSame(run, ChartRuns.currentOrNull());
        assertEquals(ChartRunState.INSIDE, run.runState());
        verify(sector, never()).removeStarSystemNextFrame(any());
        verify(pocket, never()).removeEntity(any());
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
    void anOrphanPocketIsPurged() {
        ChartLoadSweep.sweep(sector);

        verify(pocket, Mockito.atLeastOnce()).removeEntity(pocketStar);
        verify(sector).removeStarSystemNextFrame(pocket);
    }

    @Test
    void aClosingRunIsFinishedOnLoad() {
        ChartRuns.store(runInState(ChartRunState.CLOSING));

        ChartLoadSweep.sweep(sector);

        verify(sector).removeStarSystemNextFrame(pocket);
        assertNull(ChartRuns.currentOrNull());
    }

    @Test
    void aRunWhosePocketIsGoneIsDeletedWithItsWormhole() {
        ChartRuns.store(runInState(ChartRunState.OPEN));
        when(sector.getStarSystem(ChartIds.pocketSystemId(RUN_ID))).thenReturn(null);
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
    void aPocketThePlayerIsStuckInWithNoRecordIsAdoptedSoTheyCanLeave() {
        when(playerFleet.getContainingLocation()).thenReturn(pocket);
        when(sector.getRespawnLocation()).thenReturn(hyperspace);
        when(sector.getRespawnCoordinates()).thenReturn(new Vector2f(10f, 20f));
        when(hyperspace.isHyperspace()).thenReturn(true);
        CustomCampaignEntityAPI inner = mock(CustomCampaignEntityAPI.class);
        when(pocket.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(inner);

        ChartLoadSweep.sweep(sector);

        ChartRun adopted = ChartRuns.currentOrNull();
        assertNotNull(adopted);
        assertEquals(RUN_ID, adopted.runId());
        assertEquals(ChartRunState.INSIDE, adopted.runState());
        assertEquals(ChartIds.HYPERSPACE_LOCATION_ID, adopted.originLocationId());
        verify(pocket).addCustomEntity(Mockito.eq(ChartIds.innerWormholeId(RUN_ID)), anyString(), anyString(), anyString());
        verify(sector, never()).removeStarSystemNextFrame(any());
    }

    @Test
    void aClosingRunWithThePlayerStillInsideResumes() {
        ChartRun run = runInState(ChartRunState.INSIDE);
        run.beginClosing();
        ChartRuns.store(run);
        when(playerFleet.getContainingLocation()).thenReturn(pocket);
        when(pocket.getEntityById(ChartIds.innerWormholeId(RUN_ID))).thenReturn(mock(CustomCampaignEntityAPI.class));

        ChartLoadSweep.sweep(sector);

        assertEquals(ChartRunState.INSIDE, run.runState());
        verify(sector, never()).removeStarSystemNextFrame(any());
    }

    @Test
    void anOpenRunThatLostItsOuterWormholeIsClosedInsteadOfBlockingNewRuns() {
        ChartRuns.store(runInState(ChartRunState.OPEN));

        ChartLoadSweep.sweep(sector);

        verify(sector).removeStarSystemNextFrame(pocket);
        assertNull(ChartRuns.currentOrNull());
    }

    @Test
    void aPocketThePlayerIsInsideKeepsItsEntitiesEvenWhenAnotherRunIsLive() {
        ChartRuns.store(new ChartRun("other", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1));
        when(sector.getStarSystem(ChartIds.pocketSystemId("other"))).thenReturn(mock(StarSystemAPI.class));
        when(sector.getEntityById(ChartIds.outerWormholeId("other"))).thenReturn(mock(SectorEntityToken.class));
        when(playerFleet.getContainingLocation()).thenReturn(pocket);

        ChartLoadSweep.sweep(sector);

        verify(pocket, never()).removeEntity(any());
        verify(sector, never()).removeStarSystemNextFrame(pocket);
    }

    @Test
    void aBrokenSectorNeverThrowsOutOfTheSweep() {
        when(sector.getStarSystems()).thenThrow(new IllegalStateException("not loaded"));

        ChartLoadSweep.sweep(sector);
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
