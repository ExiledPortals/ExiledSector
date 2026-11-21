package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import exiledsector.MockSector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutoforgeEscortsListenerTest {

    private MockSector mockSector;
    private CombatEngineAPI engine;
    private final List<ShipAPI> forged = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        engine = mock(CombatEngineAPI.class);
        when(engine.isEntityInPlay(any())).thenReturn(true);
        mockSector.global.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private static ShipAPI mothership(float drones) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic().getValue(AutoforgeEscortsListener.DRONES_KEY, 0f)).thenReturn(drones);
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }

    private ShipAPI forgeDrone(ShipAPI ownerShip) {
        ShipAPI drone = mock(ShipAPI.class);
        when(drone.isAlive()).thenReturn(true);
        forged.add(drone);
        return drone;
    }

    private static void advanceSeconds(AutoforgeEscortsListener listener, float seconds) {
        for (float elapsed = 0f; elapsed < seconds - 0.001f; elapsed += 0.5f) {
            listener.advance(0.5f);
        }
    }

    @Test
    void theDroneCountIsRoundedAndCapped() {
        assertEquals(3, AutoforgeEscortsListener.droneCount(3.4f));
        assertEquals(0, AutoforgeEscortsListener.droneCount(-1f));
        assertEquals(AutoforgeEscortsListener.MAX_DRONES, AutoforgeEscortsListener.droneCount(50f));
    }

    @Test
    void theDronesLaunchOnceTheDeploymentBurnEnds() {
        ShipAPI mothership = mothership(3f);
        ShipSystemAPI travelDrive = mock(ShipSystemAPI.class);
        when(travelDrive.isActive()).thenReturn(true);
        when(mothership.getTravelDrive()).thenReturn(travelDrive);
        AutoforgeEscortsListener listener = new AutoforgeEscortsListener(mothership, this::forgeDrone);

        advanceSeconds(listener, 2f);
        assertEquals(0, forged.size());

        when(travelDrive.isActive()).thenReturn(false);
        advanceSeconds(listener, 1f);

        assertEquals(3, forged.size());
        assertEquals(3, listener.livingDrones());
    }

    @Test
    void aLostDroneIsRebuiltAfterTheRebuildTime() {
        ShipAPI mothership = mothership(2f);
        AutoforgeEscortsListener listener = new AutoforgeEscortsListener(mothership, this::forgeDrone);
        advanceSeconds(listener, 0.5f);
        assertEquals(2, forged.size());

        when(forged.get(0).isAlive()).thenReturn(false);
        advanceSeconds(listener, 0.5f);
        assertEquals(1, listener.livingDrones());
        assertEquals(1, listener.dronesAwaitingRebuild());

        advanceSeconds(listener, AutoforgeEscortsListener.REBUILD_SECONDS - 1f);
        assertEquals(2, forged.size());

        advanceSeconds(listener, 1f);
        assertEquals(3, forged.size());
        assertEquals(2, listener.livingDrones());
        assertEquals(0, listener.dronesAwaitingRebuild());
    }

    @Test
    void aForgeThatCannotBuildADroneIsNotRetried() {
        ShipAPI mothership = mothership(2f);
        int[] attempts = {0};
        AutoforgeEscortsListener listener = new AutoforgeEscortsListener(mothership, ownerShip -> {
            attempts[0]++;
            return null;
        });

        advanceSeconds(listener, 5f);

        assertEquals(1, attempts[0]);
    }

    @Test
    void aShipWithoutTheEffectBuildsNothing() {
        AutoforgeEscortsListener listener = new AutoforgeEscortsListener(mothership(0f), this::forgeDrone);

        advanceSeconds(listener, 5f);

        assertEquals(0, forged.size());
    }

    @Test
    void escortsShutDownWhenTheirMothershipIsLostOrLeaves() {
        ShipAPI mothership = mothership(1f);
        ShipAPI drone = mock(ShipAPI.class);
        FighterWingAPI escortWing = mock(FighterWingAPI.class);
        when(drone.getWing()).thenReturn(escortWing);
        AutoforgeEscortsListener.EscortTether tether = new AutoforgeEscortsListener.EscortTether(drone, mothership);

        tether.advance(1f);
        verify(engine, never()).removeEntity(drone);

        when(engine.isEntityInPlay(mothership)).thenReturn(false);
        tether.advance(1f);

        verify(escortWing).removeMember(drone);
        verify(engine).removeEntity(drone);
    }
}
