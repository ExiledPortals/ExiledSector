package exiledsector.ui.node;

import exiledsector.skills.SkillTier;

import java.awt.Color;

final class SkillTreeNodeGeometry {

    static final float NODE_SIZE = 64f;
    static final float ICON_INSET_RATIO = 0.9f;

    static final float NODE_CONNECTOR_PARALLEL_GAP = 4f;
    static final float NODE_CONNECTOR_LINE_THICKNESS = 1.5f;
    static final float NODE_CONNECTOR_GLOW_LINE_THICKNESS = 2.5f;
    static final float NODE_CONNECTOR_GLOW_HALO_THICKNESS = 7f;
    static final float NODE_CONNECTOR_GLOW_HALO_ALPHA = 0.35f;

    static final Color RING_DULL_COLOR = new Color(150, 150, 150);
    static final float RING_DULL_ALPHA = 0.5f;

    private static final float ROOT_CONNECTOR_OVERLAP_RATIO = 0.7f;

    private SkillTreeNodeGeometry() {
    }

    static float donutRadius(float footprintSize) {
        return footprintSize / 2f;
    }

    static float donutGapRadius(float scale, float zoom) {
        return (NODE_CONNECTOR_PARALLEL_GAP * scale * zoom) / 2f;
    }

    static float donutOuterRadius(float footprintSize, SkillTier tier, float zoom) {
        return donutRadius(footprintSize) + donutGapRadius(tier.getSizeMultiplier(), zoom);
    }

    static float connectorEndpointRadius(SkillTier tier, float footprintSize, float zoom) {
        if (tier == SkillTier.ROOT) {
            return donutRadius(footprintSize) * ROOT_CONNECTOR_OVERLAP_RATIO;
        }
        return donutOuterRadius(footprintSize, tier, zoom);
    }

    static float squareEdgeDistance(float halfSize, float dx, float dy) {
        float largest = Math.max(Math.abs(dx), Math.abs(dy));
        if (largest == 0f) {
            return halfSize;
        }
        return halfSize * (float) Math.hypot(dx, dy) / largest;
    }

    static float beltInnerRadius(float footprintSize) {
        return footprintSize * ICON_INSET_RATIO / 2f;
    }

    static float beltOuterRadius(float footprintSize, float widthRatio) {
        return beltInnerRadius(footprintSize) + footprintSize * widthRatio;
    }
}
