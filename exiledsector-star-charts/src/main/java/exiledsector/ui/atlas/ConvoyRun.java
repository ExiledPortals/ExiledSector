package exiledsector.ui.atlas;

final class ConvoyRun {

    static final float SPEED = 55f;
    static final float DOCK_SECONDS = 2.5f;
    static final float LANE_OFFSET = 9f;
    static final float FADE_DISTANCE = 45f;
    static final float CORNER_BLEND = 20f;

    record Position(float x, float y, float headingDegrees, float visibility) {
    }

    private final ConvoyRoute route;
    private final float[] reached;
    private final float[] tangentXs;
    private final float[] tangentYs;
    private final float length;
    private final float travelSeconds;
    private final float cycleSeconds;
    private final float phaseSeconds;

    ConvoyRun(float portX, float portY, float worldX, float worldY, int order, int count) {
        this(ConvoyRoute.through(new float[]{portX, worldX}, new float[]{portY, worldY}), order, count);
    }

    ConvoyRun(ConvoyRoute route, int order, int count) {
        this.route = route;
        int points = route.size();
        this.reached = new float[points];
        for (int point = 1; point < points; point++) {
            reached[point] = reached[point - 1] + (float) Math.hypot(route.x(point) - route.x(point - 1), route.y(point) - route.y(point - 1));
        }
        this.length = points == 0 ? 0f : reached[points - 1];
        this.tangentXs = new float[points];
        this.tangentYs = new float[points];
        for (int point = 0; point < points; point++) {
            float tangentX = segmentDirection(point - 1, true) + segmentDirection(point, true);
            float tangentY = segmentDirection(point - 1, false) + segmentDirection(point, false);
            float size = (float) Math.hypot(tangentX, tangentY);
            tangentXs[point] = size > 0f ? tangentX / size : 1f;
            tangentYs[point] = size > 0f ? tangentY / size : 0f;
        }
        this.travelSeconds = length / SPEED;
        this.cycleSeconds = Math.max(DOCK_SECONDS, 2f * (travelSeconds + DOCK_SECONDS));
        this.phaseSeconds = count <= 0 ? 0f : cycleSeconds * order / count;
    }

    private float segmentDirection(int segment, boolean alongX) {
        if (segment < 0 || segment + 1 >= route.size()) {
            return 0f;
        }
        float deltaX = route.x(segment + 1) - route.x(segment);
        float deltaY = route.y(segment + 1) - route.y(segment);
        float size = (float) Math.hypot(deltaX, deltaY);
        if (size <= 0f) {
            return 0f;
        }
        return (alongX ? deltaX : deltaY) / size;
    }

    float cycleSeconds() {
        return cycleSeconds;
    }

    float length() {
        return length;
    }

    Position along(float distance) {
        return onLane(Math.max(0f, Math.min(length, distance)), true, true);
    }

    Position at(float seconds) {
        float cycle = ((seconds + phaseSeconds) % cycleSeconds + cycleSeconds) % cycleSeconds;
        if (cycle < travelSeconds) {
            return onLane(cycle / travelSeconds * length, true, true);
        }
        if (cycle < travelSeconds + DOCK_SECONDS) {
            return onLane(length, false, false);
        }
        if (cycle < 2f * travelSeconds + DOCK_SECONDS) {
            return onLane((1f - (cycle - travelSeconds - DOCK_SECONDS) / travelSeconds) * length, false, true);
        }
        return onLane(0f, true, false);
    }

    private Position onLane(float distance, boolean outbound, boolean underWay) {
        int segment = segmentAt(distance);
        int next = Math.min(segment + 1, route.size() - 1);
        float span = reached[next] - reached[segment];
        float share = span > 0f ? (distance - reached[segment]) / span : 0f;
        float lineX = route.x(segment) + (route.x(next) - route.x(segment)) * share;
        float lineY = route.y(segment) + (route.y(next) - route.y(segment)) * share;
        float fromStart = distance - reached[segment];
        float toEnd = reached[next] - distance;
        int corner = fromStart <= toEnd ? segment : next;
        float blend = Math.min(CORNER_BLEND, span / 2f);
        float straightShare = blend > 0f ? Math.min(1f, Math.min(fromStart, toEnd) / blend) : 1f;
        float directionX = span > 0f ? segmentDirection(segment, true) : tangentXs[corner];
        float directionY = span > 0f ? segmentDirection(segment, false) : tangentYs[corner];
        float tangentX = tangentXs[corner] + (directionX - tangentXs[corner]) * straightShare;
        float tangentY = tangentYs[corner] + (directionY - tangentYs[corner]) * straightShare;
        float size = (float) Math.hypot(tangentX, tangentY);
        if (size > 0f) {
            tangentX /= size;
            tangentY /= size;
        }
        float side = outbound ? LANE_OFFSET : -LANE_OFFSET;
        float x = lineX + tangentY * side;
        float y = lineY - tangentX * side;
        float sign = outbound ? 1f : -1f;
        float heading = (float) Math.toDegrees(Math.atan2(-tangentY * sign, tangentX * sign));
        float visibility = underWay ? Math.max(0f, Math.min(1f, Math.min(distance, length - distance) / FADE_DISTANCE)) : 0f;
        return new Position(underWay ? x : lineX, underWay ? y : lineY, heading, visibility);
    }

    private int segmentAt(float distance) {
        int low = 0;
        int high = Math.max(0, route.size() - 2);
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (reached[middle] <= distance) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }
}
