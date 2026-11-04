package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;

final class DistantTargetDamageListener extends TargetDamageBonusListener {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_damageToDistantTargetsPercent";
    static final float MIN_DISTANCE = 1000f;
    private static final String MOD_ID_PREFIX = "exiledSector_distantTargetDamage_";

    DistantTargetDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX, DAMAGE_PERCENT_KEY);
    }

    @Override
    boolean qualifies(ShipAPI target) {
        return Misc.getDistance(ownerShip.getLocation(), target.getLocation()) > MIN_DISTANCE;
    }
}
