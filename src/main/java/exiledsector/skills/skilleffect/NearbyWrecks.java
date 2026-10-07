package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

final class NearbyWrecks {

    private static final float SEARCH_MARGIN = 500f;

    private final ShipAPI ownerShip;
    private final Predicate<ShipAPI> wreckFilter;
    private Set<ShipAPI> aliveNearby = new HashSet<>();

    NearbyWrecks(ShipAPI ownerShip, Predicate<ShipAPI> wreckFilter) {
        this.ownerShip = ownerShip;
        this.wreckFilter = wreckFilter;
    }

    List<ShipAPI> newWithin(float range) {
        List<ShipAPI> wrecks = new ArrayList<>();
        Set<ShipAPI> stillAlive = new HashSet<>();
        for (ShipAPI other : CombatQueries.shipsNear(ownerShip.getLocation(), range + SEARCH_MARGIN, other -> other != ownerShip)) {
            if (other.isAlive() && !other.isHulk()) {
                if (wreckFilter.test(other)) {
                    stillAlive.add(other);
                }
            } else if (isWreck(other) && aliveNearby.contains(other)
                    && CombatQueries.withinRadius(other.getLocation(), ownerShip.getLocation(), range)) {
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
