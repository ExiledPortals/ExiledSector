package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
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

    OverloadHullDamageListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (target != ownerShip) {
            return;
        }
        ShipAPI attacker = owningShip(CombatQueries.attackingShip(source, ownerShip.getParamAboutToApplyDamage()));
        if (attacker != null && CombatQueries.isHostile(ownerShip, attacker)) {
            lastEnemyAttacker = attacker;
        }
    }

    static ShipAPI owningShip(ShipAPI attacker) {
        FighterWingAPI wing = attacker != null && attacker.isFighter() ? attacker.getWing() : null;
        ShipAPI carrier = wing == null ? null : wing.getSourceShip();
        return carrier != null ? carrier : attacker;
    }

    ShipAPI lastEnemyAttacker() {
        return lastEnemyAttacker;
    }

    @Override
    public void advance(float amount) {
        FluxTrackerAPI fluxTracker = ownerShip.getFluxTracker();
        if (fluxTracker == null || !fluxTracker.isOverloaded() || !ownerIsAliveNotHulk()) {
            return;
        }
        float hullDamagePercent = magnitude(HULL_DAMAGE_PERCENT_KEY);
        if (hullDamagePercent <= 0f) {
            return;
        }
        fluxTracker.stopOverload();
        float dissipatedPercent = Math.min(100f, magnitude(HARD_FLUX_DISSIPATION_PERCENT_KEY));
        if (dissipatedPercent > 0f) {
            EmpHardFluxVentListener.ventHardFlux(fluxTracker, fluxTracker.getHardFlux() * dissipatedPercent / 100f);
        }
        HullLoss.take(ownerShip, ownerShip.getMaxHitpoints() * hullDamagePercent / 100f, lastEnemyAttacker, Global.getCombatEngine());
    }
}
