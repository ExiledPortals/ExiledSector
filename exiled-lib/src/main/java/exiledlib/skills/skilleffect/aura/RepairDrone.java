package exiledlib.skills.skilleffect.aura;

import com.fs.starfarer.api.util.Misc;
import exiledlib.skills.skilleffect.CombatQueries;
import org.lwjgl.util.vector.Vector2f;

final class RepairDrone {

    static final float SPEED = 420f;
    static final float HOVER_RADIUS = 24f;
    static final float HOVER_RADIANS_PER_SECOND = 2.5f;
    static final float WORKING_DISTANCE = HOVER_RADIUS * 1.5f;
    private static final float TURN_THRESHOLD = 0.5f;

    private final Vector2f position = new Vector2f();
    private float facingDeg;
    private float hoverAngleRad;
    private boolean docked = true;
    private boolean working;

    RepairDrone(float hoverAngleRad) {
        this.hoverAngleRad = hoverAngleRad;
    }

    void advance(float amount, Vector2f home, float dockRadius, Vector2f workPoint) {
        if (docked) {
            if (workPoint == null) {
                return;
            }
            docked = false;
            position.set(home);
        }
        Vector2f destination;
        if (workPoint != null) {
            hoverAngleRad += HOVER_RADIANS_PER_SECOND * amount;
            destination = new Vector2f(workPoint.x + (float) Math.cos(hoverAngleRad) * HOVER_RADIUS,
                    workPoint.y + (float) Math.sin(hoverAngleRad) * HOVER_RADIUS);
        } else {
            destination = home;
        }
        moveToward(destination, SPEED * amount);
        working = workPoint != null && CombatQueries.withinRadius(position, workPoint, WORKING_DISTANCE);
        if (working) {
            facingDeg = Misc.getAngleInDegreesStrict(position, workPoint);
        }
        if (workPoint == null && CombatQueries.withinRadius(position, home, dockRadius)) {
            docked = true;
        }
    }

    private void moveToward(Vector2f destination, float maxStep) {
        float deltaX = destination.x - position.x;
        float deltaY = destination.y - position.y;
        float distance = (float) Math.hypot(deltaX, deltaY);
        if (distance <= 0f) {
            return;
        }
        float step = Math.min(distance, maxStep);
        position.set(position.x + deltaX / distance * step, position.y + deltaY / distance * step);
        if (step > TURN_THRESHOLD) {
            facingDeg = (float) Math.toDegrees(Math.atan2(deltaY, deltaX));
        }
    }

    Vector2f position() {
        return position;
    }

    float facingDeg() {
        return facingDeg;
    }

    boolean isDocked() {
        return docked;
    }

    boolean isWorking() {
        return working;
    }
}
