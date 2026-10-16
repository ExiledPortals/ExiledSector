package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BoundsAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

final class CombatQueries {

    private CombatQueries() {
    }

    static boolean isAliveNotHulk(ShipAPI ship) {
        return ship.isAlive() && !ship.isHulk();
    }

    static boolean isHostile(ShipAPI source, ShipAPI other) {
        return other.getOwner() != source.getOwner() && other.getOwner() != Misc.OWNER_NEUTRAL;
    }

    static float distanceSquared(Vector2f a, Vector2f b) {
        float deltaX = a.x - b.x;
        float deltaY = a.y - b.y;
        return deltaX * deltaX + deltaY * deltaY;
    }

    static boolean withinRadius(Vector2f a, Vector2f b, float radius) {
        return distanceSquared(a, b) <= radius * radius;
    }

    static float hitDamage(DamageAPI damage) {
        return damage.isDps() ? damage.getDamage() * damage.getDpsDuration() : damage.getDamage();
    }

    static List<ShipAPI> shipsMatching(Predicate<ShipAPI> filter) {
        List<ShipAPI> result = new ArrayList<>();
        for (ShipAPI ship : Global.getCombatEngine().getShips()) {
            if (filter.test(ship)) {
                result.add(ship);
            }
        }
        return result;
    }

    static List<ShipAPI> shipsNear(Vector2f loc, float radius, Predicate<ShipAPI> filter) {
        List<ShipAPI> result = new ArrayList<>();
        Set<ShipAPI> seen = Collections.newSetFromMap(new IdentityHashMap<>(8));
        Iterator<Object> iterator = objectsNear(Global.getCombatEngine().getShipGrid(), loc, radius);
        while (iterator.hasNext()) {
            if (iterator.next() instanceof ShipAPI other && seen.add(other) && filter.test(other)) {
                result.add(other);
            }
        }
        return result;
    }

    static List<ShipAPI> hostilesNear(ShipAPI owner, Vector2f point, float radius, Predicate<ShipAPI> extraFilter) {
        return shipsNear(point, radius, other -> extraFilter.test(other) && isAliveNotHulk(other)
                && other.getCollisionClass() != CollisionClass.NONE && isHostile(owner, other) && withinRadius(other.getLocation(), point, radius));
    }

    static List<ShipAPI> nearestFirst(List<ShipAPI> ships, Vector2f point, int count) {
        int shipCount = ships.size();
        int keptCount = Math.max(0, Math.min(count, shipCount));
        float[] distancesSquared = new float[shipCount];
        for (int index = 0; index < shipCount; index++) {
            distancesSquared[index] = distanceSquared(ships.get(index).getLocation(), point);
        }
        for (int slot = 0; slot < keptCount; slot++) {
            int nearest = slot;
            for (int index = slot + 1; index < shipCount; index++) {
                if (distancesSquared[index] < distancesSquared[nearest]) {
                    nearest = index;
                }
            }
            if (nearest != slot) {
                Collections.swap(ships, slot, nearest);
                float slotDistance = distancesSquared[slot];
                distancesSquared[slot] = distancesSquared[nearest];
                distancesSquared[nearest] = slotDistance;
            }
        }
        return keptCount == shipCount ? ships : ships.subList(0, keptCount);
    }

    static boolean anyNear(CollisionGridAPI grid, Vector2f loc, float radius, Predicate<Object> test) {
        Iterator<Object> iterator = objectsNear(grid, loc, radius);
        while (iterator.hasNext()) {
            if (test.test(iterator.next())) {
                return true;
            }
        }
        return false;
    }

    static Iterator<Object> objectsNear(CollisionGridAPI grid, Vector2f loc, float radius) {
        return grid.getCheckIterator(loc, radius * 2f, radius * 2f);
    }

    static ShipAPI biggestEnemyShip(ShipAPI source) {
        ShipAPI biggestShip = null;
        ShipAPI biggestStation = null;
        for (ShipAPI candidate : Global.getCombatEngine().getShips()) {
            if (!isRammable(source, candidate)) {
                continue;
            }
            if (candidate.isStation() || candidate.isStationModule()) {
                biggestStation = bigger(candidate, biggestStation, source.getLocation());
            } else {
                biggestShip = bigger(candidate, biggestShip, source.getLocation());
            }
        }
        return biggestShip != null ? biggestShip : biggestStation;
    }

    private static boolean isRammable(ShipAPI source, ShipAPI candidate) {
        return candidate != source && isHostile(source, candidate) && isAliveNotHulk(candidate)
                && !candidate.isFighter() && !candidate.isDrone() && !candidate.isShuttlePod();
    }

    private static ShipAPI bigger(ShipAPI candidate, ShipAPI best, Vector2f from) {
        if (best == null) {
            return candidate;
        }
        int sizeOrder = Integer.compare(candidate.getHullSize().ordinal(), best.getHullSize().ordinal());
        if (sizeOrder != 0) {
            return sizeOrder > 0 ? candidate : best;
        }
        int costOrder = Float.compare(deploymentCost(candidate), deploymentCost(best));
        if (costOrder != 0) {
            return costOrder > 0 ? candidate : best;
        }
        return distanceSquared(candidate.getLocation(), from) < distanceSquared(best.getLocation(), from) ? candidate : best;
    }

    private static float deploymentCost(ShipAPI ship) {
        FleetMemberAPI member = ship.getFleetMember();
        if (member != null) {
            return member.getDeploymentPointsCost();
        }
        return ship.getHullSpec() == null ? 0f : ship.getHullSpec().getFleetPoints();
    }

    static boolean boundsOverlap(CombatEntityAPI first, CombatEntityAPI second) {
        BoundsAPI firstBounds = first.getExactBounds();
        BoundsAPI secondBounds = second.getExactBounds();
        if (firstBounds == null || secondBounds == null) {
            return false;
        }
        firstBounds.update(first.getLocation(), first.getFacing());
        secondBounds.update(second.getLocation(), second.getFacing());
        List<BoundsAPI.SegmentAPI> firstSegments = firstBounds.getSegments();
        List<BoundsAPI.SegmentAPI> secondSegments = secondBounds.getSegments();
        for (BoundsAPI.SegmentAPI firstSegment : firstSegments) {
            for (BoundsAPI.SegmentAPI secondSegment : secondSegments) {
                if (segmentsIntersect(firstSegment.getP1(), firstSegment.getP2(), secondSegment.getP1(), secondSegment.getP2())) {
                    return true;
                }
            }
        }
        return isInside(first.getLocation(), secondSegments) || isInside(second.getLocation(), firstSegments);
    }

    private static boolean isInside(Vector2f point, List<BoundsAPI.SegmentAPI> polygon) {
        if (polygon.size() < 3) {
            return false;
        }
        boolean inside = false;
        for (BoundsAPI.SegmentAPI segment : polygon) {
            Vector2f start = segment.getP1();
            Vector2f end = segment.getP2();
            if ((start.y > point.y) != (end.y > point.y)
                    && point.x < start.x + (point.y - start.y) * (end.x - start.x) / (end.y - start.y)) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static boolean segmentsIntersect(Vector2f p1, Vector2f p2, Vector2f p3, Vector2f p4) {
        float d1 = cross(p3, p4, p1);
        float d2 = cross(p3, p4, p2);
        float d3 = cross(p1, p2, p3);
        float d4 = cross(p1, p2, p4);
        return ((d1 > 0f) != (d2 > 0f)) && ((d3 > 0f) != (d4 > 0f));
    }

    private static float cross(Vector2f a, Vector2f b, Vector2f c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }
}
