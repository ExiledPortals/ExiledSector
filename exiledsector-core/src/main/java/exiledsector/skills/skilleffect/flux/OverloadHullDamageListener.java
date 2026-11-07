package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.HullLoss;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class OverloadHullDamageListener extends ShipCombatListener implements AdvanceableListener, DamageListener {

    static final String HULL_DAMAGE_PERCENT_KEY = "exiledSector_overloadHullDamagePercent";
    static final String HARD_FLUX_DISSIPATION_PERCENT_KEY = "exiledSector_overloadHardFluxDissipationPercent";

    private ShipAPI lastEnemyAttacker;
    private float secondsUntilNextReplacement;
    private boolean keepingRunningOverload;

    OverloadHullDamageListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (target != ownerShip) {
            return;
        }
        ShipAPI attacker = CombatQueries.owningShip(CombatQueries.attackingShip(source, ownerShip.getParamAboutToApplyDamage()));
        if (attacker != null && CombatQueries.isHostile(ownerShip, attacker)) {
            lastEnemyAttacker = attacker;
        }
    }

    ShipAPI lastEnemyAttacker() {
        return lastEnemyAttacker;
    }

    @Override
    public void advance(float amount) {
        secondsUntilNextReplacement = Math.max(0f, secondsUntilNextReplacement - amount);
        FluxTrackerAPI fluxTracker = ownerShip.getFluxTracker();
        if (fluxTracker == null || !fluxTracker.isOverloaded()) {
            keepingRunningOverload = false;
            return;
        }
        if (keepingRunningOverload || !ownerIsAliveNotHulk()) {
            return;
        }
        float hullDamagePercent = magnitude(HULL_DAMAGE_PERCENT_KEY);
        if (hullDamagePercent <= 0f) {
            return;
        }
        if (secondsUntilNextReplacement > 0f) {
            keepingRunningOverload = true;
            return;
        }
        replace(fluxTracker, hullDamagePercent);
    }

    private void replace(FluxTrackerAPI fluxTracker, float hullDamagePercent) {
        secondsUntilNextReplacement = fluxTracker.getOverloadTimeRemaining();
        fluxTracker.stopOverload();
        float dissipatedPercent = Math.min(100f, magnitude(HARD_FLUX_DISSIPATION_PERCENT_KEY));
        if (dissipatedPercent > 0f) {
            EmpHardFluxVentListener.ventHardFlux(fluxTracker, fluxTracker.getHardFlux() * dissipatedPercent / 100f);
        }
        HullLoss.take(ownerShip, ownerShip.getMaxHitpoints() * hullDamagePercent / 100f, lastEnemyAttacker, Global.getCombatEngine());
    }
}
