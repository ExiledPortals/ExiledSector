package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ArcDischarge;
import exiledsector.skills.skilleffect.Chance;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.TickTimer;

import java.util.List;
import java.util.Random;

final class EntropyProjectorListener extends ShipCombatListener implements AdvanceableListener {

    static final String ARC_COUNT_KEY = "exiledSector_entropyProjectorArcs";
    static final String CHAIN_CHANCE_KEY = "exiledSector_entropyProjectorChainChance";
    static final float CHARGE_SECONDS = 10f;
    static final float RANGE = 1000f;
    static final float ENERGY_DAMAGE = 300f;
    static final float EMP_DAMAGE = 400f;
    static final float TARGET_SEARCH_SECONDS = 0.5f;

    private final Random random;
    private TickTimer targetSearch = TickTimer.dueOnFirstAdvance(TARGET_SEARCH_SECONDS);
    private float chargedSeconds;

    EntropyProjectorListener(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    EntropyProjectorListener(ShipAPI ownerShip, Random random) {
        super(ownerShip);
        this.random = random;
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
        ShipAPI target = nearestEnemyTo(ownerShip, null);
        if (target == null) {
            return;
        }
        float chainChance = magnitude(CHAIN_CHANCE_KEY);
        for (int arc = 0; arc < arcCount; arc++) {
            ArcDischarge.dischargeAt(ownerShip, target, 1, ENERGY_DAMAGE, EMP_DAMAGE);
            if (Chance.roll(random, chainChance)) {
                chain(target);
            }
        }
        chargedSeconds = 0f;
        targetSearch = TickTimer.dueOnFirstAdvance(TARGET_SEARCH_SECONDS);
    }

    private void chain(ShipAPI struckShip) {
        ShipAPI nextTarget = nearestEnemyTo(struckShip, struckShip);
        if (nextTarget != null) {
            ArcDischarge.chainFrom(ownerShip, struckShip, nextTarget, ENERGY_DAMAGE, EMP_DAMAGE);
        }
    }

    private ShipAPI nearestEnemyTo(ShipAPI centre, ShipAPI excluded) {
        List<ShipAPI> nearest = CombatQueries.nearestFirst(
                CombatQueries.hostilesNear(ownerShip, centre.getLocation(), RANGE, candidate -> !candidate.isFighter() && candidate != excluded),
                centre.getLocation(), 1);
        return nearest.isEmpty() ? null : nearest.get(0);
    }
}
