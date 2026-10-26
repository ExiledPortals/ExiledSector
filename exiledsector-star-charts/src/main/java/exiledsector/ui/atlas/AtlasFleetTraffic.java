package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasNode;
import exiledsector.charts.atlas.AtlasOpening;
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
    static final float FLEET_SCALE = 2f;

    private final LaneFleetTraffic traffic = new LaneFleetTraffic(LaneFleetTraffic.sectorFactions(), new Random());

    void attach(AtlasMapCanvas canvas) {
        traffic.setPorts(fleetSpace(ports(canvas)));
    }

    void advance(float amount) {
        traffic.advance(amount);
    }

    void render(TreeViewport view, float alphaMult) {
        traffic.render(new TreeViewport(view.centerX(), view.centerY(), view.zoom() * FLEET_SCALE, view.left(), view.bottom(), view.right(), view.top()),
                alphaMult);
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
                    reach * ROAM_OUTER_SHARE, AtlasOpening.territoryOwnerOf(canvas.generator(), canvas.state(), cell), neighbours));
        }
        return ports;
    }

    static List<LaneFleetTraffic.Port> fleetSpace(List<LaneFleetTraffic.Port> ports) {
        List<LaneFleetTraffic.Port> scaled = new ArrayList<>(ports.size());
        for (LaneFleetTraffic.Port port : ports) {
            scaled.add(new LaneFleetTraffic.Port(port.id(), port.x() / FLEET_SCALE, port.y() / FLEET_SCALE, port.innerRadius() / FLEET_SCALE,
                    port.outerRadius() / FLEET_SCALE, port.factionId(), port.neighbours()));
        }
        return scaled;
    }

    static float roamReach() {
        return NodeRingArt.portalReach(NodeMetrics.donutRadius(NodeMetrics.NODE_SIZE * AtlasMapCanvas.NODE_SCALE));
    }
}
