package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class HardFluxDissipationListener extends ShipCombatListener implements AdvanceableListener {

    static final String PERCENT_PER_STEP_KEY = "exiledSector_fluxDissipationPercentPerHardFluxStep";
    static final float HARD_FLUX_STEP_PERCENT = 5f;
    private static final String MOD_ID_PREFIX = "exiledSector_hardFluxDissipation_";

    private int appliedSteps;

    HardFluxDissipationListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    static int hardFluxSteps(FluxTrackerAPI flux) {
        float maxFlux = flux.getMaxFlux();
        if (!(maxFlux > 0f)) {
            return 0;
        }
        float hardFluxPercent = Math.min(100f, flux.getHardFlux() / maxFlux * 100f);
        return Math.max(0, (int) Math.floor(hardFluxPercent / HARD_FLUX_STEP_PERCENT));
    }

    @Override
    public void advance(float amount) {
        int steps = ownerIsAliveNotHulk() ? hardFluxSteps(ownerShip.getFluxTracker()) : 0;
        if (steps == appliedSteps) {
            return;
        }
        appliedSteps = steps;
        MutableStat dissipation = ownerShip.getMutableStats().getFluxDissipation();
        float bonusPercent = steps * magnitude(PERCENT_PER_STEP_KEY);
        if (bonusPercent > 0f) {
            dissipation.modifyPercent(modId, bonusPercent);
        } else {
            dissipation.unmodifyPercent(modId);
        }
    }
}
