package exiledsector.charts.anomaly.storm;

import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;

final class StormStrikes {

    static final float MIN_ORIGIN_DISTANCE = 600f;
    static final float MAX_ORIGIN_DISTANCE = 900f;

    private StormStrikes() {
    }

    static boolean canBeStruck(ShipAPI ship) {
        return ship != null && ship.isAlive() && !ship.isHulk() && !ship.isFighter() && !ship.isPhased() && ship.getParentStation() == null
                && StormRules.hullWeight(ship.getHullSize()) > 0f;
    }

    static ShipAPI pickTarget(List<ShipAPI> ships, Random random) {
        float totalWeight = 0f;
        for (ShipAPI ship : ships) {
            if (canBeStruck(ship)) {
                totalWeight += StormRules.hullWeight(ship.getHullSize());
            }
        }
        if (!(totalWeight > 0f)) {
            return null;
        }
        float pick = random.nextFloat() * totalWeight;
        ShipAPI last = null;
        for (ShipAPI ship : ships) {
            if (!canBeStruck(ship)) {
                continue;
            }
            last = ship;
            pick -= StormRules.hullWeight(ship.getHullSize());
            if (pick < 0f) {
                return ship;
            }
        }
        return last;
    }

    static Vector2f origin(Vector2f target, Random random) {
        double angle = random.nextDouble() * Math.PI * 2d;
        float distance = MIN_ORIGIN_DISTANCE + random.nextFloat() * (MAX_ORIGIN_DISTANCE - MIN_ORIGIN_DISTANCE);
        return new Vector2f(target.x + (float) Math.cos(angle) * distance, target.y + (float) Math.sin(angle) * distance);
    }
}
