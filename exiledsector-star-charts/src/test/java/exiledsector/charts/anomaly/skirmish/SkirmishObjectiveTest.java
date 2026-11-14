package exiledsector.charts.anomaly.skirmish;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.MutableValue;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyState;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.content.ChartStations;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.objective.ChartObjectives;
import exiledsector.charts.objective.ChartRewards;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkirmishObjectiveTest {

    private static final String RUN_ID = "skirmishRun";
    private static final SkirmishFlagship ATTACKER = SkirmishFlagship.standing(FightingSide.ATTACKER, 1, "attacker-fleet", "attacker-flagship");
    private static final SkirmishFlagship DEFENDER = SkirmishFlagship.standing(FightingSide.DEFENDER, 2, "defender-fleet", "defender-flagship");

    private MockSector mockSector;
    private SectorAPI sector;
    private MutableValue credits;
    private final List<CampaignFleetAPI> systemFleets = new ArrayList<>();
    private final FakeChartContentEngine tags = new FakeChartContentEngine();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        sector = mockSector.sector;
        StarSystemAPI system = mock(StarSystemAPI.class);
        when(sector.getStarSystem(ChartIds.systemId(RUN_ID))).thenReturn(system);
        when(system.getFleets()).thenAnswer(invocation -> new ArrayList<>(systemFleets));
        credits = mock(MutableValue.class);
        when(mockSector.cargo().getCredits()).thenReturn(credits);
        when(sector.getIntelManager()).thenReturn(mock(IntelManagerAPI.class));
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private ChartRun openSkirmishRun() {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(7, "hegemony", DEFENDER.fleetId(), 4, 6, List.of(FactionSkirmish.ANOMALY_ID)));
        run.recordArrival();
        ChartRuns.store(run);
        AnomalyState state = AnomalyState.of(run, FactionSkirmish.ANOMALY_ID);
        SkirmishFlagships.record(state, ATTACKER);
        SkirmishFlagships.record(state, DEFENDER);
        return run;
    }

    private CampaignFleetAPI fleet(SkirmishFlagship flagship) {
        CampaignFleetAPI fleet = tags.tagged(mock(CampaignFleetAPI.class));
        when(fleet.getId()).thenReturn(flagship.fleetId());
        MemoryAPI memory = FakeChartContentEngine.memory();
        memory.set(ChartIds.SKIRMISH_SIDE_MEMORY_KEY, flagship.side().key());
        when(fleet.getMemoryWithoutUpdate()).thenReturn(memory);
        fleet.addTag(ChartIds.runTag(RUN_ID));
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(flagship.flagshipId());
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));
        when(fleet.getFleetData()).thenReturn(fleetData);
        return fleet;
    }

    private static BattleAPI battle(boolean playerInvolved) {
        BattleAPI battle = mock(BattleAPI.class);
        when(battle.isPlayerInvolved()).thenReturn(playerInvolved);
        return battle;
    }

    @Test
    void oneFallenFlagshipLeavesTheRunOpenAndTheSecondCompletesIt() {
        ChartRun run = openSkirmishRun();

        assertTrue(ChartObjectives.fleetGone(sector, fleet(ATTACKER), FleetDespawnReason.DESTROYED_BY_BATTLE, battle(true)));
        assertFalse(run.isObjectiveComplete());
        verify(credits, never()).add(anyFloat());

        assertTrue(ChartObjectives.fleetGone(sector, fleet(DEFENDER), FleetDespawnReason.DESTROYED_BY_BATTLE, battle(false)));
        assertTrue(run.isObjectiveComplete());
        verify(credits, times(1)).add((float) ChartRewards.creditsFor(7));
        SkirmishFlagships flagships = SkirmishFlagships.of(run);
        assertEquals(FlagshipFall.BEATEN_BY_PLAYER, flagships.attacker().fall());
        assertEquals(FlagshipFall.LOST_TO_RIVAL, flagships.defender().fall());
        assertEquals(SkirmishResult.DEFENDER_WINS, flagships.result());
    }

    @Test
    void bothOfficersCanBeBeatenInOneRun() {
        ChartRun run = openSkirmishRun();

        CampaignFleetAPI defender = fleet(DEFENDER);
        SkirmishProgress first = SkirmishObjective.fellInBattle(sector, run, defender, defender.getFleetData().getMembersListCopy());
        SkirmishProgress second = SkirmishObjective.fleetGone(sector, run, fleet(ATTACKER), true);

        assertEquals(new SkirmishProgress(true, null, false, DEFENDER.officerRecordId()), first);
        assertEquals(new SkirmishProgress(true, null, true, ATTACKER.officerRecordId()), second);
        assertEquals(SkirmishResult.DROPPED, SkirmishFlagships.of(run).result());
        assertEquals(SkirmishProgress.NONE, SkirmishObjective.fleetGone(sector, run, fleet(ATTACKER), true));
    }

    @Test
    void aFlagshipMissingFromTheSystemFellToItsRival() {
        ChartRun run = openSkirmishRun();
        systemFleets.add(fleet(ATTACKER));

        assertTrue(ChartObjectives.reconcile(sector));

        assertFalse(run.isObjectiveComplete());
        assertEquals(FlagshipFall.LOST_TO_RIVAL, SkirmishFlagships.of(run).defender().fall());
        assertEquals(SkirmishResult.ATTACKER_WINS, SkirmishFlagships.of(run).result());
        assertFalse(ChartObjectives.reconcile(sector));
    }

    @Test
    void aRunWithoutTrackedFlagshipsKeepsTheOrdinaryBossObjective() {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(7, "hegemony", DEFENDER.fleetId(), 4, 6, List.of(FactionSkirmish.ANOMALY_ID)));
        run.recordArrival();
        ChartRuns.store(run);

        assertFalse(SkirmishObjective.tracks(run));
        assertTrue(ChartObjectives.fleetGone(sector, fleet(DEFENDER), FleetDespawnReason.DESTROYED_BY_BATTLE, null));
        assertTrue(run.isObjectiveComplete());
    }

    @Test
    void theAttackerMovesOnTheStationOnceADefenderFleetFalls() {
        CampaignFleetAPI attacker = fleet(ATTACKER);
        CampaignFleetAPI station = fleet(DEFENDER);
        station.addTag(ChartStations.STATION_TAG);
        List<FleetAssignment> orders = new ArrayList<>();
        doAnswer(invocation -> orders.add(invocation.getArgument(0))).when(attacker)
                .addAssignment(any(FleetAssignment.class), any(), anyFloat());
        StarSystemAPI system = mock(StarSystemAPI.class);
        when(system.getFleets()).thenReturn(List.of(attacker, station));
        when(system.getCenter()).thenReturn(mock(SectorEntityToken.class));

        assertTrue(SkirmishAdvance.pushAttacker(system));
        assertFalse(SkirmishAdvance.pushAttacker(system));

        verify(attacker, times(1)).clearAssignments();
        assertEquals(List.of(FleetAssignment.ORBIT_AGGRESSIVE, FleetAssignment.PATROL_SYSTEM), orders);
    }
}
