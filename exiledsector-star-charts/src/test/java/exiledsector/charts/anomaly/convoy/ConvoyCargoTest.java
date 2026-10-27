package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.CargoPodsEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.loot.ChartLoot;
import exiledsector.charts.loot.ChartVanillaLoot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConvoyCargoTest {

    private static JSONObject row(String id, String dropType, String scope, String group, String mode, String amount, String minTier) throws Exception {
        return new JSONObject().put("id", id).put("drop_type", dropType).put("scope", scope).put("group", group).put("mode", mode)
                .put("amount", amount).put("min_tier", minTier);
    }

    @AfterEach
    void tearDown() {
        ChartContentGenerator.resetForTests();
        ChartLoot.clear();
    }

    @Test
    void eachLostFreighterShareIsDroppedOnceAndTheRestStaysAboardForTheCache() {
        ConvoyState convoy = new ConvoyState(0, List.of(), "dest", 0f, 0f, 0f, 0f, 1L);
        convoy.loadCargo(100f, 3f, 1f);

        assertEquals(0.4f, convoy.recordFreighterPoints(60f), 0.001f);
        assertEquals(0f, convoy.recordFreighterPoints(70f), 0.001f, "repairs never refund a dropped share");
        assertEquals(0.6f, convoy.carriedShare(), 0.001f);
        assertEquals(ConvoyState.Phase.WAITING, convoy.phase());

        convoy.setPhase(ConvoyState.Phase.EN_ROUTE);
        assertEquals(0.6f, convoy.recordFreighterPoints(0f), 0.001f);
        assertEquals(ConvoyState.Phase.LOST, convoy.phase());
        assertEquals(false, convoy.deliver());
    }

    @Test
    void onlyTheFreightersAndTankersCountTowardsTheCargo() {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        FleetDataAPI data = mock(FleetDataAPI.class);
        FleetMemberAPI freighter = mock(FleetMemberAPI.class);
        FleetMemberAPI guard = mock(FleetMemberAPI.class);
        when(freighter.isCivilian()).thenReturn(true);
        when(freighter.getFleetPointCost()).thenReturn(10);
        when(guard.getFleetPointCost()).thenReturn(5);
        when(data.getMembersListCopy()).thenReturn(List.of(freighter, guard));
        when(fleet.getFleetData()).thenReturn(data);
        when(fleet.getFleetPoints()).thenReturn(15);

        assertEquals(10f, ConvoyCargo.freighterPoints(fleet, true), 0.001f);
        assertEquals(15f, ConvoyCargo.freighterPoints(fleet, false), 0.001f);
        when(fleet.isEmpty()).thenReturn(true);
        assertEquals(0f, ConvoyCargo.freighterPoints(fleet, true), 0.001f);
    }

    @Test
    void losingShipsAfterDeliveryDropsNothing() {
        ConvoyState convoy = new ConvoyState(0, List.of(), "dest", 0f, 0f, 0f, 0f, 1L);
        convoy.loadCargo(100f, 3f, 1f);
        convoy.setPhase(ConvoyState.Phase.EN_ROUTE);
        convoy.recordFreighterPoints(75f);

        convoy.deliver();

        assertEquals(0.75f, convoy.deliveredShare(), 0.001f);
        assertEquals(0f, convoy.recordFreighterPoints(10f), 0.001f);
        assertEquals(ConvoyState.Phase.DELIVERED, convoy.phase());
    }

    @Test
    void attackedFreightersSkipTheirRemainingStopsAndRunForTheDestination() {
        ConvoySystem convoySystem = new ConvoySystem(5);
        SupplyConvoy.INSTANCE.generate(convoySystem.site(ChartGenContext.none()));
        ConvoyState convoy = convoySystem.script().convoys().get(0);
        convoySystem.script().depart(convoy);

        convoySystem.script().turnHostile(convoy);

        SectorEntityToken destination = convoySystem.entities.get(convoy.destinationId());
        List<FakeChartContentEngine.Assignment> orders = convoySystem.assignments(convoySystem.fleet(convoy, ConvoyRole.FREIGHTERS));
        assertEquals(List.of(new FakeChartContentEngine.Assignment(FleetAssignment.GO_TO_LOCATION, destination),
                new FakeChartContentEngine.Assignment(FleetAssignment.ORBIT_PASSIVE, destination)), orders);
    }

    @Test
    void cargoPodsLandWhereTheFreightersFellAndLastForTheRun() {
        LocationAPI system = mock(LocationAPI.class);
        CustomCampaignEntityAPI pods = new FakeChartContentEngine().tagged(mock(CustomCampaignEntityAPI.class));
        CargoAPI podCargo = mock(CargoAPI.class);
        CargoPodsEntityPlugin plugin = mock(CargoPodsEntityPlugin.class);
        when(pods.getLocation()).thenReturn(new Vector2f());
        when(pods.getVelocity()).thenReturn(new Vector2f());
        when(pods.getCargo()).thenReturn(podCargo);
        when(pods.getCustomPlugin()).thenReturn(plugin);
        when(system.addCustomEntity(any(), any(), eq(Entities.CARGO_PODS), any())).thenReturn(pods);
        CargoAPI dropped = mock(CargoAPI.class);

        assertSame(pods, ConvoyCargo.dropPods(system, "run1", dropped, new Vector2f(300f, -200f)));

        assertEquals(new Vector2f(300f, -200f), pods.getLocation());
        verify(podCargo).addAll(dropped);
        verify(plugin).setNeverExpire(true);
        verify(pods).addTag(ChartIds.runTag("run1"));
        CargoAPI empty = mock(CargoAPI.class);
        when(empty.isEmpty()).thenReturn(true);
        assertNull(ConvoyCargo.dropPods(system, "run1", empty, new Vector2f()));
    }

    @Test
    void theConvoyLootRowsScaleWithTheCargoSteps() throws Exception {
        ChartLoot.register(new JSONArray()
                .put(row("supply", "resource", "", "supply", "", "", ""))
                .put(row("weapons1", "weapon", "", "weapons1", "", "", ""))
                .put(row("convoy_supply", "resource", "convoy", "supply", "value", "2000", "1"))
                .put(row("convoy_weapons", "weapon", "convoy", "weapons1", "random", "3", "1"))
                .put(row("fleet_weapons", "weapon", "fleet", "weapons1", "random", "1", "1")), group -> true);

        ChartVanillaLoot.SiteDrops drops = ChartVanillaLoot.drops(ConvoyRules.LOOT_SCOPE, 4, 2f, ChartGenContext.none());

        assertEquals(1, drops.value().size());
        DropData supply = drops.value().get(0);
        assertEquals(2000, supply.value);
        assertEquals(2f, supply.valueMult, 0.001f);
        assertEquals(1, drops.random().size());
        assertEquals(6f, drops.random().get(0).valueMult, 0.001f);
        assertEquals(ChartVanillaLoot.SiteDrops.NONE, ChartVanillaLoot.drops(ConvoyRules.LOOT_SCOPE, 4, 0f, ChartGenContext.none()));
        assertEquals(List.of("chart.convoy.cargo.resource", "chart.convoy.cargo.weapon"), ConvoyCargo.manifestKeys(4).subList(0, 2));
    }
}
