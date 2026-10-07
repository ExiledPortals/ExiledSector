package exiledsector.ui.node;

import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.layout.ConnectorCurve;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.LineBatch;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.Set;

import static exiledsector.ui.node.SkillTreeNodeGeometry.NODE_CONNECTOR_GLOW_HALO_ALPHA;
import static exiledsector.ui.node.SkillTreeNodeGeometry.NODE_CONNECTOR_GLOW_HALO_THICKNESS;
import static exiledsector.ui.node.SkillTreeNodeGeometry.NODE_CONNECTOR_GLOW_LINE_THICKNESS;
import static exiledsector.ui.node.SkillTreeNodeGeometry.NODE_CONNECTOR_LINE_THICKNESS;
import static exiledsector.ui.node.SkillTreeNodeGeometry.NODE_CONNECTOR_PARALLEL_GAP;
import static exiledsector.ui.node.SkillTreeNodeGeometry.NODE_SIZE;
import static exiledsector.ui.node.SkillTreeNodeGeometry.RING_DULL_ALPHA;
import static exiledsector.ui.node.SkillTreeNodeGeometry.RING_DULL_COLOR;
import static exiledsector.ui.node.SkillTreeNodeGeometry.connectorEndpointRadius;

final class SkillTreeNodeConnectorRenderer {

    private static final int CURVE_ARC_SAMPLES = 40;
    private static final int CURVE_RENDER_SEGMENTS = 20;
    private static final float WORMHOLE_TIP_FADE_LENGTH = 14f;
    private static final float WORMHOLE_TIP_FADE_MAX_FRACTION = 0.4f;
    private static final float[] PLAIN_BREAKPOINTS = {0f, 1f};
    private static final float[] DULL_FADE_BREAKPOINTS = {0f, 0.5f, 1f};
    private static final int RING_DULL_RGB = RING_DULL_COLOR.getRGB();
    private static final int TEMPLATE_RGB = SkillTreePanelStyle.POSITIVE_STAT_COLOR.getRGB();
    private static final float TEMPLATE_ALPHA = 0.9f;
    private static final int BLACK_RGB = Color.BLACK.getRGB();
    private static final float EDGE_CULL_MARGIN = NODE_CONNECTOR_GLOW_HALO_THICKNESS + NODE_CONNECTOR_PARALLEL_GAP;

    private final SkillTreePanelStyle panelStyle;
    private final NodeSearch nodeSearch;

    private final LineBatch dullLines = new LineBatch(NODE_CONNECTOR_LINE_THICKNESS);
    private final LineBatch glowHaloLines = new LineBatch(NODE_CONNECTOR_GLOW_HALO_THICKNESS);
    private final LineBatch glowLines = new LineBatch(NODE_CONNECTOR_GLOW_LINE_THICKNESS);
    private final Segment scratchSegment = new Segment();
    private final Segment scratchPart = new Segment();
    private final float[] cumulativeArcLength = new float[CURVE_ARC_SAMPLES + 1];
    private final WormholeOpenness wormholeOpenness;

    SkillTreeNodeConnectorRenderer(SkillTreePanelStyle panelStyle, NodeSearch nodeSearch, WormholeOpenness wormholeOpenness) {
        this.panelStyle = panelStyle;
        this.nodeSearch = nodeSearch;
        this.wormholeOpenness = wormholeOpenness;
    }

    void draw(TreeViewport viewport, NodeAllocator.Snapshot allocation, Set<String> templateNodeIds, ConnectorFills connectorFills,
              float alphaMult) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        clearBatch();

        for (SkillTreeTopology.Connector connector : SkillTree.topology().connectors()) {
            drawConnector(connector, viewport, allocation, templateNodeIds, connectorFills, alphaMult);
        }

        flushBatch();
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawConnector(SkillTreeTopology.Connector connector, TreeViewport viewport, NodeAllocator.Snapshot allocation,
                               Set<String> templateNodeIds, ConnectorFills connectorFills, float alphaMult) {
        if (!viewport.overlaps(viewport.screenX(connector.minX()) - EDGE_CULL_MARGIN, viewport.screenY(connector.maxY()) - EDGE_CULL_MARGIN,
                viewport.screenX(connector.maxX()) + EDGE_CULL_MARGIN, viewport.screenY(connector.minY()) + EDGE_CULL_MARGIN)) {
            return;
        }
        SkillNode node = connector.from();
        SkillNode other = connector.to();
        ConnectorCurve curve = connector.curve();
        float nodeX = viewport.screenX(node.getOffsetX());
        float nodeY = viewport.screenY(node.getOffsetY());
        float otherX = viewport.screenX(other.getOffsetX());
        float otherY = viewport.screenY(other.getOffsetY());

        float zoom = viewport.zoom();
        ShipSkillData skillData = allocation.skillData();
        String satisfiedRootId = allocation.satisfiedRootId();
        float nodeTowardX = curve == null ? otherX : 2f * viewport.screenX(curve.getControlOffsetX()) - (nodeX + otherX) / 2f;
        float nodeTowardY = curve == null ? otherY : 2f * viewport.screenY(curve.getControlOffsetY()) - (nodeY + otherY) / 2f;
        float otherTowardX = curve == null ? nodeX : nodeTowardX;
        float otherTowardY = curve == null ? nodeY : nodeTowardY;
        ConnectorEndpoint nodeEndpoint = new ConnectorEndpoint(nodeX, nodeY,
                endpointRadius(node, zoom, nodeTowardX - nodeX, nodeTowardY - nodeY));
        ConnectorEndpoint otherEndpoint = new ConnectorEndpoint(otherX, otherY,
                endpointRadius(other, zoom, otherTowardX - otherX, otherTowardY - otherY));
        boolean bothSatisfied = skillData.isSatisfied(node.getId(), satisfiedRootId) && skillData.isSatisfied(other.getId(), satisfiedRootId);
        boolean nodeInTemplate = templateNodeIds.contains(node.getId());
        boolean otherInTemplate = templateNodeIds.contains(other.getId());

        ConnectorFade fade = new ConnectorFade(
                ConnectorKind.of(bothSatisfied, nodeInTemplate, otherInTemplate),
                isWormhole(other) || allocation.isHidden(other),
                isWormhole(node) || allocation.isHidden(node),
                isOpenWormhole(other, skillData, satisfiedRootId),
                isOpenWormhole(node, skillData, satisfiedRootId));
        ConnectorFills.FillRange fillRange = bothSatisfied ? connectorFills.filledRange(other.getId(), node.getId()) : ConnectorFills.FillRange.FULL;
        EdgeFill edgeFill = fillRange.isFull() ? EdgeFill.NONE : new EdgeFill(fillRange, ConnectorKind.of(false, nodeInTemplate, otherInTemplate));

        LineStyle lineStyle = new LineStyle(fade, edgeFill, zoom, alphaMult * nodeSearch.connectorAlpha(node, other, allocation));
        if (curve == null) {
            drawStraightNodeConnectorLine(otherEndpoint, nodeEndpoint, lineStyle);
        } else {
            drawCurvedNodeConnectorLine(otherEndpoint, viewport.screenX(curve.getControlOffsetX()),
                    viewport.screenY(curve.getControlOffsetY()), nodeEndpoint, lineStyle);
        }
    }

    private float endpointRadius(SkillNode node, float zoom, float towardX, float towardY) {
        SkillTier tier = node.getType().getTier();
        float footprintSize = NODE_SIZE * zoom * tier.getSizeMultiplier();
        if (tier == SkillTier.SOCKET) return SkillTreeSocketRenderer.visibleEdgeDistance(footprintSize, towardX, towardY);
        float fullRadius = connectorEndpointRadius(tier, footprintSize, zoom);
        if (tier != SkillTier.WORMHOLE) return fullRadius;
        return fullRadius * (1f - wormholeOpenness.of(node.getId()));
    }

    private static boolean isOpenWormhole(SkillNode node, ShipSkillData skillData, String satisfiedRootId) {
        return node.getType().getTier() == SkillTier.WORMHOLE && skillData.isSatisfied(node.getId(), satisfiedRootId);
    }

    private static boolean isWormhole(SkillNode node) {
        return node.getType().getTier() == SkillTier.WORMHOLE;
    }

    private void drawStraightNodeConnectorLine(ConnectorEndpoint a, ConnectorEndpoint b, LineStyle lineStyle) {
        float x1 = a.x();
        float y1 = a.y();
        float r1 = a.radius();
        float x2 = b.x();
        float y2 = b.y();
        float r2 = b.radius();
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= r1 + r2) return;

        float dirX = dx / length;
        float dirY = dy / length;
        float startX = x1 + dirX * r1;
        float startY = y1 + dirY * r1;
        float endX = x2 - dirX * r2;
        float endY = y2 - dirY * r2;
        SegmentFade segmentFade = segmentFade(lineStyle, length - r1 - r2);

        float[] breakpoints = straightBreakpoints(segmentFade);
        for (int i = 1; i < breakpoints.length; i++) {
            float from = breakpoints[i - 1];
            float to = breakpoints[i];
            if (to > from) {
                scratchSegment.set(startX + (endX - startX) * from, startY + (endY - startY) * from,
                        startX + (endX - startX) * to, startY + (endY - startY) * to, from, to);
                drawFadedSegment(scratchSegment, segmentFade);
            }
        }
    }

    private static float[] straightBreakpoints(SegmentFade segmentFade) {
        if (segmentFade.fade().dullFading()) {
            return DULL_FADE_BREAKPOINTS;
        }
        if (segmentFade.fade().glowTipFading()) {
            return new float[]{0f, segmentFade.tipFraction1(), 1f - segmentFade.tipFraction2(), 1f};
        }
        return PLAIN_BREAKPOINTS;
    }

    private static SegmentFade segmentFade(LineStyle lineStyle, float visibleLength) {
        ConnectorFade fade = lineStyle.fade();
        return new SegmentFade(fade,
                tipFraction(fade.glowing() && fade.tipFadeR1ToBlack(), visibleLength, lineStyle.zoom()),
                tipFraction(fade.glowing() && fade.tipFadeR2ToBlack(), visibleLength, lineStyle.zoom()),
                lineStyle.alphaMult(), lineStyle.edgeFill());
    }

    private static float tipFraction(boolean fades, float visibleLength, float zoom) {
        if (!fades || visibleLength <= 0f) {
            return 0f;
        }
        return Math.min(WORMHOLE_TIP_FADE_LENGTH * zoom, visibleLength * WORMHOLE_TIP_FADE_MAX_FRACTION) / visibleLength;
    }

    private void drawFadedSegment(Segment s, SegmentFade segmentFade) {
        if (segmentFade.edgeFill() != EdgeFill.NONE) {
            drawPartiallyFilledSegment(s, segmentFade);
            return;
        }
        drawSegmentAsIs(s, segmentFade);
    }

    private void drawPartiallyFilledSegment(Segment s, SegmentFade segmentFade) {
        ConnectorFills.FillRange fillRange = segmentFade.edgeFill().fillRange();
        float fillStart = Math.max(s.progress1, Math.min(s.progress2, fillRange.startFraction()));
        float fillEnd = Math.max(s.progress1, Math.min(s.progress2, fillRange.endFraction()));
        if (segmentFade.fade().glowTipFading()) {
            drawWormholeSegmentWithInnerLineFill(s, fillStart, fillEnd, segmentFade);
            return;
        }
        drawFillPart(s, s.progress1, fillStart, segmentFade);
        drawFillPart(s, fillStart, fillEnd, segmentFade);
        drawFillPart(s, fillEnd, s.progress2, segmentFade);
    }

    private void drawWormholeSegmentWithInnerLineFill(Segment s, float fillStart, float fillEnd, SegmentFade segmentFade) {
        addTipFadeLine(glowHaloLines, s, segmentFade, segmentFade.alphaMult() * NODE_CONNECTOR_GLOW_HALO_ALPHA);
        if (fillEnd <= fillStart || s.progress2 <= s.progress1) return;
        addTipFadeLine(glowLines, scratchPart.setPart(s, fillStart, fillEnd), segmentFade, segmentFade.alphaMult());
    }

    private void drawFillPart(Segment s, float from, float to, SegmentFade segmentFade) {
        if (to <= from || s.progress2 <= s.progress1) return;
        scratchPart.setPart(s, from, to);
        if (segmentFade.edgeFill().fillRange().contains(from, to)) {
            drawSegmentAsIs(scratchPart, segmentFade);
            return;
        }
        ConnectorFade fade = segmentFade.fade();
        if (fade.fadeR1ToBlack() || fade.fadeR2ToBlack()) return;
        drawConnectorSegment(scratchPart, segmentFade.edgeFill().unfilledKind(), segmentFade.alphaMult());
    }

    private void drawSegmentAsIs(Segment s, SegmentFade segmentFade) {
        ConnectorFade fade = segmentFade.fade();
        float alphaMult = segmentFade.alphaMult();
        if (fade.dullFading() && fade.kind() == ConnectorKind.TEMPLATE) {
            drawFadedTemplateSegment(s, fade, alphaMult);
        } else if (fade.dullFading()) {
            drawFadedDullSegment(s, fade, alphaMult);
        } else if (fade.glowTipFading()) {
            drawGlowingSegmentWithTipFade(s, segmentFade);
        } else {
            drawConnectorSegment(s, fade.kind(), alphaMult);
        }
    }

    private void drawCurvedNodeConnectorLine(ConnectorEndpoint a, float throughX, float throughY, ConnectorEndpoint b,
                                             LineStyle lineStyle) {
        float r1 = a.radius();
        float r2 = b.radius();
        float cx = 2f * throughX - (a.x() + b.x()) / 2f;
        float cy = 2f * throughY - (a.y() + b.y()) / 2f;
        QuadraticCurve curve = new QuadraticCurve(a.x(), a.y(), cx, cy, b.x(), b.y());

        float[] cumLen = computeCumulativeArcLength(curve);
        float totalLength = cumLen[CURVE_ARC_SAMPLES];
        if (totalLength <= r1 + r2) return;

        float tStart = curveParamAtArcLength(cumLen, r1);
        float tEnd = curveParamAtArcLength(cumLen, totalLength - r2);
        if (tEnd <= tStart) return;

        renderCurveSegments(curve, tStart, tEnd, segmentFade(lineStyle, totalLength - r1 - r2));
    }

    private float[] computeCumulativeArcLength(QuadraticCurve curve) {
        float[] cumLen = cumulativeArcLength;
        cumLen[0] = 0f;
        float prevX = curve.xAt(0f);
        float prevY = curve.yAt(0f);
        for (int i = 1; i <= CURVE_ARC_SAMPLES; i++) {
            float t = (float) i / CURVE_ARC_SAMPLES;
            float x = curve.xAt(t);
            float y = curve.yAt(t);
            float dx = x - prevX;
            float dy = y - prevY;
            cumLen[i] = cumLen[i - 1] + (float) Math.sqrt(dx * dx + dy * dy);
            prevX = x;
            prevY = y;
        }
        return cumLen;
    }

    private void renderCurveSegments(QuadraticCurve curve, float tStart, float tEnd, SegmentFade segmentFade) {
        float prevX = curve.xAt(tStart);
        float prevY = curve.yAt(tStart);
        for (int i = 1; i <= CURVE_RENDER_SEGMENTS; i++) {
            float t = tStart + (tEnd - tStart) * i / CURVE_RENDER_SEGMENTS;
            float x = curve.xAt(t);
            float y = curve.yAt(t);
            float progressPrev = (float) (i - 1) / CURVE_RENDER_SEGMENTS;
            float progressCur = (float) i / CURVE_RENDER_SEGMENTS;
            drawFadedSegment(scratchSegment.set(prevX, prevY, x, y, progressPrev, progressCur), segmentFade);
            prevX = x;
            prevY = y;
        }
    }

    private float curveParamAtArcLength(float[] cumLen, float targetLength) {
        if (targetLength <= 0f) return 0f;
        if (targetLength >= cumLen[CURVE_ARC_SAMPLES]) return 1f;
        for (int i = 1; i <= CURVE_ARC_SAMPLES; i++) {
            if (cumLen[i] >= targetLength) {
                float segLength = cumLen[i] - cumLen[i - 1];
                float frac = segLength <= 0f ? 0f : (targetLength - cumLen[i - 1]) / segLength;
                return ((i - 1) + frac) / CURVE_ARC_SAMPLES;
            }
        }
        return 1f;
    }

    private void drawConnectorSegment(Segment s, ConnectorKind kind, float alphaMult) {
        if (kind == ConnectorKind.GLOW) {
            int accentRgb = panelStyle.getAccentColor().getRGB();
            glowHaloLines.add(s.x1, s.y1, accentRgb, s.x2, s.y2, accentRgb, alphaMult * NODE_CONNECTOR_GLOW_HALO_ALPHA);
            glowLines.add(s.x1, s.y1, accentRgb, s.x2, s.y2, accentRgb, alphaMult);
            return;
        }
        if (kind == ConnectorKind.TEMPLATE) {
            glowLines.add(s.x1, s.y1, TEMPLATE_RGB, s.x2, s.y2, TEMPLATE_RGB, alphaMult * TEMPLATE_ALPHA);
            return;
        }
        addDullPair(s, RING_DULL_RGB, RING_DULL_RGB, alphaMult * RING_DULL_ALPHA);
    }

    private void drawFadedDullSegment(Segment s, ConnectorFade fade, float alphaMult) {
        int color0 = colorForFadeProgress(s.progress1, fade.fadeR1ToBlack(), fade.fadeR2ToBlack(), RING_DULL_RGB);
        int color1 = colorForFadeProgress(s.progress2, fade.fadeR1ToBlack(), fade.fadeR2ToBlack(), RING_DULL_RGB);
        addDullPair(s, color0, color1, alphaMult * RING_DULL_ALPHA);
    }

    private void addDullPair(Segment s, int color0, int color1, float alpha) {
        float dx = s.x2 - s.x1;
        float dy = s.y2 - s.y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.0001f) return;
        float dirX = dx / length;
        float dirY = dy / length;
        float perpX = -dirY * (NODE_CONNECTOR_PARALLEL_GAP / 2f);
        float perpY = dirX * (NODE_CONNECTOR_PARALLEL_GAP / 2f);
        dullLines.add(s.x1 + perpX, s.y1 + perpY, color0, s.x2 + perpX, s.y2 + perpY, color1, alpha);
        dullLines.add(s.x1 - perpX, s.y1 - perpY, color0, s.x2 - perpX, s.y2 - perpY, color1, alpha);
    }

    private void drawFadedTemplateSegment(Segment s, ConnectorFade fade, float alphaMult) {
        int color0 = colorForFadeProgress(s.progress1, fade.fadeR1ToBlack(), fade.fadeR2ToBlack(), TEMPLATE_RGB);
        int color1 = colorForFadeProgress(s.progress2, fade.fadeR1ToBlack(), fade.fadeR2ToBlack(), TEMPLATE_RGB);
        glowLines.add(s.x1, s.y1, color0, s.x2, s.y2, color1, alphaMult * TEMPLATE_ALPHA);
    }

    private void drawGlowingSegmentWithTipFade(Segment s, SegmentFade segmentFade) {
        addTipFadeLine(glowHaloLines, s, segmentFade, segmentFade.alphaMult() * NODE_CONNECTOR_GLOW_HALO_ALPHA);
        addTipFadeLine(glowLines, s, segmentFade, segmentFade.alphaMult());
    }

    private void addTipFadeLine(LineBatch batch, Segment s, SegmentFade segmentFade, float alphaMult) {
        int color0 = colorForTipFade(s.progress1, segmentFade.tipFraction1(), segmentFade.tipFraction2());
        int color1 = colorForTipFade(s.progress2, segmentFade.tipFraction1(), segmentFade.tipFraction2());
        batch.add(s.x1, s.y1, color0, s.x2, s.y2, color1, alphaMult);
    }

    private int colorForTipFade(float progress, float tipFraction1, float tipFraction2) {
        float t = 0f;
        if (tipFraction1 > 0f && progress < tipFraction1) {
            t = 1f - progress / tipFraction1;
        } else if (tipFraction2 > 0f && progress > 1f - tipFraction2) {
            t = 1f - (1f - progress) / tipFraction2;
        }
        return lerpOpaqueRgb(panelStyle.getAccentColor().getRGB(), BLACK_RGB, t);
    }

    private static int colorForFadeProgress(float progress, boolean fadeR1ToBlack, boolean fadeR2ToBlack, int baseRgb) {
        float t;
        if (progress <= 0.5f) {
            t = fadeR1ToBlack ? (1f - progress / 0.5f) : 0f;
        } else {
            t = fadeR2ToBlack ? ((progress - 0.5f) / 0.5f) : 0f;
        }
        return lerpOpaqueRgb(baseRgb, BLACK_RGB, t);
    }

    private static int lerpOpaqueRgb(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = lerpChannel(a >> 16 & 0xFF, b >> 16 & 0xFF, t);
        int g = lerpChannel(a >> 8 & 0xFF, b >> 8 & 0xFF, t);
        int bl = lerpChannel(a & 0xFF, b & 0xFF, t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    private static int lerpChannel(int from, int to, float t) {
        return Math.round(from + (to - from) * t);
    }

    private void clearBatch() {
        dullLines.clear();
        glowLines.clear();
        glowHaloLines.clear();
    }

    private void flushBatch() {
        dullLines.flush();
        glowHaloLines.flush();
        glowLines.flush();
    }

    private record ConnectorEndpoint(float x, float y, float radius) {
    }

    private record LineStyle(ConnectorFade fade, EdgeFill edgeFill, float zoom, float alphaMult) {
    }

    private static final class Segment {
        float x1;
        float y1;
        float x2;
        float y2;
        float progress1;
        float progress2;

        Segment set(float x1, float y1, float x2, float y2, float progress1, float progress2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.progress1 = progress1;
            this.progress2 = progress2;
            return this;
        }

        Segment setPart(Segment whole, float from, float to) {
            float f0 = (from - whole.progress1) / (whole.progress2 - whole.progress1);
            float f1 = (to - whole.progress1) / (whole.progress2 - whole.progress1);
            return set(whole.x1 + (whole.x2 - whole.x1) * f0, whole.y1 + (whole.y2 - whole.y1) * f0,
                    whole.x1 + (whole.x2 - whole.x1) * f1, whole.y1 + (whole.y2 - whole.y1) * f1, from, to);
        }
    }

    private record ConnectorFade(ConnectorKind kind, boolean fadeR1ToBlack, boolean fadeR2ToBlack,
                                  boolean tipFadeR1ToBlack, boolean tipFadeR2ToBlack) {

        boolean glowing() {
            return kind == ConnectorKind.GLOW;
        }

        boolean dullFading() {
            return !glowing() && (fadeR1ToBlack || fadeR2ToBlack);
        }

        boolean glowTipFading() {
            return glowing() && (tipFadeR1ToBlack || tipFadeR2ToBlack);
        }
    }

    private record SegmentFade(ConnectorFade fade, float tipFraction1, float tipFraction2, float alphaMult, EdgeFill edgeFill) {
    }

    private record EdgeFill(ConnectorFills.FillRange fillRange, ConnectorKind unfilledKind) {
        static final EdgeFill NONE = new EdgeFill(ConnectorFills.FillRange.FULL, ConnectorKind.GLOW);
    }

    private record QuadraticCurve(float x0, float y0, float cx, float cy, float x2, float y2) {
        float xAt(float t) {
            float omt = 1f - t;
            return omt * omt * x0 + 2f * omt * t * cx + t * t * x2;
        }

        float yAt(float t) {
            float omt = 1f - t;
            return omt * omt * y0 + 2f * omt * t * cy + t * t * y2;
        }
    }
}
