package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.ShipAPI;
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
}
