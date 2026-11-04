package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class PhaseEntryFluxShedListener extends ShipCombatListener implements AdvanceableListener {

    static final String SHED_PERCENT_KEY = "exiledSector_phaseEntryHardFluxToSoftPercent";

    private boolean wasPhased;

    PhaseEntryFluxShedListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        boolean phased = ownerShip.isPhased();
        if (phased && !wasPhased) {
            softenHardFlux(ownerShip.getFluxTracker(), magnitude(SHED_PERCENT_KEY));
        }
        wasPhased = phased;
    }

    static void softenHardFlux(FluxTrackerAPI flux, float percent) {
        float hardFlux = flux.getHardFlux();
        if (percent > 0f && hardFlux > 0f) {
            flux.setHardFlux(hardFlux * Math.max(0f, 1f - percent / 100f));
        }
    }
}
