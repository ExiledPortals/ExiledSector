package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArchaicWeaponRangeListenerTest {

    private static WeaponAPI weapon(boolean archaic) {
        WeaponAPI weapon = mock(WeaponAPI.class, RETURNS_DEEP_STUBS);
        when(weapon.getSpec().hasTag(ArchaicWeaponRangeListener.ARCHAIC_WEAPON_TAG)).thenReturn(archaic);
        return weapon;
    }

    @Test
    void onlyArchaicWeaponsGainTheRangePercent() {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getMutableStats().getDynamic().getValue(ArchaicWeaponRangeListener.RANGE_PERCENT_KEY, 0f)).thenReturn(50f);
        ArchaicWeaponRangeListener listener = new ArchaicWeaponRangeListener();

        assertEquals(50f, listener.getWeaponRangePercentMod(ship, weapon(true)), 0.001f);
        assertEquals(0f, listener.getWeaponRangePercentMod(ship, weapon(false)), 0.001f);
        assertEquals(1f, listener.getWeaponRangeMultMod(ship, weapon(true)), 0.001f);
        assertEquals(0f, listener.getWeaponRangeFlatMod(ship, weapon(true)), 0.001f);
    }
}
