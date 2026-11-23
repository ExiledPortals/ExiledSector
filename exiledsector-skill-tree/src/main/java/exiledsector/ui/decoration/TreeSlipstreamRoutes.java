package exiledsector.ui.decoration;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.layout.ConnectorCurve;
import exiledsector.skills.layout.TreeTopology;
import exiledsector.skills.tags.SkillTags;
import exiledsector.ui.node.NodeMetrics;
import exiledsector.ui.slipstream.CorridorGrid;
import exiledsector.ui.slipstream.CorridorRoutes;
import exiledsector.ui.slipstream.SlipstreamRoutes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class TreeSlipstreamRoutes {

    static final float WIDTH_VIEW_SHARE = 0.12f;
    static final float MIN_WIDTH = 150f;
    static final float MAX_WIDTH = 1200f;
    static final float NODE_RADIUS = NodeMetrics.NODE_SIZE;
    static final float CONNECTOR_RADIUS = NodeMetrics.NODE_SIZE / 4f;

    private TreeSlipstreamRoutes() {
    }

    static SlipstreamRoutes forActiveTree() {
        Collection<SkillNode> nodes = SkillTree.getAllNodes().values();
        List<RoamVolume> volumes = new ArrayList<>();
        for (String volume : SkillTags.VOLUME) {
            RoamVolume roamVolume = TreeRoamVolumes.volume(volume, SkillTree.getDecorations().stars(), nodes);
            if (roamVolume != null) {
                volumes.add(roamVolume);
            }
        }
        List<TreeTopology.Connector<SkillNode>> connectors = SkillTree.topology().connectors();
        return between(volumes, List.copyOf(nodes), connectors);
    }

    static SlipstreamRoutes between(List<RoamVolume> volumes, List<? extends SkillNode> nodes, List<TreeTopology.Connector<SkillNode>> connectors) {
        return new CorridorRoutes(WIDTH_VIEW_SHARE, MIN_WIDTH, MAX_WIDTH, grid -> block(grid, volumes, nodes, connectors));
    }

    private static void block(CorridorGrid grid, List<RoamVolume> volumes, List<? extends SkillNode> nodes,
                              List<TreeTopology.Connector<SkillNode>> connectors) {
        for (RoamVolume volume : volumes) {
            grid.blockCircle(volume.centerX(), volume.centerY(), volume.outerRadius());
        }
        for (SkillNode node : nodes) {
            grid.blockCircle(node.getX(), node.getY(), NODE_RADIUS);
        }
        for (TreeTopology.Connector<SkillNode> connector : connectors) {
            if (!grid.reaches(connector.minX(), connector.minY(), connector.maxX(), connector.maxY())) {
                continue;
            }
            SkillNode from = connector.from();
            SkillNode to = connector.to();
            ConnectorCurve curve = connector.curve();
            if (curve == null) {
                grid.blockLine(from.getX(), from.getY(), to.getX(), to.getY(), CONNECTOR_RADIUS);
            } else {
                grid.blockCurve(from.getX(), from.getY(), 2f * curve.getControlOffsetX() - (from.getX() + to.getX()) / 2f,
                        2f * curve.getControlOffsetY() - (from.getY() + to.getY()) / 2f, to.getX(), to.getY(), CONNECTOR_RADIUS);
            }
        }
    }
}
