package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class PhasedPeakTimeListener extends ShipCombatListener implements AdvanceableListener {

    static final String PAUSED_KEY = "exiledSector_peakTimePausedWhilePhased";

    private boolean wasPhased;
    private float heldTimeDeployed;

    PhasedPeakTimeListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        boolean phased = ownerShip.isPhased() && magnitude(PAUSED_KEY) > 0f;
        if (phased) {
            float timeDeployed = ownerShip.getTimeDeployedForCRReduction();
            if (wasPhased && timeDeployed > heldTimeDeployed) {
                ownerShip.setTimeDeployed(heldTimeDeployed);
            } else {
                heldTimeDeployed = timeDeployed;
            }
        }
        wasPhased = phased;
    }
}
