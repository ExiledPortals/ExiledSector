package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;

final class MalfunctionAuraListener extends ShipCombatListener {

    static final String MALFUNCTION_PERCENT_KEY = "exiledSector_malfunctionAuraPercent";

    MalfunctionAuraListener(ShipAPI ownerShip) {
        super(ownerShip);
        MalfunctionAuraField.register(this);
    }

    ShipAPI source() {
        return ownerShip;
    }

    float malfunctionPercent() {
        return ownerShip.isRetreating() ? 0f : magnitude(MALFUNCTION_PERCENT_KEY);
    }
}
