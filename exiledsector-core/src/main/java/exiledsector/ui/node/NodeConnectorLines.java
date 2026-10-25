package exiledsector.ui.node;

import exiledsector.ui.util.LineBatch;

import static exiledsector.ui.node.NodeMetrics.NODE_CONNECTOR_GLOW_HALO_ALPHA;
import static exiledsector.ui.node.NodeMetrics.NODE_CONNECTOR_GLOW_HALO_THICKNESS;
import static exiledsector.ui.node.NodeMetrics.NODE_CONNECTOR_GLOW_LINE_THICKNESS;
import static exiledsector.ui.node.NodeMetrics.NODE_CONNECTOR_LINE_THICKNESS;
import static exiledsector.ui.node.NodeMetrics.NODE_CONNECTOR_PARALLEL_GAP;
import static exiledsector.ui.node.NodeMetrics.RING_DULL_ALPHA;
import static exiledsector.ui.node.NodeMetrics.RING_DULL_COLOR;

public final class NodeConnectorLines {

    public static final int RING_DULL_RGB = RING_DULL_COLOR.getRGB();

    private final LineBatch dullLines = new LineBatch(NODE_CONNECTOR_LINE_THICKNESS);
    private final LineBatch glowHaloLines = new LineBatch(NODE_CONNECTOR_GLOW_HALO_THICKNESS);
    private final LineBatch glowLines = new LineBatch(NODE_CONNECTOR_GLOW_LINE_THICKNESS);

    public void clear() {
        dullLines.clear();
        glowLines.clear();
        glowHaloLines.clear();
    }

    public void flush() {
        dullLines.flush();
        glowHaloLines.flush();
        glowLines.flush();
    }

    public void dull(float x1, float y1, float x2, float y2, float alphaMult) {
        dullPair(x1, y1, RING_DULL_RGB, x2, y2, RING_DULL_RGB, alphaMult * RING_DULL_ALPHA);
    }

    public void dullPair(float x1, float y1, int color0, float x2, float y2, int color1, float alpha) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.0001f) return;
        float dirX = dx / length;
        float dirY = dy / length;
        float perpX = -dirY * (NODE_CONNECTOR_PARALLEL_GAP / 2f);
        float perpY = dirX * (NODE_CONNECTOR_PARALLEL_GAP / 2f);
        dullLines.add(x1 + perpX, y1 + perpY, color0, x2 + perpX, y2 + perpY, color1, alpha);
        dullLines.add(x1 - perpX, y1 - perpY, color0, x2 - perpX, y2 - perpY, color1, alpha);
    }

    public void glow(float x1, float y1, float x2, float y2, int accentRgb, float alphaMult) {
        glowHalo(x1, y1, accentRgb, x2, y2, accentRgb, alphaMult * NODE_CONNECTOR_GLOW_HALO_ALPHA);
        glowLine(x1, y1, accentRgb, x2, y2, accentRgb, alphaMult);
    }

    public void glowHalo(float x1, float y1, int color0, float x2, float y2, int color1, float alpha) {
        glowHaloLines.add(x1, y1, color0, x2, y2, color1, alpha);
    }

    public void glowLine(float x1, float y1, int color0, float x2, float y2, int color1, float alpha) {
        glowLines.add(x1, y1, color0, x2, y2, color1, alpha);
    }
}
