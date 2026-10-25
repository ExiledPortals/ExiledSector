package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Style;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.skills.skilleffect.render.Fade;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.ui.CampaignOverlay;
import exiledsector.ui.CanvasCamera;
import exiledsector.ui.ChromeMetrics;
import exiledsector.ui.PointerGesture;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.decoration.StarfieldRenderer;
import exiledsector.ui.socket.BenchMode;
import exiledsector.ui.socket.BenchModeSupport;
import exiledsector.ui.socket.ModificationBay;
import exiledsector.ui.socket.SocketPanelPlacement;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.TextLabel;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class AtlasScreen extends BaseCustomUIPanelPlugin implements CampaignOverlay.Screen {

    static final float PANEL_MARGIN = 20f;
    static final float BORDER_INSET = 16f;
    static final float BENCH_MARGIN = 40f;
    static final float CHROME_PAD = 24f;
    static final float SUMMARY_GAP = 6f;
    static final float CLOSE_WIDTH = 120f;
    static final float OPEN_SECONDS = 0.2f;
    static final float DIM_SECONDS = 0.2f;
    static final float BACKDROP_ALPHA = 0.8f;
    static final float BENCH_DIM_ALPHA = 0.65f;
    static final Color HINT_COLOUR = new Color(150, 150, 150);

    private enum Control { CLOSE }

    private final CampaignOverlay.Host host;
    private final SkillTreePanelStyle panelStyle = new SkillTreePanelStyle();
    private final StarfieldRenderer starfield = new StarfieldRenderer(panelStyle);
    private final CanvasCamera camera = new CanvasCamera(AtlasMapCanvas.MAX_ZOOM);
    private final PointerGesture<Void> gesture = new PointerGesture<>();
    private final TextLabel titleLabel = new TextLabel(SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel summaryLabel = new TextLabel(SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final TextLabel hintLabel = new TextLabel(SkillTreePanelStyle.TOOLTIP_FLAVOUR_FONT_SIZE, HINT_COLOUR);
    private final TextLabel tooltipTitle = new TextLabel(SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel tooltipBody = new TextLabel(SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private AtlasMapCanvas map;
    private CustomPanelAPI panelRoot;
    private PositionAPI panelPosition;
    private ModificationBay bay;
    private AtlasCell tooltipCell;
    private AtlasCell hoveredCell;
    private String wormholeMessage;
    private float mouseX;
    private float mouseY;
    private float openLevel;
    private float benchDim;
    private boolean closing;
    private boolean closeRequested;

    private AtlasScreen(CampaignOverlay.Host host) {
        this.host = host;
    }

    public static boolean open(CampaignUIAPI campaignUI) {
        return CampaignOverlay.open(campaignUI, AtlasScreen::new, null);
    }

    @Override
    public void attach(CustomPanelAPI panel) {
        panelRoot = panel;
        map = new AtlasMapCanvas(AtlasRuntime.generator(Global.getSector()), AtlasState.get(Global.getSector()));
        camera.jumpZoomTo(AtlasMapCanvas.DEFAULT_ZOOM);
        camera.centreOn(map.focusX(), map.focusY());
        I18n.forGameText(this::buildChrome);
    }

    private void buildChrome() {
        titleLabel.set(Translation.text("chart.atlas.title"));
        writeSummary();
        hintLabel.set(Translation.text("chart.atlas.hint"));
        PositionAPI position = panelRoot.getPosition();
        TooltipMakerAPI element = panelRoot.createUIElement(CLOSE_WIDTH, ChromeMetrics.CONTROL_HEIGHT, false);
        element.addButton(Translation.text("chart.atlas.close"), Control.CLOSE, CLOSE_WIDTH, ChromeMetrics.CONTROL_HEIGHT, 0f);
        panelRoot.addUIElement(element).inTL(position.getWidth() - PANEL_MARGIN - CHROME_PAD - CLOSE_WIDTH, PANEL_MARGIN + CHROME_PAD);
    }

    private void writeSummary() {
        AtlasState state = map.state();
        summaryLabel.set(Translation.msg("chart.atlas.summary").arg("points", state.atlasPoints()).arg("fragments", state.keyFragments()).text());
    }

    @Override
    public void positionChanged(PositionAPI position) {
        panelPosition = position;
    }

    private float innerLeft() {
        return panelPosition.getX() + PANEL_MARGIN;
    }

    private float innerBottom() {
        return panelPosition.getY() + PANEL_MARGIN;
    }

    private float innerWidth() {
        return panelPosition.getWidth() - PANEL_MARGIN * 2f;
    }

    private float innerHeight() {
        return panelPosition.getHeight() - PANEL_MARGIN * 2f;
    }

    private TreeViewport viewport() {
        return camera.viewport(panelPosition);
    }

    @Override
    public void buttonPressed(Object buttonId) {
        if (buttonId == Control.CLOSE) {
            close();
        }
    }

    private void close() {
        if (bay != null && bay.isOpen()) {
            bay.close();
        }
        closing = true;
    }

    private void pick(AtlasCell cell) {
        if (!map.isReachable(cell)) {
            SkillTreeSounds.refused();
            return;
        }
        map.pulse(cell);
        SkillTreeSounds.panelOpened();
        BenchModeSupport.OpenTarget target = new BenchModeSupport.OpenTarget(cell,
                () -> AtlasNodeText.confirmLines(map.generator(), map.state(), cell));
        SocketPanelPlacement area = new SocketPanelPlacement(BENCH_MARGIN, BENCH_MARGIN, panelPosition.getWidth() - BENCH_MARGIN * 2f,
                panelPosition.getHeight() - BENCH_MARGIN * 2f, false);
        bay = ModificationBay.openMode(panelRoot, area, BenchMode.STAR_CHARTS, item -> null, target, new ModificationBay.Listener() {
            @Override
            public void closed() {
                bay = null;
                if (wormholeMessage != null) {
                    closing = true;
                }
            }

            @Override
            public void debugActionApplied() {
                map = new AtlasMapCanvas(map.generator(), map.state(), map.scenery());
                tooltipCell = null;
                map.pulse(cell);
                map.collapse(cell);
                I18n.forGameText(AtlasScreen.this::writeSummary);
            }

            @Override
            public void pressedInside() {
            }

            @Override
            public void wormholeOpened(String message) {
                wormholeMessage = message;
            }
        });
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (panelPosition == null || map == null) {
            return;
        }
        for (InputEventAPI event : events) {
            if (event.isConsumed()) {
                continue;
            }
            if (closing) {
                event.consume();
            } else if (bay != null) {
                benchInput(event);
            } else if (event.isKeyboardEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
                if (event.isKeyUpEvent()) {
                    close();
                }
                event.consume();
            } else if (event.isMouseEvent()) {
                mapInput(event);
            } else {
                event.consume();
            }
        }
    }

    private void benchInput(InputEventAPI event) {
        if (event.isMouseMoveEvent()) {
            mouseX = event.getX();
            mouseY = event.getY();
        }
        if (event.isKeyUpEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
            bay.escape();
        } else if (event.isLMBDownEvent() && !bay.contains(event.getX(), event.getY())) {
            bay.close();
        }
        event.consume();
    }

    private void mapInput(InputEventAPI event) {
        float x = event.getX();
        float y = event.getY();
        if (event.isMouseMoveEvent()) {
            mouseX = x;
            mouseY = y;
            if (gesture.moveTo(x, y)) {
                camera.startDrag();
                camera.dragBy(x - gesture.pressX(), y - gesture.pressY());
            } else {
                camera.dragBy(event.getDX(), event.getDY());
            }
        } else if (event.isMouseDownEvent() && event.isLMBEvent()) {
            AtlasCell target = map.cellAt(viewport(), x, y);
            if (target != null) {
                gesture.pressTarget(null, target, x, y, event.isCtrlDown(), event.isShiftDown(), true);
            } else {
                gesture.pressPan(null, x, y);
                camera.startDrag();
            }
        } else if (event.isMouseUpEvent() && event.isLMBUpEvent()) {
            AtlasCell released = map.cellAt(viewport(), x, y);
            if (released != null && gesture.releasedOn(released)) {
                pick(released);
            }
            gesture.clear();
            camera.stopDrag();
        } else if (event.isMouseScrollEvent()) {
            camera.scrollAt(event.getEventValue() > 0, x, y, panelPosition);
        }
        event.consume();
    }

    @Override
    public void advance(float amount) {
        if (panelRoot == null || map == null) {
            return;
        }
        openLevel = Fade.step(openLevel, !closing, amount / OPEN_SECONDS);
        benchDim = Fade.step(benchDim, bay != null, amount / DIM_SECONDS);
        if (closing && openLevel <= 0f && !closeRequested) {
            closeRequested = true;
            if (wormholeMessage != null) {
                host.leave(wormholeMessage);
            } else {
                host.close();
            }
            return;
        }
        starfield.advance(amount);
        map.advance(amount);
        camera.advanceZoom(amount);
        camera.advancePan(amount);
        if (panelPosition != null) {
            AtlasCell hovered = bay == null && !camera.isDragging() ? map.cellAt(viewport(), mouseX, mouseY) : null;
            if (hovered != null && !hovered.equals(hoveredCell)) {
                map.pulse(hovered);
            }
            hoveredCell = hovered;
        }
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (panelPosition == null || map == null) {
            return;
        }
        float alpha = Fade.smooth(openLevel) * alphaMult;
        if (alpha <= 0f) {
            return;
        }
        GLDraw.fillQuad(panelPosition.getX(), panelPosition.getY(), panelPosition.getWidth(), panelPosition.getHeight(), Color.BLACK,
                BACKDROP_ALPHA * alpha);
        GLDraw.fillQuad(innerLeft(), innerBottom(), innerWidth(), innerHeight(), Color.BLACK, alpha);
        BorderedPanel.draw(innerLeft(), innerBottom(), innerWidth(), innerHeight(), alpha);
        float screenScale = Global.getSettings().getScreenScaleMult();
        try (GlScope clip = GlScope.clip((int) ((innerLeft() + BORDER_INSET) * screenScale), (int) (innerBottom() * screenScale),
                (int) ((innerWidth() - BORDER_INSET * 2f) * screenScale), (int) (innerHeight() * screenScale))) {
            starfield.render(panelPosition, camera.panX(), camera.panY(), alpha);
            map.render(viewport(), panelStyle.getAccentColor(), alpha);
        }
        drawChrome(alpha);
        if (benchDim > 0f) {
            GLDraw.fillQuad(innerLeft(), innerBottom(), innerWidth(), innerHeight(), Color.BLACK, BENCH_DIM_ALPHA * Fade.smooth(benchDim) * alpha);
        } else if (hoveredCell != null && !gesture.isPanning()) {
            drawTooltip(alpha);
        }
    }

    private void drawChrome(float alpha) {
        float left = innerLeft() + CHROME_PAD;
        float top = innerBottom() + innerHeight() - CHROME_PAD;
        titleLabel.setAlpha(alpha).draw(left, top);
        summaryLabel.setAlpha(alpha).draw(left, top - titleLabel.height() - SUMMARY_GAP);
        hintLabel.setAlpha(alpha).draw(left, innerBottom() + CHROME_PAD + hintLabel.height());
    }

    private void drawTooltip(float alpha) {
        if (!hoveredCell.equals(tooltipCell)) {
            tooltipCell = hoveredCell;
            I18n.forGameText(this::writeTooltip);
        }
        panelStyle.drawTitleBodyTooltip(tooltipTitle, tooltipBody, mouseX, mouseY, alpha);
    }

    private void writeTooltip() {
        List<AtlasNodeText.Line> lines = AtlasNodeText.describe(map.generator(), map.state(), tooltipCell, map.isReachable(tooltipCell));
        tooltipTitle.set(lines.isEmpty() ? "" : lines.get(0).text());
        List<StyledText> body = new ArrayList<>();
        for (AtlasNodeText.Line line : lines.subList(Math.min(1, lines.size()), lines.size())) {
            body.add(styled(line));
        }
        tooltipBody.setStyled(StyledText.join(StyledText.of("\n"), body), SkillTreePanelStyle::standardHighlightColor);
    }

    static StyledText styled(AtlasNodeText.Line line) {
        return switch (line.tone()) {
            case MARKER -> StyledText.styled(line.text(), Style.HIGHLIGHT);
            case GOOD -> StyledText.styled(line.text(), Style.GOOD);
            case BAD -> StyledText.styled(line.text(), Style.BAD);
            case HINT -> StyledText.styled(line.text(), Style.MUTED);
            default -> StyledText.of(line.text());
        };
    }
}
