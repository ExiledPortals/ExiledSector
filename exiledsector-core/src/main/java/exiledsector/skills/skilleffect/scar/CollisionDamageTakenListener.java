package exiledsector.skills.skilleffect.scar;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.defense.CollisionDamageListener;

final class CollisionDamageTakenListener extends DamageTakenReductionListener {

    static final String REDUCTION_PERCENT_KEY = "exiledSector_scarCollisionDamageTakenPercent";
    private static final String MOD_ID_PREFIX = "exiledSector_scarCollisionDamageTaken_";

    CollisionDamageTakenListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX, REDUCTION_PERCENT_KEY);
    }

    @Override
    protected boolean isReducedHit(Object param, DamageAPI damage) {
        return CollisionDamageListener.isCollisionDamage(param, damage) && CollisionDamageListener.touchesAnything(ownerShip);
    }
}
