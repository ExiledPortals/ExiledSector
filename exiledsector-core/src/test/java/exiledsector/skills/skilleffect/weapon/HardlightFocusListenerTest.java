package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import exiledsector.skills.skilleffect.CombatTestShips;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.AdditionalMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HardlightFocusListenerTest {

    private static ShipAPI ship(float percentPerStep) {
        return CombatTestShips.ship(mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS)).id("lumen")
                .dynamic(Map.of(HardlightFocusListener.DAMAGE_PERCENT_PER_RANGE_KEY, percentPerStep)).alive().at(new Vector2f()).build();
    }

    private static WeaponAPI beam(WeaponSize size, float baseRange) {
        WeaponAPI weapon = mock(WeaponAPI.class, RETURNS_DEEP_STUBS);
        when(weapon.isBeam()).thenReturn(true);
        when(weapon.getSize()).thenReturn(size);
        when(weapon.getSlot()).thenReturn(mock(WeaponSlotAPI.class));
        when(weapon.getSpec().getMaxRange()).thenReturn(baseRange);
        return weapon;
    }

    @Test
    void beamsLoseTheirBaseRangeOverTheSizeCap() {
        ShipAPI ship = ship(15f);
        HardlightFocusListener listener = new HardlightFocusListener(ship);

        assertEquals(-300f, listener.getWeaponBaseRangeFlatMod(ship, beam(WeaponSize.SMALL, 800f)));
        assertEquals(-400f, listener.getWeaponBaseRangeFlatMod(ship, beam(WeaponSize.MEDIUM, 1000f)));
        assertEquals(0f, listener.getWeaponBaseRangeFlatMod(ship, beam(WeaponSize.LARGE, 600f)));
    }

    @Test
    void nonBeamsUnmountedWeaponsAndShipsWithoutTheEffectKeepTheirRange() {
        ShipAPI ship = ship(15f);
        HardlightFocusListener listener = new HardlightFocusListener(ship);
        WeaponAPI projectile = beam(WeaponSize.SMALL, 800f);
        when(projectile.isBeam()).thenReturn(false);
        WeaponAPI unmounted = beam(WeaponSize.SMALL, 800f);
        when(unmounted.getSlot()).thenReturn(null);
        ShipAPI without = ship(0f);

        assertEquals(0f, listener.getWeaponBaseRangeFlatMod(ship, projectile));
        assertEquals(0f, listener.getWeaponBaseRangeFlatMod(ship, unmounted));
        assertEquals(0f, new HardlightFocusListener(without).getWeaponBaseRangeFlatMod(without, beam(WeaponSize.SMALL, 800f)));
    }

    @Test
    void damageRisesPerHundredRangeCutWithSmallerSharesForBiggerBeams() {
        assertEquals(45f, HardlightFocusListener.damagePercent(beam(WeaponSize.SMALL, 800f), 15f), 1e-4f);
        assertEquals(40f, HardlightFocusListener.damagePercent(beam(WeaponSize.MEDIUM, 1000f), 15f), 1e-4f);
        assertEquals(30f, HardlightFocusListener.damagePercent(beam(WeaponSize.LARGE, 1100f), 15f), 1e-4f);
    }

    @Test
    void onlyTheOwnersBeamsDealTheExtraDamage() {
        ShipAPI ship = ship(15f);
        HardlightFocusListener listener = new HardlightFocusListener(ship);
        WeaponAPI weapon = beam(WeaponSize.SMALL, 700f);
        BeamAPI ownBeam = mock(BeamAPI.class);
        when(ownBeam.getSource()).thenReturn(ship);
        when(ownBeam.getWeapon()).thenReturn(weapon);
        BeamAPI otherBeam = mock(BeamAPI.class);
        when(otherBeam.getSource()).thenReturn(mock(ShipAPI.class));
        when(otherBeam.getWeapon()).thenReturn(weapon);
        DamageAPI damage = mock(DamageAPI.class, RETURNS_DEEP_STUBS);

        assertNull(listener.modifyDamageDealt(otherBeam, null, damage, null, false));
        assertNull(listener.modifyDamageDealt(new Object(), null, damage, null, false));
        assertEquals("exiledSector_hardlightFocus_lumen", listener.modifyDamageDealt(ownBeam, null, damage, null, false));
        verify(damage.getModifier()).modifyMult(org.mockito.ArgumentMatchers.eq("exiledSector_hardlightFocus_lumen"), eq(1.3f, 1e-5f));
    }
}
