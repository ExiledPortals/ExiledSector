package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.ShipCombatListener;

abstract class AuraSourceListener extends ShipCombatListener implements AuraField.Source {

    AuraSourceListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public final ShipAPI source() {
        return ownerShip;
    }

    final float unlessRetreating(float strength) {
        return ownerShip.isRetreating() ? 0f : strength;
    }
}
