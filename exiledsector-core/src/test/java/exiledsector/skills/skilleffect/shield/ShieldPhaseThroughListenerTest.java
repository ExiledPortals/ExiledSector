package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShieldPhaseThroughListenerTest {

    private static ShipAPI ship() {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getMutableStats().getDynamic().getValue(ShieldPhaseThroughListener.ENERGY_CHANCE_PERCENT_KEY, 0f)).thenReturn(20f);
        when(ship.getMutableStats().getDynamic().getValue(ShieldPhaseThroughListener.BALLISTIC_CHANCE_PERCENT_KEY, 0f)).thenReturn(10f);
        return ship;
    }

    private static <T extends DamagingProjectileAPI> T projectile(Class<T> projectileClass, WeaponType weaponType,
                                                                  Map<String, Object> customData) {
        T projectile = mock(projectileClass);
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getType()).thenReturn(weaponType);
        when(projectile.getWeapon()).thenReturn(weapon);
        when(projectile.getCustomData()).thenReturn(customData);
        return projectile;
    }

    private static Random rolling(float value) {
        Random random = mock(Random.class);
        when(random.nextFloat()).thenReturn(value);
        return random;
    }

    @Test
    void energyAndBallisticShotsUseTheirOwnChance() {
        ShieldPhaseThroughListener listener = new ShieldPhaseThroughListener(ship());

        assertEquals(20f, listener.chancePercent(projectile(DamagingProjectileAPI.class, WeaponType.ENERGY, Map.of())));
        assertEquals(10f, listener.chancePercent(projectile(DamagingProjectileAPI.class, WeaponType.BALLISTIC, Map.of())));
        assertEquals(0f, listener.chancePercent(projectile(DamagingProjectileAPI.class, WeaponType.MISSILE, Map.of())));
    }

    @Test
    void missilesBeamsAndShotsAlreadyPassedThroughNeverPhase() {
        ShieldPhaseThroughListener listener = new ShieldPhaseThroughListener(ship());

        assertEquals(0f, listener.chancePercent(projectile(MissileAPI.class, WeaponType.ENERGY, Map.of())));
        assertEquals(0f, listener.chancePercent(mock(BeamAPI.class)));
        assertEquals(0f, listener.chancePercent(projectile(DamagingProjectileAPI.class, WeaponType.ENERGY,
                Map.of(ShieldPhaseThroughListener.PHASED_THROUGH_KEY, true))));
    }

    @Test
    void aSuccessfulRollOnAShieldHitCancelsTheDamage() {
        ShipAPI ship = ship();
        DamageAPI damage = mock(DamageAPI.class, RETURNS_DEEP_STUBS);
        DamagingProjectileAPI shot = projectile(DamagingProjectileAPI.class, WeaponType.ENERGY, new HashMap<>());

        String result = new ShieldPhaseThroughListener(ship, rolling(0.1f)).modifyDamageTaken(shot, ship, damage, null, true);

        assertEquals(ShieldPhaseThroughListener.PHASE_THROUGH_MOD_ID, result);
        verify(damage.getModifier()).modifyMult(ShieldPhaseThroughListener.PHASE_THROUGH_MOD_ID, 0f);
    }

    @Test
    void hullHitsAndFailedRollsLeaveTheDamageAlone() {
        ShipAPI ship = ship();
        DamageAPI damage = mock(DamageAPI.class, RETURNS_DEEP_STUBS);
        DamagingProjectileAPI shot = projectile(DamagingProjectileAPI.class, WeaponType.ENERGY, new HashMap<>());

        assertNull(new ShieldPhaseThroughListener(ship, rolling(0.1f)).modifyDamageTaken(shot, ship, damage, null, false));
        assertNull(new ShieldPhaseThroughListener(ship, rolling(0.5f)).modifyDamageTaken(shot, ship, damage, null, true));
        verify(damage.getModifier(), never()).modifyMult(ShieldPhaseThroughListener.PHASE_THROUGH_MOD_ID, 0f);
    }
}
