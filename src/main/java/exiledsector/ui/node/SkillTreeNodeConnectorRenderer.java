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

    private final SkillTreePanelStyle style;
    private final NodeSearch search;

    private final LineBatch dullLines = new LineBatch(NODE_CONNECTOR_LINE_THICKNESS);
    private final LineBatch glowHaloLines = new LineBatch(NODE_CONNECTOR_GLOW_HALO_THICKNESS);
    private final LineBatch glowLines = new LineBatch(NODE_CONNECTOR_GLOW_LINE_THICKNESS);
    private final float[] cumulativeArcLength = new float[CURVE_ARC_SAMPLES + 1];
    private final WormholeOpenness wormholeOpenness;

    SkillTreeNodeConnectorRenderer(SkillTreePanelStyle style, NodeSearch search, WormholeOpenness wormholeOpenness) {
        this.style = style;
        this.search = search;
        this.wormholeOpenness = wormholeOpenness;
    }

    void draw(TreeViewport viewport, NodeAllocator.Snapshot tree, Set<String> templateNodeIds, ConnectorFills fills,
              float alphaMult) {
        float zoom = viewport.zoom();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        clearBatch();

        for (SkillTreeTopology.Connector connector : SkillTree.topology().connectors()) {
            drawConnector(connector, viewport, tree, templateNodeIds, fills, alphaMult);
        }

        flushBatch();
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawConnector(SkillTreeTopology.Connector connector, TreeViewport viewport, NodeAllocator.Snapshot tree,
                               Set<String> templateNodeIds, ConnectorFills fills, float alphaMult) {
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
        ShipSkillData data = tree.data();
        String satisfiedRootId = tree.satisfiedRootId();
        ConnectorEndpoint nodeEndpoint = new ConnectorEndpoint(nodeX, nodeY, endpointRadius(node, zoom));
        ConnectorEndpoint otherEndpoint = new ConnectorEndpoint(otherX, otherY, endpointRadius(other, zoom));
        boolean bothSatisfied = data.isSatisfied(node.getId(), satisfiedRootId) && data.isSatisfied(other.getId(), satisfiedRootId);
        boolean nodeInTemplate = templateNodeIds.contains(node.getId());
        boolean otherInTemplate = templateNodeIds.contains(other.getId());

        ConnectorFade fade = new ConnectorFade(
                ConnectorKind.of(bothSatisfied, nodeInTemplate, otherInTemplate),
                isWormhole(other) || tree.isHidden(other),
                isWormhole(node) || tree.isHidden(node),
                isOpenWormhole(other, data, satisfiedRootId),
                isOpenWormhole(node, data, satisfiedRootId));
        ConnectorFills.FillRange fill = bothSatisfied ? fills.filledRange(other.getId(), node.getId()) : ConnectorFills.FillRange.FULL;
        EdgeFill edgeFill = fill.isFull() ? EdgeFill.NONE : new EdgeFill(fill, ConnectorKind.of(false, nodeInTemplate, otherInTemplate));

        float edgeAlpha = alphaMult * search.connectorAlpha(node, other, tree);
        if (curve == null) {
            drawStraightNodeConnectorLine(otherEndpoint, nodeEndpoint, fade, edgeFill, zoom, edgeAlpha);
        } else {
            drawCurvedNodeConnectorLine(otherEndpoint, viewport.screenX(curve.getControlOffsetX()),
                    viewport.screenY(curve.getControlOffsetY()), nodeEndpoint, fade, edgeFill, zoom, edgeAlpha);
        }
    }

    private float endpointRadius(SkillNode node, float zoom) {
        float fullRadius = connectorEndpointRadius(node.getType().getTier(), NODE_SIZE * zoom * node.getType().getTier().getSizeMultiplier(), zoom);
        if (node.getType().getTier() != SkillTier.WORMHOLE) return fullRadius;
        return fullRadius * (1f - wormholeOpenness.of(node.getId()));
    }

    private static boolean isOpenWormhole(SkillNode node, ShipSkillData data, String satisfiedRootId) {
        return node.getType().getTier() == SkillTier.WORMHOLE && data.isSatisfied(node.getId(), satisfiedRootId);
    }

    private static boolean isWormhole(SkillNode node) {
        return node.getType().getTier() == SkillTier.WORMHOLE;
    }

    private void drawStraightNodeConnectorLine(ConnectorEndpoint a, ConnectorEndpoint b, ConnectorFade fade, EdgeFill edgeFill,
                                               float zoom, float alphaMult) {
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
        SegmentFade segmentFade = segmentFade(fade, edgeFill, length - r1 - r2, zoom, alphaMult);

        float[] breakpoints = straightBreakpoints(segmentFade);
        for (int i = 1; i < breakpoints.length; i++) {
            float from = breakpoints[i - 1];
            float to = breakpoints[i];
            if (to > from) {
                drawFadedSegment(startX + (endX - startX) * from, startY + (endY - startY) * from,
                        startX + (endX - startX) * to, startY + (endY - startY) * to, from, to, segmentFade);
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

    private static SegmentFade segmentFade(ConnectorFade fade, EdgeFill edgeFill, float visibleLength, float zoom, float alphaMult) {
        return new SegmentFade(fade,
                tipFraction(fade.glowing() && fade.tipFadeR1ToBlack(), visibleLength, zoom),
                tipFraction(fade.glowing() && fade.tipFadeR2ToBlack(), visibleLength, zoom),
                alphaMult, edgeFill);
    }

    private static float tipFraction(boolean fades, float visibleLength, float zoom) {
        if (!fades || visibleLength <= 0f) {
            return 0f;
        }
        return Math.min(WORMHOLE_TIP_FADE_LENGTH * zoom, visibleLength * WORMHOLE_TIP_FADE_MAX_FRACTION) / visibleLength;
    }

    private void drawFadedSegment(float x1, float y1, float x2, float y2, float progress1, float progress2,
                                  SegmentFade segmentFade) {
        if (segmentFade.edgeFill() != EdgeFill.NONE) {
            drawPartiallyFilledSegment(x1, y1, x2, y2, progress1, progress2, segmentFade);
            return;
        }
        drawSegmentAsIs(x1, y1, x2, y2, progress1, progress2, segmentFade);
    }

    private void drawPartiallyFilledSegment(float x1, float y1, float x2, float y2, float progress1, float progress2,
                                            SegmentFade segmentFade) {
        ConnectorFills.FillRange fill = segmentFade.edgeFill().range();
        float fillStart = Math.max(progress1, Math.min(progress2, fill.start()));
        float fillEnd = Math.max(progress1, Math.min(progress2, fill.end()));
        if (segmentFade.fade().glowTipFading()) {
            drawWormholeSegmentWithInnerLineFill(x1, y1, x2, y2, progress1, progress2, fillStart, fillEnd, segmentFade);
            return;
        }
        drawFillPart(x1, y1, x2, y2, progress1, progress2, progress1, fillStart, segmentFade);
        drawFillPart(x1, y1, x2, y2, progress1, progress2, fillStart, fillEnd, segmentFade);
        drawFillPart(x1, y1, x2, y2, progress1, progress2, fillEnd, progress2, segmentFade);
    }

    private void drawWormholeSegmentWithInnerLineFill(float x1, float y1, float x2, float y2, float progress1, float progress2,
                                                      float fillStart, float fillEnd, SegmentFade segmentFade) {
        float tip1 = segmentFade.tipFraction1();
        float tip2 = segmentFade.tipFraction2();
        float alphaMult = segmentFade.alphaMult();
        addTipFadeLine(glowHaloLines, x1, y1, x2, y2, progress1, progress2, tip1, tip2, alphaMult * NODE_CONNECTOR_GLOW_HALO_ALPHA);
        if (fillEnd <= fillStart || progress2 <= progress1) return;
        float f0 = (fillStart - progress1) / (progress2 - progress1);
        float f1 = (fillEnd - progress1) / (progress2 - progress1);
        addTipFadeLine(glowLines, x1 + (x2 - x1) * f0, y1 + (y2 - y1) * f0, x1 + (x2 - x1) * f1, y1 + (y2 - y1) * f1,
                fillStart, fillEnd, tip1, tip2, alphaMult);
    }

    private void drawFillPart(float x1, float y1, float x2, float y2, float progress1, float progress2,
                              float from, float to, SegmentFade segmentFade) {
        if (to <= from || progress2 <= progress1) return;
        float f0 = (from - progress1) / (progress2 - progress1);
        float f1 = (to - progress1) / (progress2 - progress1);
        float xa = x1 + (x2 - x1) * f0;
        float ya = y1 + (y2 - y1) * f0;
        float xb = x1 + (x2 - x1) * f1;
        float yb = y1 + (y2 - y1) * f1;
        if (segmentFade.edgeFill().range().contains(from, to)) {
            drawSegmentAsIs(xa, ya, xb, yb, from, to, segmentFade);
            return;
        }
        ConnectorFade fade = segmentFade.fade();
        if (fade.fadeR1ToBlack() || fade.fadeR2ToBlack()) return;
        drawConnectorSegment(xa, ya, xb, yb, segmentFade.edgeFill().unfilledKind(), segmentFade.alphaMult());
    }

    private void drawSegmentAsIs(float x1, float y1, float x2, float y2, float progress1, float progress2,
                                 SegmentFade segmentFade) {
        ConnectorFade fade = segmentFade.fade();
        float alphaMult = segmentFade.alphaMult();
        if (fade.dullFading() && fade.kind() == ConnectorKind.TEMPLATE) {
            drawFadedTemplateSegment(x1, y1, x2, y2, progress1, progress2, fade.fadeR1ToBlack(), fade.fadeR2ToBlack(), alphaMult);
        } else if (fade.dullFading()) {
            drawFadedDullSegment(x1, y1, x2, y2, progress1, progress2, fade.fadeR1ToBlack(), fade.fadeR2ToBlack(), alphaMult);
        } else if (fade.glowTipFading()) {
            drawGlowingSegmentWithTipFade(x1, y1, x2, y2, progress1, progress2,
                    segmentFade.tipFraction1(), segmentFade.tipFraction2(), alphaMult);
        } else {
            drawConnectorSegment(x1, y1, x2, y2, fade.kind(), alphaMult);
        }
    }

    private void drawCurvedNodeConnectorLine(ConnectorEndpoint a, float throughX, float throughY, ConnectorEndpoint b,
                                             ConnectorFade fade, EdgeFill edgeFill, float zoom, float alphaMult) {
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

        renderCurveSegments(curve, tStart, tEnd, totalLength - r1 - r2, fade, edgeFill, zoom, alphaMult);
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

    private void renderCurveSegments(QuadraticCurve curve, float tStart, float tEnd, float visibleArcLength,
                                      ConnectorFade fade, EdgeFill edgeFill, float zoom, float alphaMult) {
        SegmentFade segmentFade = segmentFade(fade, edgeFill, visibleArcLength, zoom, alphaMult);

        float prevX = curve.xAt(tStart);
        float prevY = curve.yAt(tStart);
        for (int i = 1; i <= CURVE_RENDER_SEGMENTS; i++) {
            float t = tStart + (tEnd - tStart) * i / CURVE_RENDER_SEGMENTS;
            float x = curve.xAt(t);
            float y = curve.yAt(t);
            float progressPrev = (float) (i - 1) / CURVE_RENDER_SEGMENTS;
            float progressCur = (float) i / CURVE_RENDER_SEGMENTS;
            drawFadedSegment(prevX, prevY, x, y, progressPrev, progressCur, segmentFade);
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

    private void drawConnectorSegment(float x1, float y1, float x2, float y2, ConnectorKind kind, float alphaMult) {
        if (kind == ConnectorKind.GLOW) {
            int accent = style.getAccentColor().getRGB();
            glowHaloLines.add(x1, y1, accent, x2, y2, accent, alphaMult * NODE_CONNECTOR_GLOW_HALO_ALPHA);
            glowLines.add(x1, y1, accent, x2, y2, accent, alphaMult);
            return;
        }
        if (kind == ConnectorKind.TEMPLATE) {
            glowLines.add(x1, y1, TEMPLATE_RGB, x2, y2, TEMPLATE_RGB, alphaMult * TEMPLATE_ALPHA);
            return;
        }

        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.0001f) return;
        float dirX = dx / length;
        float dirY = dy / length;
        float perpX = -dirY * (NODE_CONNECTOR_PARALLEL_GAP / 2f);
        float perpY = dirX * (NODE_CONNECTOR_PARALLEL_GAP / 2f);

        float alpha = alphaMult * RING_DULL_ALPHA;
        dullLines.add(x1 + perpX, y1 + perpY, RING_DULL_RGB, x2 + perpX, y2 + perpY, RING_DULL_RGB, alpha);
        dullLines.add(x1 - perpX, y1 - perpY, RING_DULL_RGB, x2 - perpX, y2 - perpY, RING_DULL_RGB, alpha);
    }

    private void drawFadedDullSegment(float x1, float y1, float x2, float y2, float progress0, float progress1,
                                       boolean fadeR1ToBlack, boolean fadeR2ToBlack, float alphaMult) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.0001f) return;
        float dirX = dx / length;
        float dirY = dy / length;
        float perpX = -dirY * (NODE_CONNECTOR_PARALLEL_GAP / 2f);
        float perpY = dirX * (NODE_CONNECTOR_PARALLEL_GAP / 2f);

        int color0 = colorForFadeProgress(progress0, fadeR1ToBlack, fadeR2ToBlack, RING_DULL_RGB);
        int color1 = colorForFadeProgress(progress1, fadeR1ToBlack, fadeR2ToBlack, RING_DULL_RGB);
        float alpha = alphaMult * RING_DULL_ALPHA;

        dullLines.add(x1 + perpX, y1 + perpY, color0, x2 + perpX, y2 + perpY, color1, alpha);
        dullLines.add(x1 - perpX, y1 - perpY, color0, x2 - perpX, y2 - perpY, color1, alpha);
    }

    private void drawFadedTemplateSegment(float x1, float y1, float x2, float y2, float progress0, float progress1,
                                          boolean fadeR1ToBlack, boolean fadeR2ToBlack, float alphaMult) {
        int color0 = colorForFadeProgress(progress0, fadeR1ToBlack, fadeR2ToBlack, TEMPLATE_RGB);
        int color1 = colorForFadeProgress(progress1, fadeR1ToBlack, fadeR2ToBlack, TEMPLATE_RGB);
        glowLines.add(x1, y1, color0, x2, y2, color1, alphaMult * TEMPLATE_ALPHA);
    }

    private void drawGlowingSegmentWithTipFade(float x1, float y1, float x2, float y2, float progress0, float progress1,
                                                float tipFraction1, float tipFraction2, float alphaMult) {
        addTipFadeLine(glowHaloLines, x1, y1, x2, y2, progress0, progress1, tipFraction1, tipFraction2,
                alphaMult * NODE_CONNECTOR_GLOW_HALO_ALPHA);
        addTipFadeLine(glowLines, x1, y1, x2, y2, progress0, progress1, tipFraction1, tipFraction2, alphaMult);
    }

    private void addTipFadeLine(LineBatch batch, float x1, float y1, float x2, float y2, float progress0, float progress1,
                                float tipFraction1, float tipFraction2, float alphaMult) {
        int color0 = colorForTipFade(progress0, tipFraction1, tipFraction2);
        int color1 = colorForTipFade(progress1, tipFraction1, tipFraction2);
        batch.add(x1, y1, color0, x2, y2, color1, alphaMult);
    }

    private int colorForTipFade(float progress, float tipFraction1, float tipFraction2) {
        float t = 0f;
        if (tipFraction1 > 0f && progress < tipFraction1) {
            t = 1f - progress / tipFraction1;
        } else if (tipFraction2 > 0f && progress > 1f - tipFraction2) {
            t = 1f - (1f - progress) / tipFraction2;
        }
        return lerpOpaqueRgb(style.getAccentColor().getRGB(), BLACK_RGB, t);
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

    private record EdgeFill(ConnectorFills.FillRange range, ConnectorKind unfilledKind) {
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
