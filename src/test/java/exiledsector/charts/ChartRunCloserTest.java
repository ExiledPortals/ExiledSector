package exiledsector.charts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.NascentGravityWellAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.RingBandAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.listeners.CoreDiscoverEntityPlugin.SalvorsTallyAdder;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.enc.StrandedGiveTJScript;
import com.fs.starfarer.api.impl.campaign.intel.misc.SalvorsTallyIntel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartRunCloserTest {

    private static final String RUN_ID = "abc";

    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private StarSystemAPI pocket;
    private StarSystemAPI realSystem;
    private LocationAPI hyperspace;
    private CampaignFleetAPI playerFleet;
    private CampaignFleetAPI pocketFleet;
    private PlanetAPI star;
    private CustomCampaignEntityAPI inner;
    private CustomCampaignEntityAPI outer;
    private CampaignUIAPI campaignUi;
    private IntelManagerAPI intelManager;
    private IntelInfoPlugin pocketIntel;
    private IntelInfoPlugin otherIntel;
    private FakeCampaignUiData uiData;
    private MemoryAPI globalMemory;
    private ChartRun run;

    @BeforeEach
    void setUp() {
        Map<String, Object> persistentData = new HashMap<>();
        sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);

        pocket = mock(StarSystemAPI.class);
        realSystem = mock(StarSystemAPI.class);
        hyperspace = mock(LocationAPI.class);
        playerFleet = mock(CampaignFleetAPI.class);
        pocketFleet = mock(CampaignFleetAPI.class);
        star = mock(PlanetAPI.class);
        inner = mock(CustomCampaignEntityAPI.class);
        outer = mock(CustomCampaignEntityAPI.class);
        campaignUi = mock(CampaignUIAPI.class);
        intelManager = mock(IntelManagerAPI.class);
        pocketIntel = mock(IntelInfoPlugin.class);
        otherIntel = mock(IntelInfoPlugin.class);
        uiData = new FakeCampaignUiData();
        globalMemory = mock(MemoryAPI.class);

        when(sector.getStarSystem(ChartIds.pocketSystemId(RUN_ID))).thenReturn(pocket);
        when(sector.getStarSystems()).thenReturn(List.of(realSystem, pocket));
        when(sector.getHyperspace()).thenReturn(hyperspace);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        when(sector.getCampaignUI()).thenReturn(campaignUi);
        when(sector.getUIData()).thenReturn(uiData);
        when(sector.getIntelManager()).thenReturn(intelManager);
        when(sector.hasScript(StrandedGiveTJScript.class)).thenReturn(true);
        when(sector.getMemoryWithoutUpdate()).thenReturn(globalMemory);
        when(sector.getEntityById(ChartIds.outerWormholeId(RUN_ID))).thenReturn(outer);
        when(globalMemory.getKeys()).thenReturn(List.of("$exiledSector_chart_abc_flag", "$somethingElse"));

        when(playerFleet.getContainingLocation()).thenReturn(hyperspace);
        when(playerFleet.getInteractionTarget()).thenReturn(inner);
        when(star.getContainingLocation()).thenReturn(pocket);
        when(inner.getContainingLocation()).thenReturn(pocket);
        when(pocketFleet.getContainingLocation()).thenReturn(pocket);
        when(outer.getContainingLocation()).thenReturn(hyperspace);
        when(outer.hasTag(ChartIds.runTag(RUN_ID))).thenReturn(true);
        when(pocket.getFleets()).thenReturn(List.of(pocketFleet));
        when(pocket.getAllEntities()).thenReturn(List.of(star, inner, pocketFleet));
        when(hyperspace.getEntitiesWithTag(ChartIds.runTag(RUN_ID))).thenReturn(List.of(outer));
        when(intelManager.getIntel()).thenReturn(List.of(pocketIntel, otherIntel));
        when(pocketIntel.getMapLocation(null)).thenReturn(star);
        when(otherIntel.getMapLocation(null)).thenReturn(mock(SectorEntityToken.class));
        uiData.setCampaignMapLocation(pocket);
        uiData.setCourseTarget(inner);

        run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        ChartRuns.store(run);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Test
    void closesInThePlannedOrder() {
        int failures = ChartRunCloser.close(sector, run);

        assertEquals(0, failures);
        InOrder order = inOrder(playerFleet, campaignUi, sector, pocketFleet, intelManager, pocket, hyperspace);
        order.verify(playerFleet).setInteractionTarget(null);
        order.verify(campaignUi).clearLaidInCourse();
        order.verify(sector).removeScriptsOfClass(StrandedGiveTJScript.class);
        order.verify(sector).addScript(any(StrandedGiveTJScript.class));
        order.verify(pocketFleet).despawn(FleetDespawnReason.PLAYER_FAR_AWAY, null);
        order.verify(intelManager).removeIntel(pocketIntel);
        order.verify(pocket).removeEntity(star);
        order.verify(pocket).removeEntity(inner);
        order.verify(hyperspace).removeEntity(outer);
        order.verify(sector).removeStarSystemNextFrame(pocket);
        assertNull(ChartRuns.currentOrNull());
    }

    @Test
    void onlyIntelPointingIntoThePocketIsEnded() {
        ChartRunCloser.close(sector, run);

        verify(intelManager).removeIntel(pocketIntel);
        verify(intelManager, never()).removeIntel(otherIntel);
    }

    @Test
    void forgetsTheCampaignMapLocationAndCourseWhenTheyPointIntoThePocket() {
        ChartRunCloser.close(sector, run);

        assertNull(uiData.getCampaignMapLocation());
        assertNull(uiData.getCourseTarget());
    }

    @Test
    void leavesTheCampaignMapAndCourseAloneWhenTheyPointElsewhere() {
        SectorEntityToken elsewhere = mock(SectorEntityToken.class);
        when(elsewhere.getContainingLocation()).thenReturn(realSystem);
        uiData.setCampaignMapLocation(realSystem);
        uiData.setCourseTarget(elsewhere);
        when(playerFleet.getInteractionTarget()).thenReturn(elsewhere);

        ChartRunCloser.close(sector, run);

        assertSame(realSystem, uiData.getCampaignMapLocation());
        assertSame(elsewhere, uiData.getCourseTarget());
        verify(playerFleet, never()).setInteractionTarget(any());
        verify(campaignUi, never()).clearLaidInCourse();
    }

    @Test
    void movesTheRespawnLocationOutOfThePocket() {
        when(sector.getRespawnLocation()).thenReturn(pocket);

        ChartRunCloser.close(sector, run);

        verify(sector).setRespawnLocation(hyperspace);
    }

    @Test
    void neverRemovesThePlayerFleet() {
        when(pocket.getFleets()).thenReturn(List.of(pocketFleet, playerFleet));
        when(pocket.getAllEntities()).thenReturn(List.of(star, playerFleet));

        ChartRunCloser.close(sector, run);

        verify(playerFleet, never()).despawn(any(), any());
        verify(pocket, never()).removeEntity(playerFleet);
    }

    @Test
    void clearsOnlyThisRunsMemoryKeys() {
        ChartRunCloser.close(sector, run);

        verify(globalMemory).unset("$exiledSector_chart_abc_flag");
        verify(globalMemory, never()).unset("$somethingElse");
    }

    @Test
    void removesScriptsBoundToTheRun() {
        BoundScript ownScript = new BoundScript(RUN_ID);
        BoundScript otherScript = new BoundScript("other");
        when(sector.getScripts()).thenReturn(new ArrayList<>(List.of(ownScript, otherScript)));

        ChartRunCloser.close(sector, run);

        verify(sector).removeScript(ownScript);
        verify(sector, never()).removeScript(otherScript);
    }

    @Test
    void officersStillPointingAtAFleetLeftInThePocketArePointedAtTheCurrentFleet() {
        CampaignFleetAPI deadFleet = mock(CampaignFleetAPI.class);
        when(deadFleet.getContainingLocation()).thenReturn(pocket);
        PersonAPI officer = mock(PersonAPI.class);
        PersonAPI commander = mock(PersonAPI.class);
        PersonAPI elsewhereOfficer = mock(PersonAPI.class);
        CampaignFleetAPI otherFleet = mock(CampaignFleetAPI.class);
        when(officer.getFleet()).thenReturn(deadFleet);
        when(commander.getFleet()).thenReturn(deadFleet);
        when(elsewhereOfficer.getFleet()).thenReturn(otherFleet);
        OfficerDataAPI officerData = mock(OfficerDataAPI.class);
        OfficerDataAPI elsewhereData = mock(OfficerDataAPI.class);
        when(officerData.getPerson()).thenReturn(officer);
        when(elsewhereData.getPerson()).thenReturn(elsewhereOfficer);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getOfficersCopy()).thenReturn(List.of(officerData, elsewhereData));
        when(playerFleet.getFleetData()).thenReturn(fleetData);
        when(playerFleet.getCommander()).thenReturn(commander);

        ChartRunCloser.close(sector, run);

        verify(officer).setFleet(playerFleet);
        verify(commander).setFleet(playerFleet);
        verify(elsewhereOfficer, never()).setFleet(any());
    }

    @Test
    void queuedIntelPointingIntoThePocketIsUnqueued() {
        IntelInfoPlugin queued = mock(IntelInfoPlugin.class);
        when(queued.getMapLocation(null)).thenReturn(star);
        when(intelManager.getCommQueue()).thenReturn(List.of(queued, otherIntel));

        ChartRunCloser.close(sector, run);

        verify(intelManager).unqueueIntel(queued);
        verify(intelManager, never()).unqueueIntel(otherIntel);
    }

    @Test
    void anyHyperspacePresenceAnotherModGaveThePocketIsRemoved() {
        SectorEntityToken anchor = mock(SectorEntityToken.class);
        when(pocket.getHyperspaceAnchor()).thenReturn(anchor);
        JumpPointAPI jumpIn = mock(JumpPointAPI.class);
        JumpPointAPI jumpElsewhere = mock(JumpPointAPI.class);
        when(jumpIn.getDestinations()).thenReturn(List.of(new JumpPointAPI.JumpDestination(star, "in")));
        SectorEntityToken elsewhere = mock(SectorEntityToken.class);
        when(elsewhere.getStarSystem()).thenReturn(realSystem);
        when(jumpElsewhere.getDestinations()).thenReturn(List.of(new JumpPointAPI.JumpDestination(elsewhere, "out")));
        when(star.getStarSystem()).thenReturn(pocket);
        when(hyperspace.getJumpPoints()).thenReturn(List.of(jumpIn, jumpElsewhere));
        NascentGravityWellAPI well = mock(NascentGravityWellAPI.class);
        when(well.getTarget()).thenReturn(star);
        when(hyperspace.getGravityWells()).thenReturn(List.of(well));

        ChartRunCloser.close(sector, run);

        verify(hyperspace).removeEntity(anchor);
        verify(hyperspace).removeEntity(jumpIn);
        verify(hyperspace).removeEntity(well);
        verify(hyperspace, never()).removeEntity(jumpElsewhere);
    }

    @Test
    void closingTwiceIsHarmless() {
        assertEquals(0, ChartRunCloser.close(sector, run));
        assertEquals(0, ChartRunCloser.close(sector, run));
        assertNull(ChartRuns.currentOrNull());
    }

    @Test
    void closingARunWhosePocketIsGoneStillRemovesTheWormholeAndTheRecord() {
        when(sector.getStarSystem(ChartIds.pocketSystemId(RUN_ID))).thenReturn(null);

        assertEquals(0, ChartRunCloser.close(sector, run));

        verify(hyperspace, Mockito.atLeastOnce()).removeEntity(outer);
        verify(sector, never()).removeStarSystemNextFrame(any());
        assertNull(ChartRuns.currentOrNull());
    }

    @Test
    void aFailingStepDoesNotStopTheRest() {
        when(sector.getUIData()).thenThrow(new IllegalStateException("engine changed"));
        when(sector.getIntelManager()).thenThrow(new NoSuchMethodError("engine changed"));

        int failures = ChartRunCloser.close(sector, run);

        assertTrue(failures >= 2, "failed steps " + failures);
        verify(pocketFleet).despawn(FleetDespawnReason.PLAYER_FAR_AWAY, null);
        verify(pocket).removeEntity(star);
        verify(sector).removeStarSystemNextFrame(pocket);
        assertNull(ChartRuns.currentOrNull());
    }

    @Test
    void aPocketAlreadyRemovedFromTheSectorIsNotRemovedAgain() {
        when(sector.getStarSystems()).thenReturn(List.of(realSystem));

        ChartRunCloser.close(sector, run);

        verify(sector, never()).removeStarSystemNextFrame(any());
    }

    @Test
    void closingAnOrphanPocketLeavesTheLiveRunAndItsPocketAlone() {
        StarSystemAPI orphan = mock(StarSystemAPI.class);
        when(sector.getStarSystems()).thenReturn(List.of(orphan, pocket));

        ChartRunCloser.close(sector, "orphan", orphan);

        assertSame(run, ChartRuns.currentOrNull());
        verify(sector).removeStarSystemNextFrame(orphan);
        verify(sector, never()).removeStarSystemNextFrame(pocket);
        verify(campaignUi, never()).clearLaidInCourse();
    }

    @Test
    void theStrandedScriptIsNotAddedWhenTheSectorNeverHadIt() {
        when(sector.hasScript(StrandedGiveTJScript.class)).thenReturn(false);

        ChartRunCloser.close(sector, run);

        verify(sector, never()).addScript(any());
        verify(sector, never()).removeScriptsOfClass(StrandedGiveTJScript.class);
    }

    @Test
    void intelThatCannotNameItsMapLocationIsLeftAlone() {
        when(pocketIntel.getMapLocation(null)).thenThrow(new IllegalStateException("plugin needs a map"));

        assertEquals(0, ChartRunCloser.close(sector, run));
        verify(intelManager, never()).removeIntel(any());
    }

    @Test
    void everyKindOfEntityTheLayoutGeneratorCreatesIsRemoved() {
        List<SectorEntityToken> generated = List.of(star, mock(PlanetAPI.class), mock(CampaignTerrainAPI.class), mock(RingBandAPI.class),
                mock(AsteroidAPI.class), mock(JumpPointAPI.class), mock(SectorEntityToken.class), inner, pocketFleet);
        when(pocket.getAllEntities()).thenReturn(generated);

        assertEquals(0, ChartRunCloser.close(sector, run));

        for (SectorEntityToken entity : generated) {
            if (entity != pocketFleet) {
                verify(pocket).removeEntity(entity);
            }
        }
    }

    @Test
    void aSalvorsTallyStillWaitingToPostForThePocketIsDropped() {
        SalvorsTallyAdder pocketTally = new SalvorsTallyAdder(new SalvorsTallyIntel(pocket), 10f);
        SalvorsTallyAdder otherTally = new SalvorsTallyAdder(new SalvorsTallyIntel(realSystem), 10f);
        when(sector.getScripts()).thenReturn(new ArrayList<>(List.of(pocketTally, otherTally)));

        assertEquals(0, ChartRunCloser.close(sector, run));

        verify(sector).removeScript(pocketTally);
        verify(sector, never()).removeScript(otherTally);
    }

    private record BoundScript(String runId) implements EveryFrameScript, ChartRunBound {

        @Override
        public boolean belongsToRun(String otherRunId) {
            return runId.equals(otherRunId);
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public boolean runWhilePaused() {
            return false;
        }

        @Override
        public void advance(float amount) {
        }
    }
}
