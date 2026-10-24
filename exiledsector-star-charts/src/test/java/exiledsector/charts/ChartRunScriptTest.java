package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import exiledlib.MockSector;
import exiledlib.charts.FakeCampaignUiData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartRunScriptTest {

    private static final String RUN_ID = "abc";

    private MockSector mockSector;
    private SectorAPI sector;
    private StarSystemAPI system;
    private LocationAPI hyperspace;
    private CampaignFleetAPI playerFleet;
    private AbilityPlugin transverseJump;
    private CustomCampaignEntityAPI outer;
    private CampaignUIAPI campaignUi;
    private ChartRunScript watchdog;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        sector = mockSector.sector;
        system = mock(StarSystemAPI.class);
        hyperspace = mock(LocationAPI.class);
        playerFleet = mockSector.playerFleet();
        transverseJump = mock(AbilityPlugin.class);
        outer = mock(CustomCampaignEntityAPI.class);
        campaignUi = mock(CampaignUIAPI.class);
        when(sector.getHyperspace()).thenReturn(hyperspace);
        when(sector.getCampaignUI()).thenReturn(campaignUi);
        when(sector.getStarSystem(ChartIds.systemId(RUN_ID))).thenReturn(system);
        when(sector.getStarSystems()).thenReturn(List.of(system));
        when(sector.getEntityById(ChartIds.outerWormholeId(RUN_ID))).thenReturn(outer);
        when(outer.getContainingLocation()).thenReturn(hyperspace);
        when(playerFleet.getAbility(Abilities.TRANSVERSE_JUMP)).thenReturn(transverseJump);
        when(playerFleet.getContainingLocation()).thenReturn(hyperspace);
        watchdog = new ChartRunScript();
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private ChartRun storeRun(int entries) {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, entries);
        ChartRuns.store(run);
        return run;
    }

    private void playerIn(LocationAPI location) {
        when(playerFleet.getContainingLocation()).thenReturn(location);
    }

    @Test
    void runsWhilePausedAndDoesNothingWithoutARun() {
        assertTrue(watchdog.runWhilePaused());
        watchdog.advance(0.1f);

        verify(sector, never()).getPlayerFleet();
    }

    @Test
    void arrivingWithTheLastEntryCollapsesTheOuterWormholeAndBlocksTransverseJump() {
        ChartRun run = storeRun(1);
        playerIn(system);

        watchdog.advance(0.1f);

        assertEquals(ChartRunState.INSIDE, run.runState());
        verify(hyperspace).removeEntity(outer);
        verify(transverseJump).forceDisable();
    }

    @Test
    void transverseJumpStaysBlockedEveryFrameInside() {
        storeRun(2);
        playerIn(system);

        watchdog.advance(0f);
        watchdog.advance(0f);
        watchdog.advance(0f);

        verify(transverseJump, times(3)).forceDisable();
    }

    @Test
    void arrivingWithEntriesLeftKeepsTheOuterWormhole() {
        ChartRun run = storeRun(2);
        playerIn(system);

        watchdog.advance(0.1f);

        assertEquals(ChartRunState.INSIDE, run.runState());
        verify(hyperspace, never()).removeEntity(any());
    }

    @Test
    void nothingChangesWhileThePlayerIsMidJump() {
        ChartRun run = storeRun(1);
        playerIn(system);
        watchdog.advance(0.1f);
        run.requestReturn();
        playerIn(hyperspace);
        when(playerFleet.isInHyperspaceTransition()).thenReturn(true);

        watchdog.advance(0.1f);

        assertEquals(ChartRunState.INSIDE, run.runState());
        assertTrue(run.isReturnRequested());
    }

    @Test
    void returningWithEntriesLeftReopensTheRun() {
        ChartRun run = storeRun(2);
        playerIn(system);
        watchdog.advance(0.1f);
        run.requestReturn();
        playerIn(hyperspace);

        watchdog.advance(0.1f);

        assertEquals(ChartRunState.OPEN, run.runState());
        assertSame(run, ChartRuns.currentOrNull());
    }

    @Test
    void leavingWithEntriesLeftDropsACourseLaidToSomethingInsideTheSystem() {
        ChartRun run = storeRun(2);
        FakeCampaignUiData uiData = new FakeCampaignUiData();
        CustomCampaignEntityAPI systemPlanet = mock(CustomCampaignEntityAPI.class);
        when(systemPlanet.getContainingLocation()).thenReturn(system);
        uiData.setCourseTarget(systemPlanet);
        when(sector.getUIData()).thenReturn(uiData);
        playerIn(system);
        watchdog.advance(0.1f);
        run.requestReturn();
        playerIn(hyperspace);

        watchdog.advance(0.1f);

        assertEquals(ChartRunState.OPEN, run.runState());
        verify(campaignUi).clearLaidInCourse();
        assertNull(uiData.getCourseTarget());
    }

    @Test
    void dyingInsideClosesTheRunOnALaterFrameOnceNoDialogIsShowing() {
        ChartRun run = storeRun(3);
        playerIn(system);
        watchdog.advance(0.1f);
        playerIn(hyperspace);
        when(campaignUi.isShowingDialog()).thenReturn(true);

        watchdog.advance(0.1f);
        assertEquals(ChartRunState.CLOSING, run.runState());
        watchdog.advance(0.1f);
        watchdog.advance(0.1f);
        verify(sector, never()).removeStarSystemNextFrame(any());

        when(campaignUi.isShowingDialog()).thenReturn(false);
        watchdog.advance(0.1f);
        verify(sector, never()).removeStarSystemNextFrame(any());
        watchdog.advance(0.1f);

        verify(sector).removeStarSystemNextFrame(system);
        assertNull(ChartRuns.currentOrNull());
    }

    @Test
    void anAbortedReturnClearsTheRequestSoALaterDeathStillCloses() {
        ChartRun run = storeRun(3);
        playerIn(system);
        watchdog.advance(0.1f);
        run.requestReturn();

        watchdog.advance(0.1f);

        assertFalse(run.isReturnRequested());
    }

    @Test
    void aCollapsedRunClosesOnTheSecondFrameAfterward() {
        ChartRun run = storeRun(1);
        run.collapseOuter();

        watchdog.advance(0f);
        verify(sector, never()).removeStarSystemNextFrame(any());
        watchdog.advance(0f);

        verify(sector).removeStarSystemNextFrame(system);
        assertNull(ChartRuns.currentOrNull());
    }

    @Test
    void aClosingRunWaitsWhileThePlayerIsStillInsideTheSystem() {
        ChartRun run = storeRun(1);
        playerIn(system);
        watchdog.advance(0f);
        run.beginClosing();

        watchdog.advance(0f);
        watchdog.advance(0f);

        verify(sector, never()).removeStarSystemNextFrame(any());
        assertSame(run, ChartRuns.currentOrNull());
    }

    @Test
    void aLiveRunWhoseSystemVanishedStartsClosing() {
        ChartRun run = storeRun(1);
        when(sector.getStarSystem(ChartIds.systemId(RUN_ID))).thenReturn(null);

        watchdog.advance(0f);

        assertEquals(ChartRunState.CLOSING, run.runState());
    }

    @Test
    void aTransverseJumpAlreadyChargingIsCancelledInsideTheSystem() {
        storeRun(2);
        playerIn(system);
        when(transverseJump.isActiveOrInProgress()).thenReturn(true);

        watchdog.advance(0f);

        verify(transverseJump).deactivate();
        verify(transverseJump).forceDisable();
    }

    @Test
    void returningToAnOuterWormholeThatVanishedClosesTheRun() {
        ChartRun run = storeRun(2);
        playerIn(system);
        watchdog.advance(0.1f);
        run.requestReturn();
        playerIn(hyperspace);
        when(sector.getEntityById(ChartIds.outerWormholeId(RUN_ID))).thenReturn(null);

        watchdog.advance(0.1f);

        assertEquals(ChartRunState.CLOSING, run.runState());
    }

    @Test
    void anEngineFailureIsSwallowed() {
        storeRun(1);
        when(sector.getPlayerFleet()).thenThrow(new IllegalStateException("engine changed"));

        watchdog.advance(0f);
        watchdog.advance(0f);

        assertFalse(watchdog.isDone());
    }
}
