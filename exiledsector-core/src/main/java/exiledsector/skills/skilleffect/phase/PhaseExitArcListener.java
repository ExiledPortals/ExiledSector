package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.ArcDischarge;

final class PhaseExitArcListener extends PhaseTransitionListener {

    static final String ARC_COUNT_KEY = "exiledSector_phaseExitEmpArcs";

    PhaseExitArcListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    void onPhaseExit() {
        if (ownerIsAliveNotHulk()) {
            ArcDischarge.discharge(ownerShip, Math.round(magnitude(ARC_COUNT_KEY)));
        }
    }
}
