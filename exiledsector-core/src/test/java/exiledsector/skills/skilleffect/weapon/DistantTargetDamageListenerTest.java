package exiledsector.skills.skilleffect.weapon;

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

class DistantTargetDamageListenerTest {

    private static ShipAPI ship(int owner, float x, float percent) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        when(ship.getId()).thenReturn("ship_" + owner + "_" + x);
        when(ship.getMutableStats().getDynamic().getValue(DistantTargetDamageListener.DAMAGE_PERCENT_KEY, 0f)).thenReturn(percent);
        return ship;
    }

    @Test
    void hitsOnAnEnemyShipBeyondTheRangeDealIncreasedDamage() {
        DamageAPI damage = mock(DamageAPI.class, RETURNS_DEEP_STUBS);
        DistantTargetDamageListener listener = new DistantTargetDamageListener(ship(0, 0f, 8f));

        String modId = listener.modifyDamageDealt(null, ship(1, DistantTargetDamageListener.MIN_DISTANCE + 50f, 0f), damage, new Vector2f(), false);

        assertNotNull(modId);
        verify(damage.getModifier()).modifyPercent(modId, 8f);
    }

    @Test
    void closeEnemiesAndDistantAlliesTakeNormalDamage() {
        DamageAPI damage = mock(DamageAPI.class, RETURNS_DEEP_STUBS);
        DistantTargetDamageListener listener = new DistantTargetDamageListener(ship(0, 0f, 8f));

        assertNull(listener.modifyDamageDealt(null, ship(1, DistantTargetDamageListener.MIN_DISTANCE - 50f, 0f), damage, new Vector2f(), false));
        assertNull(listener.modifyDamageDealt(null, ship(0, DistantTargetDamageListener.MIN_DISTANCE + 50f, 0f), damage, new Vector2f(), false));
        verify(damage.getModifier(), never()).modifyPercent(anyString(), anyFloat());
    }
}
