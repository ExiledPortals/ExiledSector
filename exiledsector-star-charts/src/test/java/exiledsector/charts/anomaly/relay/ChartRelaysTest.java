package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledlib.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.objective.ChartObjective;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartRelaysTest {

    private MockSector mockSector;
    private CampaignClockAPI clock;
    private float elapsedDays;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getBaseTravelSpeed()).thenReturn(10f);
        when(settings.getSpeedPerBurnLevel()).thenReturn(20f);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        clock = mock(CampaignClockAPI.class);
        when(clock.getTimestamp()).thenReturn(100L);
        when(clock.getElapsedDaysSince(anyLong())).thenAnswer(invocation -> elapsedDays);
        when(mockSector.sector.getClock()).thenReturn(clock);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private static ChartRun run() {
        return new ChartRun("run1", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
    }

    static ChartRun liveRelayRun() {
        ChartRun run = run();
        run.recordContent(new ChartContent(4, "pirates", ChartObjective.BOSS, List.of("boss"), "boss", 3, 2, List.of(RelayNetwork.ID)));
        run.recordArrival();
        return run;
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

    @Test
    void stationKeepingNeedsTheFleetCloseAndSlowForTheWholeHold() {
        CampaignFleetAPI player = RelayFixtures.fleet(true, new Vector2f(100f, 0f));
        SectorEntityToken buoy = RelayFixtures.relay(RelayKind.NAV_BUOY, new Vector2f());

        assertFalse(ChartRelays.synced(clock, player, buoy, RelayKind.NAV_BUOY));
        elapsedDays = RelayRules.STATION_KEEPING_DAYS;
        assertTrue(ChartRelays.synced(clock, player, buoy, RelayKind.NAV_BUOY));

        player.getVelocity().set(200f, 0f);
        assertFalse(ChartRelays.synced(clock, player, buoy, RelayKind.NAV_BUOY));
        player.getVelocity().set(0f, 0f);
        assertFalse(ChartRelays.synced(clock, player, buoy, RelayKind.NAV_BUOY));

        CampaignFleetAPI far = RelayFixtures.fleet(true, new Vector2f(RelayRules.STATION_KEEPING_RANGE + 50f, 0f));
        assertFalse(ChartRelays.synced(clock, far, buoy, RelayKind.NAV_BUOY));
    }

    @Test
    void theCommRelayNeedsTheTransponderOnInRangeAndTheSensorArrayASensorBurst() {
        CampaignFleetAPI player = RelayFixtures.fleet(true, new Vector2f(RelayRules.WAKE_RANGE - 100f, 0f));
        SectorEntityToken relay = RelayFixtures.relay(RelayKind.COMM_RELAY, new Vector2f());
        SectorEntityToken array = RelayFixtures.relay(RelayKind.SENSOR_ARRAY, new Vector2f());
        AbilityPlugin burst = mock(AbilityPlugin.class);
        when(player.getAbility(Abilities.SENSOR_BURST)).thenReturn(burst);
        elapsedDays = RelayRules.HANDSHAKE_DAYS;

        assertFalse(ChartRelays.synced(clock, player, relay, RelayKind.COMM_RELAY));
        when(player.isTransponderOn()).thenReturn(true);
        assertFalse(ChartRelays.synced(clock, player, relay, RelayKind.COMM_RELAY));
        assertTrue(ChartRelays.synced(clock, player, relay, RelayKind.COMM_RELAY));

        assertFalse(ChartRelays.synced(clock, player, array, RelayKind.SENSOR_ARRAY));
        when(burst.isActiveOrInProgress()).thenReturn(true);
        assertTrue(ChartRelays.synced(clock, player, array, RelayKind.SENSOR_ARRAY));
        CampaignFleetAPI far = RelayFixtures.fleet(true, new Vector2f(RelayRules.WAKE_RANGE + 100f, 0f));
        when(far.getAbility(Abilities.SENSOR_BURST)).thenReturn(burst);
        assertFalse(ChartRelays.synced(clock, far, array, RelayKind.SENSOR_ARRAY));
    }

    @Test
    void restoringPowerTakesTheCostOnceAndWakesASyncedRelay() {
        ChartRun run = run();
        StarSystemAPI system = mock(StarSystemAPI.class);
        SectorEntityToken array = RelayFixtures.relay(RelayKind.SENSOR_ARRAY, new Vector2f());
        CargoAPI cargo = mock(CargoAPI.class);
        when(cargo.getSupplies()).thenReturn(100f);
        when(cargo.getCommodityQuantity(Commodities.HEAVY_MACHINERY)).thenReturn(4f);
        RelayRules.Cost cost = new RelayRules.Cost(30, 10);

        assertFalse(ChartRelays.canAfford(cargo, cost));
        assertFalse(ChartRelays.restorePower(run, cargo, system, array, RelayKind.SENSOR_ARRAY, cost));
        when(cargo.getCommodityQuantity(Commodities.HEAVY_MACHINERY)).thenReturn(10f);
        RelayStates.add(run, RelayKind.SENSOR_ARRAY, RelayStates.SYNCED);

        assertTrue(ChartRelays.restorePower(run, cargo, system, array, RelayKind.SENSOR_ARRAY, cost));

        verify(cargo).removeSupplies(30f);
        verify(cargo).removeCommodity(Commodities.HEAVY_MACHINERY, 10f);
        assertTrue(RelayStates.isWoken(run, RelayKind.SENSOR_ARRAY));
        verify(array).setFaction(Factions.PLAYER);
        assertFalse(ChartRelays.restorePower(run, cargo, system, array, RelayKind.SENSOR_ARRAY, cost));
    }

    @Test
    void theRunCheckSyncsRelaysAndWakesThePaidOnes() {
        ChartRun run = liveRelayRun();
        ChartRuns.store(run);
        StarSystemAPI system = mock(StarSystemAPI.class);
        when(mockSector.sector.getStarSystem(run.systemId())).thenReturn(system);
        CampaignFleetAPI player = RelayFixtures.fleet(true, new Vector2f(500f, 0f));
        when(player.getContainingLocation()).thenReturn(system);
        when(mockSector.sector.getPlayerFleet()).thenReturn(player);
        AbilityPlugin burst = mock(AbilityPlugin.class);
        when(burst.isActiveOrInProgress()).thenReturn(true);
        when(player.getAbility(Abilities.SENSOR_BURST)).thenReturn(burst);
        SectorEntityToken array = RelayFixtures.relay(RelayKind.SENSOR_ARRAY, new Vector2f());
        SectorEntityToken buoy = RelayFixtures.relay(RelayKind.NAV_BUOY, new Vector2f(9000f, 0f));
        when(system.getEntitiesWithTag(RelayNetwork.RELAY_TAG)).thenReturn(List.of(array, buoy));
        RelayStates.add(run, RelayKind.SENSOR_ARRAY, RelayStates.PAID);

        assertTrue(ChartRelays.check(mockSector.sector));

        assertTrue(RelayStates.isWoken(run, RelayKind.SENSOR_ARRAY));
        assertFalse(RelayStates.has(run, RelayKind.NAV_BUOY, RelayStates.SYNCED));
        assertFalse(ChartRelays.check(mockSector.sector));
        when(player.getContainingLocation()).thenReturn(mock(StarSystemAPI.class));
        assertFalse(ChartRelays.check(mockSector.sector));
    }

    @Test
    void theCostRisesWithTheTierAndFollowsTheCostLine() {
        ChartRun run = liveRelayRun();
        RelayRules.Cost plain = ChartRelays.costFor(run);

        assertEquals(new RelayRules.Cost(RelayRules.SUPPLIES_BASE + 4 * RelayRules.SUPPLIES_PER_TIER,
                RelayRules.MACHINERY_BASE + 4 * RelayRules.MACHINERY_PER_TIER), plain);
        assertEquals(new RelayRules.Cost(Math.round(plain.supplies() * 0.5f), Math.round(plain.machinery() * 0.5f)),
                RelayRules.costFor(4, ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_RELAY_COST_PERCENT, -50f)))
                        .anomalyMult(RelayNetwork.COST_KEY)));
        assertEquals("50% reduced supply and heavy machinery cost to wake relays", ChartEffect.CHART_RELAY_COST_PERCENT.description(-50f).plain());
        assertEquals("20% increased supply and heavy machinery cost to wake relays", ChartEffect.CHART_RELAY_COST_PERCENT.description(20f).plain());
    }

    @Test
    void theHoldClockResetsWhenTheConditionBreaks() {
        MemoryAPI memory = RelayFixtures.memory();

        assertFalse(ChartRelays.held(clock, memory, true, 1f));
        assertTrue(memory.contains(ChartRelays.SYNC_SINCE_MEMORY_KEY));
        assertFalse(ChartRelays.held(clock, memory, false, 1f));
        assertFalse(memory.contains(ChartRelays.SYNC_SINCE_MEMORY_KEY));
        assertTrue(ChartRelays.held(clock, memory, true, 0f));
    }
}
