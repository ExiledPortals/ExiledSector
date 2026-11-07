package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DroneDamageListenerTest {

    private static ShipAPI ship(int owner, boolean fighter, float minCrew) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isFighter()).thenReturn(fighter);
        when(ship.getHullSpec().getMinCrew()).thenReturn(minCrew);
        return ship;
    }

    private static ShipAPI carrier(float percent) {
        ShipAPI carrier = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(carrier.getMutableStats().getDynamic().getValue(DroneDamageListener.DAMAGE_PERCENT_KEY, 0f)).thenReturn(percent);
        return carrier;
    }

    @Test
    void hitsOnAnEnemyDroneDealIncreasedDamage() {
        DamageAPI damage = mock(DamageAPI.class, RETURNS_DEEP_STUBS);
        DroneDamageListener listener = new DroneDamageListener(ship(0, true, 1f), carrier(8f));

        String modId = listener.modifyDamageDealt(null, ship(1, true, 0f), damage, new Vector2f(), false);

        assertNotNull(modId);
        verify(damage.getModifier()).modifyPercent(modId, 8f);
    }

    @Test
    void crewedStrikecraftShipsAndFriendlyDronesTakeNormalDamage() {
        DamageAPI damage = mock(DamageAPI.class, RETURNS_DEEP_STUBS);
        DroneDamageListener listener = new DroneDamageListener(ship(0, true, 1f), carrier(8f));

        assertNull(listener.modifyDamageDealt(null, ship(1, true, 1f), damage, new Vector2f(), false));
        assertNull(listener.modifyDamageDealt(null, ship(1, false, 0f), damage, new Vector2f(), false));
        assertNull(listener.modifyDamageDealt(null, ship(0, true, 0f), damage, new Vector2f(), false));
        verify(damage.getModifier(), never()).modifyPercent(anyString(), anyFloat());
    }
}
