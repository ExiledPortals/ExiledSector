package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasNode;
import exiledsector.ui.node.NodeMetrics;
import exiledsector.ui.slipstream.CorridorGrid;
import exiledsector.ui.slipstream.CorridorRoutes;
import exiledsector.ui.slipstream.SlipstreamRoutes;

final class AtlasSlipstreamRoutes {

    static final float WIDTH_VIEW_SHARE = 0.12f;
    static final float MIN_WIDTH = 200f;
    static final float MAX_WIDTH = 400f;
    static final float FOOTPRINT = NodeMetrics.NODE_SIZE * AtlasMapCanvas.NODE_SCALE;
    static final float SYSTEM_RADIUS = FOOTPRINT * (AtlasMapCanvas.ANOMALY_ORBIT_SHARE + AtlasMapCanvas.ANOMALY_ICON_SHARE);

    private AtlasSlipstreamRoutes() {
    }

    static SlipstreamRoutes on(AtlasMapCanvas canvas) {
        return new CorridorRoutes(WIDTH_VIEW_SHARE, MIN_WIDTH, MAX_WIDTH, grid -> block(grid, canvas));
    }

    static float systemRadius() {
        return Math.max(SYSTEM_RADIUS, AtlasFleetTraffic.roamReach());
    }

    private static void block(CorridorGrid grid, AtlasMapCanvas canvas) {
        float radius = systemRadius();
        for (AtlasCell cell : canvas.nodeCells()) {
            AtlasNode node = canvas.nodeAt(cell);
            if (node != null && !node.isVoid()) {
                grid.blockCircle(canvas.displayX(cell), canvas.displayY(cell), radius);
            }
        }
    }
}
