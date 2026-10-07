package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.HashSet;
import java.util.Set;

final class TerrifyingPresenceListener extends ShipCombatListener implements AdvanceableListener {

    static final String ACCURACY_PENALTY_PERCENT_KEY = "exiledSector_terrifyingPresenceAccuracyPenaltyPercent";
    static final float RANGE = 1000f;
    private static final float UPDATE_SECONDS = 0.25f;
    private static final String MOD_ID_PREFIX = "exiledSector_terrifyingPresence_";

    private final IntervalUtil updateInterval = new IntervalUtil(UPDATE_SECONDS, UPDATE_SECONDS);
    private Set<ShipAPI> affectedEnemies = new HashSet<>();

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
        Set<ShipAPI> inRange = penalty > 0f ? new HashSet<>(CombatQueries.shipsMatching(this::isTerrified)) : new HashSet<>();
        for (ShipAPI previous : affectedEnemies) {
            if (!inRange.contains(previous)) {
                previous.getMutableStats().getAutofireAimAccuracy().unmodify(modId);
            }
        }
        for (ShipAPI enemy : inRange) {
            enemy.getMutableStats().getAutofireAimAccuracy().modifyFlat(modId, -penalty);
        }
        affectedEnemies = inRange;
    }

    private boolean isPresent() {
        return ownerIsAliveNotHulk() && !ownerShip.isRetreating();
    }

    private boolean isTerrified(ShipAPI other) {
        return CombatQueries.isAliveNotHulk(other) && CombatQueries.isHostile(ownerShip, other)
                && CombatQueries.withinRadius(other.getLocation(), ownerShip.getLocation(), RANGE);
    }
}
