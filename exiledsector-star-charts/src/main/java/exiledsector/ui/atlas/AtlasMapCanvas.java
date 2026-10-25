package exiledsector.ui.atlas;

import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.ChartSketchPalette;
import exiledsector.charts.ChartSystemSketch;
import exiledsector.charts.ChartSystemSketcher;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasNode;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasReach;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.node.BeamStroke;
import exiledsector.ui.node.NodeMetrics;
import exiledsector.ui.node.NodeRingArt;
import exiledsector.ui.node.WormholeArt;

import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class AtlasMapCanvas {

    static final float CELL_PIXELS = 2400f;
    static final float NODE_SCALE = 6f;
    static final float LINE_SCALE_SHARE = 0.5f;
    static final float DISPLAY_JITTER_SHARE = AtlasGenerator.DISPLAY_JITTER_SHARE;
    static final float DEFAULT_ZOOM = 0.25f;
    static final float MAX_ZOOM = 0.8f;
    static final float PREVIEW_FILL = 0.85f;
    static final float DIM_PREVIEW_ALPHA = 0.55f;
    static final float COLLAPSE_SECONDS = 1.5f;
    static final int MAX_SCENERY_CELLS = 48;
    static final BeamStroke.Style LANE_STYLE = new BeamStroke.Style("graphics/fx/beamfringe.png", "graphics/fx/beamcore.png", 14f, 5f, 4f, 30f);
    static final BeamStroke.Style RING_STYLE = new BeamStroke.Style("graphics/fx/beamfringeb.png", "graphics/fx/beamcorec.png", 12f, 4f, 4f, 20f);
    static final float DULL_LANE_ALPHA = 0.4f;
    static final float FOG_LANE_ALPHA = 0.3f;
    static final float DIM_RING_ALPHA = 0.4f;
    static final float FOG_RING_ALPHA = 0.3f;
    static final float WHITENESS = 0.7f;
    static final float FOG_SIZE_SHARE = 0.2f;
    static final float HIT_SHARE = 0.6f;
    static final float INFESTATION_RING_SHARE = 0.6f;
    static final float INFESTATION_RING_ALPHA = 0.7f;
    static final Color ORIGIN_TINT = new Color(150, 220, 255);
    static final Color SPENT_TINT = new Color(150, 40, 40);
    static final Color CLEANSED_COLOUR = new Color(235, 240, 255);

    private final AtlasGenerator generator;
    private final AtlasState state;
    private final Map<AtlasCell, AtlasNode> nodes = new LinkedHashMap<>();
    private final Set<AtlasCell> visible;
    private final Set<AtlasCell> reachable;
    private final Map<AtlasCell, String> nodeKeys = new HashMap<>();
    private final Map<AtlasCell, AtlasSystemPreview.Prepared> sketches = new HashMap<>();
    private final AtlasSystemPreview preview = new AtlasSystemPreview();
    private final NodeRingArt ringArt = new NodeRingArt();
    private final WormholeArt wormholeArt = new WormholeArt(ringArt);
    private final Map<AtlasCell, Float> collapsing = new HashMap<>();
    private final BeamStroke beams = new BeamStroke();
    private final Map<Color, BeamStroke.Look> beamLooks = new HashMap<>();
    private final AtlasScenery scenery;

    AtlasMapCanvas(AtlasGenerator generator, AtlasState state) {
        this(generator, state, new AtlasScenery(generator.seed()));
    }

    AtlasMapCanvas(AtlasGenerator generator, AtlasState state, AtlasScenery scenery) {
        this.generator = generator;
        this.state = state;
        this.scenery = scenery;
        this.visible = AtlasReach.refreshReveal(generator, state);
        this.reachable = AtlasReach.reachable(generator, state);
        for (AtlasCell cell : visible) {
            nodes.put(cell, generator.node(cell));
        }
        for (AtlasCell cell : visible) {
            for (AtlasCell neighbour : generator.lanes(cell)) {
                nodes.computeIfAbsent(neighbour, generator::node);
            }
        }
        ChartSketchPalette palette = ChartSketchPalette.get();
        for (AtlasCell cell : visible) {
            AtlasNode node = nodes.get(cell);
            if (!node.isVoid() && !node.isOrigin()) {
                ChartSystemSketch sketch = ChartSystemSketcher.sketch(AtlasOpening.layoutSeed(state, cell),
                        AtlasData.recipeFor(node.regionId(), node.layoutId()), AtlasOpening.phenomenonFor(state, cell, node), palette);
                sketches.put(cell, AtlasSystemPreview.Prepared.of(nodeKey(cell), sketch));
            }
        }
        scenery.attach(this);
    }

    AtlasScenery scenery() {
        return scenery;
    }

    Set<AtlasCell> visibleCells() {
        return visible;
    }

    AtlasNode nodeAt(AtlasCell cell) {
        return nodes.get(cell);
    }

    AtlasGenerator generator() {
        return generator;
    }

    AtlasState state() {
        return state;
    }

    boolean isReachable(AtlasCell cell) {
        return reachable.contains(cell);
    }

    float focusX() {
        return focus(true);
    }

    float focusY() {
        return focus(false);
    }

    private float focus(boolean horizontal) {
        Set<AtlasCell> focus = reachable.isEmpty() ? Set.of(AtlasCell.ORIGIN) : reachable;
        float sum = 0f;
        for (AtlasCell cell : focus) {
            sum += horizontal ? displayX(cell) : displayY(cell);
        }
        return sum / focus.size();
    }

    void advance(float amount) {
        scenery.advance(amount);
        ringArt.advance(amount);
        preview.advance(amount);
        collapsing.replaceAll((cell, openness) -> Math.max(0f, openness - amount / COLLAPSE_SECONDS));
        collapsing.values().removeIf(openness -> openness <= 0f);
    }

    void collapse(AtlasCell cell) {
        if (state.isCompleted(cell)) {
            collapsing.put(cell, 1f);
        }
    }

    float openness(AtlasCell cell) {
        return state.isCompleted(cell) ? WormholeArt.easedOpenness(collapsing.getOrDefault(cell, 0f)) : 1f;
    }

    private float nodeScale(AtlasCell cell) {
        return WormholeArt.visualScale(openness(cell));
    }

    void pulse(AtlasCell cell) {
        ringArt.startPulse(nodeKey(cell));
    }

    private String nodeKey(AtlasCell cell) {
        return nodeKeys.computeIfAbsent(cell, key -> "atlas_" + key.pack());
    }

    float displayX(AtlasCell cell) {
        return displayed(cell.gridX(), generator.mapX(cell));
    }

    float displayY(AtlasCell cell) {
        return displayed(cell.gridY(), generator.mapY(cell));
    }

    static float displayed(int grid, float mapped) {
        return (grid + (mapped - grid) * DISPLAY_JITTER_SHARE) * CELL_PIXELS;
    }

    float screenX(TreeViewport view, AtlasCell cell) {
        return view.screenX(displayX(cell));
    }

    float screenY(TreeViewport view, AtlasCell cell) {
        return view.screenY(displayY(cell));
    }

    static float footprint(TreeViewport view) {
        return NodeMetrics.NODE_SIZE * NODE_SCALE * view.zoom();
    }

    static float lineZoom(TreeViewport view) {
        return view.zoom() * NODE_SCALE * LINE_SCALE_SHARE;
    }

    AtlasCell cellAt(TreeViewport view, float x, float y) {
        float hitRadius = footprint(view) * HIT_SHARE;
        AtlasCell nearest = null;
        float nearestDistance = hitRadius * hitRadius;
        for (AtlasCell cell : visible) {
            if (generator.isVoid(cell)) {
                continue;
            }
            float dx = screenX(view, cell) - x;
            float dy = screenY(view, cell) - y;
            float distance = dx * dx + dy * dy;
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = cell;
            }
        }
        return nearest;
    }

    void render(TreeViewport view, Color accent, float alphaMult) {
        scenery.renderBackdrop(view, alphaMult);
        drawLanes(view, accent, alphaMult);
        drawNodes(view, accent, alphaMult);
        scenery.renderAbove(view, alphaMult);
    }

    static Color regionColour(String regionId, Color fallback) {
        Color tint = AtlasData.regionTint(regionId);
        return tint == null ? fallback : tint;
    }

    private void drawLanes(TreeViewport view, Color accent, float alphaMult) {
        float widthScale = lineZoom(view);
        float edge = LANE_STYLE.fringeWidth() * widthScale / 2f;
        float innerRadius = NodeMetrics.donutRadius(footprint(view));
        float fogRadius = NodeMetrics.donutRadius(footprint(view) * FOG_SIZE_SHARE) + edge;
        for (AtlasCell cell : visible) {
            for (AtlasCell neighbour : generator.lanes(cell)) {
                boolean bothVisible = visible.contains(neighbour);
                if (bothVisible && neighbour.pack() < cell.pack()) {
                    continue;
                }
                addLane(view, cell, neighbour, innerRadius * nodeScale(cell) + edge,
                        bothVisible ? innerRadius * nodeScale(neighbour) + edge : fogRadius, bothVisible, accent, alphaMult);
            }
        }
    }

    private void addLane(TreeViewport view, AtlasCell from, AtlasCell to, float fromRadius, float toRadius, boolean bothVisible, Color accent,
                         float alphaMult) {
        float x1 = screenX(view, from);
        float y1 = screenY(view, from);
        float x2 = screenX(view, to);
        float y2 = screenY(view, to);
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= fromRadius + toRadius || !view.overlaps(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2))) {
            return;
        }
        float dirX = dx / length;
        float dirY = dy / length;
        float startX = x1 + dirX * fromRadius;
        float startY = y1 + dirY * fromRadius;
        float endX = x2 - dirX * toRadius;
        float endY = y2 - dirY * toRadius;
        float elapsed = ringArt.elapsedSeconds();
        float widthScale = lineZoom(view);
        if (state.isCompleted(from) && state.isCompleted(to)) {
            beams.line(startX, startY, endX, endY, LANE_STYLE, beamLook(accent), alphaMult, alphaMult, elapsed, widthScale);
        } else if (bothVisible) {
            beams.line(startX, startY, endX, endY, LANE_STYLE, dullLook(), DULL_LANE_ALPHA * alphaMult, DULL_LANE_ALPHA * alphaMult, elapsed,
                    widthScale);
        } else {
            beams.line(startX, startY, endX, endY, LANE_STYLE, dullLook(), FOG_LANE_ALPHA * alphaMult, 0f, elapsed, widthScale);
        }
    }

    private BeamStroke.Look beamLook(Color colour) {
        return beamLooks.computeIfAbsent(colour, base -> new BeamStroke.Look(base, whitened(base), 0.8f, 0.9f));
    }

    private BeamStroke.Look dullLook() {
        return beamLooks.computeIfAbsent(NodeMetrics.RING_DULL_COLOR, base -> new BeamStroke.Look(base, base, 0.6f, 0.3f));
    }

    static Color whitened(Color colour) {
        return new Color(towardWhite(colour.getRed()), towardWhite(colour.getGreen()), towardWhite(colour.getBlue()));
    }

    private static int towardWhite(int channel) {
        return Math.round(channel + (255 - channel) * WHITENESS);
    }

    private void drawRing(float x, float y, float radius, String key, Color colour, float alpha, float widthScale) {
        float elapsed = ringArt.elapsedSeconds();
        beams.ring(x, y, radius, RING_STYLE, beamLook(colour), alpha, elapsed, widthScale);
        float pulse = ringArt.pulseProgress(key);
        if (pulse >= 0f) {
            beams.ring(x, y, radius * NodeRingArt.pulseRadiusFraction(pulse), RING_STYLE, beamLook(colour), (1f - pulse) * alpha, elapsed, widthScale);
        }
    }

    private void drawNodes(TreeViewport view, Color accent, float alphaMult) {
        float footprint = footprint(view);
        float zoom = lineZoom(view);
        for (Map.Entry<AtlasCell, AtlasNode> entry : nodes.entrySet()) {
            AtlasCell cell = entry.getKey();
            AtlasNode node = entry.getValue();
            if (node.isVoid()) {
                continue;
            }
            float x = screenX(view, cell);
            float y = screenY(view, cell);
            if (!view.isVisible(x, y, NodeRingArt.portalReach(NodeMetrics.donutRadius(footprint)))) {
                continue;
            }
            if (!visible.contains(cell)) {
                beams.ring(x, y, NodeMetrics.donutRadius(footprint * FOG_SIZE_SHARE), RING_STYLE, dullLook(), FOG_RING_ALPHA * alphaMult,
                        ringArt.elapsedSeconds(), zoom);
                continue;
            }
            boolean lit = state.isCompleted(cell);
            boolean open = reachable.contains(cell);
            Color tint = ringTint(cell, node);
            float openness = openness(cell);
            float innerRadius = NodeMetrics.donutRadius(footprint) * WormholeArt.visualScale(openness);
            NodeRingArt.NotableLook look = new NodeRingArt.NotableLook(lit, open, tint, tint, accent);
            if (openness > 0f) {
                ringArt.drawPortalRings(x, y, innerRadius, nodeKey(cell), look, openness * alphaMult);
            }
            wormholeArt.drawDormant(x, y, innerRadius, tint, alphaMult, openness);
            float ringAlpha = lit ? 1f : open ? ringArt.breathing() : DIM_RING_ALPHA;
            drawRing(x, y, innerRadius, nodeKey(cell), tint, ringAlpha * alphaMult, zoom);
            AtlasSystemPreview.Prepared sketch = sketches.get(cell);
            if (sketch != null && openness > 0f) {
                preview.render(sketch, x, y, innerRadius * PREVIEW_FILL, ringArt.elapsedSeconds(), (lit || open ? 1f : DIM_PREVIEW_ALPHA) * openness * alphaMult);
            }
            if (node.infestationId() != null) {
                boolean cleansed = state.has(cell, AtlasState.CLEANSED);
                beams.ring(x, y, footprint * INFESTATION_RING_SHARE, RING_STYLE, beamLook(cleansed ? CLEANSED_COLOUR : Misc.getNegativeHighlightColor()),
                        INFESTATION_RING_ALPHA * alphaMult, ringArt.elapsedSeconds(), zoom);
            }
        }
    }

    private Color ringTint(AtlasCell cell, AtlasNode node) {
        if (node.isOrigin()) {
            return ORIGIN_TINT;
        }
        if (AtlasReach.isSpentBastion(generator, state, cell) || state.has(cell, AtlasState.FAILED)) {
            return SPENT_TINT;
        }
        return regionColour(node.regionId(), NodeRingArt.RING_PINK_COLOR);
    }
}
