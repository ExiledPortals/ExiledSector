package exiledlib.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledlib.skills.skilleffect.ShipCombatListener;

final class MalfunctionAuraListener extends ShipCombatListener implements AuraField.Source {

    static final String MALFUNCTION_PERCENT_KEY = "exiledLib_malfunctionAuraPercent";

    MalfunctionAuraListener(ShipAPI ownerShip) {
        super(ownerShip);
        MalfunctionAuraField.register(this);
    }

    @Override
    public ShipAPI source() {
        return ownerShip;
    }

    float malfunctionPercent() {
        return ownerShip.isRetreating() ? 0f : magnitude(MALFUNCTION_PERCENT_KEY);
    }
}
