package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;

final class PhaseEntryFluxShedListener extends PhaseTransitionListener {

    static final String SHED_PERCENT_KEY = "exiledSector_phaseEntryHardFluxToSoftPercent";

    PhaseEntryFluxShedListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    void onPhaseEntry() {
        softenHardFlux(ownerShip.getFluxTracker(), magnitude(SHED_PERCENT_KEY));
    }

    static void softenHardFlux(FluxTrackerAPI flux, float percent) {
        float hardFlux = flux.getHardFlux();
        if (percent > 0f && hardFlux > 0f) {
            flux.setHardFlux(hardFlux * Math.max(0f, 1f - percent / 100f));
        }
    }
}
