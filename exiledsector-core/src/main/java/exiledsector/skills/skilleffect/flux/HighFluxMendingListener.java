package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class HighFluxMendingListener extends ShipCombatListener implements AdvanceableListener {

    static final String REGEN_PERCENT_KEY = "exiledSector_highFluxHullRegenPercent";
    static final float FLUX_LEVEL_THRESHOLD_PERCENT = 50f;
    static final float MAX_TOTAL_REGEN_PERCENT_OF_HULL = 20f;

    private float remainingRegen = -1f;

    HighFluxMendingListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        if (amount <= 0f || !ownerIsAliveNotHulk()) {
            return;
        }
        float maxHitpoints = ownerShip.getMaxHitpoints();
        if (remainingRegen < 0f) {
            remainingRegen = maxHitpoints * MAX_TOTAL_REGEN_PERCENT_OF_HULL / 100f;
        }
        float hitpoints = ownerShip.getHitpoints();
        if (remainingRegen <= 0f || hitpoints >= maxHitpoints || ownerShip.getFluxTracker().getFluxLevel() * 100f <= FLUX_LEVEL_THRESHOLD_PERCENT) {
            return;
        }
        float regenPercent = magnitude(REGEN_PERCENT_KEY);
        if (regenPercent <= 0f) {
            return;
        }
        float healed = Math.min(maxHitpoints * regenPercent / 100f * amount, Math.min(maxHitpoints - hitpoints, remainingRegen));
        remainingRegen -= healed;
        ownerShip.setHitpoints(hitpoints + healed);
    }
}
