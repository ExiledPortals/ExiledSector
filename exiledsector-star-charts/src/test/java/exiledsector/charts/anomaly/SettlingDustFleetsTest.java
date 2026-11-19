package exiledsector.charts.anomaly;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.run.ChartRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SettlingDustFleetsTest {

    private static final float TOLERANCE = 0.01f;

    private MockSector mockSector;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        FactoryAPI factory = mock(FactoryAPI.class);
        mockSector.global.when(Global::getFactory).thenReturn(factory);
        when(factory.createFleetMember(eq(FleetMemberType.SHIP), any(ShipVariantAPI.class)))
                .thenAnswer(invocation -> member(5, 7f, false));
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private static FleetMemberAPI member(int fleetPoints, float deploymentPoints, boolean flagship) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.clone()).thenReturn(mock(ShipVariantAPI.class));
        when(member.getVariant()).thenReturn(variant);
        when(member.getFleetPointCost()).thenReturn(fleetPoints);
        when(member.getDeploymentPointsCost()).thenReturn(deploymentPoints);
        when(member.isFlagship()).thenReturn(flagship);
        when(member.getRepairTracker()).thenReturn(mock(RepairTrackerAPI.class));
        return member;
    }

    private static CampaignFleetAPI chartFleet(String runId, List<FleetMemberAPI> members, float startFleetPoints) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        MemoryAPI memory = FakeChartContentEngine.memory();
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleet.getId()).thenReturn("guard");
        when(fleet.getMemoryWithoutUpdate()).thenReturn(memory);
        when(fleet.getFleetData()).thenReturn(fleetData);
        when(fleetData.getMembersListCopy()).thenAnswer(invocation -> new ArrayList<>(members));
        when(fleetData.pickShipName(any(FleetMemberAPI.class), any(Random.class))).thenReturn("Copy");
        doAnswer(invocation -> members.add(invocation.getArgument(0))).when(fleetData).addFleetMember(any(FleetMemberAPI.class));
        memory.set(ChartIds.fleetRunKey(runId), true);
        memory.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        memory.set(ChartIds.START_FP_MEMORY_KEY, startFleetPoints);
        memory.set(ChartIds.START_DP_MEMORY_KEY, 30f);
        return fleet;
    }

    @Test
    void theLargestShipThatFitsIsCopiedAndTooBigShipsAreNot() {
        FleetMemberAPI flagship = member(20, 25f, true);
        FleetMemberAPI destroyer = member(6, 8f, false);
        FleetMemberAPI frigate = member(3, 4f, false);
        List<FleetMemberAPI> members = List.of(flagship, destroyer, frigate);

        assertSame(destroyer, SettlingDustFleets.pickSource(members, 7f));
        assertSame(frigate, SettlingDustFleets.pickSource(members, 2f));
        assertNull(SettlingDustFleets.pickSource(members, 1f));
        assertNull(SettlingDustFleets.pickSource(List.of(flagship), 50f));
    }

    @Test
    void eachStackGrowsTheFleetTowardsItsShareOfTheStartingSize() {
        List<FleetMemberAPI> members = new ArrayList<>(List.of(member(5, 7f, false)));
        CampaignFleetAPI fleet = chartFleet(LiveAnomalyRuns.RUN_ID, members, 20f);
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();

        SettlingDustFleets.growTo(fleet, 30f);
        assertEquals(2, members.size());
        assertEquals(25f, memory.getFloat(ChartIds.START_FP_MEMORY_KEY), TOLERANCE);
        assertEquals(37f, memory.getFloat(ChartIds.START_DP_MEMORY_KEY), TOLERANCE);

        SettlingDustFleets.growTo(fleet, 60f);
        assertEquals(3, members.size());
        assertEquals(10f, memory.getFloat(SettlingDustFleets.ADDED_FLEET_POINTS_KEY), TOLERANCE);
        assertEquals(30f, memory.getFloat(ChartIds.START_FP_MEMORY_KEY), TOLERANCE);
    }

    @Test
    void onlyHostileChartFleetsOfTheRunGrowAndNeverTheBoss() {
        ChartRun run = LiveAnomalyRuns.run();
        CampaignFleetAPI guard = chartFleet(run.runId(), new ArrayList<>(), 20f);
        CampaignFleetAPI otherRun = chartFleet("run2", new ArrayList<>(), 20f);
        CampaignFleetAPI looted = chartFleet(run.runId(), new ArrayList<>(), 20f);
        looted.getMemoryWithoutUpdate().set(ChartIds.LOOT_SPENT_MEMORY_KEY, true);
        CampaignFleetAPI friendly = chartFleet(run.runId(), new ArrayList<>(), 20f);
        friendly.getMemoryWithoutUpdate().unset(MemFlags.MEMORY_KEY_MAKE_HOSTILE);

        assertTrue(SettlingDustFleets.grows(guard, run));
        assertFalse(SettlingDustFleets.grows(otherRun, run));
        assertFalse(SettlingDustFleets.grows(looted, run));
        assertFalse(SettlingDustFleets.grows(friendly, run));
        assertFalse(SettlingDustFleets.grows(null, run));
    }
}
