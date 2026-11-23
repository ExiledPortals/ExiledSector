package exiledsector.charts.objective;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.ChartFleetMarks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VengeanceScriptTest {

    private static final String RUN_ID = "run1";
    private static final Vector2f RALLY_AT = new Vector2f(0f, 0f);
    private static final Vector2f FAR_AWAY = new Vector2f(VengeanceScript.GATHER_RADIUS * 5f, 0f);

    private final List<CampaignFleetAPI> fleets = new ArrayList<>();
    private StarSystemAPI system;
    private SectorEntityToken rallyPoint;
    private CampaignFleetAPI player;
    private VengeanceScript script;

    @BeforeEach
    void setUp() {
        system = mock(StarSystemAPI.class);
        when(system.getFleets()).thenReturn(fleets);
        rallyPoint = mock(SectorEntityToken.class);
        when(rallyPoint.getLocation()).thenReturn(RALLY_AT);
        player = mock(CampaignFleetAPI.class);
        when(player.isPlayerFleet()).thenReturn(true);
        when(player.getContainingLocation()).thenReturn(system);
        script = new VengeanceScript(system, rallyPoint);
    }

    private CampaignFleetAPI avenger(Vector2f at) {
        CampaignFleetAPI fleet = BossVengeanceTest.fleet(RUN_ID);
        ChartFleetMarks.markAvenger(fleet);
        when(fleet.getLocation()).thenReturn(at);
        List<FleetAssignmentDataAPI> orders = new ArrayList<>();
        when(fleet.getAI().getAssignmentsCopy()).thenAnswer(invocation -> new ArrayList<>(orders));
        fleets.add(fleet);
        return fleet;
    }

    private static void verifyOrdered(CampaignFleetAPI fleet, FleetAssignment assignment, SectorEntityToken target) {
        verify(fleet).addAssignmentAtStart(eq(assignment), eq(target),
                eq(ChartContentGenerator.ASSIGNMENT_DAYS), anyString(), any());
    }

    @Test
    void avengersMusterWhereTheBossFellUntilMostHaveArrived() {
        CampaignFleetAPI arrived = avenger(RALLY_AT);
        CampaignFleetAPI onTheWay = avenger(FAR_AWAY);

        script.tick(player, 1f);

        assertFalse(script.isStriking());
        verifyOrdered(arrived, VengeanceScript.MUSTER, rallyPoint);
        verifyOrdered(onTheWay, VengeanceScript.MUSTER, rallyPoint);
        verify(onTheWay, never()).addAssignmentAtStart(eq(VengeanceScript.STRIKE), any(), anyFloat(), anyString(), any());
    }

    @Test
    void onceGatheredTheyStrikeThePlayerTogether() {
        CampaignFleetAPI first = avenger(RALLY_AT);
        CampaignFleetAPI second = avenger(RALLY_AT);
        CampaignFleetAPI third = avenger(RALLY_AT);
        avenger(FAR_AWAY);

        script.tick(player, 1f);

        assertTrue(script.isStriking());
        for (CampaignFleetAPI fleet : List.of(first, second, third)) {
            verifyOrdered(fleet, VengeanceScript.STRIKE, player);
        }
    }

    @Test
    void stragglersNoLongerHoldTheStrikeBackOnceTheMusterRunsLong() {
        CampaignFleetAPI straggler = avenger(FAR_AWAY);
        avenger(FAR_AWAY);

        script.tick(player, VengeanceScript.MOST_MUSTER_DAYS - 1f);
        assertFalse(script.isStriking());
        script.tick(player, 1f);

        assertTrue(script.isStriking());
        verifyOrdered(straggler, VengeanceScript.STRIKE, player);
    }

    @Test
    void theyWaitForThePlayerAndLeaveFleetsInBattleAlone() {
        CampaignFleetAPI fighting = avenger(RALLY_AT);
        when(fighting.getBattle()).thenReturn(mock(BattleAPI.class));
        when(player.getContainingLocation()).thenReturn(mock(StarSystemAPI.class));

        script.tick(player, VengeanceScript.MOST_MUSTER_DAYS);

        assertFalse(script.isStriking());
        verify(fighting, never()).addAssignmentAtStart(any(), any(), anyFloat(), anyString(), any());
    }

    @Test
    void theScriptEndsOnceNoAvengerIsLeft() {
        CampaignFleetAPI unmarked = BossVengeanceTest.fleet(RUN_ID);
        fleets.add(unmarked);

        script.tick(player, 1f);

        assertTrue(script.isDone());
        verify(unmarked, never()).addAssignmentAtStart(any(), any(), anyFloat(), anyString(), any());
    }
}
