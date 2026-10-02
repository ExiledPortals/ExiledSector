package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.HashSet;
import java.util.Set;

final class NearbyDestructionHeartless implements AdvanceableListener {

    static final String RANGE_KEY = "exiledSector_heartlessNearbyDestructionRange";
    private static final float CHECK_SECONDS = 0.25f;
    private static final float SEARCH_MARGIN = 500f;

    private final ShipAPI ship;
    private final IntervalUtil interval = new IntervalUtil(CHECK_SECONDS, CHECK_SECONDS);
    private Set<ShipAPI> aliveNearby = new HashSet<>();

    NearbyDestructionHeartless(ShipAPI ship) {
        this.ship = ship;
    }

    @Override
    public void advance(float amount) {
        if (!ship.isAlive() || ship.isHulk()) {
            return;
        }
        interval.advance(amount);
        if (!interval.intervalElapsed()) {
            return;
        }
        float range = ship.getMutableStats().getDynamic().getValue(RANGE_KEY, 0f);
        if (range <= 0f) {
            return;
        }
        int wrecks = 0;
        Set<ShipAPI> stillAlive = new HashSet<>();
        for (ShipAPI other : CombatQueries.shipsNear(ship.getLocation(), range + SEARCH_MARGIN, this::counts)) {
            if (other.isAlive() && !other.isHulk()) {
                stillAlive.add(other);
            } else if (isWreck(other) && aliveNearby.contains(other)
                    && CombatQueries.withinRadius(other.getLocation(), ship.getLocation(), range)) {
                wrecks++;
            }
        }
        aliveNearby = stillAlive;
        if (wrecks > 0) {
            HeartlessStacks stacks = HeartlessStacks.of(ship);
            for (int i = 0; i < wrecks; i++) {
                stacks.gain();
            }
        }
    }

    private boolean counts(ShipAPI other) {
        return other != ship && !other.isFighter() && other.getParentStation() == null;
    }

    private static boolean isWreck(ShipAPI other) {
        return other.isHulk() || other.getHitpoints() <= 0f;
    }
}
