package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.impl.combat.RiftTorpedoEffect;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.MissileSpecAPI;

import java.awt.Color;
import java.util.Set;

public final class RiftExplosions {

    public static final String DAMAGE_PERCENT_KEY = "exiledSector_riftExplosionDamagePercent";
    static final Set<String> RIFT_SOUNDS = Set.of("riftbeam_rift", "riftcascade_rift", "rift_lightning_explosion", "system_emp_emitter_impact");

    private RiftExplosions() {
    }

    public static boolean isRift(DamagingProjectileAPI projectile) {
        DamagingExplosionSpec explosion = projectile.getExplosionSpecIfExplosion();
        if (explosion != null) {
            return isRiftExplosion(explosion);
        }
        MissileSpecAPI missileSpec = projectile instanceof MissileAPI missile ? missile.getSpec() : null;
        return missileSpec != null && missileSpec.getOnHitEffect() instanceof RiftTorpedoEffect;
    }

    static boolean isRiftExplosion(DamagingExplosionSpec explosion) {
        Color explosionColour = explosion.getExplosionColor();
        boolean drawsNoExplosionOfItsOwn = explosion.getParticleCount() == 0 && (explosionColour == null || explosionColour.getAlpha() == 0);
        String soundSetId = explosion.getSoundSetId();
        return drawsNoExplosionOfItsOwn && soundSetId != null && RIFT_SOUNDS.contains(soundSetId);
    }
}
