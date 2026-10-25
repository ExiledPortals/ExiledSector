package exiledsector.ui.node;

import java.awt.Color;

public final class NodeMetrics {

    public static final float NODE_SIZE = 64f;
    public static final float ICON_INSET_RATIO = 0.9f;

    public static final float NODE_CONNECTOR_PARALLEL_GAP = 4f;
    public static final float NODE_CONNECTOR_LINE_THICKNESS = 1.5f;
    public static final float NODE_CONNECTOR_GLOW_LINE_THICKNESS = 2.5f;
    public static final float NODE_CONNECTOR_GLOW_HALO_THICKNESS = 7f;
    public static final float NODE_CONNECTOR_GLOW_HALO_ALPHA = 0.35f;

    public static final Color RING_DULL_COLOR = new Color(150, 150, 150);
    public static final float RING_DULL_ALPHA = 0.5f;

    private NodeMetrics() {
    }

    public static float donutRadius(float footprintSize) {
        return footprintSize / 2f;
    }

    public static float donutGapRadius(float scale, float zoom) {
        return (NODE_CONNECTOR_PARALLEL_GAP * scale * zoom) / 2f;
    }
}
