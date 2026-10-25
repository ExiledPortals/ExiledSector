package exiledsector.ui.atlas;

import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.ChartSketchPalette;
import exiledsector.charts.ChartSystemSketch;
import exiledsector.charts.ChartSystemSketcher;
import exiledsector.charts.atlas.AtlasBiome;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasNode;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasReach;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.node.NodeConnectorLines;
import exiledsector.ui.node.NodeMetrics;
import exiledsector.ui.node.NodeRingArt;
import exiledsector.ui.node.WormholeArt;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloShapes;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AtlasMapCanvas {

    static final float CELL_PIXELS = 2400f;
    static final float NODE_SCALE = 6f;
    static final float LINE_SCALE_SHARE = 0.5f;
    static final float DISPLAY_JITTER_SHARE = 0.35f;
    static final float DEFAULT_ZOOM = 0.25f;
    static final float MAX_ZOOM = 0.8f;
    static final float PREVIEW_FILL = 0.85f;
    static final float DIM_PREVIEW_ALPHA = 0.55f;
    static final float COLLAPSE_SECONDS = 1.5f;
    static final float FOG_SIZE_SHARE = 0.2f;
    static final float FOG_ALPHA = 0.35f;
    static final float HIT_SHARE = 0.6f;
    static final float INFESTATION_RING_SHARE = 0.6f;
    static final float BIOME_GLOW_SCALE = 1.35f;
    static final float BIOME_GLOW_ALPHA = 0.3f;
    static final Color ORIGIN_TINT = new Color(150, 220, 255);
    static final Color SPENT_TINT = new Color(150, 40, 40);
    static final Color CLEANSED_COLOUR = new Color(235, 240, 255);
    static final Color NORMAL_GLOW = new Color(80, 90, 130);
    private static final int BLACK_RGB = Color.BLACK.getRGB();

    private final AtlasGenerator generator;
    private final AtlasState state;
    private final Map<AtlasCell, AtlasNode> nodes = new LinkedHashMap<>();
    private final Set<AtlasCell> visible;
    private final Set<AtlasCell> reachable;
    private final List<AtlasBiome> biomes = new ArrayList<>();
    private final Map<AtlasCell, String> nodeKeys = new HashMap<>();
    private final Map<AtlasCell, AtlasSystemPreview.Prepared> sketches = new HashMap<>();
    private final AtlasSystemPreview preview = new AtlasSystemPreview();
    private final NodeRingArt ringArt = new NodeRingArt();
    private final WormholeArt wormholeArt = new WormholeArt(ringArt);
    private final Map<AtlasCell, Float> collapsing = new HashMap<>();
    private final NodeConnectorLines connectorLines = new NodeConnectorLines();

    AtlasMapCanvas(AtlasGenerator generator, AtlasState state) {
        this.generator = generator;
        this.state = state;
        this.visible = AtlasReach.refreshReveal(generator, state);
        this.reachable = AtlasReach.reachable(generator, state);
        Set<Long> seenSites = new LinkedHashSet<>();
        for (AtlasCell cell : visible) {
            nodes.put(cell, generator.node(cell));
            for (AtlasBiome biome : generator.sitesTouching(cell)) {
                if (!biome.isNormal() && seenSites.add(biome.siteKey())) {
                    biomes.add(biome);
                }
            }
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

    private float displayX(AtlasCell cell) {
        return displayed(cell.gridX(), generator.mapX(cell));
    }

    private float displayY(AtlasCell cell) {
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
        try (GlScope flat = GlScope.flat()) {
            drawBiomes(view, alphaMult);
        }
        drawLanes(view, accent, alphaMult);
        drawNodes(view, accent, alphaMult);
    }

    private void drawBiomes(TreeViewport view, float alphaMult) {
        for (AtlasBiome biome : biomes) {
            float x = view.screenX(displayed(Math.round(biome.centreX()), biome.centreX()));
            float y = view.screenY(displayed(Math.round(biome.centreY()), biome.centreY()));
            float radius = biome.radius() * CELL_PIXELS * view.zoom() * BIOME_GLOW_SCALE;
            if (view.isVisible(x, y, radius)) {
                HoloShapes.radialGlow(x, y, 0f, radius, regionColour(biome.regionId(), NORMAL_GLOW), BIOME_GLOW_ALPHA * alphaMult);
            }
        }
    }

    static Color regionColour(String regionId, Color fallback) {
        Color tint = AtlasData.regionTint(regionId);
        return tint == null ? fallback : tint;
    }

    private void drawLanes(TreeViewport view, Color accent, float alphaMult) {
        float gapRadius = NodeMetrics.donutGapRadius(1f, lineZoom(view));
        float innerRadius = NodeMetrics.donutRadius(footprint(view));
        float fogRadius = NodeMetrics.donutRadius(footprint(view) * FOG_SIZE_SHARE) + gapRadius;
        int accentRgb = accent.getRGB();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        connectorLines.clear();
        for (AtlasCell cell : visible) {
            for (AtlasCell neighbour : generator.lanes(cell)) {
                boolean bothVisible = visible.contains(neighbour);
                if (bothVisible && neighbour.pack() < cell.pack()) {
                    continue;
                }
                addLane(view, cell, neighbour, innerRadius * nodeScale(cell) + gapRadius,
                        bothVisible ? innerRadius * nodeScale(neighbour) + gapRadius : fogRadius, bothVisible, accentRgb, alphaMult);
            }
        }
        connectorLines.flush();
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void addLane(TreeViewport view, AtlasCell from, AtlasCell to, float fromRadius, float toRadius, boolean bothVisible, int accentRgb,
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
        if (state.isCompleted(from) && state.isCompleted(to)) {
            connectorLines.glow(startX, startY, endX, endY, accentRgb, alphaMult);
        } else if (bothVisible) {
            connectorLines.dull(startX, startY, endX, endY, alphaMult);
        } else {
            connectorLines.dullPair(startX, startY, NodeConnectorLines.RING_DULL_RGB, endX, endY, BLACK_RGB, alphaMult * NodeMetrics.RING_DULL_ALPHA);
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
                drawFog(x, y, footprint * FOG_SIZE_SHARE, zoom, accent, alphaMult * FOG_ALPHA);
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
            ringArt.drawNodeOutline(x, y, innerRadius, zoom, nodeKey(cell), look, alphaMult);
            AtlasSystemPreview.Prepared sketch = sketches.get(cell);
            if (sketch != null && openness > 0f) {
                preview.render(sketch, x, y, innerRadius * PREVIEW_FILL, ringArt.elapsedSeconds(), (lit || open ? 1f : DIM_PREVIEW_ALPHA) * openness * alphaMult);
            }
            if (node.infestationId() != null) {
                try (GlScope flat = GlScope.flat()) {
                    boolean cleansed = state.has(cell, AtlasState.CLEANSED);
                    HoloShapes.circle(x, y, footprint * INFESTATION_RING_SHARE, cleansed ? CLEANSED_COLOUR : Misc.getNegativeHighlightColor(),
                            1.5f * zoom, 0.7f * alphaMult);
                }
            }
        }
    }

    private void drawFog(float x, float y, float size, float zoom, Color accent, float alpha) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        ringArt.drawDonut(x, y, NodeMetrics.donutRadius(size), 1f, zoom, false, accent, alpha);
        GL11.glDisable(GL11.GL_BLEND);
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
