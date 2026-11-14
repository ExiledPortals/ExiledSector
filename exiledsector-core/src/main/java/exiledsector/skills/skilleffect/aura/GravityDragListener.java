package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;

final class GravityDragListener extends AuraSourceListener {

    static final String TIMEFLOW_PERCENT_KEY = "exiledSector_gravityDragTimeflowPercent";
    static final String PHASED_TIMEFLOW_PERCENT_KEY = "exiledSector_gravityDragPhasedTimeflowPercent";
    static final float PHASED_MULT = 2f;

    GravityDragListener(ShipAPI ownerShip) {
        super(ownerShip);
        GravityDragField.register(this);
    }

    float reductionPercentAtPointBlank() {
        if (ownerShip.isRetreating()) {
            return 0f;
        }
        float percent = magnitude(TIMEFLOW_PERCENT_KEY);
        return ownerShip.isPhased() ? percent * PHASED_MULT + magnitude(PHASED_TIMEFLOW_PERCENT_KEY) : percent;
    }
}
