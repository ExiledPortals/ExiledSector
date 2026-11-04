package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CombatGateTest {

    private static ShipAPI atHullLevel(float hullLevel) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getHullLevel()).thenReturn(hullLevel);
        return ship;
    }

    @Test
    void onlyHullBelowTheLowHullLevelCounts() {
        assertTrue(CombatGate.LOW_HULL.holds(atHullLevel(0.1f)));
        assertTrue(CombatGate.LOW_HULL.holds(atHullLevel(CombatGate.LOW_HULL_LEVEL - 0.01f)));
        assertFalse(CombatGate.LOW_HULL.holds(atHullLevel(CombatGate.LOW_HULL_LEVEL)));
        assertFalse(CombatGate.LOW_HULL.holds(atHullLevel(1f)));
    }
}
