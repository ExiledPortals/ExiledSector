package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ShipCombatListener;

abstract class PhaseTransitionListener extends ShipCombatListener implements AdvanceableListener {

    private boolean wasPhased;

    PhaseTransitionListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    PhaseTransitionListener(ShipAPI ownerShip, String modIdPrefix) {
        super(ownerShip, modIdPrefix);
    }

    @Override
    public final void advance(float amount) {
        boolean phased = ownerShip.isPhased();
        if (phased && !wasPhased) {
            onPhaseEntry();
        } else if (!phased && wasPhased) {
            onPhaseExit();
        }
        wasPhased = phased;
        afterTransitionCheck(amount);
    }

    void onPhaseEntry() {
    }

    void onPhaseExit() {
    }

    void afterTransitionCheck(float amount) {
    }
}
