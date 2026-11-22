package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import exiledsector.skills.skilleffect.CombatTestShips;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HullfoamListenerTest {

    private static ShipAPI ship(float poolPercent) {
        ShipAPI ship = CombatTestShips.ship(mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS)).id("foam")
                .dynamic(Map.of(HullfoamListener.REGEN_PERCENT_KEY, 1f, HullfoamListener.POOL_PERCENT_KEY, poolPercent)).alive()
                .at(new Vector2f()).build();
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getHitpoints()).thenReturn(1000f);
        when(ship.getHullSpec()).thenReturn(hullSpec);
        when(ship.getMaxHitpoints()).thenReturn(1000f);
        return ship;
    }

    @Test
    void theRepairCapFallsToHalfHullAsTheFoamRunsOut() {
        assertEquals(1f, HullfoamListener.repairCapLevel(0f), 1e-5f);
        assertEquals(0.75f, HullfoamListener.repairCapLevel(0.5f), 1e-5f);
        assertEquals(0.5f, HullfoamListener.repairCapLevel(1f), 1e-5f);
    }

    @Test
    void foamRepairsHullAtItsRateUpToTheCap() {
        ShipAPI ship = ship(100f);
        HullfoamListener listener = new HullfoamListener(ship);

        when(ship.getHitpoints()).thenReturn(500f);
        listener.advance(1f);
        verify(ship).setHitpoints(510f);

        when(ship.getHitpoints()).thenReturn(990f);
        listener.advance(1f);
        verify(ship).setHitpoints(995f);
    }

    @Test
    void anEmptyPoolStopsRepairs() {
        ShipAPI ship = ship(1f);
        HullfoamListener listener = new HullfoamListener(ship);
        when(ship.getHitpoints()).thenReturn(100f);
        listener.advance(1f);
        verify(ship).setHitpoints(110f);

        when(ship.getHitpoints()).thenReturn(110f);
        listener.advance(1f);
        verify(ship, times(1)).setHitpoints(anyFloat());
    }
}
