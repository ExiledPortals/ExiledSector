package exiledsector.ui.atlas;

import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.charts.anomaly.AtlasRule;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.layout.sketch.ChartSketchPalette;
import exiledsector.charts.layout.sketch.ChartSystemSketch;
import exiledsector.charts.layout.sketch.ChartSystemSketcher;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.atlas.AtlasCatalystUse;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasNode;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasReach;
import exiledsector.charts.atlas.AtlasState;
import com.fs.starfarer.api.campaign.FactionAPI;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerSightings;
import exiledsector.charts.officer.OfficerText;
import exiledsector.items.UseVerdict;
import exiledsector.ui.SmoothZoom;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.node.BeamStroke;
import exiledsector.ui.node.NodeBeams;
import exiledsector.ui.node.NodeMetrics;
import exiledsector.ui.node.NodeRingArt;
import exiledsector.ui.node.WormholeArt;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static exiledsector.ui.node.NodeBeams.DULL_ALPHA;
import static exiledsector.ui.node.NodeBeams.DULL_LOOK;

final class AtlasMapCanvas {

    static final float CELL_PIXELS = 2400f;
    static final float NODE_SCALE = 6f;
    static final float DISPLAY_JITTER_SHARE = AtlasGenerator.DISPLAY_JITTER_SHARE;
    static final float MAX_ZOOM = 0.8f;
    static final float MIN_ZOOM = SmoothZoom.MIN_ZOOM / 2f;
    static final float PREVIEW_FILL = 0.85f;
    static final float DIM_PREVIEW_ALPHA = 0.55f;
    static final float COLLAPSE_SECONDS = 1.5f;
    static final int MAX_SCENERY_CELLS = 48;
    static final float FOG_LANE_ALPHA = 0.3f;
    static final float FOG_RING_ALPHA = 0.3f;
    static final float FOG_SIZE_SHARE = 0.2f;
    static final float HIT_SHARE = 0.6f;
    static final float INFESTATION_RING_SHARE = 0.6f;
    static final float INFESTATION_RING_ALPHA = 0.7f;
    static final Color ORIGIN_TINT = new Color(150, 220, 255);
    static final Color SPENT_TINT = new Color(150, 40, 40);
    static final float ANOMALY_ORBIT_SHARE = 0.86f;
    static final float ANOMALY_ICON_SHARE = 0.24f;
    static final float ANOMALY_FIRST_ANGLE = (float) Math.toRadians(60.0);
    static final float ANOMALY_ANGLE_STEP = (float) Math.toRadians(38.0);
    static final float ANOMALY_RING_WIDTH_SHARE = 0.5f;
    static final float TRACE_WIDTH_SHARE = 0.6f;
    static final float TRACE_ALPHA = 0.8f;
    static final Color TRACE_COLOUR = new Color(120, 200, 255);
    static final float TARGET_RING_SHARE = 1.3f;
    static final float TARGET_PULSE_SPEED = 3f;
    static final float TARGET_USABLE_ALPHA = 0.65f;
    static final float TARGET_PULSE_ALPHA = 0.35f;
    static final float TARGET_REFUSED_ALPHA = 0.35f;
    static final int LOOK_STATES = 4;

    static final float PING_START_SHARE = 1.1f;
    static final float PING_REACH_PER_STAGE = 0.45f;
    static final float PING_SECONDS = 2.4f;
    static final float PING_SPEEDUP_PER_STAGE = 0.25f;
    static final float PING_ALPHA = 0.9f;
    static final int MAX_PINGS = 4;
    static final float PING_CULL_SHARE = PING_START_SHARE + PING_REACH_PER_STAGE * (MAX_PINGS + 1);
    static final float FINAL_HALO_SHARE = 1.35f;
    static final float FINAL_HALO_SPEED = 2.5f;
    static final Color FINAL_STAGE_COLOUR = new Color(255, 200, 90);

    record AnomalyIcon(AtlasCell cell, String anomalyId, String iconPath, int slot, boolean fogged, AtlasAnomalyRecord.Chain chain,
                       AtlasOfficer officer, Color ringColour) {

        AnomalyIcon(AtlasCell cell, String anomalyId, String iconPath, int slot, boolean fogged, AtlasAnomalyRecord.Chain chain) {
            this(cell, anomalyId, iconPath, slot, fogged, chain, null, null);
        }

        AnomalyIcon(AtlasCell cell, String anomalyId, String iconPath, int slot) {
            this(cell, anomalyId, iconPath, slot, false, null);
        }

        static AnomalyIcon officer(AtlasCell cell, AtlasOfficer officer, int slot) {
            FactionAPI faction = OfficerText.faction(officer);
            String portraitPath = OfficerText.portraitPath(officer);
            Color ringColour = faction == null ? OfficerBoard.FALLBACK_FRAME_COLOUR : faction.getBaseUIColor();
            return new AnomalyIcon(cell, null, portraitPath == null ? "" : portraitPath, slot, false, null, officer, ringColour);
        }

        boolean isOfficer() {
            return officer != null;
        }
    }

    record Trace(AtlasCell from, AtlasCell to) {
    }

    private final AtlasGenerator generator;
    private final AtlasState state;
    private final Map<AtlasCell, AtlasNode> nodes = new LinkedHashMap<>();
    private final Set<AtlasCell> visible;
    private final Set<AtlasCell> shown;
    private final Set<AtlasCell> reachable;
    private final Map<AtlasCell, String> nodeKeys = new HashMap<>();
    private final Map<AtlasCell, AtlasSystemPreview.Prepared> sketches = new HashMap<>();
    private final List<AnomalyIcon> anomalyIcons = new ArrayList<>();
    private final List<Trace> traces = new ArrayList<>();
    private final SpriteCache spriteCache = new SpriteCache(AtlasMapCanvas.class);
    private final AtlasSystemPreview preview;
    private final NodeRingArt ringArt;
    private final WormholeArt wormholeArt;
    private final Map<AtlasCell, Float> collapsing = new HashMap<>();
    private Map<AtlasCell, UseVerdict> targetVerdicts = Map.of();
    private final NodeBeams beams;
    private final AtlasScenery scenery;
    private final ChartSystemSketch gatewaySketch;
    private final Map<Color, NodeRingArt.NotableLook[]> notableLooks = new HashMap<>();
    private Color lookAccent;

    AtlasMapCanvas(AtlasGenerator generator, AtlasState state) {
        this(generator, state, new AtlasScenery(generator.seed()));
    }

    AtlasMapCanvas(AtlasGenerator generator, AtlasState state, AtlasScenery scenery) {
        this(generator, state, scenery, null);
    }

    AtlasMapCanvas(AtlasGenerator generator, AtlasState state, AtlasScenery scenery, ChartSystemSketch gatewaySketch) {
        this(generator, state, scenery, gatewaySketch, new NodeRingArt(), new AtlasSystemPreview());
    }

    private AtlasMapCanvas(AtlasGenerator generator, AtlasState state, AtlasScenery scenery, ChartSystemSketch gatewaySketch, NodeRingArt ringArt,
                           AtlasSystemPreview preview) {
        this.generator = generator;
        this.state = state;
        this.scenery = scenery;
        this.gatewaySketch = gatewaySketch;
        this.ringArt = ringArt;
        this.preview = preview;
        this.wormholeArt = new WormholeArt(ringArt);
        this.beams = new NodeBeams(ringArt);
        this.visible = AtlasReach.refreshReveal(generator, state);
        this.reachable = AtlasReach.reachable(generator, state);
        OfficerSightings.identifyInView(state, visible);
        this.shown = shownCells(visible, OfficerSightings.shownSystems(state));
        for (AtlasCell cell : shown) {
            nodes.put(cell, generator.node(cell));
        }
        for (AtlasCell cell : visible) {
            for (AtlasCell neighbour : generator.lanes(cell)) {
                nodes.computeIfAbsent(neighbour, generator::node);
            }
        }
        ChartSketchPalette palette = ChartSketchPalette.get();
        for (AtlasCell cell : shown) {
            AtlasNode node = nodes.get(cell);
            if (!node.isVoid() && !node.isOrigin()) {
                List<String> anomalyIds = AtlasAnomalies.effective(generator, state, cell);
                ChartSystemSketch sketch = ChartSystemSketcher.sketch(AtlasOpening.layoutSeed(state, cell),
                        AtlasData.recipeFor(node.regionId(), node.layoutId()), anomalyIds, palette);
                sketches.put(cell, AtlasSystemPreview.Prepared.of(nodeKey(cell), sketch, anomalyIds));
                for (int slot = 0; slot < anomalyIds.size(); slot++) {
                    String anomalyId = anomalyIds.get(slot);
                    ChartAnomalies.Definition definition = ChartAnomalies.definition(anomalyId);
                    anomalyIcons.add(new AnomalyIcon(cell, anomalyId, definition == null ? "" : definition.icon(), slot, false, chainAt(cell, anomalyId)));
                }
                AtlasOfficer officer = shownOfficerAt(cell);
                if (officer != null) {
                    anomalyIcons.add(AnomalyIcon.officer(cell, officer, anomalyIds.size()));
                }
            }
        }
        if (gatewaySketch != null && visible.contains(AtlasCell.ORIGIN)) {
            sketches.put(AtlasCell.ORIGIN, AtlasSystemPreview.Prepared.of(nodeKey(AtlasCell.ORIGIN), gatewaySketch));
        }
        addChains();
        scenery.attach(this);
    }

    AtlasOfficer shownOfficerAt(AtlasCell cell) {
        AtlasOfficer officer = state.officerAt(cell);
        return officer != null && officer.identified() ? officer : null;
    }

    private static Set<AtlasCell> shownCells(Set<AtlasCell> visibleCells, Set<AtlasCell> officerSystems) {
        Set<AtlasCell> cells = new LinkedHashSet<>(visibleCells);
        cells.addAll(officerSystems);
        return cells;
    }

    float elapsedSeconds() {
        return ringArt.elapsedSeconds();
    }

    AtlasMapCanvas rebuilt() {
        return new AtlasMapCanvas(generator, state, scenery, gatewaySketch, ringArt, preview);
    }

    private void addChains() {
        for (AtlasAnomalyRecord record : state.anomalyRecords()) {
            AtlasAnomalyRecord.Chain chain = record.chain();
            if (chain == null || generator.isVoid(record.cell()) || !AtlasAnomalies.effective(generator, state, record.cell()).contains(record.anomalyId())) {
                continue;
            }
            if (chain.from() != null && !chain.from().equals(record.cell())) {
                traces.add(new Trace(chain.from(), record.cell()));
            }
            if (!shown.contains(record.cell())) {
                nodes.computeIfAbsent(record.cell(), generator::node);
                ChartAnomalies.Definition definition = ChartAnomalies.definition(record.anomalyId());
                anomalyIcons.add(new AnomalyIcon(record.cell(), record.anomalyId(), definition == null ? "" : definition.icon(), 0, true, chain));
            }
        }
    }

    private AtlasAnomalyRecord.Chain chainAt(AtlasCell cell, String anomalyId) {
        ChartAnomaly anomaly = ChartAnomalies.byId(anomalyId);
        return anomaly == null || state.isCompleted(cell) ? null : AtlasRule.of(anomaly).chainAt(generator, state, cell);
    }

    List<Trace> traces() {
        return traces;
    }

    AtlasScenery scenery() {
        return scenery;
    }

    Set<AtlasCell> visibleCells() {
        return visible;
    }

    boolean isShown(AtlasCell cell) {
        return shown.contains(cell);
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

    void target(AtlasCatalystUse use) {
        Map<AtlasCell, UseVerdict> verdicts = new HashMap<>();
        if (use != null) {
            for (AtlasCell cell : visible) {
                if (!generator.isVoid(cell)) {
                    verdicts.put(cell, use.check(cell));
                }
            }
        }
        targetVerdicts = verdicts;
    }

    boolean isTargeting() {
        return !targetVerdicts.isEmpty();
    }

    UseVerdict targetVerdict(AtlasCell cell) {
        return targetVerdicts.get(cell);
    }

    boolean isReachable(AtlasCell cell) {
        return reachable.contains(cell);
    }

    float gatewayX() {
        return displayX(AtlasCell.ORIGIN);
    }

    float gatewayY() {
        return displayY(AtlasCell.ORIGIN);
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
        if (cell.equals(AtlasCell.ORIGIN) && sketches.containsKey(cell)) {
            return 1f;
        }
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

    static float beamWidthScale(TreeViewport view) {
        return NodeBeams.widthScale(view.zoom() * NODE_SCALE);
    }

    AtlasCell cellAt(TreeViewport view, float x, float y) {
        float hitRadius = footprint(view) * HIT_SHARE;
        AtlasCell nearest = null;
        float nearestDistance = hitRadius * hitRadius;
        for (AtlasCell cell : shown) {
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

    List<AnomalyIcon> anomalyIcons() {
        return anomalyIcons;
    }

    AnomalyIcon anomalyAt(TreeViewport view, float x, float y) {
        float footprint = footprint(view);
        for (AnomalyIcon icon : anomalyIcons) {
            float scale = nodeScale(icon.cell());
            float radius = anomalyIconRadius(footprint, scale);
            float dx = anomalyX(view, icon, footprint, scale) - x;
            float dy = anomalyY(view, icon, footprint, scale) - y;
            if (openness(icon.cell()) > 0f && dx * dx + dy * dy <= radius * radius) {
                return icon;
            }
        }
        return null;
    }

    private static float anomalyIconRadius(float footprint, float scale) {
        return footprint * ANOMALY_ICON_SHARE * scale;
    }

    private static float anomalyAngle(AnomalyIcon icon) {
        return ANOMALY_FIRST_ANGLE - icon.slot() * ANOMALY_ANGLE_STEP;
    }

    private float anomalyX(TreeViewport view, AnomalyIcon icon, float footprint, float scale) {
        float orbit = icon.fogged() ? 0f : footprint * ANOMALY_ORBIT_SHARE * scale;
        return screenX(view, icon.cell()) + (float) Math.cos(anomalyAngle(icon)) * orbit;
    }

    private float anomalyY(TreeViewport view, AnomalyIcon icon, float footprint, float scale) {
        float orbit = icon.fogged() ? 0f : footprint * ANOMALY_ORBIT_SHARE * scale;
        return screenY(view, icon.cell()) + (float) Math.sin(anomalyAngle(icon)) * orbit;
    }

    boolean canFocus(AtlasCell cell) {
        return cell != null && sketches.containsKey(cell);
    }

    record Focus(float x, float y, float reach, float level, AtlasSystemPreview.Spotlight spotlight) {
    }

    void renderFocus(TreeViewport view, AtlasCell cell, Focus focus, float alphaMult) {
        AtlasSystemPreview.Prepared sketch = sketches.get(cell);
        float openness = openness(cell);
        if (sketch == null || openness <= 0f) {
            return;
        }
        float level = focus.level();
        float boardRadius = previewRadius(view, cell);
        float focusRadius = focus.reach() / AtlasSystemPreview.ORBIT_REACH_SHARE;
        float focusOuter = sketch.sketch().outerRadius() > 0f ? sketch.sketch().outerRadius() : sketch.outerRadius();
        float x = lerp(screenX(view, cell), focus.x(), level);
        float y = lerp(screenY(view, cell), focus.y(), level);
        float radius = boardRadius * (float) Math.pow(focusRadius / boardRadius, level);
        float outer = sketch.outerRadius() * (float) Math.pow(focusOuter / sketch.outerRadius(), level);
        float alpha = lerp(previewAlpha(cell) * openness, 1f, level) * alphaMult;
        preview.render(sketch, x, y, radius, outer, ringArt.elapsedSeconds(), alpha, focus.spotlight());
    }

    static float lerp(float from, float to, float level) {
        return from + (to - from) * level;
    }

    private float previewRadius(TreeViewport view, AtlasCell cell) {
        return NodeMetrics.donutRadius(footprint(view)) * nodeScale(cell) * PREVIEW_FILL;
    }

    private float previewAlpha(AtlasCell cell) {
        return state.isCompleted(cell) || reachable.contains(cell) ? 1f : DIM_PREVIEW_ALPHA;
    }

    void render(TreeViewport view, Color accent, float alphaMult, AtlasCell hiddenPreview) {
        scenery.renderBackdrop(view, alphaMult);
        drawLanes(view, accent, alphaMult);
        drawNodes(view, accent, alphaMult, hiddenPreview);
        drawAnomalies(view, alphaMult);
        scenery.renderAbove(view, alphaMult);
    }

    static Color regionColour(String regionId, Color fallback) {
        Color tint = AtlasData.regionTint(regionId);
        return tint == null ? fallback : tint;
    }

    private void drawLanes(TreeViewport view, Color accent, float alphaMult) {
        float widthScale = beamWidthScale(view);
        float innerRadius = NodeMetrics.donutRadius(footprint(view));
        float fogRadius = laneReach(NodeMetrics.donutRadius(footprint(view) * FOG_SIZE_SHARE), widthScale);
        for (AtlasCell cell : visible) {
            for (AtlasCell neighbour : generator.lanes(cell)) {
                boolean bothVisible = visible.contains(neighbour);
                if (bothVisible && neighbour.pack() < cell.pack()) {
                    continue;
                }
                addLane(view, cell, neighbour, laneReach(innerRadius * nodeScale(cell), widthScale),
                        bothVisible ? laneReach(innerRadius * nodeScale(neighbour), widthScale) : fogRadius, bothVisible, accent, alphaMult);
            }
        }
        BeamStroke.Look traceLook = beams.lit(TRACE_COLOUR);
        float traceWidthScale = widthScale * TRACE_WIDTH_SHARE;
        float traceAlpha = TRACE_ALPHA * alphaMult;
        for (Trace trace : traces) {
            float x1 = screenX(view, trace.from());
            float y1 = screenY(view, trace.from());
            float x2 = screenX(view, trace.to());
            float y2 = screenY(view, trace.to());
            if (view.overlaps(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2))) {
                float toRadius = visible.contains(trace.to()) ? laneReach(innerRadius * nodeScale(trace.to()), widthScale) : fogRadius;
                beams.connector(x1, y1, laneReach(innerRadius * nodeScale(trace.from()), widthScale), x2, y2, toRadius, traceLook, traceAlpha,
                        traceAlpha, traceWidthScale);
            }
        }
        beams.flush();
    }

    private static float laneReach(float ringRadius, float widthScale) {
        return NodeBeams.laneReach(NodeBeams.ringOuterEdge(ringRadius, widthScale), widthScale);
    }

    private void addLane(TreeViewport view, AtlasCell from, AtlasCell to, float fromRadius, float toRadius, boolean bothVisible, Color accent,
                         float alphaMult) {
        float x1 = screenX(view, from);
        float y1 = screenY(view, from);
        float x2 = screenX(view, to);
        float y2 = screenY(view, to);
        if (!view.overlaps(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2))) {
            return;
        }
        float widthScale = beamWidthScale(view);
        if (state.isCompleted(from) && state.isCompleted(to)) {
            beams.connector(x1, y1, fromRadius, x2, y2, toRadius, beams.lit(accent), alphaMult, alphaMult, widthScale);
        } else if (bothVisible) {
            float dullAlpha = DULL_ALPHA * alphaMult;
            beams.connector(x1, y1, fromRadius, x2, y2, toRadius, DULL_LOOK, dullAlpha, dullAlpha, widthScale);
        } else {
            beams.connector(x1, y1, fromRadius, x2, y2, toRadius, DULL_LOOK, FOG_LANE_ALPHA * alphaMult, 0f, widthScale);
        }
    }

    private void drawNodes(TreeViewport view, Color accent, float alphaMult, AtlasCell hiddenPreview) {
        float footprint = footprint(view);
        float widthScale = beamWidthScale(view);
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
            if (!shown.contains(cell)) {
                beams.ring(x, y, NodeMetrics.donutRadius(footprint * FOG_SIZE_SHARE), null, DULL_LOOK, FOG_RING_ALPHA * alphaMult, widthScale);
                continue;
            }
            boolean lit = state.isCompleted(cell);
            boolean open = reachable.contains(cell);
            Color tint = ringTint(cell, node);
            float openness = openness(cell);
            float innerRadius = NodeMetrics.donutRadius(footprint) * WormholeArt.visualScale(openness);
            NodeRingArt.NotableLook look = notableLook(lit, open, tint, accent);
            if (openness > 0f) {
                ringArt.drawPortalRings(x, y, innerRadius, nodeKey(cell), look, openness * alphaMult);
            }
            wormholeArt.drawDormant(x, y, innerRadius, tint, alphaMult, openness);
            NodeBeams.RingLight light = lit ? NodeBeams.RingLight.LIT : open ? NodeBeams.RingLight.BREATHING : NodeBeams.RingLight.DIM;
            beams.nodeRing(x, y, innerRadius, null, light, tint, nodeKey(cell), alphaMult, widthScale);
            drawTargetMarker(targetVerdicts.get(cell), x, y, innerRadius, alphaMult, widthScale);
            beams.flush();
            AtlasSystemPreview.Prepared sketch = sketches.get(cell);
            if (sketch != null && openness > 0f && !cell.equals(hiddenPreview)) {
                preview.render(sketch, x, y, innerRadius * PREVIEW_FILL, ringArt.elapsedSeconds(), previewAlpha(cell) * openness * alphaMult);
            }
            if (node.infestationId() != null && !state.has(cell, AtlasState.INFESTATION_CLEARED)) {
                beams.ring(x, y, footprint * INFESTATION_RING_SHARE, null, beams.lit(Misc.getNegativeHighlightColor()),
                        INFESTATION_RING_ALPHA * alphaMult, widthScale);
            }
        }
        beams.flush();
    }

    private void drawTargetMarker(UseVerdict verdict, float x, float y, float innerRadius, float alphaMult, float widthScale) {
        if (verdict == null) {
            return;
        }
        float radius = innerRadius * TARGET_RING_SHARE;
        if (verdict.allowed()) {
            float pulse = (float) Math.sin(ringArt.elapsedSeconds() * TARGET_PULSE_SPEED) * 0.5f + 0.5f;
            beams.ring(x, y, radius, null, beams.lit(Misc.getPositiveHighlightColor()),
                    (TARGET_USABLE_ALPHA + TARGET_PULSE_ALPHA * pulse) * alphaMult, widthScale);
        } else {
            beams.ring(x, y, radius, null, beams.lit(Misc.getNegativeHighlightColor()), TARGET_REFUSED_ALPHA * alphaMult, widthScale);
        }
    }

    private void drawAnomalies(TreeViewport view, float alphaMult) {
        float footprint = footprint(view);
        float ringWidthScale = beamWidthScale(view) * ANOMALY_RING_WIDTH_SHARE;
        for (AnomalyIcon icon : anomalyIcons) {
            float openness = openness(icon.cell());
            float scale = nodeScale(icon.cell());
            float radius = anomalyIconRadius(footprint, scale);
            float x = anomalyX(view, icon, footprint, scale);
            float y = anomalyY(view, icon, footprint, scale);
            if (openness <= 0f || !view.isVisible(x, y, radius * PING_CULL_SHARE)) {
                continue;
            }
            float alpha = openness * alphaMult;
            drawIconFace(icon, x, y, radius, alpha);
            beams.ring(x, y, NodeBeams.huggingRingRadius(radius, ringWidthScale), null, beams.lit(icon.ringColour() == null ? Misc.getNegativeHighlightColor() : icon.ringColour()), alpha, ringWidthScale);
            if (icon.chain() != null) {
                addChainPings(icon.chain(), x, y, radius, alpha, ringWidthScale);
            }
        }
        beams.flush();
    }

    private void drawIconFace(AnomalyIcon icon, float x, float y, float radius, float alpha) {
        if (icon.iconPath().isEmpty()) {
            return;
        }
        if (icon.isOfficer()) {
            SpriteDraw.drawDiscAtCenter(spriteCache.texture(icon.iconPath()), x, y, radius, alpha);
        } else {
            SpriteDraw.drawAtCenter(spriteCache, icon.iconPath(), x, y, radius * 2f, radius * 2f, Color.WHITE, alpha);
        }
    }

    private void addChainPings(AtlasAnomalyRecord.Chain chain, float x, float y, float radius, float alpha, float ringWidthScale) {
        BeamStroke.Look look = beams.lit(chain.isLast() ? FINAL_STAGE_COLOUR : TRACE_COLOUR);
        int pings = Math.min(MAX_PINGS, chain.stage());
        float startRadius = radius * PING_START_SHARE;
        float endRadius = radius * (PING_START_SHARE + PING_REACH_PER_STAGE * (pings + 1));
        float seconds = PING_SECONDS / (1f + PING_SPEEDUP_PER_STAGE * (pings - 1));
        for (int ping = 0; ping < pings; ping++) {
            float progress = pingProgress(ringArt.elapsedSeconds(), seconds, ping, pings);
            float fade = (1f - progress) * (1f - progress);
            beams.ring(x, y, lerp(startRadius, endRadius, progress), null, look, PING_ALPHA * fade * alpha, ringWidthScale);
        }
        if (chain.isLast()) {
            float breath = (float) Math.sin(ringArt.elapsedSeconds() * FINAL_HALO_SPEED) * 0.5f + 0.5f;
            beams.ring(x, y, radius * FINAL_HALO_SHARE, null, look, (0.4f + 0.6f * breath) * alpha, ringWidthScale);
        }
    }

    static float pingProgress(float elapsedSeconds, float seconds, int ping, int pings) {
        float phase = elapsedSeconds / seconds + ping / (float) pings;
        return phase - (float) Math.floor(phase);
    }

    private NodeRingArt.NotableLook notableLook(boolean lit, boolean open, Color tint, Color accent) {
        if (!accent.equals(lookAccent)) {
            notableLooks.clear();
            lookAccent = accent;
        }
        NodeRingArt.NotableLook[] byState = notableLooks.computeIfAbsent(tint, key -> new NodeRingArt.NotableLook[LOOK_STATES]);
        int index = (lit ? 2 : 0) + (open ? 1 : 0);
        if (byState[index] == null) {
            byState[index] = new NodeRingArt.NotableLook(lit, open, tint, tint, accent);
        }
        return byState[index];
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
