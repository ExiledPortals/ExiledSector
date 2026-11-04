package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class AuxiliaryScreenListener extends ShipCombatListener implements AuraField.Source {

    static final String DAMAGE_TAKEN_PERCENT_KEY = "exiledSector_auxiliaryScreenDamageTakenPercent";

    AuxiliaryScreenListener(ShipAPI ownerShip) {
        super(ownerShip);
        AuxiliaryScreenField.register(this);
    }

    @Override
    public ShipAPI source() {
        return ownerShip;
    }

    float damageTakenReductionPercent() {
        return ownerShip.isRetreating() ? 0f : Math.min(AuxiliaryScreenField.MAX_REDUCTION_PERCENT, magnitude(DAMAGE_TAKEN_PERCENT_KEY));
    }
}
