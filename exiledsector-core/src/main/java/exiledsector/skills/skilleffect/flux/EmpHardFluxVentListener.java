package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class EmpHardFluxVentListener extends ShipCombatListener implements DamageListener {

    static final String VENT_PERCENT_KEY = "exiledSector_empDamageTakenHardFluxVentPercent";

    EmpHardFluxVentListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (target != ownerShip || result.getEmpDamage() <= 0f || !ownerIsAliveNotHulk()) {
            return;
        }
        FluxTrackerAPI flux = ownerShip.getFluxTracker();
        if (flux != null) {
            ventHardFlux(flux, result.getEmpDamage() * magnitude(VENT_PERCENT_KEY) / 100f);
        }
    }

    static float ventHardFlux(FluxTrackerAPI flux, float amount) {
        float vented = Math.min(flux.getHardFlux(), amount);
        if (!(vented > 0f)) {
            return 0f;
        }
        flux.setHardFlux(flux.getHardFlux() - vented);
        flux.setCurrFlux(flux.getCurrFlux() - vented);
        return vented;
    }
}
