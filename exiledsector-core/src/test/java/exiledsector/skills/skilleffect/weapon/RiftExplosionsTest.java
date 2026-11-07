package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.RiftTorpedoEffect;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

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

class RiftExplosionsTest {

    private static final Color INVISIBLE = new Color(0, 0, 0, 0);

    private static DamagingProjectileAPI explosion(String soundSetId, int particleCount, Color explosionColour) {
        DamagingExplosionSpec spec = mock(DamagingExplosionSpec.class);
        when(spec.getSoundSetId()).thenReturn(soundSetId);
        when(spec.getParticleCount()).thenReturn(particleCount);
        when(spec.getExplosionColor()).thenReturn(explosionColour);
        DamagingProjectileAPI projectile = mock(DamagingProjectileAPI.class);
        when(projectile.getExplosionSpecIfExplosion()).thenReturn(spec);
        return projectile;
    }

    private static RiftExplosionDamageListener listener(float percent) {
        ShipAPI owner = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(owner.getId()).thenReturn("rift_ship");
        when(owner.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic().getValue(RiftExplosions.DAMAGE_PERCENT_KEY, 0f)).thenReturn(percent);
        return RiftExplosionDamageListener.forShip(owner);
    }

    private static DamageAPI damage() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getModifier()).thenReturn(mock(MutableStat.class));
        return damage;
    }

    @Test
    void everyVanillaRiftMineCountsAsARiftExplosion() {
        for (String sound : RiftExplosions.RIFT_SOUNDS) {
            assertTrue(RiftExplosions.isRift(explosion(sound, 0, INVISIBLE)), sound);
        }
    }

    @Test
    void anExplosionThatDrawsItsOwnBlastIsNotARiftEvenWithARiftSound() {
        assertFalse(RiftExplosions.isRift(explosion("system_emp_emitter_impact", 50, INVISIBLE)));
        assertFalse(RiftExplosions.isRift(explosion("system_emp_emitter_impact", 0, Color.WHITE)));
        assertFalse(RiftExplosions.isRift(explosion("explosion_flak", 0, INVISIBLE)));
    }

    @Test
    void anExplosionWithNoSoundOrColourIsNotARift() {
        assertFalse(RiftExplosions.isRift(explosion(null, 0, null)));
    }

    @Test
    void riftTorpedoHitsCountButOrdinaryShotsDoNot() {
        MissileSpecAPI torpedoSpec = mock(MissileSpecAPI.class);
        when(torpedoSpec.getOnHitEffect()).thenReturn(new RiftTorpedoEffect());
        MissileAPI torpedo = mock(MissileAPI.class);
        when(torpedo.getSpec()).thenReturn(torpedoSpec);

        assertTrue(RiftExplosions.isRift(torpedo));
        assertFalse(RiftExplosions.isRift(mock(MissileAPI.class)));
        assertFalse(RiftExplosions.isRift(mock(DamagingProjectileAPI.class)));
    }

    @Test
    void riftExplosionsTakeTheIncreasedRiftDamage() {
        DamageAPI hit = damage();

        String modId = listener(20f).modifyDamageDealt(explosion("riftbeam_rift", 0, INVISIBLE), null, hit, new Vector2f(), false);

        assertEquals("exiledSector_riftExplosionDamage_rift_ship", modId);
        verify(hit.getModifier()).modifyPercent(modId, 20f);
    }

    @Test
    void otherDamageIsLeftAlone() {
        DamageAPI hit = damage();

        assertNull(listener(20f).modifyDamageDealt(explosion("explosion_flak", 30, Color.WHITE), null, hit, new Vector2f(), false));
        assertNull(listener(0f).modifyDamageDealt(explosion("riftbeam_rift", 0, INVISIBLE), null, hit, new Vector2f(), false));
        verify(hit.getModifier(), never()).modifyPercent(anyString(), anyFloat());
    }

    @Test
    void aStrikecraftsRiftExplosionsTakeItsCarriersBonus() {
        ShipAPI fighter = mock(ShipAPI.class);
        when(fighter.getId()).thenReturn("fighter");
        ShipAPI carrier = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(carrier.getMutableStats().getDynamic().getValue(RiftExplosionDamageListener.STRIKECRAFT_DAMAGE_PERCENT_KEY, 0f)).thenReturn(10f);
        when(carrier.getMutableStats().getDynamic().getValue(RiftExplosions.DAMAGE_PERCENT_KEY, 0f)).thenReturn(30f);
        DamageAPI hit = damage();

        String modId = RiftExplosionDamageListener.forStrikecraft(fighter, carrier)
                .modifyDamageDealt(explosion("riftbeam_rift", 0, INVISIBLE), null, hit, new Vector2f(), false);

        assertEquals("exiledSector_riftExplosionDamage_fighter", modId);
        verify(hit.getModifier()).modifyPercent(modId, 10f);
    }
}
