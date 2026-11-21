package exiledsector.ui.node;

import exiledsector.skills.SkillTier;

import static exiledsector.ui.node.NodeMetrics.ICON_INSET_RATIO;
import static exiledsector.ui.node.NodeMetrics.donutRadius;

public final class SkillTreeNodeGeometry {

    private static final float ROOT_CONNECTOR_OVERLAP_RATIO = 0.7f;
    private static final float SOCKET_CORNER_RATIO = 0.25f;
    private static final float SOCKET_EDGE_MARGIN_RATIO = 10f / 31f;
    private static final float SOCKET_BEVEL_RATIO = 30f / 31f;
    public static final float LANE_WIDTH_MULT = 2f;

    private SkillTreeNodeGeometry() {
    }

    public static float laneWidthScale(float zoom) {
        return NodeBeams.widthScale(zoom) * LANE_WIDTH_MULT;
    }

    public static float ringWidthScale(SkillTier tier, float zoom) {
        return NodeBeams.widthScale(zoom) * tier.getSizeMultiplier();
    }

    public static float ringRadius(SkillTier tier, float footprintSize, float zoom) {
        if (tier == SkillTier.ROOT) {
            return donutRadius(footprintSize);
        }
        return NodeBeams.huggingRingRadius(footprintSize * ICON_INSET_RATIO / 2f, ringWidthScale(tier, zoom));
    }

    public static float connectorEndpointRadius(SkillTier tier, float footprintSize, float zoom) {
        if (tier == SkillTier.ROOT) {
            return donutRadius(footprintSize) * ROOT_CONNECTOR_OVERLAP_RATIO;
        }
        if (tier.isWormhole()) {
            return donutRadius(footprintSize) + NodeBeams.laneEdge(NodeBeams.widthScale(zoom));
        }
        float ringOuterEdge = NodeBeams.ringOuterEdge(ringRadius(tier, footprintSize, zoom), ringWidthScale(tier, zoom));
        return NodeBeams.laneReach(ringOuterEdge, laneWidthScale(zoom));
    }

    public static float socketEdgeDistance(float frameSize, float towardX, float towardY) {
        float halfSize = frameSize / 2f;
        float corner = frameSize * SOCKET_CORNER_RATIO;
        return octagonEdgeDistance(halfSize - corner * SOCKET_EDGE_MARGIN_RATIO, 2f * halfSize - corner * SOCKET_BEVEL_RATIO, towardX, towardY);
    }

    public static float octagonEdgeDistance(float halfSize, float diagonalLimit, float dx, float dy) {
        float absX = Math.abs(dx);
        float absY = Math.abs(dy);
        float largest = Math.max(absX, absY);
        if (largest == 0f) {
            return halfSize;
        }
        float length = (float) Math.hypot(dx, dy);
        return Math.min(halfSize * length / largest, diagonalLimit * length / (absX + absY));
    }

    public static float beltInnerRadius(float footprintSize) {
        return footprintSize * ICON_INSET_RATIO / 2f;
    }

    public static float beltOuterRadius(float footprintSize, float widthRatio) {
        return beltInnerRadius(footprintSize) + footprintSize * widthRatio;
    }
}
