package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

final class Refraction {

    private static final float EXIT_MARGIN = 10f;

    private Refraction() {
    }

    static Vector2f origin(ShipAPI hitShip, Vector2f impactPoint, Vector2f towards) {
        Vector2f direction = Vector2f.sub(towards, impactPoint, null);
        if (direction.lengthSquared() <= 0f) {
            return new Vector2f(impactPoint);
        }
        direction.normalise();
        float exitDistance = exitDistance(hitShip.getShieldCenterEvenIfNoShield(), blockingRadius(hitShip), impactPoint, direction);
        if (exitDistance <= 0f) {
            return new Vector2f(impactPoint);
        }
        float travel = exitDistance + EXIT_MARGIN;
        return new Vector2f(impactPoint.x + direction.x * travel, impactPoint.y + direction.y * travel);
    }

    static float exitDistance(Vector2f center, float radius, Vector2f point, Vector2f direction) {
        Vector2f offset = Vector2f.sub(point, center, null);
        float along = Vector2f.dot(offset, direction);
        float discriminant = along * along - (offset.lengthSquared() - radius * radius);
        return discriminant < 0f ? 0f : -along + (float) Math.sqrt(discriminant);
    }

    private static float blockingRadius(ShipAPI ship) {
        ShieldAPI shield = ship.getShield();
        return shield != null && shield.isOn() ? shield.getRadius() : ship.getCollisionRadius();
    }
}
