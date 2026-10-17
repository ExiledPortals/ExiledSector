package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;

final class GravityDragListener extends ShipCombatListener {

    static final String TIMEFLOW_PERCENT_KEY = "exiledSector_gravityDragTimeflowPercent";
    static final float PHASED_MULT = 2f;

    GravityDragListener(ShipAPI ownerShip) {
        super(ownerShip);
        GravityDragField.register(this);
    }

    ShipAPI source() {
        return ownerShip;
    }

    float reductionPercentAtPointBlank() {
        float percent = magnitude(TIMEFLOW_PERCENT_KEY);
        return ownerShip.isPhased() ? percent * PHASED_MULT : percent;
    }
}
