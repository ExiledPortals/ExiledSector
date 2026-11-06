package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.HullLoss;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class OverloadHullDamageListener extends ShipCombatListener implements AdvanceableListener {

    static final String HULL_DAMAGE_PERCENT_KEY = "exiledSector_overloadHullDamagePercent";

    OverloadHullDamageListener(ShipAPI ownerShip) {
        super(ownerShip);
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
        HullLoss.take(ownerShip, ownerShip.getMaxHitpoints() * hullDamagePercent / 100f, ownerShip, Global.getCombatEngine());
    }
}
