package exiledsector.skills.skilleffect.scar;

import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AllyProximityTest {

    private static ShipAPI ship(int owner, float x) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        return ship;
    }

    @Test
    void anAlliedShipInRangeCounts() {
        assertTrue(AllyProximity.isNearbyAlly(ship(1, 0f), ship(1, AllyProximity.ALLY_RANGE - 10f)));
    }

    @Test
    void distantAlliesEnemiesFightersAndOwnModulesDoNotCount() {
        ShipAPI boss = ship(1, 0f);
        ShipAPI fighter = ship(1, 50f);
        when(fighter.isFighter()).thenReturn(true);
        ShipAPI module = ship(1, 50f);
        when(module.getParentStation()).thenReturn(boss);

        assertFalse(AllyProximity.isNearbyAlly(boss, ship(1, AllyProximity.ALLY_RANGE + 10f)));
        assertFalse(AllyProximity.isNearbyAlly(boss, ship(0, 50f)));
        assertFalse(AllyProximity.isNearbyAlly(boss, fighter));
        assertFalse(AllyProximity.isNearbyAlly(boss, module));
        assertFalse(AllyProximity.isNearbyAlly(boss, boss));
    }
}
