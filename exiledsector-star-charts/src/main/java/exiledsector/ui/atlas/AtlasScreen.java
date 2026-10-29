package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.render.Fade;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasCatalystUse;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.layout.sketch.SectorSystemSketcher;
import exiledsector.charts.wormhole.WormholeGateway;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.items.AtlasCatalystItems;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.MaterialItems;
import exiledsector.items.UseVerdict;
import exiledsector.ui.CampaignOverlay;
import exiledsector.ui.CanvasCamera;
import exiledsector.ui.ChromeMetrics;
import exiledsector.ui.PointerGesture;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.decoration.StarfieldRenderer;
import exiledsector.ui.socket.AtlasAnomalyPanel;
import exiledsector.ui.socket.AtlasCatalystPanel;
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
import java.util.concurrent.atomic.AtomicReference;

public final class AtlasScreen extends BaseCustomUIPanelPlugin implements CampaignOverlay.Screen {

    static final float PANEL_MARGIN = 20f;
    static final float BORDER_INSET = 16f;
    static final float BENCH_MARGIN = 40f;
    static final float CHROME_PAD = 24f;
    static final float SUMMARY_GAP = 6f;
    static final float CLOSE_WIDTH = 120f;
    static final float BORDERS_WIDTH = 160f;
    static final float BIOMES_WIDTH = 120f;
    static final float CATALYSTS_WIDTH = 150f;
    static final float BUTTON_GAP = 10f;
    static final float OPEN_SECONDS = 0.2f;
    static final float DIM_SECONDS = 0.2f;
    static final float BACKDROP_ALPHA = 0.8f;
    static final float BENCH_DIM_ALPHA = 0.65f;
    static final float FOCUS_SECONDS = 0.6f;
    static final float FOCUS_DIM_ALPHA = 0.85f;
    static final float CHOOSE_WIDTH = 320f;
    static final float CHOOSE_HEIGHT = 50f;
    static final float TOOLTIP_MAX_TEXT_HEIGHT = 800f;
    static final Color HINT_COLOUR = new Color(150, 150, 150);

    private enum Control { CLOSE, BORDERS, BIOMES, CATALYSTS, CHOOSE_CHART }

    enum Overlay { NONE, FACTIONS, BIOMES }

    private static final AtomicReference<Overlay> OVERLAY_SHOWN = new AtomicReference<>(Overlay.FACTIONS);

    private final CampaignOverlay.Host host;
    private final SkillTreePanelStyle panelStyle = new SkillTreePanelStyle();
    private final StarfieldRenderer starfield = new StarfieldRenderer(panelStyle);
    private final CanvasCamera camera = new CanvasCamera(AtlasMapCanvas.MIN_ZOOM, AtlasMapCanvas.MAX_ZOOM);
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
    private ButtonAPI bordersButton;
    private ButtonAPI biomesButton;
    private ButtonAPI catalystsButton;
    private AtlasCatalystPanel catalystPanel;
    private AtlasCatalysts.Catalyst selectedCatalyst;
    private AtlasAnomalyPanel anomalyPanel;
    private TooltipMakerAPI chooseElement;
    private ButtonAPI chooseButton;
    private AtlasCell focusedCell;
    private boolean focusing;
    private boolean focusOpenedCatalysts;
    private float focusLevel;
    private Object tooltipSubject;
    private AtlasCell hoveredCell;
    private AtlasMapCanvas.AnomalyIcon hoveredAnomaly;
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
        SectorAPI sector = Global.getSector();
        AtlasGenerator generator = AtlasRuntime.generator(sector);
        map = new AtlasMapCanvas(generator, AtlasState.get(sector), new AtlasScenery(generator.seed()),
                SectorSystemSketcher.sketch(WormholeGateway.playerGatewaySystem(sector)));
        showOverlay(true);
        camera.jumpZoomTo(AtlasMapCanvas.MIN_ZOOM);
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
        TooltipMakerAPI toggles = panelRoot.createUIElement(BORDERS_WIDTH, ChromeMetrics.CONTROL_HEIGHT, false);
        bordersButton = toggles.addAreaCheckbox(Translation.text("chart.atlas.borders"), Control.BORDERS, Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), BORDERS_WIDTH, ChromeMetrics.CONTROL_HEIGHT, 0f);
        bordersButton.setChecked(OVERLAY_SHOWN.get() == Overlay.FACTIONS);
        float bordersLeft = position.getWidth() - PANEL_MARGIN - CHROME_PAD - CLOSE_WIDTH - BUTTON_GAP - BORDERS_WIDTH;
        panelRoot.addUIElement(toggles).inTL(bordersLeft, PANEL_MARGIN + CHROME_PAD);
        TooltipMakerAPI biomeToggle = panelRoot.createUIElement(BIOMES_WIDTH, ChromeMetrics.CONTROL_HEIGHT, false);
        biomesButton = biomeToggle.addAreaCheckbox(Translation.text("chart.atlas.biomes"), Control.BIOMES, Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), BIOMES_WIDTH, ChromeMetrics.CONTROL_HEIGHT, 0f);
        biomesButton.setChecked(OVERLAY_SHOWN.get() == Overlay.BIOMES);
        float biomesLeft = bordersLeft - BUTTON_GAP - BIOMES_WIDTH;
        panelRoot.addUIElement(biomeToggle).inTL(biomesLeft, PANEL_MARGIN + CHROME_PAD);
        TooltipMakerAPI catalystToggle = panelRoot.createUIElement(CATALYSTS_WIDTH, ChromeMetrics.CONTROL_HEIGHT, false);
        catalystsButton = catalystToggle.addAreaCheckbox(catalystsLabel(), Control.CATALYSTS, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                Misc.getBrightPlayerColor(), CATALYSTS_WIDTH, ChromeMetrics.CONTROL_HEIGHT, 0f);
        panelRoot.addUIElement(catalystToggle).inTL(biomesLeft - BUTTON_GAP - CATALYSTS_WIDTH, PANEL_MARGIN + CHROME_PAD);
    }

    private static String catalystsLabel() {
        int carried = AtlasCatalystItems.carriedTotal(CraftingMaterials.forPlayer(PlayerFleet.cargoOrNull()));
        return Translation.msg("chart.atlas.catalysts").arg("count", carried).text();
    }

    private void toggleCatalysts() {
        if (catalystPanel != null) {
            catalystPanel.close();
            return;
        }
        PositionAPI position = panelRoot.getPosition();
        float top = PANEL_MARGIN + CHROME_PAD + ChromeMetrics.CONTROL_HEIGHT + BUTTON_GAP;
        float left = position.getWidth() - PANEL_MARGIN - CHROME_PAD - AtlasCatalystPanel.PANEL_WIDTH;
        catalystPanel = AtlasCatalystPanel.open(panelRoot, left, top, position.getHeight() - top - PANEL_MARGIN - CHROME_PAD,
                new AtlasCatalystPanel.Listener() {
                    @Override
                    public void selected(AtlasCatalysts.Catalyst catalyst) {
                        selectedCatalyst = catalyst;
                        retarget();
                        if (catalyst != null && focusing) {
                            targetPicked(focusedCell);
                        }
                    }

                    @Override
                    public void used(AtlasCell cell) {
                        map = map.rebuilt();
                        map.pulse(cell);
                        retarget();
                        if (catalystsButton != null) {
                            catalystsButton.setText(catalystsLabel());
                        }
                        if (anomalyPanel != null) {
                            anomalyPanel.refresh();
                        }
                    }

                    @Override
                    public void closed() {
                        catalystPanel = null;
                        focusOpenedCatalysts = false;
                        selectedCatalyst = null;
                        retarget();
                        if (catalystsButton != null) {
                            catalystsButton.setChecked(false);
                        }
                    }

                    @Override
                    public void pressedInside() {
                    }
                });
        SkillTreeSounds.panelOpened();
    }

    private void enterFocus(AtlasCell cell) {
        if (catalystPanel != null && catalystPanel.confirming()) {
            return;
        }
        focusedCell = cell;
        focusing = true;
        tooltipSubject = null;
        gesture.clear();
        camera.stopMoving();
        map.pulse(cell);
        SkillTreeSounds.panelOpened();
        if (catalystPanel == null) {
            openFocusCatalysts();
        } else if (catalystPanel.selected() != null) {
            catalystPanel.escape();
        }
        I18n.forGameText(() -> addChooseButton(cell));
        float top = PANEL_MARGIN + CHROME_PAD + ChromeMetrics.CONTROL_HEIGHT + BUTTON_GAP;
        anomalyPanel = AtlasAnomalyPanel.open(panelRoot, PANEL_MARGIN + CHROME_PAD, top, panelRoot.getPosition().getHeight() - top - PANEL_MARGIN - CHROME_PAD,
                this::focusedAnomalies, () -> {
                    anomalyPanel = null;
                    leaveFocus();
                });
    }

    private void openFocusCatalysts() {
        toggleCatalysts();
        focusOpenedCatalysts = true;
        if (catalystsButton != null) {
            catalystsButton.setChecked(true);
        }
    }

    private void addChooseButton(AtlasCell cell) {
        PositionAPI position = panelRoot.getPosition();
        TooltipMakerAPI element = panelRoot.createUIElement(CHOOSE_WIDTH, CHOOSE_HEIGHT, false);
        element.setButtonFontOrbitron20Bold();
        chooseButton = element.addButton(Translation.text("chart.atlas.focus.chooseChart"), Control.CHOOSE_CHART, CHOOSE_WIDTH, CHOOSE_HEIGHT, 0f);
        element.setButtonFontDefault();
        chooseButton.setEnabled(map.isReachable(cell));
        panelRoot.addUIElement(element).inTL((position.getWidth() - CHOOSE_WIDTH) / 2f, position.getHeight() - PANEL_MARGIN - CHROME_PAD - CHOOSE_HEIGHT);
        element.setOpacity(0f);
        chooseElement = element;
    }

    private void removeChooseButton() {
        if (chooseElement != null) {
            panelRoot.removeComponent(chooseElement);
            chooseElement = null;
            chooseButton = null;
        }
    }

    private void leaveFocus() {
        if (!focusing) {
            return;
        }
        focusing = false;
        if (chooseButton != null) {
            chooseButton.setEnabled(false);
        }
        if (anomalyPanel != null) {
            anomalyPanel.close();
        }
        if (catalystPanel != null && focusOpenedCatalysts) {
            catalystPanel.close();
        } else if (catalystPanel != null && catalystPanel.selected() != null) {
            catalystPanel.escape();
        }
        focusOpenedCatalysts = false;
    }

    private List<AtlasAnomalyPanel.Entry> focusedAnomalies() {
        List<AtlasAnomalyPanel.Entry> entries = new ArrayList<>();
        if (focusedCell == null) {
            return entries;
        }
        for (String anomalyId : AtlasAnomalies.effective(map.generator(), map.state(), focusedCell)) {
            ChartAnomalies.Definition definition = ChartAnomalies.definition(anomalyId);
            entries.add(new AtlasAnomalyPanel.Entry(AtlasNodeText.name("anomaly", anomalyId), definition == null ? "" : definition.icon(),
                    AtlasNodeText.anomalyBody(map.generator(), map.state(), focusedCell, anomalyId)));
        }
        return entries;
    }

    private float focusCentreX() {
        return innerLeft() + innerWidth() / 2f;
    }

    private float focusCentreY() {
        return innerBottom() + innerHeight() / 2f;
    }

    private float focusReach() {
        return Math.min(innerWidth() / 2f - BORDER_INSET, innerHeight() / 2f);
    }

    private AtlasCatalystUse catalystUse() {
        return selectedCatalyst == null ? null
                : AtlasCatalystUse.of(selectedCatalyst, map.generator(), map.state(), AtlasAnomalies.openRunCell(), map.visibleCells());
    }

    private void retarget() {
        AtlasCatalystUse use = catalystUse();
        I18n.forGameText(() -> map.target(use));
        tooltipSubject = null;
    }

    private String systemName(AtlasCell cell) {
        List<AtlasNodeText.Line> lines = AtlasNodeText.describe(map.generator(), map.state(), cell, map.isReachable(cell));
        return lines.isEmpty() ? "" : lines.get(0).text();
    }

    private void targetPicked(AtlasCell cell) {
        AtlasCatalystUse use = catalystUse();
        UseVerdict verdict = map.targetVerdict(cell);
        if (use == null || catalystPanel == null || catalystPanel.confirming() || verdict == null) {
            return;
        }
        map.pulse(cell);
        String systemName = I18n.forGameText(() -> systemName(cell));
        if (verdict.allowed()) {
            SkillTreeSounds.panelOpened();
            catalystPanel.requestUse(use, cell, systemName);
        } else {
            SkillTreeSounds.refused();
            catalystPanel.showRefusal(I18n.forGameText(() -> verdict.text(MaterialItems.displayName(use.itemId()), systemName)));
        }
    }

    private void showOverlay(boolean instantly) {
        map.scenery().borders().show(OVERLAY_SHOWN.get() == Overlay.FACTIONS, instantly);
        map.scenery().biomes().show(OVERLAY_SHOWN.get() == Overlay.BIOMES, instantly);
    }

    private void toggleOverlay(Overlay overlay, ButtonAPI pressed, ButtonAPI other) {
        OVERLAY_SHOWN.set(pressed.isChecked() ? overlay : Overlay.NONE);
        if (pressed.isChecked() && other != null) {
            other.setChecked(false);
        }
        showOverlay(false);
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
        } else if (buttonId == Control.BORDERS && bordersButton != null) {
            toggleOverlay(Overlay.FACTIONS, bordersButton, biomesButton);
        } else if (buttonId == Control.BIOMES && biomesButton != null) {
            toggleOverlay(Overlay.BIOMES, biomesButton, bordersButton);
        } else if (buttonId == Control.CATALYSTS && catalystsButton != null) {
            toggleCatalysts();
            catalystsButton.setChecked(catalystPanel != null);
        } else if (buttonId == Control.CHOOSE_CHART && focusing && bay == null) {
            pick(focusedCell);
        }
    }

    private void close() {
        leaveFocus();
        if (bay != null && bay.isOpen()) {
            bay.close();
        }
        if (catalystPanel != null) {
            catalystPanel.close();
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
        if (catalystPanel != null) {
            catalystPanel.close();
        }
        BenchModeSupport.OpenTarget target = new BenchModeSupport.OpenTarget(cell,
                () -> AtlasNodeText.confirmLines(map.generator(), map.state(), cell), () -> AtlasNodeText.sideChoices(map.generator(), map.state(), cell));
        SocketPanelPlacement area = new SocketPanelPlacement(BENCH_MARGIN, BENCH_MARGIN, panelPosition.getWidth() - BENCH_MARGIN * 2f,
                panelPosition.getHeight() - BENCH_MARGIN * 2f, false);
        bay = ModificationBay.openMode(panelRoot, area, BenchMode.STAR_CHARTS, item -> null, target, new ModificationBay.Listener() {
            @Override
            public void closed() {
                bay = null;
                if (wormholeMessage != null) {
                    closing = true;
                } else if (focusing && catalystPanel == null) {
                    openFocusCatalysts();
                }
            }

            @Override
            public void debugActionApplied() {
                map = map.rebuilt();
                tooltipSubject = null;
                map.pulse(cell);
                if (anomalyPanel != null) {
                    anomalyPanel.refresh();
                }
                if (chooseButton != null && focusing) {
                    chooseButton.setEnabled(map.isReachable(cell));
                }
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
            } else if (focusedCell != null) {
                focusInput(event);
            } else if (event.isKeyboardEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
                if (event.isKeyUpEvent()) {
                    escape();
                }
                event.consume();
            } else if (event.isMouseEvent()) {
                mapInput(event);
            } else {
                event.consume();
            }
        }
    }

    private void escape() {
        if (catalystPanel == null) {
            close();
        } else if (!catalystPanel.escape()) {
            catalystPanel.close();
        }
    }

    private void focusInput(InputEventAPI event) {
        if (event.isMouseMoveEvent()) {
            mouseX = event.getX();
            mouseY = event.getY();
        }
        if (event.isKeyUpEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE && focusing) {
            if (catalystPanel != null && catalystPanel.confirming()) {
                catalystPanel.escape();
            } else {
                leaveFocus();
            }
        }
        event.consume();
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
                if (map.isTargeting()) {
                    targetPicked(released);
                } else {
                    pick(released);
                }
            }
            gesture.clear();
            camera.stopDrag();
        } else if (event.isRMBDownEvent()) {
            AtlasCell clicked = map.cellAt(viewport(), x, y);
            if (map.canFocus(clicked)) {
                enterFocus(clicked);
            } else if (map.isTargeting() && catalystPanel != null) {
                catalystPanel.escape();
            }
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
        focusLevel = Fade.step(focusLevel, focusing, amount / FOCUS_SECONDS);
        if (!focusing && focusLevel <= 0f) {
            focusedCell = null;
            removeChooseButton();
        }
        if (chooseElement != null) {
            chooseElement.setOpacity(Fade.smooth(focusLevel));
        }
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
            boolean hovering = bay == null && focusedCell == null && !camera.isDragging() && (catalystPanel == null || !catalystPanel.contains(mouseX, mouseY));
            hoveredAnomaly = hovering ? map.anomalyAt(viewport(), mouseX, mouseY) : null;
            AtlasCell hovered = hovering && hoveredAnomaly == null ? map.cellAt(viewport(), mouseX, mouseY) : null;
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
            map.render(viewport(), panelStyle.getAccentColor(), alpha, focusedCell);
            if (focusedCell != null) {
                float level = Fade.smooth(focusLevel);
                GLDraw.fillQuad(innerLeft(), innerBottom(), innerWidth(), innerHeight(), Color.BLACK, FOCUS_DIM_ALPHA * level * alpha);
                map.renderFocus(viewport(), focusedCell, focusCentreX(), focusCentreY(), focusReach(), level, alpha);
            }
        }
        drawChrome(alpha);
        if (benchDim > 0f) {
            GLDraw.fillQuad(innerLeft(), innerBottom(), innerWidth(), innerHeight(), Color.BLACK, BENCH_DIM_ALPHA * Fade.smooth(benchDim) * alpha);
        } else if (hoveredAnomaly != null && !gesture.isPanning()) {
            drawTooltip(hoveredAnomaly, this::writeAnomalyTooltip, alpha);
        } else if (hoveredCell != null && !gesture.isPanning()) {
            drawTooltip(hoveredCell, this::writeTooltip, alpha);
        }
    }

    private void drawChrome(float alpha) {
        float left = innerLeft() + CHROME_PAD;
        float top = innerBottom() + innerHeight() - CHROME_PAD;
        titleLabel.setAlpha(alpha).draw(left, top);
        summaryLabel.setAlpha(alpha).draw(left, top - titleLabel.height() - SUMMARY_GAP);
        hintLabel.setAlpha(alpha).draw(left, innerBottom() + CHROME_PAD + hintLabel.height());
    }

    private void drawTooltip(Object subject, Runnable writer, float alpha) {
        if (!subject.equals(tooltipSubject)) {
            tooltipSubject = subject;
            I18n.forGameText(writer);
        }
        panelStyle.drawTitleBodyTooltip(tooltipTitle, tooltipBody, mouseX, mouseY, alpha);
    }

    private void writeAnomalyTooltip() {
        writeTooltipText(AtlasNodeText.name("anomaly", hoveredAnomaly.anomalyId()),
                AtlasNodeText.anomalyBody(map.generator(), map.state(), hoveredAnomaly.cell(), hoveredAnomaly.anomalyId()));
    }

    private void writeTooltipText(String title, StyledText body) {
        tooltipTitle.setWrapped(title, SkillTreePanelStyle.TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_TEXT_HEIGHT);
        tooltipBody.setParagraphs(List.of(body), SkillTreePanelStyle.TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_TEXT_HEIGHT,
                SkillTreePanelStyle::standardHighlightColor);
    }

    private void writeTooltip() {
        List<AtlasNodeText.Line> lines = AtlasNodeText.describe(map.generator(), map.state(), hoveredCell, map.isReachable(hoveredCell));
        String title = lines.isEmpty() ? "" : lines.get(0).text();
        List<StyledText> body = new ArrayList<>();
        for (AtlasNodeText.Line line : lines.subList(Math.min(1, lines.size()), lines.size())) {
            body.add(styled(line));
        }
        UseVerdict verdict = map.targetVerdict(hoveredCell);
        AtlasCatalystUse use = catalystUse();
        if (verdict != null && use != null) {
            body.add(0, verdict.allowed() ? StyledText.styled(use.usableText(), Style.GOOD)
                    : StyledText.styled(verdict.text(MaterialItems.displayName(use.itemId()), title), Style.BAD));
        }
        writeTooltipText(title, StyledText.join(StyledText.of("\n"), body));
    }

    static StyledText styled(AtlasNodeText.Line line) {
        return switch (line.tone()) {
            case MARKER -> StyledText.styled(line.text(), Style.HIGHLIGHT);
            case GOOD -> StyledText.styled(line.text(), Style.GOOD);
            case BAD -> StyledText.styled(line.text(), Style.BAD);
            case HINT -> StyledText.styled(line.text(), Style.MUTED);
            case STYLED -> StyledText.parse(line.text());
            default -> StyledText.of(line.text());
        };
    }
}
