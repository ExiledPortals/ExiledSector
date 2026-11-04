package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LowHullTest {

    private static ShipAPI atHullLevel(float hullLevel) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getHullLevel()).thenReturn(hullLevel);
        return ship;
    }

    @Test
    void onlyHullBelowTheThresholdCounts() {
        assertTrue(LowHull.isBelowThreshold(atHullLevel(0.1f)));
        assertTrue(LowHull.isBelowThreshold(atHullLevel(LowHull.HULL_LEVEL - 0.01f)));
        assertFalse(LowHull.isBelowThreshold(atHullLevel(LowHull.HULL_LEVEL)));
        assertFalse(LowHull.isBelowThreshold(atHullLevel(1f)));
    }
}
