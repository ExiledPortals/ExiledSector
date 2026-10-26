package exiledsector.ui.node;

import exiledsector.skills.SkillTier;

import static exiledsector.ui.node.NodeMetrics.ICON_INSET_RATIO;
import static exiledsector.ui.node.NodeMetrics.donutRadius;

final class SkillTreeNodeGeometry {

    private static final float ROOT_CONNECTOR_OVERLAP_RATIO = 0.7f;
    static final float LANE_WIDTH_MULT = 2f;

    private SkillTreeNodeGeometry() {
    }

    static float laneWidthScale(float zoom) {
        return NodeBeams.widthScale(zoom) * LANE_WIDTH_MULT;
    }

    static float ringWidthScale(SkillTier tier, float zoom) {
        return NodeBeams.widthScale(zoom) * tier.getSizeMultiplier();
    }

    static float ringRadius(SkillTier tier, float footprintSize, float zoom) {
        if (tier == SkillTier.ROOT) {
            return donutRadius(footprintSize);
        }
        return NodeBeams.huggingRingRadius(footprintSize * ICON_INSET_RATIO / 2f, ringWidthScale(tier, zoom));
    }

    static float connectorEndpointRadius(SkillTier tier, float footprintSize, float zoom) {
        if (tier == SkillTier.ROOT) {
            return donutRadius(footprintSize) * ROOT_CONNECTOR_OVERLAP_RATIO;
        }
        if (tier.isWormhole()) {
            return donutRadius(footprintSize) + NodeBeams.laneEdge(NodeBeams.widthScale(zoom));
        }
        float ringOuterEdge = NodeBeams.ringOuterEdge(ringRadius(tier, footprintSize, zoom), ringWidthScale(tier, zoom));
        return NodeBeams.laneReach(ringOuterEdge, laneWidthScale(zoom));
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
