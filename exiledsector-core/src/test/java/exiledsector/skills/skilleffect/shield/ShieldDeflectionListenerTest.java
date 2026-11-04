package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShieldDeflectionListenerTest {

    private static ShipAPI shieldedShip(float chancePercent) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getMutableStats().getDynamic().getValue(ShieldDeflectionListener.CHANCE_PERCENT_KEY, 0f)).thenReturn(chancePercent);
        return ship;
    }

    private static Random rolling(float roll) {
        Random random = mock(Random.class);
        when(random.nextFloat()).thenReturn(roll);
        return random;
    }

    private static DamagingProjectileAPI projectile(ProjectileSpawnType spawnType) {
        DamagingProjectileAPI projectile = mock(DamagingProjectileAPI.class);
        when(projectile.getSpawnType()).thenReturn(spawnType);
        return projectile;
    }

    private static DamageAPI damage() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getModifier()).thenReturn(mock(MutableStat.class));
        return damage;
    }

    @Test
    void aSuccessfulRollDeflectsTheProjectileForNoFlux() {
        ShipAPI ship = shieldedShip(5f);
        DamageAPI hit = damage();

        String modId = new ShieldDeflectionListener(ship, rolling(0.04f))
                .modifyDamageTaken(projectile(ProjectileSpawnType.BALLISTIC), ship, hit, new Vector2f(), true);

        assertEquals(ShieldDeflectionListener.DEFLECT_MOD_ID, modId);
        verify(hit.getModifier()).modifyMult(ShieldDeflectionListener.DEFLECT_MOD_ID, 0f);
    }

    @Test
    void failedRollsHullHitsAndOtherShipsAreLeftAlone() {
        ShipAPI ship = shieldedShip(5f);
        DamageAPI hit = damage();
        DamagingProjectileAPI shot = projectile(ProjectileSpawnType.PLASMA);

        assertNull(new ShieldDeflectionListener(ship, rolling(0.06f)).modifyDamageTaken(shot, ship, hit, new Vector2f(), true));
        assertNull(new ShieldDeflectionListener(ship, rolling(0f)).modifyDamageTaken(shot, ship, hit, new Vector2f(), false));
        assertNull(new ShieldDeflectionListener(ship, rolling(0f)).modifyDamageTaken(shot, shieldedShip(5f), hit, new Vector2f(), true));
        verify(hit.getModifier(), never()).modifyMult(anyString(), anyFloat());
    }

    @Test
    void onlyNonMissileProjectilesCanBeDeflected() {
        MissileAPI missile = mock(MissileAPI.class);
        when(missile.getSpawnType()).thenReturn(ProjectileSpawnType.MISSILE);

        assertTrue(ShieldDeflectionListener.isDeflectable(projectile(ProjectileSpawnType.BALLISTIC_AS_BEAM)));
        assertFalse(ShieldDeflectionListener.isDeflectable(missile));
        assertFalse(ShieldDeflectionListener.isDeflectable(projectile(ProjectileSpawnType.OTHER)));
        assertFalse(ShieldDeflectionListener.isDeflectable(mock(BeamAPI.class)));
    }
}
