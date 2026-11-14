package exiledsector.charts.layout;

import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

public final class ChartGeometry {

    private ChartGeometry() {
    }

    public static Vector2f pointAround(Vector2f centre, double radians, float radius) {
        return new Vector2f(centre.x + (float) Math.cos(radians) * radius, centre.y + (float) Math.sin(radians) * radius);
    }

    public static Vector2f pointAtRandomAngle(Vector2f centre, float radius, Random random) {
        return pointAround(centre, random.nextDouble() * Math.PI * 2d, radius);
    }

    public static float degreesFrom(Vector2f centre, Vector2f point) {
        return (float) Math.toDegrees(Math.atan2(point.y - centre.y, point.x - centre.x));
    }
}
