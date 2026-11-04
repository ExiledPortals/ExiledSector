package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HelplessShipDamageListenerTest {

    private static ShipAPI ship(String id, int owner, boolean overloaded, boolean venting, boolean flamedOut) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn(id);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getFluxTracker().isOverloaded()).thenReturn(overloaded);
        when(ship.getFluxTracker().isVenting()).thenReturn(venting);
        when(ship.getEngineController().isFlamedOut()).thenReturn(flamedOut);
        return ship;
    }

    private static DamageAPI damage() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getModifier()).thenReturn(mock(MutableStat.class));
        return damage;
    }

    private static HelplessShipDamageListener listener(float percent) {
        ShipAPI owner = ship("raider", 0, false, false, false);
        when(owner.getMutableStats().getDynamic().getValue(HelplessShipDamageListener.DAMAGE_PERCENT_KEY, 0f)).thenReturn(percent);
        return new HelplessShipDamageListener(owner);
    }

    @Test
    void overloadedVentingAndFlamedOutEnemiesTakeIncreasedDamage() {
        HelplessShipDamageListener listener = listener(15f);
        for (ShipAPI target : new ShipAPI[]{ship("overloaded", 1, true, false, false), ship("venting", 1, false, true, false),
                ship("flamed out", 1, false, false, true)}) {
            DamageAPI hit = damage();

            String modId = listener.modifyDamageDealt(null, target, hit, new Vector2f(), false);

            assertEquals("exiledLib_helplessShipDamage_raider", modId);
            verify(hit.getModifier()).modifyPercent(modId, 15f);
        }
    }

    @Test
    void readyEnemiesAndHelplessAlliesTakeNormalDamage() {
        HelplessShipDamageListener listener = listener(15f);
        DamageAPI hit = damage();

        assertNull(listener.modifyDamageDealt(null, ship("ready", 1, false, false, false), hit, new Vector2f(), false));
        assertNull(listener.modifyDamageDealt(null, ship("ally", 0, true, true, true), hit, new Vector2f(), false));
        assertNull(listener.modifyDamageDealt(null, null, hit, new Vector2f(), false));
        assertNull(listener(0f).modifyDamageDealt(null, ship("overloaded", 1, true, false, false), hit, new Vector2f(), false));
        verify(hit.getModifier(), never()).modifyPercent(anyString(), anyFloat());
    }
}
