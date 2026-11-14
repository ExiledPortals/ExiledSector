package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.ParameterBonus;

final class AuxiliaryScreenListener extends AuraSourceListener {

    static final String DAMAGE_TAKEN_PERCENT_KEY = "exiledSector_auxiliaryScreenDamageTakenPercent";
    static final String RANGE_FLAT_KEY = "exiledSector_auxiliaryScreenRangeBonus";

    AuxiliaryScreenListener(ShipAPI ownerShip) {
        super(ownerShip);
        AuxiliaryScreenField.register(this);
    }

    float range() {
        return range(magnitude(RANGE_FLAT_KEY));
    }

    static float range(float rangeBonus) {
        return ParameterBonus.plusFlat(AuxiliaryScreenField.RANGE, rangeBonus);
    }

    float damageTakenReductionPercent() {
        return unlessRetreating(Math.min(AuxiliaryScreenField.MAX_REDUCTION_PERCENT, magnitude(DAMAGE_TAKEN_PERCENT_KEY)));
    }
}
