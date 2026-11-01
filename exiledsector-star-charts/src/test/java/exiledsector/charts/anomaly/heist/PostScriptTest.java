package exiledsector.charts.anomaly.heist;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostScriptTest {

    private static final float EPSILON = 1e-4f;

    private MockSector mockSector;
    private PostSystem postSystem;
    private ChartRun run;
    private PostScript script;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        postSystem = new PostSystem();
        run = new ChartRun(PostSystem.RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        ChartRuns.store(run);
        script = PostSystem.placeIn(postSystem.site(ChartGenContext.none(), new Vector2f(9000f, 0f), 4L), run);
        when(mockSector.playerFleet().getContainingLocation()).thenReturn(postSystem.system);
    }

    @AfterEach
    void tearDown() {
        ChartContentGenerator.resetForTests();
        mockSector.close();
    }

    private void seenBy(CampaignFleetAPI picket, VisibilityLevel level) {
        when(picket.getVisibilityLevelOfPlayerFleet()).thenReturn(level);
    }

    private PostState state() {
        return PostState.of(run);
    }

    @Test
    void sensorContactBuildsSuspicionAndLosingItDrainsIt() {
        CampaignFleetAPI picket = script.livingPickets().get(0);
        seenBy(picket, VisibilityLevel.SENSOR_CONTACT);

        script.check(mockSector.sector, run, 1f);
        assertEquals(HeistRules.SUSPICION_PER_DAY, state().suspicion(), EPSILON);

        seenBy(picket, VisibilityLevel.NONE);
        script.check(mockSector.sector, run, 1f);
        assertEquals(HeistRules.SUSPICION_PER_DAY - HeistRules.SUSPICION_DRAIN_PER_DAY, state().suspicion(), EPSILON);
        assertTrue(state().quiet());
    }

    @Test
    void fullSuspicionRaisesTheAlarm() {
        seenBy(script.livingPickets().get(0), VisibilityLevel.SENSOR_CONTACT);

        script.check(mockSector.sector, run, 1f / HeistRules.SUSPICION_PER_DAY + 0.1f);

        assertTrue(state().alarm());
    }

    @Test
    void anIdentifyingPicketRaisesTheAlarmOnceItsReportGoesOut() {
        CampaignFleetAPI picket = script.livingPickets().get(0);
        seenBy(picket, VisibilityLevel.COMPOSITION_DETAILS);

        script.check(mockSector.sector, run, HeistRules.CHECK_DAYS);
        assertTrue(state().quiet());
        assertTrue(script.reportDaysLeft() >= 0f);

        seenBy(picket, VisibilityLevel.NONE);
        script.check(mockSector.sector, run, HeistRules.REPORT_DAYS);
        assertTrue(state().alarm());
    }

    @Test
    void destroyingTheIdentifyingPicketStopsItsReport() {
        CampaignFleetAPI picket = script.livingPickets().get(0);
        seenBy(picket, VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS);
        script.check(mockSector.sector, run, HeistRules.CHECK_DAYS);

        when(picket.isAlive()).thenReturn(false);
        script.check(mockSector.sector, run, HeistRules.REPORT_DAYS * 2f);

        assertTrue(state().quiet());
        assertEquals(-1f, script.reportDaysLeft());
    }

    @Test
    void aPicketThatSurvivesABattleWithThePlayerRaisesTheAlarm() {
        CampaignFleetAPI picket = script.livingPickets().get(0);
        BattleAPI battle = mock(BattleAPI.class);
        when(battle.isPlayerInvolved()).thenReturn(true);

        when(picket.isEmpty()).thenReturn(true);
        script.reportBattleOccurred(picket, null, battle);
        script.reportFleetDespawnedToListener(picket, FleetDespawnReason.DESTROYED_BY_BATTLE, null);
        assertTrue(state().quiet());

        CampaignFleetAPI survivor = script.livingPickets().get(0);
        script.reportBattleOccurred(survivor, null, battle);
        assertTrue(state().alarm());
    }

    @Test
    void theAlarmSendsThePicketsToThePostAndPurgesTheVault() {
        assertTrue(script.raiseAlarm(run));
        assertFalse(script.raiseAlarm(run));

        for (CampaignFleetAPI picket : script.livingPickets()) {
            List<FakeChartContentEngine.Assignment> assignments = postSystem.assignments(picket);
            assertEquals(FleetAssignment.GO_TO_LOCATION, assignments.get(0).assignment());
            assertSame(postSystem.post(), assignments.get(0).target());
        }
        script.check(mockSector.sector, run, 1f);
        assertEquals(1f - HeistRules.PURGE_PER_DAY, state().vaultShare(), EPSILON);
        script.check(mockSector.sector, run, 100f);
        assertEquals(HeistRules.VAULT_FLOOR, state().vaultShare(), EPSILON);
    }

    @Test
    void picketsReachingThePostFormOneGuard() {
        List<CampaignFleetAPI> pickets = script.livingPickets();
        List<List<FleetMemberAPI>> rosters = new ArrayList<>();
        for (CampaignFleetAPI picket : pickets) {
            rosters.add(fleetData(picket));
        }
        script.raiseAlarm(run);
        for (CampaignFleetAPI picket : pickets) {
            picket.setLocation(postSystem.post().getLocation().x, postSystem.post().getLocation().y);
        }

        script.check(mockSector.sector, run, HeistRules.CHECK_DAYS);

        CampaignFleetAPI guard = script.guard();
        assertSame(pickets.get(0), guard);
        assertTrue(Pickets.isGuard(guard));
        assertTrue(guard.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE));
        assertFalse(guard.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE));
        assertEquals(FleetAssignment.ORBIT_AGGRESSIVE, postSystem.assignments(guard).get(0).assignment());
        assertEquals(pickets.size(), rosters.get(0).size());
        for (int index = 1; index < pickets.size(); index++) {
            verify(pickets.get(index)).despawn(FleetDespawnReason.OTHER, null);
        }
        assertTrue(script.livingPickets().isEmpty());
    }

    @Test
    void engagingFromThePostGathersEveryPicketAtOnce() {
        List<CampaignFleetAPI> pickets = script.livingPickets();
        pickets.forEach(this::fleetData);
        script.raiseAlarm(run);

        CampaignFleetAPI guard = script.gatherDefenders();

        assertSame(pickets.get(0), guard);
        assertTrue(script.livingPickets().isEmpty());
        assertTrue(script.defended());
        when(guard.isEmpty()).thenReturn(true);
        assertNull(script.guard());
        assertFalse(script.defended());
    }

    private List<FleetMemberAPI> fleetData(CampaignFleetAPI fleet) {
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        List<FleetMemberAPI> members = new ArrayList<>();
        members.add(mock(FleetMemberAPI.class));
        when(fleetData.getMembersListCopy()).thenAnswer(invocation -> new ArrayList<>(members));
        doAnswer(invocation -> members.remove((FleetMemberAPI) invocation.getArgument(0))).when(fleetData).removeFleetMember(any());
        doAnswer(invocation -> members.add(invocation.getArgument(0))).when(fleetData).addFleetMember(any(FleetMemberAPI.class));
        when(fleet.getFleetData()).thenReturn(fleetData);
        doAnswer(invocation -> {
            when(fleet.isAlive()).thenReturn(false);
            return null;
        }).when(fleet).despawn(any(), any());
        return members;
    }
}
