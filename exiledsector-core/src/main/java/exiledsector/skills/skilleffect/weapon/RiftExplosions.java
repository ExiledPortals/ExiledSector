package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ProximityFuseAIAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.RiftTorpedoEffect;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;
import java.util.Set;

public final class RiftExplosions {

    public static final String DAMAGE_PERCENT_KEY = "exiledSector_riftExplosionDamagePercent";
    public static final String RIFT_MINE_WEAPON_ID = "riftbeam_minelayer";
    static final Set<String> RIFT_SOUNDS = Set.of("riftbeam_rift", "riftcascade_rift", "rift_lightning_explosion", "system_emp_emitter_impact");
    private static final float RIFT_FADE_IN_SECONDS = 0.05f;

    private RiftExplosions() {
    }

    public static void spawn(ShipAPI source, Vector2f point, float riftDamage, Random random) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || point == null) {
            return;
        }
        if (!(engine.spawnProjectile(source, null, RIFT_MINE_WEAPON_ID, new Vector2f(point), random.nextFloat() * 360f, null) instanceof MissileAPI rift)) {
            return;
        }
        rift.getDamage().setDamage(riftDamage);
        if (rift.getMissileAI() instanceof ProximityFuseAIAPI fuse) {
            fuse.updateDamage();
        }
        rift.getVelocity().scale(0f);
        rift.fadeOutThenIn(RIFT_FADE_IN_SECONDS);
        rift.setFlightTime(rift.getMaxFlightTime());
        rift.addDamagedAlready(source);
        rift.setNoMineFFConcerns(true);
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
