package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;

final class MalfunctionAuraListener extends AuraSourceListener {

    static final String MALFUNCTION_PERCENT_KEY = "exiledSector_malfunctionAuraPercent";

    MalfunctionAuraListener(ShipAPI ownerShip) {
        super(ownerShip);
        MalfunctionAuraField.register(this);
    }

    float malfunctionPercent() {
        return unlessRetreating(magnitude(MALFUNCTION_PERCENT_KEY));
    }
}
