package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.weapon.RiftExplosions;
import org.lwjgl.util.vector.Vector2f;

final class StrikecraftRiftExplosionDamageListener implements DamageDealtModifier {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_strikecraftRiftExplosionDamagePercent";
    static final String DAMAGE_MOD_ID = "exiledSector_strikecraftRiftExplosionDamage";

    private final ShipAPI carrier;

    StrikecraftRiftExplosionDamageListener(ShipAPI carrier) {
        this.carrier = carrier;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(param instanceof DamagingProjectileAPI projectile) || !RiftExplosions.isRift(projectile)) {
            return null;
        }
        float percent = FighterListenerEffect.carrierValue(carrier, DAMAGE_PERCENT_KEY);
        if (percent == 0f) {
            return null;
        }
        damage.getModifier().modifyPercent(DAMAGE_MOD_ID, percent);
        return DAMAGE_MOD_ID;
    }
}
