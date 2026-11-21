package exiledsector.ui.atlas;

import exiledsector.ui.node.BeamStroke;
import exiledsector.ui.node.NodeBeams;
import exiledsector.ui.node.NodeRingArt;

import java.awt.Color;

final class OfficerBoardBeams {

    static final Color LINK_COLOUR = Color.WHITE;
    static final Color BATTLE_COLOUR = new Color(255, 64, 56);
    static final Color IMPENDING_COLOUR = new Color(70, 150, 255);
    static final float LINK_WIDTH_SCALE = 0.3f;
    static final float ARROW_WIDTH_SCALE = 0.45f;
    static final float HELIX_ALPHA = 0.9f;
    static final float RESTING_LINK_ALPHA = 0.6f;
    static final float HIGHLIGHTED_LINK_ALPHA = 1f;
    static final float ARROWHEAD_SPREAD = 0.5f;
    static final float FADE_REACH = 0.75f;

    private final NodeRingArt ringArt = new NodeRingArt();
    private final NodeBeams beams = new NodeBeams(ringArt);

    void advance(float amount) {
        ringArt.advance(amount);
    }

    void link(float fromX, float fromY, float toX, float toY, boolean highlighted, float alpha) {
        float linkAlpha = linkAlpha(highlighted, alpha);
        beams.lane(fromX, fromY, toX, toY, 0f, beams.lit(LINK_COLOUR), linkAlpha, linkAlpha, LINK_WIDTH_SCALE);
    }

    void fadingLink(float fromX, float fromY, float toX, float toY, boolean fadesTowardEnd, boolean highlighted, float alpha) {
        float linkAlpha = linkAlpha(highlighted, alpha);
        float fadeEnd = fadesTowardEnd ? FADE_REACH : 1f - FADE_REACH;
        float fadeX = fromX + (toX - fromX) * fadeEnd;
        float fadeY = fromY + (toY - fromY) * fadeEnd;
        if (fadesTowardEnd) {
            beams.lane(fromX, fromY, fadeX, fadeY, 0f, beams.lit(LINK_COLOUR), linkAlpha, 0f, LINK_WIDTH_SCALE);
        } else {
            float skipped = (float) Math.hypot(fadeX - fromX, fadeY - fromY);
            beams.lane(fadeX, fadeY, toX, toY, skipped, beams.lit(LINK_COLOUR), 0f, linkAlpha, LINK_WIDTH_SCALE);
        }
    }

    private static float linkAlpha(boolean highlighted, float alpha) {
        return (highlighted ? HIGHLIGHTED_LINK_ALPHA : RESTING_LINK_ALPHA) * alpha;
    }

    void arrow(float[] points, OfficerBoardArrows.Style style, float headLength, float alpha) {
        if (points.length < 4 || alpha <= 0f) {
            return;
        }
        boolean dashed = style == OfficerBoardArrows.Style.DASHED;
        BeamStroke.Look look = beams.lit(dashed ? IMPENDING_COLOUR : BATTLE_COLOUR);
        BeamStroke.Helix helix = beams.helix(ARROW_WIDTH_SCALE);
        float distance = 0f;
        int segments = points.length / 2 - 1;
        for (int segment = 0; segment < segments; segment++) {
            float fromX = points[segment * 2];
            float fromY = points[segment * 2 + 1];
            float toX = points[segment * 2 + 2];
            float toY = points[segment * 2 + 3];
            if (!dashed || segment % 2 == 0) {
                beams.lane(fromX, fromY, toX, toY, distance, look, alpha, alpha, ARROW_WIDTH_SCALE);
                beams.helixLane(fromX, fromY, toX, toY, distance, helix, look, alpha * HELIX_ALPHA, alpha * HELIX_ALPHA, ARROW_WIDTH_SCALE);
            }
            distance += (float) Math.hypot(toX - fromX, toY - fromY);
        }
        arrowhead(points, headLength, look, alpha);
    }

    private void arrowhead(float[] points, float headLength, BeamStroke.Look look, float alpha) {
        int last = points.length - 2;
        float tipX = points[last];
        float tipY = points[last + 1];
        float dx = tipX - points[last - 2];
        float dy = tipY - points[last - 1];
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0f) {
            return;
        }
        float backX = -dx / length * headLength;
        float backY = -dy / length * headLength;
        float sideX = -backY * ARROWHEAD_SPREAD;
        float sideY = backX * ARROWHEAD_SPREAD;
        beams.lane(tipX, tipY, tipX + backX + sideX, tipY + backY + sideY, 0f, look, alpha, alpha, ARROW_WIDTH_SCALE);
        beams.lane(tipX, tipY, tipX + backX - sideX, tipY + backY - sideY, 0f, look, alpha, alpha, ARROW_WIDTH_SCALE);
    }

    static float[] straightLine(float fromX, float toX, float y, int segments) {
        float[] points = new float[(segments + 1) * 2];
        for (int step = 0; step <= segments; step++) {
            points[step * 2] = fromX + (toX - fromX) * step / segments;
            points[step * 2 + 1] = y;
        }
        return points;
    }

    void flush() {
        beams.flush();
    }
}
