package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

final class NanoforgeMendingListener implements AdvanceableListener {

    static final String REGEN_PERCENT_KEY = "exiledSector_nanoforgeMendingRegenPercent";
    static final float UNDAMAGED_SECONDS = 5f;
    static final float MAX_TOTAL_REGEN_PERCENT_OF_HULL = 100f;

    private final ShipAPI ownerShip;
    private float lastHitpoints = -1f;
    private float secondsSinceHullDamage;
    private float remainingRegen = -1f;

    NanoforgeMendingListener(ShipAPI ownerShip) {
        this.ownerShip = ownerShip;
    }

    @Override
    public void advance(float amount) {
        if (amount <= 0f || !ownerShip.isAlive() || ownerShip.isHulk()) {
            return;
        }
        if (remainingRegen < 0f) {
            remainingRegen = ownerShip.getMaxHitpoints() * MAX_TOTAL_REGEN_PERCENT_OF_HULL / 100f;
        }
        float hitpoints = ownerShip.getHitpoints();
        if (hitpoints < lastHitpoints) {
            secondsSinceHullDamage = 0f;
        } else {
            secondsSinceHullDamage += amount;
        }
        if (secondsSinceHullDamage >= UNDAMAGED_SECONDS && remainingRegen > 0f) {
            hitpoints = mend(hitpoints, amount);
        }
        lastHitpoints = hitpoints;
    }

    private float mend(float hitpoints, float amount) {
        float maxHitpoints = ownerShip.getMaxHitpoints();
        float regenPercent = ownerShip.getMutableStats().getDynamic().getValue(REGEN_PERCENT_KEY, 0f);
        if (hitpoints >= maxHitpoints || regenPercent <= 0f) {
            return hitpoints;
        }
        float healed = Math.min(maxHitpoints * regenPercent / 100f * amount, Math.min(maxHitpoints - hitpoints, remainingRegen));
        remainingRegen -= healed;
        float mended = hitpoints + healed;
        ownerShip.setHitpoints(mended);
        return mended;
    }
}
