package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;

final class GravityDragListener extends ShipCombatListener implements AuraField.Source {

    static final String TIMEFLOW_PERCENT_KEY = "exiledSector_gravityDragTimeflowPercent";
    static final float PHASED_MULT = 2f;

    GravityDragListener(ShipAPI ownerShip) {
        super(ownerShip);
        GravityDragField.register(this);
    }

    @Override
    public ShipAPI source() {
        return ownerShip;
    }

    float reductionPercentAtPointBlank() {
        if (ownerShip.isRetreating()) {
            return 0f;
        }
        float percent = magnitude(TIMEFLOW_PERCENT_KEY);
        return ownerShip.isPhased() ? percent * PHASED_MULT : percent;
    }
}
