package exiledsector.ui.node;

import exiledsector.skills.ExoshipNodes;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.layout.ConnectorCurve;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.TreeLayers;
import exiledsector.ui.TreeViewport;

import java.awt.Color;
import java.util.List;
import java.util.Set;

import static exiledsector.ui.node.NodeBeams.DULL_ALPHA;
import static exiledsector.ui.node.NodeBeams.DULL_LOOK;
import static exiledsector.ui.node.NodeMetrics.NODE_SIZE;
import static exiledsector.ui.node.SkillTreeNodeGeometry.connectorEndpointRadius;
import static exiledsector.ui.node.SkillTreeNodeGeometry.laneWidthScale;

final class SkillTreeNodeConnectorRenderer {

    private static final int CURVE_ARC_SAMPLES = 40;
    private static final int CURVE_RENDER_SEGMENTS = 20;
    private static final int MIN_CURVE_RENDER_SEGMENTS = 8;
    private static final float CURVE_SEGMENT_LENGTH = 12f;
    private static final float WORMHOLE_TIP_FADE_LENGTH = 14f;
    private static final float WORMHOLE_TIP_FADE_MAX_FRACTION = 0.4f;
    private static final float[] PLAIN_BREAKPOINTS = {0f, 1f};
    private static final float[] END_FADE_BREAKPOINTS = {0f, 0.5f, 1f};
    private static final Color TEMPLATE_COLOR = SkillTreePanelStyle.POSITIVE_STAT_COLOR;
    private static final float TEMPLATE_ALPHA = 0.9f;
    private static final float EXOSHIP_ENDPOINT_SHARE = 0.4f;
    private static final float GRANT_TETHER_ALPHA = 0.35f;
    private static final float HELIX_ALPHA = 0.9f;
    private static final float HELIX_MIN_ZOOM = 0.5f;
    private static final float HELIX_FADE_IN_ZOOM = 0.2f;
    private static final int MAX_BUBBLES_PER_CONNECTOR = 4;
    private static final int BUBBLE_TAIL_COPIES = 4;
    private static final float BUBBLE_SIZE = 14f;
    private static final float BUBBLE_TAIL_SPACING = 2.5f;
    private static final float BUBBLE_TAIL_SHRINK_PER_COPY = 0.18f;
    private static final float BUBBLE_TAIL_FADE_PER_COPY = 0.22f;
    private static final float BUBBLE_END_FADE = 8f;

    private final SkillTreePanelStyle panelStyle;
    private final NodeSearch nodeSearch;

    private final NodeBeams beams;
    private final Segment scratchSegment = new Segment();
    private final Segment scratchPart = new Segment();
    private final float[] cumulativeArcLength = new float[CURVE_ARC_SAMPLES + 1];
    private final float[] bubbleFractions = new float[MAX_BUBBLES_PER_CONNECTOR];
    private final float[] pathPoint = new float[4];
    private final ConnectorFlow flow = new ConnectorFlow();
    private int bubbleCount;
    private boolean bubblesTowardEnd;
    private final WormholeOpenness wormholeOpenness;
    private final SocketWormholes socketWormholes;
    private float exoshipRadius;

    SkillTreeNodeConnectorRenderer(SkillTreePanelStyle panelStyle, NodeSearch nodeSearch, WormholeOpenness wormholeOpenness,
                                    SocketWormholes socketWormholes, NodeBeams beams) {
        this.panelStyle = panelStyle;
        this.nodeSearch = nodeSearch;
        this.wormholeOpenness = wormholeOpenness;
        this.socketWormholes = socketWormholes;
        this.beams = beams;
    }

    void advance(float amount) {
        flow.advance(amount);
    }

    void draw(TreeLayers layers, NodeAllocator.Snapshot allocation, Set<String> templateNodeIds, Set<String> plannedOnlyNodeIds,
              AllocationAnimation allocationAnimation) {
        TemplateHighlight templateHighlight = new TemplateHighlight(templateNodeIds, plannedOnlyNodeIds);
        exoshipRadius = layers.exoshipRadius();
        flow.refresh(allocation, plannedOnlyNodeIds);

        for (SkillTreeTopology.Connector connector : SkillTree.topology().connectors()) {
            boolean carried = ExoshipNodes.isCarried(connector.from());
            float alphaMult = carried ? layers.carriedAlpha() : layers.treeAlpha();
            if (alphaMult > 0f && carried == ExoshipNodes.isCarried(connector.to())) {
                drawConnector(connector, carried ? layers.carriedViewport() : layers.treeViewport(), allocation, templateHighlight,
                        allocationAnimation, alphaMult);
            }
        }
    }

    void drawGrantTethers(TreeLayers layers, SkillNode socket, List<String> grantedNodeIds, Color tetherColor) {
        if (socket == null || tetherColor == null || grantedNodeIds.isEmpty()) {
            return;
        }
        exoshipRadius = layers.exoshipRadius();
        TreeViewport socketViewport = layers.viewportFor(socket);
        float socketX = socketViewport.screenX(socket.getOffsetX());
        float socketY = socketViewport.screenY(socket.getOffsetY());
        BeamStroke.Look tetherLook = beams.lit(tetherColor);
        float widthScale = laneWidthScale(socketViewport.zoom());
        for (int grantedIndex = 0; grantedIndex < grantedNodeIds.size(); grantedIndex++) {
            SkillNode granted = SkillTree.get(grantedNodeIds.get(grantedIndex));
            if (granted == null) {
                continue;
            }
            TreeViewport grantedViewport = layers.viewportFor(granted);
            float grantedX = grantedViewport.screenX(granted.getOffsetX());
            float grantedY = grantedViewport.screenY(granted.getOffsetY());
            float towardX = grantedX - socketX;
            float towardY = grantedY - socketY;
            float socketRadius = endpointRadius(socket, socketViewport.zoom(), towardX, towardY);
            float grantedRadius = endpointRadius(granted, grantedViewport.zoom(), -towardX, -towardY);
            float tetherAlpha = GRANT_TETHER_ALPHA * Math.min(layers.alphaFor(socket), layers.alphaFor(granted));
            beams.connector(socketX, socketY, socketRadius, grantedX, grantedY, grantedRadius, tetherLook, tetherAlpha, tetherAlpha, widthScale);
        }
    }

    private void drawConnector(SkillTreeTopology.Connector connector, TreeViewport viewport, NodeAllocator.Snapshot allocation,
                               TemplateHighlight templateHighlight, AllocationAnimation allocationAnimation, float alphaMult) {
        float cullMargin = 2f * NodeBeams.laneEdge(laneWidthScale(viewport.zoom()));
        if (!viewport.overlaps(viewport.screenX(connector.minX()) - cullMargin, viewport.screenY(connector.maxY()) - cullMargin,
                viewport.screenX(connector.maxX()) + cullMargin, viewport.screenY(connector.minY()) + cullMargin)) {
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
        AllocationAnimation.FillRange animatedRange = allocationAnimation.animatedRange(other.getId(), node.getId());
        boolean bothShown = allocationAnimation.shown(node.getId(), skillData.isSatisfied(node.getId(), satisfiedRootId))
                && allocationAnimation.shown(other.getId(), skillData.isSatisfied(other.getId(), satisfiedRootId));
        boolean bothSatisfied = (bothShown || animatedRange != null)
                && !templateHighlight.plannedOnlyNodeIds().contains(node.getId()) && !templateHighlight.plannedOnlyNodeIds().contains(other.getId());
        boolean nodeInTemplate = templateHighlight.templateNodeIds().contains(node.getId());
        boolean otherInTemplate = templateHighlight.templateNodeIds().contains(other.getId());

        ConnectorFade fade = new ConnectorFade(
                ConnectorKind.of(bothSatisfied, nodeInTemplate, otherInTemplate),
                isWormhole(other) || allocation.isHidden(other),
                isWormhole(node) || allocation.isHidden(node),
                isOpenWormhole(other, skillData, satisfiedRootId),
                isOpenWormhole(node, skillData, satisfiedRootId));
        AllocationAnimation.FillRange fillRange = bothSatisfied && animatedRange != null ? animatedRange : AllocationAnimation.FillRange.FULL;
        EdgeFill edgeFill = fillRange.isFull() ? EdgeFill.NONE : new EdgeFill(fillRange, ConnectorKind.of(false, nodeInTemplate, otherInTemplate));
        prepareBubbles(fade.glowing() && edgeFill == EdgeFill.NONE, node, other);

        ConnectorStyle connectorStyle = new ConnectorStyle(fade, edgeFill, zoom, alphaMult * nodeSearch.connectorAlpha(node, other, allocation));
        if (curve == null) {
            drawStraightConnector(otherEndpoint, nodeEndpoint, connectorStyle);
        } else {
            drawCurvedConnector(otherEndpoint, viewport.screenX(curve.getControlOffsetX()),
                    viewport.screenY(curve.getControlOffsetY()), nodeEndpoint, connectorStyle);
        }
    }

    private void prepareBubbles(boolean flowing, SkillNode node, SkillNode other) {
        bubbleCount = 0;
        if (!flowing) {
            return;
        }
        float nodeDistance = flow.distance(node.getId());
        float otherDistance = flow.distance(other.getId());
        if (Float.isNaN(nodeDistance) || Float.isNaN(otherDistance)) {
            return;
        }
        float length = (float) Math.hypot(node.getOffsetX() - other.getOffsetX(), node.getOffsetY() - other.getOffsetY());
        bubblesTowardEnd = otherDistance <= nodeDistance;
        bubbleCount = flow.bubbles(Math.min(nodeDistance, otherDistance), length, bubbleFractions);
    }

    private float endpointRadius(SkillNode node, float zoom, float towardX, float towardY) {
        if (ExoshipNodes.isExoship(node)) return exoshipRadius * zoom * EXOSHIP_ENDPOINT_SHARE;
        SkillTier tier = node.getType().getTier();
        float footprintSize = NODE_SIZE * zoom * tier.getSizeMultiplier();
        if (tier == SkillTier.SOCKET) return socketEndpointRadius(node.getId(), footprintSize, zoom, towardX, towardY);
        float fullRadius = connectorEndpointRadius(tier, footprintSize, zoom);
        if (!tier.isWormhole()) return fullRadius;
        return fullRadius * (1f - wormholeOpenness.of(node.getId()));
    }

    private float socketEndpointRadius(String socketId, float footprintSize, float zoom, float towardX, float towardY) {
        float socketEdge = SkillTreeSocketRenderer.visibleEdgeDistance(footprintSize, towardX, towardY);
        float morph = socketWormholes.morph(socketId);
        if (morph <= 0f) return socketEdge;
        float wormholeFootprint = NODE_SIZE * zoom * SkillTier.WORMHOLE_EXIT.getSizeMultiplier();
        float wormholeEdge = connectorEndpointRadius(SkillTier.WORMHOLE_EXIT, wormholeFootprint, zoom) * (1f - socketWormholes.openness(socketId));
        return socketEdge + (wormholeEdge - socketEdge) * morph;
    }

    private static boolean isOpenWormhole(SkillNode node, ShipSkillData skillData, String satisfiedRootId) {
        return isWormhole(node) && skillData.isSatisfied(node.getId(), satisfiedRootId);
    }

    private static boolean isWormhole(SkillNode node) {
        return node.getType().getTier().isWormhole() && !ExoshipNodes.isExoship(node);
    }

    private void drawStraightConnector(ConnectorEndpoint a, ConnectorEndpoint b, ConnectorStyle connectorStyle) {
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
        SegmentFade segmentFade = segmentFade(connectorStyle, length - r1 - r2);

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
        drawBubbles(segmentFade, r1, length, x1, y1, dirX, dirY, null, null);
    }

    private static float[] straightBreakpoints(SegmentFade segmentFade) {
        if (segmentFade.fade().endFading()) {
            return END_FADE_BREAKPOINTS;
        }
        if (segmentFade.fade().glowTipFading()) {
            return new float[]{0f, segmentFade.tipFraction1(), 1f - segmentFade.tipFraction2(), 1f};
        }
        return PLAIN_BREAKPOINTS;
    }

    private SegmentFade segmentFade(ConnectorStyle connectorStyle, float visibleLength) {
        ConnectorFade fade = connectorStyle.fade();
        float widthScale = laneWidthScale(connectorStyle.zoom());
        float helixAlpha = fade.glowing() ? helixAlpha(connectorStyle.zoom()) : 0f;
        return new SegmentFade(fade,
                tipFraction(fade.glowing() && fade.tipFadesAtR1(), visibleLength, connectorStyle.zoom()),
                tipFraction(fade.glowing() && fade.tipFadesAtR2(), visibleLength, connectorStyle.zoom()),
                connectorStyle.alphaMult(), connectorStyle.edgeFill(), visibleLength, widthScale,
                helixAlpha > 0f ? beams.helix(widthScale) : null, helixAlpha);
    }

    private static float helixAlpha(float zoom) {
        return Math.max(0f, Math.min(1f, (zoom - HELIX_MIN_ZOOM) / HELIX_FADE_IN_ZOOM));
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
        AllocationAnimation.FillRange fillRange = segmentFade.edgeFill().fillRange();
        float fillStart = Math.max(s.progress1, Math.min(s.progress2, fillRange.startFraction()));
        float fillEnd = Math.max(s.progress1, Math.min(s.progress2, fillRange.endFraction()));
        if (segmentFade.fade().glowTipFading()) {
            drawWormholeSegmentWithCoreFill(s, fillStart, fillEnd, segmentFade);
            return;
        }
        drawFillPart(s, s.progress1, fillStart, segmentFade);
        drawFillPart(s, fillStart, fillEnd, segmentFade);
        drawFillPart(s, fillEnd, s.progress2, segmentFade);
    }

    private void drawWormholeSegmentWithCoreFill(Segment s, float fillStart, float fillEnd, SegmentFade segmentFade) {
        BeamStroke.Look glow = look(ConnectorKind.GLOW);
        addTipFadeBeam(s, segmentFade, glow.fringeOnly());
        if (fillEnd <= fillStart || s.progress2 <= s.progress1) return;
        addTipFadeBeam(scratchPart.setPart(s, fillStart, fillEnd), segmentFade, glow.coreOnly());
    }

    private void drawFillPart(Segment s, float from, float to, SegmentFade segmentFade) {
        if (to <= from || s.progress2 <= s.progress1) return;
        scratchPart.setPart(s, from, to);
        if (segmentFade.edgeFill().fillRange().contains(from, to)) {
            drawSegmentAsIs(scratchPart, segmentFade);
            return;
        }
        ConnectorFade fade = segmentFade.fade();
        if (fade.fadesOutAtR1() || fade.fadesOutAtR2()) return;
        drawConnectorSegment(scratchPart, segmentFade.edgeFill().unfilledKind(), segmentFade);
    }

    private void drawSegmentAsIs(Segment s, SegmentFade segmentFade) {
        ConnectorFade fade = segmentFade.fade();
        if (fade.endFading()) {
            float alpha = segmentFade.alphaMult() * kindAlpha(fade.kind());
            addBeam(s, segmentFade, look(fade.kind()), alpha * endFade(s.progress1, fade), alpha * endFade(s.progress2, fade), false);
        } else if (fade.glowTipFading()) {
            addTipFadeBeam(s, segmentFade, look(ConnectorKind.GLOW));
        } else {
            drawConnectorSegment(s, fade.kind(), segmentFade);
        }
    }

    private void drawCurvedConnector(ConnectorEndpoint a, float throughX, float throughY, ConnectorEndpoint b, ConnectorStyle connectorStyle) {
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

        SegmentFade segmentFade = segmentFade(connectorStyle, totalLength - r1 - r2);
        renderCurveSegments(curve, cumLen, r1, segmentFade);
        drawBubbles(segmentFade, r1, totalLength, 0f, 0f, 0f, 0f, curve, cumLen);
    }

    private void drawBubbles(SegmentFade segmentFade, float startLength, float totalLength, float startX, float startY, float dirX, float dirY,
                             QuadraticCurve curve, float[] cumLen) {
        if (bubbleCount == 0 || segmentFade.helix() == null) {
            return;
        }
        BeamStroke.Look look = look(ConnectorKind.GLOW);
        BeamStroke.Helix helix = segmentFade.helix();
        float widthScale = segmentFade.widthScale();
        float visibleLength = segmentFade.visibleLength();
        float tailDirection = bubblesTowardEnd ? -1f : 1f;
        for (int bubble = 0; bubble < bubbleCount; bubble++) {
            float fromStart = bubblesTowardEnd ? bubbleFractions[bubble] : 1f - bubbleFractions[bubble];
            float head = fromStart * totalLength - startLength;
            for (int copy = BUBBLE_TAIL_COPIES; copy >= 0; copy--) {
                float along = head + tailDirection * copy * BUBBLE_TAIL_SPACING * widthScale;
                float endFade = Math.min(1f, Math.min(along, visibleLength - along) / (BUBBLE_END_FADE * widthScale));
                if (endFade <= 0f) {
                    continue;
                }
                connectorPoint(startLength + along, startX, startY, dirX, dirY, curve, cumLen);
                float offset = helix.offset(along);
                float size = BUBBLE_SIZE * widthScale * helix.width(along) * (1f - copy * BUBBLE_TAIL_SHRINK_PER_COPY);
                float alpha = segmentFade.alphaMult() * segmentFade.helixAlpha() * endFade * helix.shade(along) * (1f - copy * BUBBLE_TAIL_FADE_PER_COPY);
                beams.bubble(pathPoint[0] + pathPoint[2] * offset, pathPoint[1] + pathPoint[3] * offset, size,
                        copy == 0 ? look.core() : look.fringe(), alpha);
            }
        }
    }

    private void connectorPoint(float distance, float startX, float startY, float dirX, float dirY, QuadraticCurve curve, float[] cumLen) {
        if (curve == null) {
            pathPoint[0] = startX + dirX * distance;
            pathPoint[1] = startY + dirY * distance;
            pathPoint[2] = -dirY;
            pathPoint[3] = dirX;
            return;
        }
        float t = curveParamAtArcLength(cumLen, distance);
        float tangentX = curve.dxAt(t);
        float tangentY = curve.dyAt(t);
        float tangentLength = (float) Math.sqrt(tangentX * tangentX + tangentY * tangentY);
        pathPoint[0] = curve.xAt(t);
        pathPoint[1] = curve.yAt(t);
        pathPoint[2] = tangentLength > 0f ? -tangentY / tangentLength : 0f;
        pathPoint[3] = tangentLength > 0f ? tangentX / tangentLength : 1f;
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

    private void renderCurveSegments(QuadraticCurve curve, float[] cumLen, float startLength, SegmentFade segmentFade) {
        float visibleLength = segmentFade.visibleLength();
        int segments = Math.max(MIN_CURVE_RENDER_SEGMENTS, Math.min(CURVE_RENDER_SEGMENTS, (int) (visibleLength / CURVE_SEGMENT_LENGTH)));
        float tStart = curveParamAtArcLength(cumLen, startLength);
        float prevX = curve.xAt(tStart);
        float prevY = curve.yAt(tStart);
        for (int i = 1; i <= segments; i++) {
            float t = curveParamAtArcLength(cumLen, startLength + visibleLength * i / segments);
            float x = curve.xAt(t);
            float y = curve.yAt(t);
            float progressPrev = (float) (i - 1) / segments;
            float progressCur = (float) i / segments;
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

    private void drawConnectorSegment(Segment s, ConnectorKind kind, SegmentFade segmentFade) {
        float alpha = segmentFade.alphaMult() * kindAlpha(kind);
        addBeam(s, segmentFade, look(kind), alpha, alpha, kind == ConnectorKind.GLOW);
    }

    private void addTipFadeBeam(Segment s, SegmentFade segmentFade, BeamStroke.Look look) {
        float alpha = segmentFade.alphaMult();
        addBeam(s, segmentFade, look, alpha * tipFade(s.progress1, segmentFade), alpha * tipFade(s.progress2, segmentFade),
                look == look(ConnectorKind.GLOW));
    }

    private void addBeam(Segment s, SegmentFade segmentFade, BeamStroke.Look look, float startAlpha, float endAlpha, boolean helixed) {
        float distanceOffset = s.progress1 * segmentFade.visibleLength();
        beams.lane(s.x1, s.y1, s.x2, s.y2, distanceOffset, look, startAlpha, endAlpha, segmentFade.widthScale());
        if (helixed && segmentFade.helix() != null) {
            float helixAlpha = HELIX_ALPHA * segmentFade.helixAlpha();
            beams.helixLane(s.x1, s.y1, s.x2, s.y2, distanceOffset, segmentFade.helix(), look, helixAlpha * startAlpha, helixAlpha * endAlpha,
                    segmentFade.widthScale());
        }
    }

    private BeamStroke.Look look(ConnectorKind kind) {
        return switch (kind) {
            case GLOW -> beams.lit(panelStyle.getAccentColor());
            case TEMPLATE -> beams.lit(TEMPLATE_COLOR);
            case DULL -> DULL_LOOK;
        };
    }

    private static float kindAlpha(ConnectorKind kind) {
        return switch (kind) {
            case GLOW -> 1f;
            case TEMPLATE -> TEMPLATE_ALPHA;
            case DULL -> DULL_ALPHA;
        };
    }

    private static float tipFade(float progress, SegmentFade segmentFade) {
        float tipFraction1 = segmentFade.tipFraction1();
        float tipFraction2 = segmentFade.tipFraction2();
        if (tipFraction1 > 0f && progress < tipFraction1) {
            return progress / tipFraction1;
        }
        if (tipFraction2 > 0f && progress > 1f - tipFraction2) {
            return (1f - progress) / tipFraction2;
        }
        return 1f;
    }

    private static float endFade(float progress, ConnectorFade fade) {
        if (progress <= 0.5f) {
            return fade.fadesOutAtR1() ? progress / 0.5f : 1f;
        }
        return fade.fadesOutAtR2() ? (1f - progress) / 0.5f : 1f;
    }

    private record ConnectorEndpoint(float x, float y, float radius) {
    }

    private record ConnectorStyle(ConnectorFade fade, EdgeFill edgeFill, float zoom, float alphaMult) {
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

    private record ConnectorFade(ConnectorKind kind, boolean fadesOutAtR1, boolean fadesOutAtR2,
                                  boolean tipFadesAtR1, boolean tipFadesAtR2) {

        boolean glowing() {
            return kind == ConnectorKind.GLOW;
        }

        boolean endFading() {
            return !glowing() && (fadesOutAtR1 || fadesOutAtR2);
        }

        boolean glowTipFading() {
            return glowing() && (tipFadesAtR1 || tipFadesAtR2);
        }
    }

    private record SegmentFade(ConnectorFade fade, float tipFraction1, float tipFraction2, float alphaMult, EdgeFill edgeFill, float visibleLength,
                               float widthScale, BeamStroke.Helix helix, float helixAlpha) {
    }

    private record TemplateHighlight(Set<String> templateNodeIds, Set<String> plannedOnlyNodeIds) {
    }

    private record EdgeFill(AllocationAnimation.FillRange fillRange, ConnectorKind unfilledKind) {
        static final EdgeFill NONE = new EdgeFill(AllocationAnimation.FillRange.FULL, ConnectorKind.GLOW);
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

        float dxAt(float t) {
            return 2f * (1f - t) * (cx - x0) + 2f * t * (x2 - cx);
        }

        float dyAt(float t) {
            return 2f * (1f - t) * (cy - y0) + 2f * t * (y2 - cy);
        }
    }
}
