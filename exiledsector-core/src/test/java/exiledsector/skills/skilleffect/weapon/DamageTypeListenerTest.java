package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DamageTypeListenerTest {

    private static final String MOD_ID = "exiledSector_damageType_ship";

    private static ShipAPI ship() {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn("ship");
        when(ship.getMutableStats().getDynamic().getValue(DamageTypeListener.KINETIC_PERCENT_KEY, 0f)).thenReturn(15f);
        when(ship.getMutableStats().getDynamic().getValue(DamageTypeListener.FRAGMENTATION_PERCENT_KEY, 0f)).thenReturn(30f);
        return ship;
    }

    private static DamageAPI damage(DamageType type) {
        DamageAPI damage = mock(DamageAPI.class, RETURNS_DEEP_STUBS);
        when(damage.getType()).thenReturn(type);
        return damage;
    }

    @Test
    void eachDamageTypeGetsItsOwnBonus() {
        DamageTypeListener listener = new DamageTypeListener(ship());
        DamageAPI kinetic = damage(DamageType.KINETIC);
        DamageAPI fragmentation = damage(DamageType.FRAGMENTATION);

        assertEquals(MOD_ID, listener.modifyDamageDealt(null, null, kinetic, null, true));
        assertEquals(MOD_ID, listener.modifyDamageDealt(null, null, fragmentation, null, false));

        verify(kinetic.getModifier()).modifyPercent(MOD_ID, 15f);
        verify(fragmentation.getModifier()).modifyPercent(MOD_ID, 30f);
    }

    @Test
    void typesWithoutABonusAreLeftAlone() {
        DamageTypeListener listener = new DamageTypeListener(ship());
        DamageAPI energy = damage(DamageType.ENERGY);
        DamageAPI untyped = damage(null);

        assertNull(listener.modifyDamageDealt(null, null, energy, null, true));
        assertNull(listener.modifyDamageDealt(null, null, untyped, null, true));
        verify(energy.getModifier(), never()).modifyPercent(anyString(), anyFloat());
    }
}
