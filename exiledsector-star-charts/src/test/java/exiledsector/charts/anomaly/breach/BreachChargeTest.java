package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.StatBonus;
import exiledsector.charts.anomaly.debris.DormantFleets;
import exiledsector.charts.content.ChartContentGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BreachChargeTest {

    private final List<CampaignFleetAPI> fleets = new ArrayList<>();
    private StarSystemAPI system;
    private SectorEntityToken breach;

    @BeforeEach
    void setUp() {
        system = mock(StarSystemAPI.class);
        when(system.getFleets()).thenReturn(fleets);
        breach = mock(SectorEntityToken.class);
    }

    private CampaignFleetAPI fleet(List<FleetAssignmentDataAPI> assignments) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        CampaignFleetAIAPI ai = mock(CampaignFleetAIAPI.class);
        when(ai.getAssignmentsCopy()).thenAnswer(invocation -> new ArrayList<>(assignments));
        when(fleet.getAI()).thenReturn(ai);
        fleets.add(fleet);
        return fleet;
    }

    private static FleetAssignmentDataAPI assignment(FleetAssignment type, SectorEntityToken target) {
        FleetAssignmentDataAPI assignment = mock(FleetAssignmentDataAPI.class);
        when(assignment.getAssignment()).thenReturn(type);
        when(assignment.getTarget()).thenReturn(target);
        return assignment;
    }

    @Test
    void everyFleetThatCanMoveChargesTheBreachAheadOfItsOwnOrders() {
        CampaignFleetAPI patrol = fleet(List.of(assignment(FleetAssignment.PATROL_SYSTEM, mock(SectorEntityToken.class))));

        BreachCharge.rally(system, breach);

        verify(patrol).addAssignmentAtStart(eq(FleetAssignment.ATTACK_LOCATION), eq(breach), eq(ChartContentGenerator.ASSIGNMENT_DAYS),
                anyString(), any());
    }

    @Test
    void aFleetAlreadyChargingIsNotGivenTheOrderAgain() {
        CampaignFleetAPI charging = fleet(List.of(assignment(BreachCharge.ASSIGNMENT, breach)));

        BreachCharge.rally(system, breach);

        verify(charging, never()).addAssignmentAtStart(any(), any(), anyFloat(), anyString(), any());
    }

    @Test
    void thePlayerStationsDormantDebrisAndFleetsWithoutAnAiStayPut() {
        CampaignFleetAPI player = fleet(List.of());
        when(player.isPlayerFleet()).thenReturn(true);
        CampaignFleetAPI station = fleet(List.of());
        when(station.isStationMode()).thenReturn(true);
        CampaignFleetAPI leaving = fleet(List.of());
        when(leaving.isDespawning()).thenReturn(true);
        CampaignFleetAPI dormant = fleet(List.of());
        when(dormant.hasTag(DormantFleets.FLEET_TAG)).thenReturn(true);
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.getBoolean(DormantFleets.DORMANT_KEY)).thenReturn(true);
        when(dormant.getMemoryWithoutUpdate()).thenReturn(memory);
        CampaignFleetAPI unpowered = mock(CampaignFleetAPI.class);
        fleets.add(unpowered);

        BreachCharge.rally(system, breach);

        for (CampaignFleetAPI fleet : List.of(player, station, leaving, dormant, unpowered)) {
            assertFalse(BreachCharge.canCharge(fleet));
            verify(fleet, never()).addAssignmentAtStart(any(), any(), anyFloat(), anyString(), any());
        }
    }

    @Test
    void rudeAwakeningWakesDormantDebrisSoItChargesTooAndTheIncursionGuardNeverCharges() {
        CampaignFleetAPI dormant = fleet(List.of());
        when(dormant.hasTag(DormantFleets.FLEET_TAG)).thenReturn(true);
        Map<String, Object> stored = new HashMap<>();
        stored.put(DormantFleets.DORMANT_KEY, true);
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.getBoolean(anyString())).thenAnswer(invocation -> Boolean.TRUE.equals(stored.get((String) invocation.getArgument(0))));
        doAnswer(invocation -> stored.remove((String) invocation.getArgument(0))).when(memory).unset(anyString());
        when(dormant.getMemoryWithoutUpdate()).thenReturn(memory);
        when(dormant.getDetectedRangeMod()).thenReturn(new StatBonus());
        when(dormant.getLocation()).thenReturn(new Vector2f());
        when(dormant.getContainingLocation()).thenReturn(system);
        CampaignFleetAPI guard = fleet(List.of());
        when(guard.hasTag(BreachIncursion.GUARD_TAG)).thenReturn(true);

        BreachCharge.rally(system, breach);
        verify(dormant, never()).addAssignmentAtStart(any(), any(), anyFloat(), anyString(), any());

        BreachCharge.rally(system, breach, true);

        assertFalse(DormantFleets.isDormant(dormant));
        verify(dormant).addAssignmentAtStart(eq(FleetAssignment.ATTACK_LOCATION), eq(breach), eq(ChartContentGenerator.ASSIGNMENT_DAYS), anyString(), any());
        verify(dormant, never()).addAssignment(eq(FleetAssignment.PATROL_SYSTEM), any(), anyFloat());
        assertFalse(BreachCharge.canCharge(guard));
        verify(guard, never()).addAssignmentAtStart(any(), any(), anyFloat(), anyString(), any());
    }

    @Test
    void releaseRemovesOnlyTheChargeOrder() {
        FleetAssignmentDataAPI patrol = assignment(FleetAssignment.PATROL_SYSTEM, mock(SectorEntityToken.class));
        FleetAssignmentDataAPI charge = assignment(BreachCharge.ASSIGNMENT, breach);
        FleetAssignmentDataAPI otherAttack = assignment(BreachCharge.ASSIGNMENT, mock(SectorEntityToken.class));
        CampaignFleetAPI fleet = fleet(List.of(charge, otherAttack, patrol));
        assertTrue(BreachCharge.canCharge(fleet));

        BreachCharge.release(system, breach);

        verify(fleet.getAI(), times(1)).removeAssignment(charge);
        verify(fleet.getAI(), never()).removeAssignment(patrol);
        verify(fleet.getAI(), never()).removeAssignment(otherAttack);
    }
}
