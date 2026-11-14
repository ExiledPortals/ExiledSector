package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;

final class RateOfFireAuraListener extends AuraSourceListener {

    static final String RATE_OF_FIRE_PERCENT_KEY = "exiledSector_rateOfFireAuraPercent";

    RateOfFireAuraListener(ShipAPI ownerShip) {
        super(ownerShip);
        RateOfFireAuraField.register(this);
    }

    float rateOfFirePenaltyPercent() {
        return unlessRetreating(magnitude(RATE_OF_FIRE_PERCENT_KEY));
    }
}
