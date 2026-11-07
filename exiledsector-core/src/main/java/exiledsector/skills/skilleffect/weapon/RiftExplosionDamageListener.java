package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class RiftExplosionDamageListener extends ShipCombatListener implements DamageDealtModifier {

    static final String DAMAGE_PERCENT_KEY = RiftExplosions.DAMAGE_PERCENT_KEY;
    private static final String MOD_ID_PREFIX = "exiledSector_riftExplosionDamage_";

    RiftExplosionDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(param instanceof DamagingProjectileAPI projectile) || !RiftExplosions.isRift(projectile)) {
            return null;
        }
        float percent = magnitude(DAMAGE_PERCENT_KEY);
        if (percent == 0f) {
            return null;
        }
        damage.getModifier().modifyPercent(modId, percent);
        return modId;
    }
}
