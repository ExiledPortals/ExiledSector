package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.CombatGate;
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

class DamagedShipDamageListenerTest {

    private static ShipAPI ship(int owner, boolean fighter, float hullLevel) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isFighter()).thenReturn(fighter);
        when(ship.getHullLevel()).thenReturn(hullLevel);
        return ship;
    }

    private static ShipAPI carrier(float percent) {
        ShipAPI carrier = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(carrier.getMutableStats().getDynamic().getValue(DamagedShipDamageListener.DAMAGE_PERCENT_KEY, 0f)).thenReturn(percent);
        return carrier;
    }

    @Test
    void hitsOnADamagedEnemyShipDealIncreasedDamage() {
        DamageAPI damage = mock(DamageAPI.class, RETURNS_DEEP_STUBS);
        DamagedShipDamageListener listener = new DamagedShipDamageListener(ship(0, true, 1f), carrier(18f));

        String modId = listener.modifyDamageDealt(null, ship(1, false, 0.3f), damage, new Vector2f(), false);

        assertNotNull(modId);
        verify(damage.getModifier()).modifyPercent(modId, 18f);
    }

    @Test
    void healthyShipsStrikecraftAndAlliesTakeNormalDamage() {
        DamageAPI damage = mock(DamageAPI.class, RETURNS_DEEP_STUBS);
        DamagedShipDamageListener listener = new DamagedShipDamageListener(ship(0, true, 1f), carrier(18f));

        assertNull(listener.modifyDamageDealt(null, ship(1, false, CombatGate.LOW_HULL_LEVEL), damage, new Vector2f(), false));
        assertNull(listener.modifyDamageDealt(null, ship(1, true, 0.1f), damage, new Vector2f(), false));
        assertNull(listener.modifyDamageDealt(null, ship(0, false, 0.1f), damage, new Vector2f(), false));
        verify(damage.getModifier(), never()).modifyPercent(anyString(), anyFloat());
    }
}
