package exiledsector.ui.node;

import exiledsector.skills.SkillTier;

import static exiledsector.ui.node.NodeMetrics.ICON_INSET_RATIO;
import static exiledsector.ui.node.NodeMetrics.donutRadius;

final class SkillTreeNodeGeometry {

    private static final float ROOT_CONNECTOR_OVERLAP_RATIO = 0.7f;

    private SkillTreeNodeGeometry() {
    }

    static float connectorEndpointRadius(SkillTier tier, float footprintSize, float zoom) {
        if (tier == SkillTier.ROOT) {
            return donutRadius(footprintSize) * ROOT_CONNECTOR_OVERLAP_RATIO;
        }
        return donutRadius(footprintSize) + NodeBeams.laneEdge(NodeBeams.widthScale(zoom));
    }

    static float octagonEdgeDistance(float halfSize, float diagonalLimit, float dx, float dy) {
        float absX = Math.abs(dx);
        float absY = Math.abs(dy);
        float largest = Math.max(absX, absY);
        if (largest == 0f) {
            return halfSize;
        }
        float length = (float) Math.hypot(dx, dy);
        return Math.min(halfSize * length / largest, diagonalLimit * length / (absX + absY));
    }

    static float beltInnerRadius(float footprintSize) {
        return footprintSize * ICON_INSET_RATIO / 2f;
    }

    static float beltOuterRadius(float footprintSize, float widthRatio) {
        return beltInnerRadius(footprintSize) + footprintSize * widthRatio;
    }
}
