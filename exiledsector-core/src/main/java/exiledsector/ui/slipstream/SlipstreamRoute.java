package exiledsector.ui.slipstream;

import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public final class SlipstreamRoute {

    private final List<Vector2f> points;
    private final float[] distanceAtPoint;
    private final float width;

    public SlipstreamRoute(List<Vector2f> routePoints, float width) {
        if (routePoints.size() < 2) {
            throw new IllegalArgumentException("A slipstream route needs at least two points");
        }
        List<Vector2f> copies = new ArrayList<>(routePoints.size());
        for (Vector2f point : routePoints) {
            copies.add(new Vector2f(point));
        }
        this.points = List.copyOf(copies);
        this.width = width;
        distanceAtPoint = new float[points.size()];
        for (int i = 1; i < points.size(); i++) {
            distanceAtPoint[i] = distanceAtPoint[i - 1] + Vector2f.sub(points.get(i), points.get(i - 1), null).length();
        }
    }

    public List<Vector2f> points() {
        return points;
    }

    public float width() {
        return width;
    }

    public float length() {
        return distanceAtPoint[distanceAtPoint.length - 1];
    }

    public Vector2f pointAt(float distance) {
        int index = segmentIndexAt(distance);
        Vector2f from = points.get(index);
        Vector2f to = points.get(index + 1);
        float segmentLength = distanceAtPoint[index + 1] - distanceAtPoint[index];
        float along = segmentLength <= 0f ? 0f : clamp((distance - distanceAtPoint[index]) / segmentLength);
        return new Vector2f(from.x + (to.x - from.x) * along, from.y + (to.y - from.y) * along);
    }

    public Vector2f headingAt(float distance) {
        int index = segmentIndexAt(distance);
        Vector2f heading = Vector2f.sub(points.get(index + 1), points.get(index), null);
        if (heading.lengthSquared() <= 0f) {
            return new Vector2f(1f, 0f);
        }
        heading.normalise();
        return heading;
    }

    public void offsetPoint(float distance, float lateralOffset, Vector2f out) {
        int index = segmentIndexAt(distance);
        Vector2f from = points.get(index);
        Vector2f to = points.get(index + 1);
        float segmentLength = distanceAtPoint[index + 1] - distanceAtPoint[index];
        float along = segmentLength <= 0f ? 0f : clamp((distance - distanceAtPoint[index]) / segmentLength);
        float headingX = segmentLength <= 0f ? 1f : (to.x - from.x) / segmentLength;
        float headingY = segmentLength <= 0f ? 0f : (to.y - from.y) / segmentLength;
        out.set(from.x + (to.x - from.x) * along - headingY * lateralOffset, from.y + (to.y - from.y) * along + headingX * lateralOffset);
    }

    private int segmentIndexAt(float distance) {
        int low = 0;
        int high = points.size() - 2;
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (distanceAtPoint[middle] <= distance) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
