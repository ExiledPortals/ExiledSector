package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRun;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartRelaysTest {

    private static ChartRun run() {
        return new ChartRun("run1", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
    }

    @Test
    void aWokenNavBuoyOrSensorArrayIsHandedToThePlayerLikeAVanillaObjective() {
        ChartRun run = run();
        StarSystemAPI system = mock(StarSystemAPI.class);
        SectorEntityToken buoy = RelayFixtures.relay(RelayKind.NAV_BUOY, new Vector2f());
        buoy.getMemoryWithoutUpdate().set(MemFlags.OBJECTIVE_NON_FUNCTIONAL, true);

        assertTrue(ChartRelays.wake(run, system, buoy, RelayKind.NAV_BUOY));

        verify(buoy).setFaction(Factions.PLAYER);
        assertFalse(buoy.getMemoryWithoutUpdate().contains(MemFlags.OBJECTIVE_NON_FUNCTIONAL));
        assertTrue(RelayStates.isWoken(run, RelayKind.NAV_BUOY));
        assertFalse(RelayStates.isWoken(run, RelayKind.SENSOR_ARRAY));
        assertFalse(ChartRelays.wake(run, system, buoy, RelayKind.NAV_BUOY));
        verify(system, never()).getFleets();
    }

    @Test
    void aWokenCommRelayRevealsEveryOtherFleetToThePlayerOnly() {
        ChartRun run = run();
        StarSystemAPI system = mock(StarSystemAPI.class);
        CampaignFleetAPI player = RelayFixtures.fleet(true, new Vector2f());
        CampaignFleetAPI enemy = RelayFixtures.fleet(false, new Vector2f(9000f, 0f));
        when(system.getFleets()).thenReturn(List.of(player, enemy));
        SectorEntityToken relay = RelayFixtures.relay(RelayKind.COMM_RELAY, new Vector2f());
        relay.getMemoryWithoutUpdate().set(MemFlags.OBJECTIVE_NON_FUNCTIONAL, true);

        assertTrue(ChartRelays.wake(run, system, relay, RelayKind.COMM_RELAY));

        assertEquals(RelayRules.REVEAL_RANGE_MULT, enemy.getStats().getDynamic().getStat(Stats.DETECTED_BY_PLAYER_RANGE_MULT).getModifiedValue(), 0.001f);
        verify(enemy).setExtendedDetectedAtRange(RelayRules.REVEAL_EXTENDED_RANGE);
        assertEquals(1f, player.getStats().getDynamic().getStat(Stats.DETECTED_BY_PLAYER_RANGE_MULT).getModifiedValue(), 0.001f);
        verify(player, never()).setExtendedDetectedAtRange(RelayRules.REVEAL_EXTENDED_RANGE);
        assertTrue(relay.getMemoryWithoutUpdate().getBoolean(MemFlags.OBJECTIVE_NON_FUNCTIONAL));
    }

    @Test
    void relayFlagsLiveOnTheRunRecord() {
        ChartRun run = run();
        assertEquals(0, RelayStates.flags(run, RelayKind.COMM_RELAY));

        RelayStates.add(run, RelayKind.COMM_RELAY, RelayStates.PAID);
        assertFalse(RelayStates.readyToWake(run, RelayKind.COMM_RELAY));
        RelayStates.add(run, RelayKind.COMM_RELAY, RelayStates.SYNCED);
        assertTrue(RelayStates.readyToWake(run, RelayKind.COMM_RELAY));
        assertEquals(RelayStates.PAID | RelayStates.SYNCED, RelayStates.flags(run, RelayKind.COMM_RELAY));
        assertEquals(0, RelayStates.flags(run, RelayKind.NAV_BUOY));
        assertFalse(RelayStates.allWoken(run));
    }
}
