package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import exiledlib.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConvoyScriptTest {

    private MockSector mockSector;
    private ConvoySystem convoySystem;
    private CampaignClockAPI clock;
    private CampaignFleetAPI playerFleet;
    private IntelManagerAPI intelManager;
    private float elapsedDays;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        convoySystem = new ConvoySystem(5);
        clock = mock(CampaignClockAPI.class);
        when(mockSector.sector.getClock()).thenReturn(clock);
        when(clock.convertToDays(anyFloat())).thenAnswer(invocation -> invocation.getArgument(0));
        when(clock.getTimestamp()).thenReturn(1000L);
        when(clock.getElapsedDaysSince(anyLong())).thenAnswer(invocation -> elapsedDays);
        when(clock.getSecondsPerDay()).thenReturn(10f);
        intelManager = mock(IntelManagerAPI.class);
        when(mockSector.sector.getIntelManager()).thenReturn(intelManager);
        when(mockSector.sector.getStarSystem(ChartIds.systemId(ConvoySystem.RUN_ID))).thenReturn(convoySystem.system);
        playerFleet = mockSector.playerFleet();
        ChartRuns.store(new ChartRun(ConvoySystem.RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1));
        SupplyConvoy.INSTANCE.generate(convoySystem.site(ChartGenContext.none()));
    }

    @AfterEach
    void tearDown() {
        ChartContentGenerator.resetForTests();
        mockSector.close();
    }

    private ConvoyState convoy() {
        return convoySystem.script().convoys().get(0);
    }

    private List<FleetAssignment> orders(ConvoyRole role) {
        List<FleetAssignment> orders = new ArrayList<>();
        for (FakeChartContentEngine.Assignment given : convoySystem.assignments(convoySystem.fleet(convoy(), role))) {
            orders.add(given.assignment());
        }
        return orders;
    }

    @Test
    void theScriptAddsOneIntelPerConvoyAndEndsWithTheRun() {
        ConvoyScript script = convoySystem.script();

        script.advance(0.01f);
        script.advance(0.01f);

        verify(intelManager, times(1)).addIntel(any(ConvoyIntel.class), anyBoolean());
        assertTrue(script.belongsToRun(ConvoySystem.RUN_ID));
        ChartRuns.delete(ConvoySystem.RUN_ID);
        script.advance(0.01f);
        assertTrue(script.isDone());
    }

    @Test
    void theConvoyWaitsAtTheEdgeUntilDaysAfterThePlayerArrives() {
        ConvoyScript script = convoySystem.script();
        script.advance(1f);
        assertEquals(ConvoyState.Phase.WAITING, convoy().phase());
        assertFalse(script.playerHasArrived());

        when(playerFleet.getContainingLocation()).thenReturn(convoySystem.system);
        script.advance(1f);
        assertTrue(script.playerHasArrived());
        elapsedDays = ConvoyRules.DEPARTURE_DAYS - 1f;
        script.advance(1f);
        assertEquals(ConvoyState.Phase.WAITING, convoy().phase());

        elapsedDays = ConvoyRules.DEPARTURE_DAYS;
        script.advance(1f);
        assertEquals(ConvoyState.Phase.EN_ROUTE, convoy().phase());
    }

    @Test
    void departingFreightersVisitEachWaypointThenUnloadAtTheDestination() {
        ConvoyScript script = convoySystem.script();
        when(playerFleet.getContainingLocation()).thenReturn(convoySystem.system);
        script.advance(1f);
        elapsedDays = ConvoyRules.DEPARTURE_DAYS;
        script.advance(1f);

        List<FakeChartContentEngine.Assignment> given = convoySystem.assignments(convoySystem.fleet(convoy(), ConvoyRole.FREIGHTERS));
        List<String> waypointIds = convoy().waypointIds();
        assertEquals(waypointIds.size() * 2 + 2, given.size());
        for (int i = 0; i < waypointIds.size(); i++) {
            assertEquals(FleetAssignment.GO_TO_LOCATION, given.get(i * 2).assignment());
            assertEquals(waypointIds.get(i), given.get(i * 2).target().getId());
            assertEquals(FleetAssignment.ORBIT_PASSIVE, given.get(i * 2 + 1).assignment());
        }
        SectorEntityToken destination = convoySystem.entities.get(convoy().destinationId());
        assertEquals(new FakeChartContentEngine.Assignment(FleetAssignment.GO_TO_LOCATION, destination), given.get(given.size() - 2));
        assertEquals(FleetAssignment.ORBIT_PASSIVE, given.get(given.size() - 1).assignment());
        assertEquals(List.of(FleetAssignment.FOLLOW), orders(ConvoyRole.ESCORT));
    }

    @Test
    void attackingOneFleetTurnsTheWholeConvoyHostileWithoutTouchingStanding() {
        ConvoyScript script = convoySystem.script();
        CampaignFleetAPI freighters = convoySystem.fleet(convoy(), ConvoyRole.FREIGHTERS);
        assertTrue(ConvoyEncounterDialog.wantsToHandle(convoySystem.fleet(convoy(), ConvoyRole.SCREEN)));

        assertTrue(script.turnHostile(convoy()));
        assertFalse(script.turnHostile(convoy()));

        assertTrue(convoy().isHostile());
        for (ConvoyRole role : ConvoyRole.values()) {
            CampaignFleetAPI fleet = convoySystem.fleet(convoy(), role);
            assertTrue(fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_MAKE_HOSTILE), role.key());
            assertFalse(fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE), role.key());
            assertTrue(fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_NO_REP_IMPACT), role.key());
            assertFalse(ConvoyEncounterDialog.wantsToHandle(fleet), role.key());
        }
        assertTrue(convoySystem.fleet(convoy(), ConvoyRole.ESCORT).getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE));
        assertFalse(freighters.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE));
        assertEquals(new FakeChartContentEngine.Assignment(FleetAssignment.ORBIT_AGGRESSIVE, freighters),
                convoySystem.assignments(convoySystem.fleet(convoy(), ConvoyRole.SCREEN)).get(0));
        verify(mockSector.sector, never()).adjustPlayerReputation(any(), any(String.class));
    }

    @Test
    void anInterdictionPulseThatReachesAConvoyFleetHoldsItAndTurnsTheConvoyHostile() {
        ConvoyScript script = convoySystem.script();
        CampaignFleetAPI escort = convoySystem.fleet(convoy(), ConvoyRole.ESCORT);
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getMaxSensorRange()).thenReturn(3000);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        StatBonus sensorRange = mock(StatBonus.class);
        when(sensorRange.computeEffective(anyFloat())).thenReturn(1000f);
        when(playerFleet.getSensorRangeMod()).thenReturn(sensorRange);
        for (ConvoyRole role : ConvoyRole.values()) {
            when(convoySystem.fleet(convoy(), role).getSensorRangeMod()).thenReturn(sensorRange);
        }
        MemoryAPI playerMemory = FakeChartContentEngine.memory();
        when(playerFleet.getMemoryWithoutUpdate()).thenReturn(playerMemory);
        when(playerFleet.getContainingLocation()).thenReturn(convoySystem.system);
        Vector2f escortAt = escort.getLocation();
        when(playerFleet.getLocation()).thenReturn(new Vector2f(escortAt.x + 5000f, escortAt.y));

        playerMemory.set(MemFlags.JUST_DID_INTERDICTION_PULSE, true);
        script.check(mockSector.sector);
        assertFalse(convoy().isHostile(), "a pulse out of range is no attack");

        elapsedDays = 1f;
        when(playerFleet.getLocation()).thenReturn(new Vector2f(escortAt.x + 100f, escortAt.y));
        script.check(mockSector.sector);
        assertTrue(convoy().isHostile());
        script.advance(0.1f);
        verify(escort).goSlowOneFrame(true);
    }

    @Test
    void theScreenRidesAheadOfTheFreightersTowardsTheirNextStop() {
        ConvoyScript script = convoySystem.script();
        CampaignFleetAPI freighters = convoySystem.fleet(convoy(), ConvoyRole.FREIGHTERS);
        freighters.setLocation(0f, 0f);
        SectorEntityToken firstStop = convoySystem.entities.get(convoy().waypointIds().get(0));

        script.advance(1f);

        SectorEntityToken lead = convoy().screenLead();
        assertEquals(ConvoyRules.SCREEN_LEAD, lead.getLocation().length(), 1f);
        float alignment = lead.getLocation().x * firstStop.getLocation().x + lead.getLocation().y * firstStop.getLocation().y;
        assertTrue(alignment > 0f, "the lead sits on the way to the first stop");
    }
}
