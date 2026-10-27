package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.generation.ChartGenContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvoyDeliveryTest {

    private ConvoySystem convoySystem;
    private ConvoyScript script;
    private ConvoyState convoy;
    private SectorEntityToken destination;

    @BeforeEach
    void setUp() {
        convoySystem = new ConvoySystem(5);
        convoySystem.engine.fleetPoints = 100;
        SupplyConvoy.INSTANCE.generate(convoySystem.site(ChartGenContext.none()));
        script = convoySystem.script();
        convoy = script.convoys().get(0);
        destination = convoySystem.entities.get(convoy.destinationId());
        script.depart(convoy);
    }

    @AfterEach
    void tearDown() {
        ChartContentGenerator.resetForTests();
    }

    @Test
    void freightersThatUnloadLeaveACacheOnTheDestinationAndHeadForTheEdge() {
        assertFalse(ConvoyCacheDialog.wantsToHandle(destination));

        script.arrived(convoy);
        assertEquals(ConvoyState.Phase.UNLOADING, convoy.phase());
        script.unloaded(convoy);

        assertEquals(ConvoyState.Phase.DELIVERED, convoy.phase());
        assertEquals(1f, convoy.deliveredShare(), 0.001f);
        assertEquals(List.of(convoy), script.cachesAt(destination));
        assertTrue(ConvoyCacheDialog.wantsToHandle(destination));
        List<FakeChartContentEngine.Assignment> orders = convoySystem.assignments(convoySystem.fleet(convoy, ConvoyRole.FREIGHTERS));
        assertEquals(new FakeChartContentEngine.Assignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, convoy.exitToken()), orders.get(orders.size() - 1));
        assertFalse(ConvoyCacheDialog.wantsToHandle(convoySystem.entities.get(convoy.waypointIds().get(0))));
    }

    @Test
    void survivingFreightersDeliverOnlyTheirShare() {
        script.turnHostile(convoy);
        convoySystem.engine.fleetPoints = 40;
        convoy.recordFreighterPoints(40f);

        script.arrived(convoy);
        script.unloaded(convoy);

        assertEquals(0.4f, convoy.deliveredShare(), 0.001f);
        assertTrue(convoy.hasCache());
    }

    @Test
    void aTakenCacheIsGoneAndDestroyedFreightersDeliverNothing() {
        script.unloaded(convoy);
        assertTrue(convoy.takeCache());
        assertFalse(convoy.takeCache());
        assertFalse(ConvoyCacheDialog.wantsToHandle(destination));

        ConvoySystem other = new ConvoySystem(4);
        SupplyConvoy.INSTANCE.generate(other.site(ChartGenContext.none()));
        ConvoyState lost = other.script().convoys().get(0);
        lost.loadCargo(100f, 1f, 1f);
        other.script().depart(lost);
        CampaignFleetAPI freighters = other.fleet(lost, ConvoyRole.FREIGHTERS);
        other.script().reportFleetDespawnedToListener(freighters, FleetDespawnReason.DESTROYED_BY_BATTLE, null);
        other.script().unloaded(lost);

        assertEquals(ConvoyState.Phase.LOST, lost.phase());
        assertFalse(lost.hasCache());
        assertTrue(other.script().cachesAt(other.entities.get(lost.destinationId())).isEmpty());
    }
}
