package exiledsector.charts.run.cleanup;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.IntelDataAPI;
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
import exiledlib.MockSector;
import exiledlib.charts.FakeCampaignUiData;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartRun;
import exiledsector.charts.ChartRunBound;
import exiledsector.charts.ChartRuns;
import exiledsector.charts.objective.ChartObjective;
import exiledsector.charts.objective.ChartObjectiveIntel;
import exiledsector.charts.objective.ChartObjectives;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private MockSector mockSector;
    private SectorAPI sector;
    private StarSystemAPI system;
    private StarSystemAPI realSystem;
    private LocationAPI hyperspace;
    private CampaignFleetAPI playerFleet;
    private CampaignFleetAPI systemFleet;
    private PlanetAPI star;
    private CustomCampaignEntityAPI inner;
    private CustomCampaignEntityAPI outer;
    private CampaignUIAPI campaignUi;
    private IntelManagerAPI intelManager;
    private IntelInfoPlugin systemIntel;
    private IntelInfoPlugin otherIntel;
    private FakeCampaignUiData uiData;
    private MemoryAPI globalMemory;
    private ChartRun run;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        sector = mockSector.sector;

        system = mock(StarSystemAPI.class);
        realSystem = mock(StarSystemAPI.class);
        hyperspace = mock(LocationAPI.class);
        playerFleet = mockSector.playerFleet();
        systemFleet = mock(CampaignFleetAPI.class);
        star = mock(PlanetAPI.class);
        inner = mock(CustomCampaignEntityAPI.class);
        outer = mock(CustomCampaignEntityAPI.class);
        campaignUi = mock(CampaignUIAPI.class);
        intelManager = mock(IntelManagerAPI.class);
        systemIntel = mock(IntelInfoPlugin.class);
        otherIntel = mock(IntelInfoPlugin.class);
        uiData = new FakeCampaignUiData();
        globalMemory = mock(MemoryAPI.class);

        when(sector.getStarSystem(ChartIds.systemId(RUN_ID))).thenReturn(system);
        when(sector.getStarSystems()).thenReturn(List.of(realSystem, system));
        when(sector.getHyperspace()).thenReturn(hyperspace);
        when(sector.getCampaignUI()).thenReturn(campaignUi);
        when(sector.getUIData()).thenReturn(uiData);
        when(sector.getIntelManager()).thenReturn(intelManager);
        when(sector.hasScript(StrandedGiveTJScript.class)).thenReturn(true);
        when(sector.getMemoryWithoutUpdate()).thenReturn(globalMemory);
        when(sector.getEntityById(ChartIds.outerWormholeId(RUN_ID))).thenReturn(outer);

        when(playerFleet.getContainingLocation()).thenReturn(hyperspace);
        when(playerFleet.getInteractionTarget()).thenReturn(inner);
        when(star.getContainingLocation()).thenReturn(system);
        when(inner.getContainingLocation()).thenReturn(system);
        when(systemFleet.getContainingLocation()).thenReturn(system);
        when(outer.getContainingLocation()).thenReturn(hyperspace);
        when(outer.hasTag(ChartIds.runTag(RUN_ID))).thenReturn(true);
        when(system.getFleets()).thenReturn(List.of(systemFleet));
        when(system.getAllEntities()).thenReturn(List.of(star, inner, systemFleet));
        when(hyperspace.getEntitiesWithTag(ChartIds.runTag(RUN_ID))).thenReturn(List.of(outer));
        when(intelManager.getIntel()).thenReturn(List.of(systemIntel, otherIntel));
        when(systemIntel.getMapLocation(null)).thenReturn(star);
        when(otherIntel.getMapLocation(null)).thenReturn(mock(SectorEntityToken.class));
        uiData.setCampaignMapLocation(system);
        uiData.setCourseTarget(inner);

        run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        ChartRuns.store(run);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    @Test
    void closesInThePlannedOrder() {
        int failures = ChartRunCloser.close(sector, run);

        assertEquals(0, failures);
        InOrder order = inOrder(playerFleet, campaignUi, sector, systemFleet, intelManager, system, hyperspace);
        order.verify(playerFleet).setInteractionTarget(null);
        order.verify(campaignUi).clearLaidInCourse();
        order.verify(sector).removeScriptsOfClass(StrandedGiveTJScript.class);
        order.verify(sector).addScript(any(StrandedGiveTJScript.class));
        order.verify(systemFleet).despawn(FleetDespawnReason.PLAYER_FAR_AWAY, null);
        order.verify(intelManager).removeIntel(systemIntel);
        order.verify(system).removeEntity(star);
        order.verify(system).removeEntity(inner);
        order.verify(hyperspace).removeEntity(outer);
        order.verify(sector).removeStarSystemNextFrame(system);
        assertNull(ChartRuns.currentOrNull());
    }

    @Test
    void onlyIntelPointingIntoTheSystemIsEnded() {
        ChartRunCloser.close(sector, run);

        verify(intelManager).removeIntel(systemIntel);
        verify(intelManager, never()).removeIntel(otherIntel);
    }

    @Test
    void forgetsTheCampaignMapLocationAndCourseWhenTheyPointIntoTheSystem() {
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
    void movesTheRespawnLocationOutOfTheSystem() {
        when(sector.getRespawnLocation()).thenReturn(system);

        ChartRunCloser.close(sector, run);

        verify(sector).setRespawnLocation(hyperspace);
    }

    @Test
    void neverRemovesThePlayerFleet() {
        when(system.getFleets()).thenReturn(List.of(systemFleet, playerFleet));
        when(system.getAllEntities()).thenReturn(List.of(star, playerFleet));

        ChartRunCloser.close(sector, run);

        verify(playerFleet, never()).despawn(any(), any());
        verify(system, never()).removeEntity(playerFleet);
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
    void officersStillPointingAtAFleetLeftInTheSystemArePointedAtTheCurrentFleet() {
        CampaignFleetAPI deadFleet = mock(CampaignFleetAPI.class);
        when(deadFleet.getContainingLocation()).thenReturn(system);
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
    void queuedIntelPointingIntoTheSystemIsUnqueued() {
        IntelInfoPlugin queued = mock(IntelInfoPlugin.class);
        when(queued.getMapLocation(null)).thenReturn(star);
        when(intelManager.getCommQueue()).thenReturn(List.of(queued, otherIntel));

        ChartRunCloser.close(sector, run);

        verify(intelManager).unqueueIntel(queued);
        verify(intelManager, never()).unqueueIntel(otherIntel);
    }

    @Test
    void anyHyperspacePresenceAnotherModGaveTheSystemIsRemoved() {
        SectorEntityToken anchor = mock(SectorEntityToken.class);
        when(system.getHyperspaceAnchor()).thenReturn(anchor);
        JumpPointAPI jumpIn = mock(JumpPointAPI.class);
        JumpPointAPI jumpElsewhere = mock(JumpPointAPI.class);
        when(jumpIn.getDestinations()).thenReturn(List.of(new JumpPointAPI.JumpDestination(star, "in")));
        SectorEntityToken elsewhere = mock(SectorEntityToken.class);
        when(elsewhere.getStarSystem()).thenReturn(realSystem);
        when(jumpElsewhere.getDestinations()).thenReturn(List.of(new JumpPointAPI.JumpDestination(elsewhere, "out")));
        when(star.getStarSystem()).thenReturn(system);
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
    void closingARunWhoseSystemIsGoneStillRemovesTheWormholeAndTheRecord() {
        when(sector.getStarSystem(ChartIds.systemId(RUN_ID))).thenReturn(null);

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
        verify(systemFleet).despawn(FleetDespawnReason.PLAYER_FAR_AWAY, null);
        verify(system).removeEntity(star);
        verify(sector).removeStarSystemNextFrame(system);
        assertNull(ChartRuns.currentOrNull());
    }

    @Test
    void aSystemAlreadyRemovedFromTheSectorIsNotRemovedAgain() {
        when(sector.getStarSystems()).thenReturn(List.of(realSystem));

        ChartRunCloser.close(sector, run);

        verify(sector, never()).removeStarSystemNextFrame(any());
    }

    @Test
    void closingAnOrphanSystemLeavesTheLiveRunAndItsSystemAlone() {
        StarSystemAPI orphan = mock(StarSystemAPI.class);
        when(sector.getStarSystems()).thenReturn(List.of(orphan, system));

        ChartRunCloser.close(sector, "orphan", orphan);

        assertSame(run, ChartRuns.currentOrNull());
        verify(sector).removeStarSystemNextFrame(orphan);
        verify(sector, never()).removeStarSystemNextFrame(system);
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
        when(systemIntel.getMapLocation(null)).thenThrow(new IllegalStateException("plugin needs a map"));

        assertEquals(0, ChartRunCloser.close(sector, run));
        verify(intelManager, never()).removeIntel(any());
    }

    @Test
    void everyKindOfEntityTheLayoutGeneratorCreatesIsRemoved() {
        List<SectorEntityToken> generated = List.of(star, mock(PlanetAPI.class), mock(CampaignTerrainAPI.class), mock(RingBandAPI.class),
                mock(AsteroidAPI.class), mock(JumpPointAPI.class), mock(SectorEntityToken.class), inner, systemFleet);
        when(system.getAllEntities()).thenReturn(generated);

        assertEquals(0, ChartRunCloser.close(sector, run));

        for (SectorEntityToken entity : generated) {
            if (entity != systemFleet) {
                verify(system).removeEntity(entity);
            }
        }
    }

    @Test
    void aSalvorsTallyStillWaitingToPostForTheSystemIsDropped() {
        SalvorsTallyAdder systemTally = new SalvorsTallyAdder(new SalvorsTallyIntel(system), 10f);
        SalvorsTallyAdder otherTally = new SalvorsTallyAdder(new SalvorsTallyIntel(realSystem), 10f);
        when(sector.getScripts()).thenReturn(new ArrayList<>(List.of(systemTally, otherTally)));

        assertEquals(0, ChartRunCloser.close(sector, run));

        verify(sector).removeScript(systemTally);
        verify(sector, never()).removeScript(otherTally);
    }

    @Test
    void theRunsObjectiveIntelAndAPostedSalvorsTallyForTheSystemAreRemovedThoughNeitherHasAMapLocation() {
        ChartObjectiveIntel objectiveIntel = new ChartObjectiveIntel(RUN_ID);
        ChartObjectiveIntel otherRunIntel = new ChartObjectiveIntel("other");
        SalvorsTallyIntel systemTally = mock(SalvorsTallyIntel.class);
        when(systemTally.getSystem()).thenReturn(system);
        SalvorsTallyIntel otherTally = mock(SalvorsTallyIntel.class);
        when(otherTally.getSystem()).thenReturn(realSystem);
        when(intelManager.getIntel()).thenReturn(List.of(objectiveIntel, otherRunIntel, systemTally, otherTally));

        assertEquals(0, ChartRunCloser.close(sector, run));

        verify(intelManager).removeIntel(objectiveIntel);
        verify(intelManager).removeIntel(systemTally);
        verify(intelManager, never()).removeIntel(otherRunIntel);
        verify(intelManager, never()).removeIntel(otherTally);
        assertTrue(objectiveIntel.isEnded());
    }

    @Test
    void theIntelTabForgetsASelectedIntelOfTheRunSoTheSaveDoesNotKeepIt() {
        IntelDataAPI intelTab = mock(IntelDataAPI.class);
        when(sector.getIntel()).thenReturn(intelTab);
        SalvorsTallyIntel systemTally = mock(SalvorsTallyIntel.class);
        when(systemTally.getSystem()).thenReturn(system);
        when(intelTab.getSelectedIntel()).thenReturn(systemTally);

        ChartRunCloser.close(sector, run);

        verify(intelTab).setSelectedIntel(null);
    }

    @Test
    void anUnrelatedSelectedIntelIsLeftSelected() {
        IntelDataAPI intelTab = mock(IntelDataAPI.class);
        when(sector.getIntel()).thenReturn(intelTab);
        when(intelTab.getSelectedIntel()).thenReturn(otherIntel);

        ChartRunCloser.close(sector, run);

        verify(intelTab, never()).setSelectedIntel(any());
    }

    @Test
    void contentFleetsAreDespawnedWhileTheRunIsClosingSoTheirDefeatNeverCounts() {
        run.recordContent(new ChartContent(3, "pirates", ChartObjective.CLEAR, List.of("guard"), null, 1, 0));
        when(systemFleet.getId()).thenReturn("guard");
        when(systemFleet.hasTag(ChartIds.runTag(RUN_ID))).thenReturn(true);
        Mockito.doAnswer(invocation -> {
            ChartObjectives.fleetGone(sector, systemFleet, FleetDespawnReason.DESTROYED_BY_BATTLE);
            return null;
        }).when(systemFleet).despawn(any(), any());
        run.beginClosing();

        ChartRunCloser.close(sector, run);

        assertEquals(0, run.objectiveTargetsDefeated());
        assertFalse(run.isRewardClaimed());
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
