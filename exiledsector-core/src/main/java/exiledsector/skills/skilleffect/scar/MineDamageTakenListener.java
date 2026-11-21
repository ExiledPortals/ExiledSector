package exiledsector.skills.skilleffect.scar;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;

final class MineDamageTakenListener extends DamageTakenReductionListener {

    static final String REDUCTION_PERCENT_KEY = "exiledSector_scarMineDamageTakenPercent";
    private static final String MOD_ID_PREFIX = "exiledSector_scarMineDamageTaken_";

    MineDamageTakenListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX, REDUCTION_PERCENT_KEY);
    }

    @Override
    protected boolean isReducedHit(Object param, DamageAPI damage) {
        return MineHits.isMineHit(param);
    }
}
