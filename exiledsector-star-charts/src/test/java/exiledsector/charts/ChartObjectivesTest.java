package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.util.MutableValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartObjectivesTest {

    private static final String RUN_ID = "objectiveRun";

    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private StarSystemAPI system;
    private MutableValue credits;
    private final List<CampaignFleetAPI> systemFleets = new ArrayList<>();
    private final FakeChartContentEngine tags = new FakeChartContentEngine();

    @BeforeEach
    void setUp() {
        Map<String, Object> persistentData = new HashMap<>();
        sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        system = mock(StarSystemAPI.class);
        when(sector.getStarSystem(ChartIds.systemId(RUN_ID))).thenReturn(system);
        when(system.getFleets()).thenAnswer(invocation -> new ArrayList<>(systemFleets));
        CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
        CargoAPI cargo = mock(CargoAPI.class);
        credits = mock(MutableValue.class);
        when(cargo.getCredits()).thenReturn(credits);
        when(playerFleet.getCargo()).thenReturn(cargo);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        when(sector.getIntelManager()).thenReturn(mock(IntelManagerAPI.class));
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private ChartRun openRun(ChartObjective objective, List<String> targets) {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(7, "hegemony", objective, targets, "boss", 4, 6));
        run.recordArrival();
        ChartRuns.store(run);
        return run;
    }

    private CampaignFleetAPI fleet(String id, boolean ofThisRun) {
        CampaignFleetAPI fleet = tags.tagged(mock(CampaignFleetAPI.class));
        when(fleet.getId()).thenReturn(id);
        if (ofThisRun) {
            fleet.addTag(ChartIds.runTag(RUN_ID));
        }
        return fleet;
    }

    @Test
    void destroyingEveryGuardianCompletesClearAndGrantsTheRewardOnce() {
        ChartRun run = openRun(ChartObjective.CLEAR, List.of("g1", "g2"));

        assertTrue(ChartObjectives.fleetGone(sector, fleet("g1", true), FleetDespawnReason.DESTROYED_BY_BATTLE));
        assertEquals(1, run.objectiveTargetsDefeated());
        assertFalse(run.isObjectiveComplete());
        verify(credits, never()).add(anyFloat());

        assertTrue(ChartObjectives.fleetGone(sector, fleet("g2", true), FleetDespawnReason.NO_MEMBERS));
        assertTrue(run.isObjectiveComplete());
        assertTrue(run.isRewardClaimed());
        verify(credits, times(1)).add((float) ChartRewards.creditsFor(7));

        assertFalse(ChartObjectives.fleetGone(sector, fleet("g2", true), FleetDespawnReason.DESTROYED_BY_BATTLE));
        assertNull(ChartRewards.grant(sector, run));
        verify(credits, times(1)).add(anyFloat());
    }

    @Test
    void despawnsThatAreNotDefeatsOrBelongToOtherRunsAreIgnored() {
        ChartRun run = openRun(ChartObjective.CLEAR, List.of("g1"));

        assertFalse(ChartObjectives.fleetGone(sector, fleet("g1", true), FleetDespawnReason.PLAYER_FAR_AWAY));
        assertFalse(ChartObjectives.fleetGone(sector, fleet("g1", false), FleetDespawnReason.DESTROYED_BY_BATTLE));
        assertFalse(ChartObjectives.fleetGone(sector, fleet("patrol", true), FleetDespawnReason.DESTROYED_BY_BATTLE));
        assertEquals(0, run.objectiveTargetsDefeated());
    }

    @Test
    void nothingCountsOnceTheRunIsClosing() {
        ChartRun run = openRun(ChartObjective.BOSS, List.of("boss"));
        run.beginClosing();

        assertFalse(ChartObjectives.fleetGone(sector, fleet("boss", true), FleetDespawnReason.DESTROYED_BY_BATTLE));
        assertFalse(run.isObjectiveComplete());
        verify(credits, never()).add(anyFloat());
    }

    @Test
    void killingTheWardenCompletesBossEvenWhenAnotherFleetDidIt() {
        ChartRun run = openRun(ChartObjective.BOSS, List.of("boss"));

        assertTrue(ChartObjectives.fleetGone(sector, fleet("boss", true), FleetDespawnReason.DESTROYED_BY_BATTLE));

        assertTrue(run.isObjectiveComplete());
    }

    @Test
    void theFleetListenerForwardsDefeatsAndSurvivesErrors() {
        ChartRun run = openRun(ChartObjective.BOSS, List.of("boss"));
        ChartContentListener listener = new ChartContentListener();
        CampaignFleetAPI broken = mock(CampaignFleetAPI.class);
        when(broken.hasTag(Mockito.anyString())).thenThrow(new IllegalStateException("odd fleet"));

        listener.reportFleetDespawnedToListener(broken, FleetDespawnReason.DESTROYED_BY_BATTLE, null);
        listener.reportFleetDespawnedToListener(fleet("boss", true), FleetDespawnReason.DESTROYED_BY_BATTLE, null);

        assertTrue(run.isObjectiveComplete());
    }

    @Test
    void reconcilingCatchesTargetsThatVanishedWithoutAnEvent() {
        ChartRun run = openRun(ChartObjective.CLEAR, List.of("g1", "g2"));
        CampaignFleetAPI survivor = fleet("g1", true);
        CampaignFleetAPI despawning = fleet("g2", true);
        when(despawning.isDespawning()).thenReturn(true);
        systemFleets.add(survivor);
        systemFleets.add(despawning);

        assertTrue(ChartObjectives.reconcile(sector));
        assertEquals(List.of("g2"), run.defeatedTargetIds());
        assertFalse(run.isObjectiveComplete());

        systemFleets.clear();
        assertTrue(ChartObjectives.reconcile(sector));
        assertTrue(run.isObjectiveComplete());
        assertFalse(ChartObjectives.reconcile(sector));
    }

    @Test
    void creditsRiseWithTheTier() {
        int previous = 0;
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_TIER; tier++) {
            int credits = ChartRewards.creditsFor(tier);
            assertTrue(credits > previous, "tier " + tier);
            assertEquals(0, credits % 1000);
            previous = credits;
        }
        assertTrue(ChartRewards.itemChanceMultFor(15) > ChartRewards.itemChanceMultFor(1));
    }

    @Test
    void theIntelFollowsItsRunAndAsksToBeRemovedOnceTheRunIsGone() {
        ChartRun run = openRun(ChartObjective.CLEAR, List.of("g1", "g2"));
        ChartObjectiveIntel intel = new ChartObjectiveIntel(RUN_ID);

        assertTrue(intel.belongsToRun(RUN_ID));
        assertFalse(intel.belongsToRun("other"));
        assertFalse(intel.belongsToRun(null));
        assertFalse(intel.shouldRemoveIntel());
        assertNull(intel.getMapLocation(null));
        assertTrue(ChartObjectiveIntel.progressText(run).contains("0"));

        ChartRuns.delete(RUN_ID);
        assertTrue(intel.shouldRemoveIntel());
    }
}
