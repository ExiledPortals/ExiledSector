package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissileEjectionListenerTest {

    private static ShipAPI plated(float chancePercent) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getMutableStats().getDynamic().getValue(MissileEjectionListener.CHANCE_PERCENT_KEY, 0f)).thenReturn(chancePercent);
        return ship;
    }

    private static Random rolling(float roll) {
        Random random = mock(Random.class);
        when(random.nextFloat()).thenReturn(roll);
        return random;
    }

    private static DamageAPI damage() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getModifier()).thenReturn(mock(MutableStat.class));
        return damage;
    }

    @Test
    void anEjectedMissileDealsNoDamageAndSkipsItsExplosionAndOnHitEffect() {
        ShipAPI ship = plated(8f);
        MissileAPI missile = mock(MissileAPI.class);
        DamageAPI hit = damage();

        String modId = new MissileEjectionListener(ship, rolling(0.05f)).modifyDamageTaken(missile, ship, hit, new Vector2f(), false);

        assertEquals(MissileEjectionListener.EJECTION_MOD_ID, modId);
        verify(hit.getModifier()).modifyMult(MissileEjectionListener.EJECTION_MOD_ID, 0f);
        verify(missile).setDidDamage(true);
    }

    @Test
    void shieldHitsFailedRollsProjectilesAndSpentMissilesAreLeftAlone() {
        ShipAPI ship = plated(8f);
        MissileAPI missile = mock(MissileAPI.class);
        MissileAPI spent = mock(MissileAPI.class);
        when(spent.didDamage()).thenReturn(true);
        DamageAPI hit = damage();

        assertNull(new MissileEjectionListener(ship, rolling(0f)).modifyDamageTaken(missile, ship, hit, new Vector2f(), true));
        assertNull(new MissileEjectionListener(ship, rolling(0.09f)).modifyDamageTaken(missile, ship, hit, new Vector2f(), false));
        assertNull(new MissileEjectionListener(ship, rolling(0f)).modifyDamageTaken(mock(DamagingProjectileAPI.class), ship, hit,
                new Vector2f(), false));
        assertNull(new MissileEjectionListener(ship, rolling(0f)).modifyDamageTaken(spent, ship, hit, new Vector2f(), false));
        verify(hit.getModifier(), never()).modifyMult(anyString(), anyFloat());
        verify(missile, never()).setDidDamage(anyBoolean());
    }
}
