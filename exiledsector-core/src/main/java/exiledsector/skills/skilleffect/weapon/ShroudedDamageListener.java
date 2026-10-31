package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class ShroudedDamageListener extends ShipCombatListener implements DamageDealtModifier {

    static final String LENS_DAMAGE_PERCENT_KEY = "exiledSector_shroudedLensDamagePercent";
    static final String RIFT_LIGHTNING_DAMAGE_PERCENT_KEY = "exiledSector_riftLightningDamagePercent";
    static final String LENS_EXPLOSION_SOUND = "abyssal_glare_explosion";
    static final String RIFT_LIGHTNING_EXPLOSION_SOUND = "rift_lightning_explosion";
    private static final String MOD_ID_PREFIX = "exiledSector_shroudedDamage_";

    ShroudedDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(param instanceof DamagingProjectileAPI projectile)) {
            return null;
        }
        String magnitudeKey = magnitudeKeyFor(projectile.getExplosionSpecIfExplosion());
        float percent = magnitudeKey == null ? 0f : magnitude(magnitudeKey);
        if (percent == 0f) {
            return null;
        }
        damage.getModifier().modifyPercent(modId, percent);
        return modId;
    }

    static String magnitudeKeyFor(DamagingExplosionSpec explosion) {
        if (explosion == null) {
            return null;
        }
        if (RIFT_LIGHTNING_EXPLOSION_SOUND.equals(explosion.getSoundSetId())) {
            return RIFT_LIGHTNING_DAMAGE_PERCENT_KEY;
        }
        if (LENS_EXPLOSION_SOUND.equals(explosion.getSoundSetId()) && explosion.getCollisionClass() == CollisionClass.PROJECTILE_NO_FF) {
            return LENS_DAMAGE_PERCENT_KEY;
        }
        return null;
    }
}
