package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.anomaly.debris.DormantFleets;
import exiledsector.charts.content.ChartContentGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
