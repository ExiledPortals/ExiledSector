package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class FluxGeneratorListener extends ShipCombatListener implements AdvanceableListener {

    static final String FLUX_LEVEL_PERCENT_KEY = "exiledSector_fluxGeneratorFluxLevelPercent";
    static final float DISSIPATION_MULTIPLE = 2f;

    FluxGeneratorListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        if (amount <= 0f || !ownerIsAliveNotHulk()) {
            return;
        }
        FluxTrackerAPI flux = ownerShip.getFluxTracker();
        float fluxCap = flux.getMaxFlux() * Math.min(1f, magnitude(FLUX_LEVEL_PERCENT_KEY) / 100f);
        float currentFlux = flux.getCurrFlux();
        if (flux.isVenting() || currentFlux >= fluxCap) {
            return;
        }
        float generated = ownerShip.getMutableStats().getFluxDissipation().getModifiedValue() * DISSIPATION_MULTIPLE * amount;
        flux.setCurrFlux(Math.min(currentFlux + generated, fluxCap));
    }
}
