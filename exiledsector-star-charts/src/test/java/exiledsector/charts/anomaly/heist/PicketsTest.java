package exiledsector.charts.anomaly.heist;

import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.loot.ChartFleetLoot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PicketsTest {

    private static final float EPSILON = 1e-4f;

    private record Fleet(CampaignFleetAPI fleet, List<FleetMemberAPI> members, List<DropData> random, List<DropData> value) {
    }

    private static Fleet fleet(float startDeploymentPoints, float paidDeploymentPoints, int ships) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        MemoryAPI memory = FakeChartContentEngine.memory();
        memory.set(ChartIds.START_DP_MEMORY_KEY, startDeploymentPoints);
        if (paidDeploymentPoints > 0f) {
            memory.set(ChartIds.LOOT_DP_PAID_MEMORY_KEY, paidDeploymentPoints);
        }
        when(fleet.getMemoryWithoutUpdate()).thenReturn(memory);
        List<FleetMemberAPI> members = new ArrayList<>();
        for (int ship = 0; ship < ships; ship++) {
            members.add(mock(FleetMemberAPI.class));
        }
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getMembersListCopy()).thenAnswer(invocation -> new ArrayList<>(members));
        doAnswer(invocation -> members.remove((FleetMemberAPI) invocation.getArgument(0))).when(fleetData).removeFleetMember(any());
        doAnswer(invocation -> members.add(invocation.getArgument(0))).when(fleetData).addFleetMember(any(FleetMemberAPI.class));
        when(fleet.getFleetData()).thenReturn(fleetData);
        List<DropData> random = new ArrayList<>(List.of(new DropData()));
        List<DropData> value = new ArrayList<>(List.of(new DropData()));
        when(fleet.getDropRandom()).thenReturn(random);
        when(fleet.getDropValue()).thenReturn(value);
        doAnswer(invocation -> random.add(invocation.getArgument(0))).when(fleet).addDropRandom(any(DropData.class));
        doAnswer(invocation -> value.add(invocation.getArgument(0))).when(fleet).addDropValue(any(DropData.class));
        return new Fleet(fleet, members, random, value);
    }

    @Test
    void aMergedPicketBringsItsShipsItsOwnDropsAndItsShareOfTheItemRolls() {
        Fleet guard = fleet(40f, 0f, 2);
        Fleet picket = fleet(30f, 10f, 3);
        List<FleetMemberAPI> picketShips = new ArrayList<>(picket.members());

        Pickets.merge(picket.fleet(), guard.fleet());

        assertEquals(5, guard.members().size());
        assertTrue(picket.members().isEmpty());
        picketShips.forEach(ship -> verify(ship).setFlagship(false));
        assertEquals(2, guard.random().size());
        assertEquals(2, guard.value().size());
        MemoryAPI memory = guard.fleet().getMemoryWithoutUpdate();
        assertEquals(70f, memory.getFloat(ChartIds.START_DP_MEMORY_KEY), EPSILON);
        assertEquals(10f, memory.getFloat(ChartIds.LOOT_DP_PAID_MEMORY_KEY), EPSILON);
        assertEquals(2, ChartFleetLoot.lootFleets(guard.fleet()));
        verify(picket.fleet()).despawn(FleetDespawnReason.OTHER, null);

        Pickets.merge(fleet(20f, 0f, 1).fleet(), guard.fleet());
        assertEquals(3, ChartFleetLoot.lootFleets(guard.fleet()));
        assertEquals(1, ChartFleetLoot.lootFleets(picket.fleet()));
    }
}
