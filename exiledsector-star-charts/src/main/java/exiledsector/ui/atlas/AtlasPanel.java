package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Translation;
import exiledsector.charts.atlas.AtlasBiome;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasNode;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasReach;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasSpecial;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.items.StarChart;
import exiledsector.ui.CanvasCamera;
import exiledsector.ui.PointerGesture;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.VanillaText;
import exiledsector.ui.socket.BenchModeSupport;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloShapes;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

public final class AtlasPanel extends BaseCustomUIPanelPlugin {

    static final float CELL_PIXELS = 80f;
    static final float NODE_RADIUS = 8f;
    static final float HOVER_RADIUS = 16f;
    static final float PAD = 12f;
    static final float HEADER_HEIGHT = 64f;
    static final float CLOSE_WIDTH = 90f;
    static final float CLOSE_HEIGHT = 26f;
    static final float TOOLTIP_WIDTH = 300f;
    static final float TOOLTIP_OFFSET = 18f;
    static final int STAR_COUNT = 320;
    static final int FAN_STEPS = 24;
    static final Color BACKGROUND = new Color(4, 6, 14);
    static final Color STAR_COLOUR = new Color(200, 210, 255);
    static final Color FAINT_COLOUR = new Color(120, 130, 160);
    static final Color EXPANSION_COLOUR = new Color(185, 110, 255);
    static final Color PHENOMENON_COLOUR = new Color(90, 220, 235);
    static final Color SPECIAL_COLOUR = new Color(255, 205, 90);
    static final Color CLEANSED_COLOUR = new Color(235, 240, 255);
    static final Color NORMAL_GLOW = new Color(80, 90, 130);

    private enum Control { CLOSE }

    private enum Mode { ATLAS }

    private final CustomPanelAPI hostPanel;
    private final StarChart chart;
    private final BenchModeSupport.TargetListener listener;
    private final AtlasGenerator generator;
    private final AtlasState state;
    private final CanvasCamera camera = new CanvasCamera();
    private final PointerGesture<Mode> gesture = new PointerGesture<>();
    private final Map<AtlasCell, AtlasNode> nodes = new LinkedHashMap<>();
    private final Set<AtlasCell> visible;
    private final Set<AtlasCell> reachable;
    private final List<AtlasBiome> biomes = new ArrayList<>();
    private final float[] starX = new float[STAR_COUNT];
    private final float[] starY = new float[STAR_COUNT];
    private final float[] starBrightness = new float[STAR_COUNT];
    private CustomPanelAPI panelRoot;
    private PositionAPI panelPosition;
    private UIComponentAPI tooltipElement;
    private AtlasCell tooltipCell;
    private AtlasCell hoveredCell;
    private float mouseX;
    private float mouseY;
    private float lastDragX;
    private float lastDragY;
    private float pulseSeconds;
    private boolean closing;
    private boolean decided;

    private AtlasPanel(CustomPanelAPI hostPanel, StarChart chart, BenchModeSupport.TargetListener listener, AtlasGenerator generator,
                       AtlasState state) {
        this.hostPanel = hostPanel;
        this.chart = chart;
        this.listener = listener;
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
        Random starRandom = new Random(state.seed());
        for (int star = 0; star < STAR_COUNT; star++) {
            starX[star] = starRandom.nextFloat();
            starY[star] = starRandom.nextFloat();
            starBrightness[star] = 0.15f + starRandom.nextFloat() * 0.5f;
        }
        centreOnFrontier();
    }

    public static AtlasPanel open(CustomPanelAPI hostPanel, StarChart chart, BenchModeSupport.TargetListener listener) {
        AtlasGenerator generator = AtlasRuntime.generator(Global.getSector());
        AtlasPanel panel = new AtlasPanel(hostPanel, chart, listener, generator, AtlasState.get(Global.getSector()));
        panel.attach();
        return panel;
    }

    private void centreOnFrontier() {
        float sumX = 0f;
        float sumY = 0f;
        Set<AtlasCell> focus = reachable.isEmpty() ? Set.of(AtlasCell.ORIGIN) : reachable;
        for (AtlasCell cell : focus) {
            sumX += generator.mapX(cell);
            sumY += generator.mapY(cell);
        }
        camera.centreOn(sumX / focus.size() * CELL_PIXELS, sumY / focus.size() * CELL_PIXELS);
    }

    private void attach() {
        PositionAPI host = hostPanel.getPosition();
        panelRoot = Global.getSettings().createCustom(host.getWidth(), host.getHeight(), this);
        hostPanel.addComponent(panelRoot).inTL(0f, 0f);
        I18n.forGameText(() -> buildHeader(host.getWidth()));
        SkillTreeSounds.panelOpened();
    }

    private void buildHeader(float width) {
        TooltipMakerAPI element = panelRoot.createUIElement(width - PAD * 2f, HEADER_HEIGHT, false);
        element.setParaFont(Fonts.ORBITRON_20AABOLD);
        VanillaText.plainPara(element, Translation.text("chart.atlas.title"), 0f, Misc.getBasePlayerColor());
        element.setParaFontDefault();
        VanillaText.plainPara(element, Translation.msg("chart.atlas.summary").arg("chart", chart == null ? "" : chart.name())
                .arg("points", state.atlasPoints()).arg("fragments", state.keyFragments()).text(), 4f, Misc.getTextColor());
        VanillaText.plainPara(element, Translation.text("chart.atlas.hint"), 2f, Misc.getGrayColor());
        element.addButton(Translation.text("chart.atlas.close"), Control.CLOSE, CLOSE_WIDTH, CLOSE_HEIGHT, 0f).getPosition().inTR(0f, 0f);
        panelRoot.addUIElement(element).inTL(PAD, PAD);
    }

    @Override
    public void positionChanged(PositionAPI position) {
        panelPosition = position;
    }

    @Override
    public void buttonPressed(Object buttonId) {
        if (buttonId == Control.CLOSE) {
            cancel();
        }
    }

    private void cancel() {
        if (!decided) {
            decided = true;
            listener.cancelled();
        }
        closing = true;
    }

    private void pick(AtlasCell cell) {
        AtlasOpening.Blocker blocker = AtlasOpening.check(generator, state, cell, chart);
        if (!blocker.allowsOpening()) {
            SkillTreeSounds.refused();
            return;
        }
        decided = true;
        closing = true;
        Supplier<List<String>> lines = () -> AtlasNodeText.confirmLines(generator, state, cell);
        listener.picked(cell, I18n.forGameText(lines));
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (panelPosition == null || closing) {
            return;
        }
        float left = panelPosition.getX();
        float bottom = panelPosition.getY();
        float width = panelPosition.getWidth();
        float height = panelPosition.getHeight();
        GLDraw.fillQuad(left, bottom, width, height, BACKGROUND, 0.96f * alphaMult);
        float screenScale = Global.getSettings().getScreenScaleMult();
        TreeViewport view = camera.viewport(panelPosition);
        try (GlScope clip = GlScope.clip((int) (left * screenScale), (int) (bottom * screenScale), (int) (width * screenScale),
                (int) (height * screenScale)); GlScope flat = GlScope.flat()) {
            drawStars(left, bottom, width, height, alphaMult);
            drawBiomes(view, alphaMult);
            drawLanes(view, alphaMult);
            drawNodes(view, alphaMult);
        }
    }

    private void drawStars(float left, float bottom, float width, float height, float alphaMult) {
        GL11.glPointSize(1.5f);
        GL11.glBegin(GL11.GL_POINTS);
        float driftX = camera.panX() * 0.05f;
        float driftY = camera.panY() * 0.05f;
        for (int star = 0; star < STAR_COUNT; star++) {
            float x = left + Math.floorMod((int) (starX[star] * width + driftX), (int) Math.max(1f, width));
            float y = bottom + Math.floorMod((int) (starY[star] * height + driftY), (int) Math.max(1f, height));
            Misc.setColor(STAR_COLOUR, starBrightness[star] * alphaMult);
            GL11.glVertex2f(x, y);
        }
        GL11.glEnd();
        GL11.glPointSize(1f);
    }

    private void drawBiomes(TreeViewport view, float alphaMult) {
        for (AtlasBiome biome : biomes) {
            float x = view.screenX(biome.centreX() * CELL_PIXELS);
            float y = view.screenY(biome.centreY() * CELL_PIXELS);
            float radius = biome.radius() * CELL_PIXELS * view.zoom() * 1.35f;
            if (view.isVisible(x, y, radius)) {
                HoloShapes.radialGlow(x, y, 0f, radius, regionColour(biome.regionId()), 0.3f * alphaMult);
            }
        }
    }

    static Color regionColour(String regionId) {
        Color tint = AtlasData.regionTint(regionId);
        return tint == null ? NORMAL_GLOW : tint;
    }

    private void drawLanes(TreeViewport view, float alphaMult) {
        for (AtlasCell cell : visible) {
            for (AtlasCell neighbour : generator.lanes(cell)) {
                boolean bothVisible = visible.contains(neighbour);
                if (bothVisible && neighbour.pack() < cell.pack()) {
                    continue;
                }
                Color colour = laneColour(cell, neighbour);
                float alpha = (bothVisible ? 0.55f : 0.2f) * alphaMult;
                HoloShapes.line(screenX(view, cell), screenY(view, cell), screenX(view, neighbour), screenY(view, neighbour), 1.5f, colour, alpha);
            }
        }
    }

    private Color laneColour(AtlasCell from, AtlasCell to) {
        boolean fromDone = state.isCompleted(from);
        boolean toDone = state.isCompleted(to);
        if (fromDone && toDone) {
            return Misc.getPositiveHighlightColor();
        }
        if (fromDone && reachable.contains(to) || toDone && reachable.contains(from)) {
            return Misc.getHighlightColor();
        }
        return FAINT_COLOUR;
    }

    private void drawNodes(TreeViewport view, float alphaMult) {
        float sizeScale = Math.max(0.6f, Math.min(1.4f, view.zoom()));
        float radius = NODE_RADIUS * sizeScale;
        float pulse = 0.5f + 0.5f * (float) Math.sin(pulseSeconds * 4f);
        for (Map.Entry<AtlasCell, AtlasNode> entry : nodes.entrySet()) {
            AtlasCell cell = entry.getKey();
            AtlasNode node = entry.getValue();
            if (node.isVoid()) {
                continue;
            }
            float x = screenX(view, cell);
            float y = screenY(view, cell);
            if (!view.isVisible(x, y, radius * 3f)) {
                continue;
            }
            if (!visible.contains(cell)) {
                HoloShapes.diamond(x, y, 2f, FAINT_COLOUR, 0.5f * alphaMult);
                continue;
            }
            Color colour = nodeColour(cell);
            disc(x, y, radius, colour, 0.85f * alphaMult);
            HoloShapes.circle(x, y, radius, colour, 1.5f, alphaMult);
            if (reachable.contains(cell)) {
                HoloShapes.circle(x, y, radius + 3f + 3f * pulse, Misc.getHighlightColor(), 1.5f, (0.4f + 0.5f * pulse) * alphaMult);
            }
            drawMarkers(cell, node, x, y, radius, alphaMult);
            if (cell.equals(hoveredCell)) {
                HoloShapes.brackets(x, y, radius + 7f, 5f, Misc.getBrightPlayerColor(), alphaMult);
            }
        }
    }

    private Color nodeColour(AtlasCell cell) {
        if (state.isCompleted(cell)) {
            return Misc.getPositiveHighlightColor();
        }
        if (AtlasReach.isSpentBastion(generator, state, cell) || state.has(cell, AtlasState.FAILED)) {
            return Misc.getNegativeHighlightColor().darker();
        }
        if (reachable.contains(cell)) {
            return Misc.getBrightPlayerColor();
        }
        return FAINT_COLOUR;
    }

    private void drawMarkers(AtlasCell cell, AtlasNode node, float x, float y, float radius, float alphaMult) {
        boolean spent = state.has(cell, AtlasState.MARKERS_SPENT);
        float offset = radius + 4f;
        if (node.powerfulBoss() && !spent) {
            disc(x + offset, y + offset, 3f, Misc.getNegativeHighlightColor(), alphaMult);
        }
        if (node.expansionId() != null && !spent) {
            disc(x - offset, y + offset, 3f, EXPANSION_COLOUR, alphaMult);
        }
        if (node.phenomenonId() != null) {
            disc(x - offset, y - offset, 3f, PHENOMENON_COLOUR, alphaMult);
        }
        AtlasSpecial special = generator.specialAt(cell);
        if (special != null) {
            HoloShapes.diamond(x, y + offset + 4f, 5f, SPECIAL_COLOUR, alphaMult);
        }
        if (node.infestationId() != null) {
            boolean cleansed = state.has(cell, AtlasState.CLEANSED);
            HoloShapes.circle(x, y, radius + 6f, cleansed ? CLEANSED_COLOUR : Misc.getNegativeHighlightColor(), 1f, 0.7f * alphaMult);
        }
    }

    private static void disc(float x, float y, float radius, Color colour, float alpha) {
        Misc.setColor(colour, alpha);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(x, y);
        for (int step = 0; step <= FAN_STEPS; step++) {
            double angle = Math.PI * 2.0 * step / FAN_STEPS;
            GL11.glVertex2f(x + (float) Math.cos(angle) * radius, y + (float) Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    private float screenX(TreeViewport view, AtlasCell cell) {
        return view.screenX(generator.mapX(cell) * CELL_PIXELS);
    }

    private float screenY(TreeViewport view, AtlasCell cell) {
        return view.screenY(generator.mapY(cell) * CELL_PIXELS);
    }

    private AtlasCell cellAt(float x, float y) {
        if (panelPosition == null) {
            return null;
        }
        TreeViewport view = camera.viewport(panelPosition);
        AtlasCell nearest = null;
        float nearestDistance = HOVER_RADIUS * HOVER_RADIUS;
        for (Map.Entry<AtlasCell, AtlasNode> entry : nodes.entrySet()) {
            if (entry.getValue().isVoid() || !visible.contains(entry.getKey())) {
                continue;
            }
            float dx = screenX(view, entry.getKey()) - x;
            float dy = screenY(view, entry.getKey()) - y;
            float distance = dx * dx + dy * dy;
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = entry.getKey();
            }
        }
        return nearest;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (closing || panelPosition == null) {
            return;
        }
        for (InputEventAPI event : events) {
            if (event.isConsumed()) {
                continue;
            }
            if (event.isKeyboardEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
                if (event.isKeyUpEvent()) {
                    cancel();
                }
                event.consume();
                continue;
            }
            if (!event.isMouseEvent()) {
                continue;
            }
            float x = event.getX();
            float y = event.getY();
            if (event.isMouseMoveEvent()) {
                mouseX = x;
                mouseY = y;
                if (gesture.moveTo(x, y)) {
                    camera.startDrag();
                    lastDragX = x;
                    lastDragY = y;
                }
                if (camera.isDragging()) {
                    camera.dragBy(x - lastDragX, y - lastDragY);
                    lastDragX = x;
                    lastDragY = y;
                }
            } else if (event.isMouseDownEvent() && event.isLMBEvent()) {
                AtlasCell target = cellAt(x, y);
                if (target != null) {
                    gesture.pressTarget(Mode.ATLAS, target, x, y, event.isCtrlDown(), event.isShiftDown(), true);
                } else {
                    gesture.pressPan(Mode.ATLAS, x, y);
                    camera.startDrag();
                    lastDragX = x;
                    lastDragY = y;
                }
            } else if (event.isMouseUpEvent() && event.isLMBUpEvent()) {
                AtlasCell released = cellAt(x, y);
                if (gesture.releasedOn(released) && reachable.contains(released)) {
                    pick(released);
                }
                gesture.clear();
                camera.stopDrag();
            } else if (event.isMouseScrollEvent()) {
                float pivotX = x - (panelPosition.getX() + panelPosition.getWidth() / 2f);
                float pivotY = y - (panelPosition.getY() + panelPosition.getHeight() / 2f);
                camera.scroll(event.getEventValue() > 0, pivotX, pivotY);
            }
            event.consume();
        }
    }

    @Override
    public void advance(float amount) {
        if (closing) {
            if (panelRoot != null) {
                hostPanel.removeComponent(panelRoot);
                panelRoot = null;
            }
            return;
        }
        pulseSeconds += amount;
        camera.advanceZoom(amount);
        camera.advancePan(amount);
        hoveredCell = cellAt(mouseX, mouseY);
        if (hoveredCell == null ? tooltipCell != null : !hoveredCell.equals(tooltipCell)) {
            I18n.forGameText(this::rebuildTooltip);
        }
    }

    private void rebuildTooltip() {
        if (tooltipElement != null) {
            panelRoot.removeComponent(tooltipElement);
            tooltipElement = null;
        }
        tooltipCell = hoveredCell;
        if (hoveredCell == null || panelPosition == null) {
            return;
        }
        List<AtlasNodeText.Line> lines = AtlasNodeText.describe(generator, state, hoveredCell, chart, reachable.contains(hoveredCell));
        TooltipMakerAPI element = panelRoot.createUIElement(TOOLTIP_WIDTH, 24f + lines.size() * 20f, false);
        boolean first = true;
        for (AtlasNodeText.Line line : lines) {
            VanillaText.plainPara(element, line.text(), first ? 0f : 3f, toneColour(line.tone()));
            first = false;
        }
        float height = element.getHeightSoFar() + 8f;
        float left = Math.min(mouseX - panelPosition.getX() + TOOLTIP_OFFSET, panelPosition.getWidth() - TOOLTIP_WIDTH - PAD);
        float top = Math.max(HEADER_HEIGHT + PAD, Math.min(panelPosition.getY() + panelPosition.getHeight() - mouseY + TOOLTIP_OFFSET,
                panelPosition.getHeight() - height - PAD));
        tooltipElement = element;
        panelRoot.addUIElement(element).inTL(left, top);
    }

    static Color toneColour(AtlasNodeText.Tone tone) {
        return switch (tone) {
            case TITLE -> Misc.getBasePlayerColor();
            case MARKER -> Misc.getHighlightColor();
            case GOOD -> Misc.getPositiveHighlightColor();
            case BAD -> Misc.getNegativeHighlightColor();
            case HINT -> Misc.getGrayColor();
            default -> Misc.getTextColor();
        };
    }
}
