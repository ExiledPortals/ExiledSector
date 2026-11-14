package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;

final class PhaseCloak {

    private PhaseCloak() {
    }

    static ShipSystemAPI of(ShipAPI ship) {
        ShipSystemAPI defenseCloak = ship.getPhaseCloak();
        if (defenseCloak != null) {
            return defenseCloak;
        }
        ShipSystemAPI shipSystem = ship.getSystem();
        return shipSystem != null && shipSystem.getSpecAPI() != null && shipSystem.getSpecAPI().isPhaseCloak() ? shipSystem : null;
    }
}
