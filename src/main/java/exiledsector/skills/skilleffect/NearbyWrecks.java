package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

final class NearbyWrecks {

    private static final float SEARCH_MARGIN = 500f;

    private final ShipAPI ship;
    private final Predicate<ShipAPI> counts;
    private Set<ShipAPI> aliveNearby = new HashSet<>();

    NearbyWrecks(ShipAPI ship, Predicate<ShipAPI> counts) {
        this.ship = ship;
        this.counts = counts;
    }

    List<ShipAPI> newWithin(float range) {
        List<ShipAPI> wrecks = new ArrayList<>();
        Set<ShipAPI> stillAlive = new HashSet<>();
        for (ShipAPI other : CombatQueries.shipsNear(ship.getLocation(), range + SEARCH_MARGIN, other -> other != ship)) {
            if (other.isAlive() && !other.isHulk()) {
                if (counts.test(other)) {
                    stillAlive.add(other);
                }
            } else if (isWreck(other) && aliveNearby.contains(other)
                    && CombatQueries.withinRadius(other.getLocation(), ship.getLocation(), range)) {
                wrecks.add(other);
            }
        }
        aliveNearby = stillAlive;
        return wrecks;
    }

    private static boolean isWreck(ShipAPI other) {
        return other.isHulk() || other.getHitpoints() <= 0f;
    }
}
