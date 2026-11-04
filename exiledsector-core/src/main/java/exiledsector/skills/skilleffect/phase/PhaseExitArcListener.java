package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ArcDischarge;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class PhaseExitArcListener extends ShipCombatListener implements AdvanceableListener {

    static final String ARC_COUNT_KEY = "exiledSector_phaseExitEmpArcs";

    private boolean wasPhased;

    PhaseExitArcListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        boolean phased = ownerShip.isPhased();
        if (wasPhased && !phased && ownerIsAliveNotHulk()) {
            ArcDischarge.discharge(ownerShip, Math.round(magnitude(ARC_COUNT_KEY)));
        }
        wasPhased = phased;
    }
}
