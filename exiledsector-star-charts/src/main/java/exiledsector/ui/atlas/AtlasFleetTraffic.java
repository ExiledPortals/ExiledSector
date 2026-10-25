package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasNode;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.decoration.LaneFleetTraffic;
import exiledsector.ui.node.NodeMetrics;
import exiledsector.ui.node.NodeRingArt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class AtlasFleetTraffic {

    static final float ROAM_INNER_SHARE = 1.05f;
    static final float ROAM_OUTER_SHARE = 1.5f;

    private final LaneFleetTraffic traffic = new LaneFleetTraffic(LaneFleetTraffic.sectorFactions(), new Random());

    void attach(AtlasMapCanvas canvas) {
        traffic.setPorts(ports(canvas));
    }

    void advance(float amount) {
        traffic.advance(amount);
    }

    void render(TreeViewport view, float alphaMult) {
        traffic.render(view, alphaMult);
    }

    static List<LaneFleetTraffic.Port> ports(AtlasMapCanvas canvas) {
        float reach = roamReach();
        List<LaneFleetTraffic.Port> ports = new ArrayList<>();
        for (AtlasCell cell : canvas.visibleCells()) {
            AtlasNode node = canvas.nodeAt(cell);
            if (node == null || node.isVoid() || node.isOrigin()) {
                continue;
            }
            List<Long> neighbours = new ArrayList<>();
            for (AtlasCell neighbour : canvas.generator().lanes(cell)) {
                if (canvas.visibleCells().contains(neighbour)) {
                    neighbours.add(neighbour.pack());
                }
            }
            ports.add(new LaneFleetTraffic.Port(cell.pack(), canvas.displayX(cell), canvas.displayY(cell), reach * ROAM_INNER_SHARE,
                    reach * ROAM_OUTER_SHARE, node.factionId(), neighbours));
        }
        return ports;
    }

    static float roamReach() {
        return NodeRingArt.portalReach(NodeMetrics.donutRadius(NodeMetrics.NODE_SIZE * AtlasMapCanvas.NODE_SCALE));
    }
}
