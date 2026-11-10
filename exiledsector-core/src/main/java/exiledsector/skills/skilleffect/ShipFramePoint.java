package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

public record ShipFramePoint(ShipAPI ship, float localX, float localY) {

    public static ShipFramePoint of(ShipAPI ship, Vector2f worldPoint) {
        double radians = Math.toRadians(-ship.getFacing());
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float dx = worldPoint.x - ship.getLocation().x;
        float dy = worldPoint.y - ship.getLocation().y;
        return new ShipFramePoint(ship, dx * cos - dy * sin, dx * sin + dy * cos);
    }

    public Vector2f worldPoint() {
        double radians = Math.toRadians(ship.getFacing());
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        return new Vector2f(ship.getLocation().x + localX * cos - localY * sin, ship.getLocation().y + localX * sin + localY * cos);
    }
}
