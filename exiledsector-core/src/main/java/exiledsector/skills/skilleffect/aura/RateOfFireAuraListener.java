package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class RateOfFireAuraListener extends ShipCombatListener implements AuraField.Source {

    static final String RATE_OF_FIRE_PERCENT_KEY = "exiledSector_rateOfFireAuraPercent";

    RateOfFireAuraListener(ShipAPI ownerShip) {
        super(ownerShip);
        RateOfFireAuraField.register(this);
    }

    @Override
    public ShipAPI source() {
        return ownerShip;
    }

    float rateOfFirePenaltyPercent() {
        return ownerShip.isRetreating() ? 0f : magnitude(RATE_OF_FIRE_PERCENT_KEY);
    }
}
