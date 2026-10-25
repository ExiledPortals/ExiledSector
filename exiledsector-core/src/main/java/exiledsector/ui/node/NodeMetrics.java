package exiledsector.ui.node;

import java.awt.Color;

public final class NodeMetrics {

    public static final float NODE_SIZE = 64f;
    public static final float ICON_INSET_RATIO = 0.9f;

    public static final Color RING_DULL_COLOR = new Color(150, 150, 150);

    private NodeMetrics() {
    }

    public static float donutRadius(float footprintSize) {
        return footprintSize / 2f;
    }
}
