package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.IdentityHashMap;
import java.util.Map;

final class TerrifyingPresenceListener extends ShipCombatListener implements AdvanceableListener {

    static final String ACCURACY_PENALTY_PERCENT_KEY = "exiledSector_terrifyingPresenceAccuracyPenaltyPercent";
    static final float RANGE = 1000f;
    private static final float UPDATE_SECONDS = 0.25f;
    private static final String MOD_ID_PREFIX = "exiledSector_terrifyingPresence_";

    private final IntervalUtil updateInterval = new IntervalUtil(UPDATE_SECONDS, UPDATE_SECONDS);
    private final AppliedAuraValues affectedEnemies = new AppliedAuraValues(AppliedAuraValues.Change.ALWAYS);

    TerrifyingPresenceListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public void advance(float amount) {
        updateInterval.advance(amount);
        if (!updateInterval.intervalElapsed()) {
            return;
        }
        float penalty = isPresent() ? magnitude(ACCURACY_PENALTY_PERCENT_KEY) / 100f : 0f;
        Map<ShipAPI, Float> penaltyByEnemy = new IdentityHashMap<>();
        if (penalty > 0f) {
            for (ShipAPI enemy : CombatQueries.shipsNear(ownerShip.getLocation(), RANGE, this::isTerrified)) {
                penaltyByEnemy.put(enemy, penalty);
            }
        }
        affectedEnemies.update(penaltyByEnemy, enemy -> enemy.getMutableStats().getAutofireAimAccuracy().unmodify(modId),
                (enemy, enemyPenalty) -> enemy.getMutableStats().getAutofireAimAccuracy().modifyFlat(modId, -enemyPenalty));
    }

    private boolean isPresent() {
        return ownerIsAliveNotHulk() && !ownerShip.isRetreating();
    }

    private boolean isTerrified(ShipAPI other) {
        return CombatQueries.isAliveNotHulk(other) && CombatQueries.isHostile(ownerShip, other)
                && CombatQueries.withinRadius(other.getLocation(), ownerShip.getLocation(), RANGE);
    }
}
