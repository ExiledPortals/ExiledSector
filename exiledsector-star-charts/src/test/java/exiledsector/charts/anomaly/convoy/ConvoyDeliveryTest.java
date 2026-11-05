package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.loot.ChartFleetLoot;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class ConvoyDeliveryTest {

    private MockSector mockSector;
    private MockedStatic<ConvoyCargo> cargoRolls;
    private final CargoAPI rolledCache = mock(CargoAPI.class);
    private ConvoySystem convoySystem;
    private ConvoyScript script;
    private ConvoyState convoy;
    private SectorEntityToken destination;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        ChartRuns.store(new ChartRun(ConvoySystem.RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1));
        convoySystem = new ConvoySystem(5);
        convoySystem.engine.fleetPoints = 100;
        SupplyConvoy.INSTANCE.generate(convoySystem.site(ChartGenContext.none()));
        script = convoySystem.script();
        convoy = script.convoys().get(0);
        destination = convoySystem.entities.get(convoy.destinationId());
        script.depart(convoy);
        cargoRolls = mockStatic(ConvoyCargo.class);
        cargoRolls.when(() -> ConvoyCargo.deliveredCache(any(), any(), any(), any(), any())).thenReturn(rolledCache);
    }

    @AfterEach
    void tearDown() {
        cargoRolls.close();
        ChartContentGenerator.resetForTests();
        mockSector.close();
    }

    @Test
    void anUnloadedConvoyLeavesACacheOnTheDestinationSpendsItsLootAndHeadsForTheEdge() {
        assertFalse(ConvoyCacheDialog.wantsToHandle(destination));
        CampaignFleetAPI fleet = convoySystem.fleet(convoy);

        script.arrived(convoy);
        assertEquals(ConvoyState.Phase.UNLOADING, convoy.phase());
        script.unloaded(convoy);

        assertEquals(ConvoyState.Phase.DELIVERED, convoy.phase());
        assertEquals(1f, convoy.deliveredShare(), 0.001f);
        cargoRolls.verify(() -> ConvoyCargo.deliveredCache(any(), any(), same(convoy), same(fleet), any()));
        assertEquals(List.of(convoy), script.cachesAt(destination));
        assertTrue(ConvoyCacheDialog.wantsToHandle(destination));
        assertTrue(ChartFleetLoot.lootSpent(fleet), "the convoy dropped its Exiled Sector loot with the cache");
        List<FakeChartContentEngine.Assignment> orders = convoySystem.assignments(fleet);
        assertEquals(new FakeChartContentEngine.Assignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, convoy.exitToken()), orders.get(orders.size() - 1));
        assertFalse(ConvoyCacheDialog.wantsToHandle(convoySystem.entities.get(convoy.waypointIds().get(0))));
    }

    @Test
    void aConvoyThatLostFreightersDeliversOnlyTheirShareOfTheCargo() {
        script.turnHostile(convoy);
        convoy.recordFreighterPoints(40f);

        script.arrived(convoy);
        script.unloaded(convoy);

        assertEquals(0.4f, convoy.deliveredShare(), 0.001f);
        assertTrue(convoy.hasCache());
    }

    @Test
    void whenOnlyTheWarshipsSurviveTheConvoyLeavesThroughTheExitWithNothingToDeliver() {
        script.turnHostile(convoy);

        script.recordLosses(convoy, 0f, null);

        assertEquals(ConvoyState.Phase.LOST, convoy.phase());
        List<FakeChartContentEngine.Assignment> orders = convoySystem.assignments(convoySystem.fleet(convoy));
        assertEquals(new FakeChartContentEngine.Assignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, convoy.exitToken()), orders.get(orders.size() - 1));
        script.arrived(convoy);
        script.unloaded(convoy);
        assertEquals(ConvoyState.Phase.LOST, convoy.phase());
        assertFalse(convoy.hasCache());
    }

    @Test
    void aTakenCacheIsGoneAndADestroyedConvoyDeliversNothing() {
        script.unloaded(convoy);
        assertSame(rolledCache, convoy.takeCache());
        assertNull(convoy.takeCache());
        assertFalse(ConvoyCacheDialog.wantsToHandle(destination));

        ConvoySystem other = new ConvoySystem(4);
        cargoRolls.close();
        SupplyConvoy.INSTANCE.generate(other.site(ChartGenContext.none()));
        cargoRolls = mockStatic(ConvoyCargo.class);
        ConvoyState lost = other.script().convoys().get(0);
        lost.loadCargo(100f, 1f, 1f);
        other.script().depart(lost);
        other.script().reportFleetDespawnedToListener(other.fleet(lost), FleetDespawnReason.DESTROYED_BY_BATTLE, null);
        other.script().unloaded(lost);

        assertEquals(ConvoyState.Phase.LOST, lost.phase());
        assertFalse(lost.hasCache());
        assertTrue(other.script().cachesAt(other.entities.get(lost.destinationId())).isEmpty());
    }
}
