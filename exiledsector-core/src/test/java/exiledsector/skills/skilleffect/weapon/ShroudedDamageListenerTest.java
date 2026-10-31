package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
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

class ShroudedDamageListenerTest {

    private static ShroudedDamageListener listener(float lensPercent, float lightningPercent) {
        ShipAPI owner = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(owner.getId()).thenReturn("dweller_hunter");
        when(owner.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic().getValue(ShroudedDamageListener.LENS_DAMAGE_PERCENT_KEY, 0f)).thenReturn(lensPercent);
        when(stats.getDynamic().getValue(ShroudedDamageListener.RIFT_LIGHTNING_DAMAGE_PERCENT_KEY, 0f)).thenReturn(lightningPercent);
        return new ShroudedDamageListener(owner);
    }

    private static DamagingProjectileAPI explosion(String soundSetId, CollisionClass collisionClass) {
        DamagingExplosionSpec spec = mock(DamagingExplosionSpec.class);
        when(spec.getSoundSetId()).thenReturn(soundSetId);
        when(spec.getCollisionClass()).thenReturn(collisionClass);
        DamagingProjectileAPI projectile = mock(DamagingProjectileAPI.class);
        when(projectile.getExplosionSpecIfExplosion()).thenReturn(spec);
        return projectile;
    }

    private static DamageAPI damage() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getModifier()).thenReturn(mock(MutableStat.class));
        return damage;
    }

    @Test
    void shroudedLensBlastsTakeTheLensBonus() {
        DamageAPI hit = damage();

        String modId = listener(20f, 30f).modifyDamageDealt(explosion(ShroudedDamageListener.LENS_EXPLOSION_SOUND, CollisionClass.PROJECTILE_NO_FF),
                null, hit, new Vector2f(), false);

        assertEquals("exiledSector_shroudedDamage_dweller_hunter", modId);
        verify(hit.getModifier()).modifyPercent(modId, 20f);
    }

    @Test
    void riftLightningExplosionsTakeTheLightningBonus() {
        DamageAPI hit = damage();

        String modId = listener(20f, 30f).modifyDamageDealt(explosion(ShroudedDamageListener.RIFT_LIGHTNING_EXPLOSION_SOUND, CollisionClass.PROJECTILE_NO_FF),
                null, hit, new Vector2f(), false);

        verify(hit.getModifier()).modifyPercent(modId, 30f);
    }

    @Test
    void abyssalGlareSharesTheLensSoundButNotItsCollisionClass() {
        DamageAPI hit = damage();

        assertNull(listener(20f, 30f).modifyDamageDealt(explosion(ShroudedDamageListener.LENS_EXPLOSION_SOUND, CollisionClass.PROJECTILE_FF),
                null, hit, new Vector2f(), false));
        verify(hit.getModifier(), never()).modifyPercent(anyString(), anyFloat());
    }

    @Test
    void otherHitsAndShipsWithoutTheEffectAreLeftAlone() {
        DamageAPI hit = damage();
        DamagingProjectileAPI shell = mock(DamagingProjectileAPI.class);

        assertNull(listener(20f, 30f).modifyDamageDealt(shell, null, hit, new Vector2f(), false));
        assertNull(listener(20f, 30f).modifyDamageDealt(explosion("explosion_flak", CollisionClass.PROJECTILE_FF), null, hit, new Vector2f(), false));
        assertNull(listener(20f, 30f).modifyDamageDealt(null, null, hit, new Vector2f(), false));
        assertNull(listener(0f, 0f).modifyDamageDealt(explosion(ShroudedDamageListener.RIFT_LIGHTNING_EXPLOSION_SOUND, CollisionClass.PROJECTILE_NO_FF),
                null, hit, new Vector2f(), false));
        verify(hit.getModifier(), never()).modifyPercent(anyString(), anyFloat());
    }
}
