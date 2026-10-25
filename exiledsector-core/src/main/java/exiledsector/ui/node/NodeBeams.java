package exiledsector.ui.node;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public final class NodeBeams {

    public enum RingLight {
        LIT, BREATHING, DIM, DULL
    }

    public static final BeamStroke.Style LANE_STYLE = new BeamStroke.Style("graphics/fx/beamfringe.png", "graphics/fx/beamcore.png", 14f, 5f, 4f, 30f);
    public static final BeamStroke.Style RING_STYLE = new BeamStroke.Style("graphics/fx/beamfringeb.png", "graphics/fx/beamcorec.png", 12f, 4f, 4f, 20f);
    public static final BeamStroke.Look DULL_LOOK = new BeamStroke.Look(NodeMetrics.RING_DULL_COLOR, NodeMetrics.RING_DULL_COLOR, 0.6f, 0.3f);
    public static final float DULL_ALPHA = 0.4f;
    public static final float DIM_ALPHA = 0.4f;
    static final float WIDTH_SHARE = 0.5f;
    static final float MIN_NODE_ZOOM = 0.4f;
    static final float LIT_FRINGE_ALPHA = 0.8f;
    static final float LIT_CORE_ALPHA = 0.9f;
    static final float WHITENESS = 0.7f;

    private final BeamStroke stroke = new BeamStroke();
    private final NodeRingArt ringArt;
    private final Map<Color, BeamStroke.Look> litLooks = new HashMap<>();

    public NodeBeams(NodeRingArt ringArt) {
        this.ringArt = ringArt;
    }

    public static float widthScale(float nodeZoom) {
        return Math.max(nodeZoom, MIN_NODE_ZOOM) * WIDTH_SHARE;
    }

    public static float laneEdge(float widthScale) {
        return LANE_STYLE.fringeWidth() * widthScale / 2f;
    }

    public static float ringEdge(float widthScale) {
        return RING_STYLE.fringeWidth() * widthScale / 2f;
    }

    public static Color whitened(Color colour) {
        return new Color(towardWhite(colour.getRed()), towardWhite(colour.getGreen()), towardWhite(colour.getBlue()));
    }

    private static int towardWhite(int channel) {
        return Math.round(channel + (255 - channel) * WHITENESS);
    }

    public BeamStroke.Look lit(Color colour) {
        return litLooks.computeIfAbsent(colour, base -> new BeamStroke.Look(base, whitened(base), LIT_FRINGE_ALPHA, LIT_CORE_ALPHA));
    }

    public void nodeRing(float cx, float cy, float radius, float[] outlineRadii, RingLight light, Color colour, String pulseKey, float alpha,
                         float widthScale) {
        BeamStroke.Look look = light == RingLight.DULL ? DULL_LOOK : lit(colour);
        ring(cx, cy, radius, outlineRadii, look, lightAlpha(light) * alpha, widthScale);
        pulse(cx, cy, radius, outlineRadii, colour, pulseKey, alpha, widthScale);
    }

    public void pulse(float cx, float cy, float radius, float[] outlineRadii, Color colour, String pulseKey, float alpha, float widthScale) {
        float progress = ringArt.pulseProgress(pulseKey);
        if (progress >= 0f) {
            ring(cx, cy, radius * NodeRingArt.pulseRadiusFraction(progress), outlineRadii, lit(colour), (1f - progress) * alpha, widthScale);
        }
    }

    public float lightAlpha(RingLight light) {
        return switch (light) {
            case LIT -> 1f;
            case BREATHING -> ringArt.breathing();
            case DIM -> DIM_ALPHA;
            case DULL -> DULL_ALPHA;
        };
    }

    public void ring(float cx, float cy, float radius, float[] outlineRadii, BeamStroke.Look look, float alpha, float widthScale) {
        stroke.addRing(cx, cy, radius, outlineRadii, RING_STYLE, look, alpha, ringArt.elapsedSeconds(), widthScale);
    }

    public boolean connector(float fromX, float fromY, float fromRadius, float toX, float toY, float toRadius, BeamStroke.Look look, float startAlpha,
                             float endAlpha, float widthScale) {
        float dx = toX - fromX;
        float dy = toY - fromY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= fromRadius + toRadius) {
            return false;
        }
        float dirX = dx / length;
        float dirY = dy / length;
        lane(fromX + dirX * fromRadius, fromY + dirY * fromRadius, toX - dirX * toRadius, toY - dirY * toRadius, 0f, look, startAlpha, endAlpha,
                widthScale);
        return true;
    }

    public void lane(float fromX, float fromY, float toX, float toY, float distanceOffset, BeamStroke.Look look, float startAlpha, float endAlpha,
                     float widthScale) {
        stroke.addLine(fromX, fromY, toX, toY, distanceOffset, LANE_STYLE, look, startAlpha, endAlpha, ringArt.elapsedSeconds(), widthScale);
    }

    public void flush() {
        stroke.flush();
    }
}
