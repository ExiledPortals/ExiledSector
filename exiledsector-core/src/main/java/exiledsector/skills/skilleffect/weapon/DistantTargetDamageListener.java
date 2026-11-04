package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class DistantTargetDamageListener extends ShipCombatListener implements DamageDealtModifier {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_damageToDistantTargetsPercent";
    static final float MIN_DISTANCE = 1000f;
    private static final String MOD_ID_PREFIX = "exiledSector_distantTargetDamage_";

    DistantTargetDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(target instanceof ShipAPI targetShip) || !CombatQueries.isHostile(ownerShip, targetShip)
                || Misc.getDistance(ownerShip.getLocation(), targetShip.getLocation()) <= MIN_DISTANCE) {
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
