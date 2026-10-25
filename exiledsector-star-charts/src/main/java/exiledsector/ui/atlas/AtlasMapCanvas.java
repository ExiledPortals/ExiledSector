package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.charts.atlas.AtlasBiome;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasNode;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasReach;
import exiledsector.charts.atlas.AtlasSpecial;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.node.NodeConnectorLines;
import exiledsector.ui.node.NodeMetrics;
import exiledsector.ui.node.NodeRingArt;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloShapes;
import exiledsector.ui.util.SpriteDraw;
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

    static final float CELL_PIXELS = 220f;
    static final float FOG_SIZE_SHARE = 0.5f;
    static final float FOG_ALPHA = 0.35f;
    static final float ICON_SHARE = 0.6f;
    static final float HIT_SHARE = 0.6f;
    static final float MARKER_RADIUS = 5f;
    static final float MARKER_ORBIT_SHARE = 0.62f;
    static final float INFESTATION_RING_SHARE = 0.6f;
    static final float BIOME_GLOW_SCALE = 1.35f;
    static final float BIOME_GLOW_ALPHA = 0.3f;
    static final Color LIT_ICON_TINT = Color.WHITE;
    static final Color DIM_ICON_TINT = new Color(90, 90, 90);
    static final Color ORIGIN_TINT = new Color(150, 220, 255);
    static final Color SPENT_TINT = new Color(150, 40, 40);
    static final Color EXPANSION_COLOUR = new Color(185, 110, 255);
    static final Color PHENOMENON_COLOUR = new Color(90, 220, 235);
    static final Color SPECIAL_COLOUR = new Color(255, 205, 90);
    static final Color CLEANSED_COLOUR = new Color(235, 240, 255);
    static final Color NORMAL_GLOW = new Color(80, 90, 130);
    static final String GATEWAY_ICON = "graphics/icons/intel/gate_active.png";
    static final String SENSOR_ARRAY_ICON = "graphics/icons/intel/comm_sniffer.png";
    static final String INFESTATION_ICON = "graphics/icons/danger3.png";
    static final String BASTION_ICON = "graphics/icons/danger4.png";
    static final String UNIQUE_ICON = "graphics/icons/intel/log_sparkle.png";
    static final String SYSTEM_ICON = "graphics/icons/intel/log_star.png";
    private static final int BLACK_RGB = Color.BLACK.getRGB();
    private static final float MARKER_ANGLE = (float) (Math.PI / 4.0);

    private final AtlasGenerator generator;
    private final AtlasState state;
    private final Map<AtlasCell, AtlasNode> nodes = new LinkedHashMap<>();
    private final Set<AtlasCell> visible;
    private final Set<AtlasCell> reachable;
    private final List<AtlasBiome> biomes = new ArrayList<>();
    private final Map<AtlasCell, String> nodeKeys = new HashMap<>();
    private final Map<AtlasCell, String> iconPaths = new HashMap<>();
    private final NodeRingArt ringArt = new NodeRingArt();
    private final NodeConnectorLines connectorLines = new NodeConnectorLines();
    private final SpriteCache spriteCache = new SpriteCache(AtlasMapCanvas.class);

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
            sum += horizontal ? generator.mapX(cell) : generator.mapY(cell);
        }
        return sum / focus.size() * CELL_PIXELS;
    }

    void advance(float amount) {
        ringArt.advance(amount);
    }

    void pulse(AtlasCell cell) {
        ringArt.startPulse(nodeKey(cell));
    }

    private String nodeKey(AtlasCell cell) {
        return nodeKeys.computeIfAbsent(cell, key -> "atlas_" + key.pack());
    }

    float screenX(TreeViewport view, AtlasCell cell) {
        return view.screenX(generator.mapX(cell) * CELL_PIXELS);
    }

    float screenY(TreeViewport view, AtlasCell cell) {
        return view.screenY(generator.mapY(cell) * CELL_PIXELS);
    }

    static float footprint(TreeViewport view) {
        return NodeMetrics.NODE_SIZE * view.zoom();
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
            float x = view.screenX(biome.centreX() * CELL_PIXELS);
            float y = view.screenY(biome.centreY() * CELL_PIXELS);
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
        float footprint = footprint(view);
        float litRadius = NodeMetrics.donutRadius(footprint) + NodeMetrics.donutGapRadius(1f, view.zoom());
        float fogRadius = litRadius * FOG_SIZE_SHARE;
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
                addLane(view, cell, neighbour, litRadius, bothVisible ? litRadius : fogRadius, bothVisible, accentRgb, alphaMult);
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
        float zoom = view.zoom();
        for (Map.Entry<AtlasCell, AtlasNode> entry : nodes.entrySet()) {
            AtlasCell cell = entry.getKey();
            AtlasNode node = entry.getValue();
            if (node.isVoid()) {
                continue;
            }
            float x = screenX(view, cell);
            float y = screenY(view, cell);
            if (!view.isVisible(x, y, footprint * 2f)) {
                continue;
            }
            if (!visible.contains(cell)) {
                drawFog(x, y, footprint * FOG_SIZE_SHARE, zoom, accent, alphaMult * FOG_ALPHA);
                continue;
            }
            boolean lit = state.isCompleted(cell);
            boolean open = reachable.contains(cell);
            Color tint = ringTint(cell, node);
            ringArt.drawNotable(x, y, footprint, zoom, nodeKey(cell), new NodeRingArt.NotableLook(lit, open, tint, tint, accent), alphaMult);
            String icon = iconPath(cell, node);
            float iconSize = footprint * ICON_SHARE;
            SpriteDraw.drawAtCenter(spriteCache, icon, x, y, iconSize, iconSize, lit || open ? LIT_ICON_TINT : DIM_ICON_TINT, alphaMult);
            try (GlScope flat = GlScope.flat()) {
                drawMarkers(cell, node, x, y, footprint, zoom, alphaMult);
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

    private String iconPath(AtlasCell cell, AtlasNode node) {
        return iconPaths.computeIfAbsent(cell, key -> resolveIcon(key, node));
    }

    private String resolveIcon(AtlasCell cell, AtlasNode node) {
        if (node.isOrigin()) {
            return GATEWAY_ICON;
        }
        AtlasSpecial special = generator.specialAt(cell);
        if (special != null) {
            return switch (special.kind()) {
                case SENSOR_ARRAY -> SENSOR_ARRAY_ICON;
                case INFESTATION -> crestOr(special.factionId(), INFESTATION_ICON);
                case BASTION -> BASTION_ICON;
                case UNIQUE -> UNIQUE_ICON;
            };
        }
        return crestOr(AtlasOpening.factionFor(generator, state, cell, node), SYSTEM_ICON);
    }

    private static String crestOr(String factionId, String fallback) {
        SectorAPI sector = Global.getSector();
        FactionAPI faction = sector == null || factionId == null || factionId.isEmpty() ? null : sector.getFaction(factionId);
        String crest = faction == null ? null : faction.getCrest();
        return crest == null || crest.isEmpty() ? fallback : crest;
    }

    private void drawMarkers(AtlasCell cell, AtlasNode node, float x, float y, float footprint, float zoom, float alphaMult) {
        boolean spent = state.has(cell, AtlasState.MARKERS_SPENT);
        float orbit = footprint * MARKER_ORBIT_SHARE;
        float radius = MARKER_RADIUS * Math.max(0.6f, zoom);
        if (node.powerfulBoss() && !spent) {
            drawMarker(x, y, orbit, MARKER_ANGLE, radius, Misc.getNegativeHighlightColor(), alphaMult);
        }
        if (node.expansionId() != null && !spent) {
            drawMarker(x, y, orbit, (float) Math.PI - MARKER_ANGLE, radius, EXPANSION_COLOUR, alphaMult);
        }
        if (!AtlasOpening.phenomenonFor(state, cell, node).isEmpty()) {
            drawMarker(x, y, orbit, (float) Math.PI + MARKER_ANGLE, radius, PHENOMENON_COLOUR, alphaMult);
        }
        if (generator.specialAt(cell) != null) {
            HoloShapes.diamond(x, y - orbit, radius * 1.4f, SPECIAL_COLOUR, alphaMult);
        }
        if (node.infestationId() != null) {
            boolean cleansed = state.has(cell, AtlasState.CLEANSED);
            HoloShapes.circle(x, y, footprint * INFESTATION_RING_SHARE, cleansed ? CLEANSED_COLOUR : Misc.getNegativeHighlightColor(), 1.5f,
                    0.7f * alphaMult);
        }
    }

    private static void drawMarker(float x, float y, float orbit, float angle, float radius, Color colour, float alphaMult) {
        HoloShapes.disc(x + (float) Math.cos(angle) * orbit, y + (float) Math.sin(angle) * orbit, radius, colour, alphaMult);
    }
}
