package exiledsector.ui.atlas;

final class ConvoyRun {

    static final float SPEED = 55f;
    static final float DOCK_SECONDS = 2.5f;
    static final float LANE_OFFSET = 14f;
    static final float FADE_DISTANCE = 45f;

    record Position(float x, float y, float headingDegrees, float visibility) {
    }

    private final float portX;
    private final float portY;
    private final float alongX;
    private final float alongY;
    private final float length;
    private final float travelSeconds;
    private final float cycleSeconds;
    private final float phaseSeconds;

    ConvoyRun(float portX, float portY, float worldX, float worldY, int order, int count) {
        this.portX = portX;
        this.portY = portY;
        float deltaX = worldX - portX;
        float deltaY = worldY - portY;
        this.length = (float) Math.hypot(deltaX, deltaY);
        this.alongX = length > 0f ? deltaX / length : 1f;
        this.alongY = length > 0f ? deltaY / length : 0f;
        this.travelSeconds = length / SPEED;
        this.cycleSeconds = 2f * (travelSeconds + DOCK_SECONDS);
        this.phaseSeconds = count <= 0 ? 0f : cycleSeconds * order / count;
    }

    float cycleSeconds() {
        return cycleSeconds;
    }

    Position at(float seconds) {
        float cycle = ((seconds + phaseSeconds) % cycleSeconds + cycleSeconds) % cycleSeconds;
        if (cycle < travelSeconds) {
            return onLane(cycle / travelSeconds, true);
        }
        if (cycle < travelSeconds + DOCK_SECONDS) {
            return docked(1f, false);
        }
        if (cycle < 2f * travelSeconds + DOCK_SECONDS) {
            return onLane(1f - (cycle - travelSeconds - DOCK_SECONDS) / travelSeconds, false);
        }
        return docked(0f, true);
    }

    private Position onLane(float share, boolean outbound) {
        float side = outbound ? LANE_OFFSET : -LANE_OFFSET;
        float distance = share * length;
        float x = portX + alongX * distance + alongY * side;
        float y = portY + alongY * distance - alongX * side;
        float visibility = Math.min(1f, Math.min(distance, length - distance) / FADE_DISTANCE);
        return new Position(x, y, headingOf(outbound), Math.max(0f, visibility));
    }

    private Position docked(float share, boolean nextOutbound) {
        return new Position(portX + alongX * share * length, portY + alongY * share * length, headingOf(nextOutbound), 0f);
    }

    private float headingOf(boolean outbound) {
        float sign = outbound ? 1f : -1f;
        return (float) Math.toDegrees(Math.atan2(-alongY * sign, alongX * sign));
    }
}
