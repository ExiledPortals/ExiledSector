package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

    static boolean withinRadius(Vector2f a, Vector2f b, float radius) {
        return Vector2f.sub(a, b, null).lengthSquared() <= radius * radius;
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
        Iterator<Object> iterator = gridIterator(Global.getCombatEngine().getShipGrid(), loc, radius);
        while (iterator.hasNext()) {
            if (iterator.next() instanceof ShipAPI other && !result.contains(other) && filter.test(other)) {
                result.add(other);
            }
        }
        return result;
    }

    static boolean anyNear(CollisionGridAPI grid, Vector2f loc, float radius, Predicate<Object> test) {
        Iterator<Object> iterator = gridIterator(grid, loc, radius);
        while (iterator.hasNext()) {
            if (test.test(iterator.next())) {
                return true;
            }
        }
        return false;
    }

    private static Iterator<Object> gridIterator(CollisionGridAPI grid, Vector2f loc, float radius) {
        return grid.getCheckIterator(loc, radius * 2f, radius * 2f);
    }
}
