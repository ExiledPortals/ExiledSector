package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ArcDischarge;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.TickTimer;

import java.util.List;

final class EntropyProjectorListener extends ShipCombatListener implements AdvanceableListener {

    static final String ARC_COUNT_KEY = "exiledSector_entropyProjectorArcs";
    static final float CHARGE_SECONDS = 10f;
    static final float RANGE = 1000f;
    static final float ENERGY_DAMAGE = 300f;
    static final float EMP_DAMAGE = 400f;
    static final float TARGET_SEARCH_SECONDS = 0.5f;

    private TickTimer targetSearch = TickTimer.dueOnFirstAdvance(TARGET_SEARCH_SECONDS);
    private float chargedSeconds;

    EntropyProjectorListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    float chargedSeconds() {
        return chargedSeconds;
    }

    @Override
    public void advance(float amount) {
        if (amount <= 0f || !ownerIsAliveNotHulk()) {
            return;
        }
        chargedSeconds = Math.min(CHARGE_SECONDS, chargedSeconds + amount);
        int arcCount = Math.round(magnitude(ARC_COUNT_KEY));
        if (chargedSeconds < CHARGE_SECONDS || arcCount <= 0 || ownerShip.isPhased() || !targetSearch.advance(amount)) {
            return;
        }
        ShipAPI target = nearestEnemy();
        if (target == null) {
            return;
        }
        ArcDischarge.dischargeAt(ownerShip, target, arcCount, ENERGY_DAMAGE, EMP_DAMAGE);
        chargedSeconds = 0f;
        targetSearch = TickTimer.dueOnFirstAdvance(TARGET_SEARCH_SECONDS);
    }

    private ShipAPI nearestEnemy() {
        List<ShipAPI> nearest = CombatQueries.nearestFirst(
                CombatQueries.hostilesNear(ownerShip, ownerShip.getLocation(), RANGE, candidate -> !candidate.isFighter()),
                ownerShip.getLocation(), 1);
        return nearest.isEmpty() ? null : nearest.get(0);
    }
}
