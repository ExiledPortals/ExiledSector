package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.render.Fade;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.OverlayFade;
import exiledsector.charts.atlas.ActiveCatalysts;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasNames;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.layout.sketch.ChartSystemSketch;
import exiledsector.charts.layout.sketch.SectorSystemSketcher;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerText;
import exiledsector.charts.wormhole.WormholeGateway;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.items.crafting.material.CraftingMaterials;
import exiledsector.items.Item;
import exiledsector.items.crafting.material.MaterialItems;
import exiledsector.ui.CampaignOverlay;
import exiledsector.ui.CanvasCamera;
import exiledsector.ui.CanvasDrag;
import exiledsector.ui.CanvasSearchBar;
import exiledsector.ui.ChromeMetrics;
import exiledsector.ui.PointerGesture;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.TooltipText;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.UiFonts;
import exiledsector.ui.VanillaText;
import exiledsector.ui.ZoomableScreen;
import exiledsector.ui.cargo.CatalystCargo;
import exiledsector.ui.chart.StarChartCraftingBench;
import exiledsector.ui.decoration.StarfieldRenderer;
import exiledsector.ui.socket.AtlasAnomalyPanel;
import exiledsector.ui.socket.BenchLayout;
import exiledsector.ui.socket.BenchMode;
import exiledsector.ui.socket.BenchModeSupport;
import exiledsector.ui.socket.GameTextTooltip;
import exiledsector.ui.socket.SocketPanelPlacement;
import exiledsector.ui.perf.ScreenPerformance;
import exiledsector.ui.socket.CraftingBenchPanel;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.TextLabel;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class AtlasScreen extends BaseCustomUIPanelPlugin implements CampaignOverlay.Screen, ZoomableScreen {

    static final float PANEL_MARGIN = 20f;
    static final float BORDER_INSET = 16f;
    static final float CHROME_PAD = 24f;
    static final float CLOSE_WIDTH = 120f;
    static final float BORDERS_WIDTH = 160f;
    static final float BIOMES_WIDTH = 120f;
    static final float TREE_WIDTH = 140f;
    static final float BUTTON_GAP = 10f;
    static final float SEARCH_WIDTH = 280f;
    static final float OPEN_SECONDS = 0.2f;
    static final float BACKDROP_ALPHA = 0.8f;
    static final float FOCUS_SECONDS = 0.6f;
    static final float FOCUS_DIM_ALPHA = 0.85f;
    static final float CHOOSE_WIDTH = 320f;
    static final float CHOOSE_HEIGHT = 50f;
    static final float SIDE_PANEL_WIDTH = AtlasAnomalyPanel.PANEL_WIDTH;
    static final float CHART_COLUMN_GAP = 16f;
    static final float CHART_SOCKET_SHARE = 0.085f;
    static final float CHART_MIN_SOCKET_RADIUS = 40f;
    static final float CHART_MAX_SOCKET_RADIUS = 72f;
    static final float CHART_STAR_SHARE = 1.35f;
    static final float CHART_STAR_HEADROOM = 1.3f;
    static final float CHART_DETAILS_GAP = 14f;
    static final float CHART_DETAILS_MAX_WIDTH = 520f;
    static final float CHART_NOTICE_HEIGHT = 40f;
    static final float CHART_OPEN_HEIGHT = 40f;
    static final float CHART_FX_ROOM = 16f;
    static final float CHART_ROW_GAP = 8f;
    static final float DEBUG_WIDTH = 90f;
    static final float DEBUG_HEIGHT = 24f;
    static final float DEBUG_TOOLTIP_WIDTH = 340f;
    static final float TOOLTIP_MAX_TEXT_HEIGHT = 800f;
    static final Color HINT_COLOUR = new Color(150, 150, 150);

    static final float OFFICERS_WIDTH = 120f;
    static final float REPLAY_WIDTH = 100f;

    private enum Control { CLOSE, BORDERS, BIOMES, TREE, FOCUS_SWITCH, DEBUG_COMPLETE, OFFICERS, OFFICERS_REPLAY }

    enum Overlay { NONE, FACTIONS, BIOMES }

    private static final AtomicReference<Overlay> OVERLAY_SHOWN = new AtomicReference<>(Overlay.FACTIONS);

    private final CampaignOverlay.Host host;
    private final SkillTreePanelStyle panelStyle = new SkillTreePanelStyle();
    private final StarfieldRenderer starfield = new StarfieldRenderer(panelStyle);
    private final CanvasCamera camera = new CanvasCamera(AtlasMapCanvas.MIN_ZOOM, AtlasMapCanvas.MAX_ZOOM);
    private final CanvasDrag<Void> drag = new CanvasDrag<>(camera);
    private final PointerGesture<Void> gesture = drag.gesture();
    private final AtlasOverlayFrame frame = new AtlasOverlayFrame();
    private final TextLabel titleLabel = new TextLabel(UiFonts.TITLE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TooltipText tooltipText = SkillTreePanelStyle.tooltipText();
    private final TextLabel tooltipTitle = new TextLabel(tooltipText.title(), SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel tooltipBody = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final AtlasSearch search = new AtlasSearch();
    private CanvasSearchBar searchBar;
    private AtlasMapCanvas map;
    private CustomPanelAPI panelRoot;
    private PositionAPI panelPosition;
    private ButtonAPI bordersButton;
    private ButtonAPI biomesButton;
    private AtlasAnomalyPanel anomalyPanel;
    private TooltipMakerAPI chooseElement;
    private ButtonAPI chooseButton;
    private CraftingBenchPanel chartBench;
    private BenchModeSupport.PickerSession chartPicker;
    private TooltipMakerAPI debugElement;
    private BenchLayout chartLayout;
    private final OfficerBoard officerBoard = new OfficerBoard();
    private TooltipMakerAPI officersElement;
    private ButtonAPI officersButton;
    private TooltipMakerAPI replayElement;
    private ButtonAPI replayButton;
    private AtlasCell focusedCell;
    private boolean focusing;
    private boolean chartMode;
    private boolean closingChartParts;
    private float focusLevel;
    private float chartLevel;
    private Object tooltipSubject;
    private AtlasCell hoveredCell;
    private final AtlasSpotlight spotlight = new AtlasSpotlight();
    private AtlasMapCanvas.AnomalyIcon hoveredAnomaly;
    private final SpriteCache spriteCache = new SpriteCache(AtlasScreen.class);
    private final OfficerBoardTooltip officerTooltip = new OfficerBoardTooltip(spriteCache);
    private final AtlasSystemCard systemCard = new AtlasSystemCard(spriteCache);
    private String wormholeMessage;
    private float mouseX;
    private float mouseY;
    private final OverlayFade fade = new OverlayFade(OPEN_SECONDS);

    private AtlasScreen(CampaignOverlay.Host host) {
        this.host = host;
    }

    static AtlasScreen create(CampaignOverlay.Host host) {
        return new AtlasScreen(host);
    }

    public static boolean open(CampaignUIAPI campaignUI) {
        return open(campaignUI, null);
    }

    public static boolean open(CampaignUIAPI campaignUI, Runnable afterClose) {
        return CampaignOverlay.open(campaignUI, AtlasScreen::new, afterClose);
    }

    static boolean openIn(InteractionDialogAPI dialog, Runnable afterClose) {
        return CampaignOverlay.openIn(dialog, AtlasScreen::new, afterClose);
    }

    @Override
    public CanvasCamera camera() {
        return camera;
    }

    @Override
    public void attach(CustomPanelAPI panel) {
        panelRoot = panel;
        SectorAPI sector = Global.getSector();
        AtlasGenerator generator = ScreenPerformance.phase("atlas generator", () -> AtlasRuntime.generator(sector));
        searchBar = new CanvasSearchBar(search, I18n.forGameText(() -> Translation.text("chart.atlas.search.placeholder")), this::jumpToNextSearchMatch,
                SEARCH_WIDTH);
        AtlasScenery scenery = ScreenPerformance.phase("scenery", () -> new AtlasScenery(generator.seed()));
        ChartSystemSketch gatewaySketch = ScreenPerformance.phase("gateway sketch",
                () -> SectorSystemSketcher.sketch(WormholeGateway.playerGatewaySystem(sector)));
        map = ScreenPerformance.phase("map canvas", () -> new AtlasMapCanvas(generator, AtlasState.get(sector), scenery, gatewaySketch));
        ScreenPerformance.phase("search index", this::indexSearch);
        showOverlay(true);
        camera.jumpZoomTo(AtlasMapCanvas.MIN_ZOOM);
        AtlasCameraMemory.Centre remembered = AtlasCameraMemory.recall(sector);
        if (remembered != null) {
            camera.centreOn(remembered.treeX(), remembered.treeY());
        } else {
            camera.centreOn(map.gatewayX(), map.gatewayY());
        }
        ScreenPerformance.phase("chrome", () -> I18n.forGameText(this::buildChrome));
    }

    private void buildChrome() {
        titleLabel.set(Translation.text("chart.atlas.title"));
        PositionAPI position = panelRoot.getPosition();
        AtlasOverlayFrame.addTopRightButton(panelRoot, Translation.text("chart.atlas.close"), Control.CLOSE, CLOSE_WIDTH);
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
        panelRoot.addUIElement(biomeToggle).inTL(bordersLeft - BUTTON_GAP - BIOMES_WIDTH, PANEL_MARGIN + CHROME_PAD);
        TooltipMakerAPI treeElement = panelRoot.createUIElement(TREE_WIDTH, ChromeMetrics.CONTROL_HEIGHT, false);
        treeElement.addButton(Translation.text("chart.atlas.tree"), Control.TREE, TREE_WIDTH, ChromeMetrics.CONTROL_HEIGHT, 0f);
        float treeLeft = position.getWidth() - PANEL_MARGIN - CHROME_PAD - CLOSE_WIDTH - BORDERS_WIDTH - BIOMES_WIDTH - TREE_WIDTH - BUTTON_GAP * 3f;
        panelRoot.addUIElement(treeElement).inTL(treeLeft, PANEL_MARGIN + CHROME_PAD);
        float bottomRowTop = position.getHeight() - PANEL_MARGIN - CHROME_PAD - ChromeMetrics.CONTROL_HEIGHT;
        float officersLeft = officersLeft(position);
        officersElement = panelRoot.createUIElement(OFFICERS_WIDTH, ChromeMetrics.CONTROL_HEIGHT, false);
        officersButton = officersElement.addButton(Translation.text("chart.atlas.officers"), Control.OFFICERS, OFFICERS_WIDTH,
                ChromeMetrics.CONTROL_HEIGHT, 0f);
        panelRoot.addUIElement(officersElement).inTL(officersLeft, bottomRowTop);
        replayElement = panelRoot.createUIElement(REPLAY_WIDTH, ChromeMetrics.CONTROL_HEIGHT, false);
        replayButton = replayElement.addButton(Translation.text("chart.atlas.officers.replay"), Control.OFFICERS_REPLAY, REPLAY_WIDTH,
                ChromeMetrics.CONTROL_HEIGHT, 0f);
        panelRoot.addUIElement(replayElement).inTL(officersLeft - BUTTON_GAP - REPLAY_WIDTH, bottomRowTop);
        replayElement.setOpacity(0f);
        replayButton.setEnabled(false);
    }

    private static float officersLeft(PositionAPI position) {
        return position.getWidth() - PANEL_MARGIN - CHROME_PAD - OFFICERS_WIDTH;
    }

    private void indexSearch() {
        map.highlight(search);
        search.index(I18n.forGameText(() -> map.searchTerms()));
    }

    private void jumpToNextSearchMatch() {
        AtlasCell match = search.nextMatch();
        if (match == null) {
            SkillTreeSounds.refused();
            return;
        }
        camera.panTo(map.displayX(match), map.displayY(match));
        map.pulse(match);
    }

    private float searchBarLeft() {
        float treeLeft = panelPosition.getWidth() - PANEL_MARGIN - CHROME_PAD - CLOSE_WIDTH - BORDERS_WIDTH - BIOMES_WIDTH - TREE_WIDTH - BUTTON_GAP * 3f;
        return panelPosition.getX() + treeLeft - BUTTON_GAP - SEARCH_WIDTH;
    }

    private float searchBarBottom() {
        return panelPosition.getY() + panelPosition.getHeight() - PANEL_MARGIN - CHROME_PAD - ChromeMetrics.CONTROL_HEIGHT;
    }

    private float searchBarOpacity() {
        return officerBoard.isShowing() ? 0f : 1f - Fade.smooth(focusLevel);
    }

    private boolean overSearchBar(float x, float y) {
        return searchBarOpacity() > 0f && searchBar.contains(searchBarLeft(), searchBarBottom(), x, y);
    }

    private float sidePanelTop() {
        return PANEL_MARGIN + CHROME_PAD + ChromeMetrics.CONTROL_HEIGHT + BUTTON_GAP;
    }

    private float sidePanelHeight() {
        return panelRoot.getPosition().getHeight() - sidePanelTop() - PANEL_MARGIN - CHROME_PAD;
    }

    private float chooseTop() {
        return panelRoot.getPosition().getHeight() - PANEL_MARGIN - CHROME_PAD - CHOOSE_HEIGHT;
    }

    private void enterFocus(AtlasCell cell) {
        focusedCell = cell;
        focusing = true;
        tooltipSubject = null;
        searchBar.unfocus();
        gesture.clear();
        camera.stopMoving();
        map.pulse(cell);
        SkillTreeSounds.panelOpened();
        I18n.forGameText(() -> {
            addChooseButton(cell);
            openAnomalyPanel();
        });
    }

    private void openAnomalyPanel() {
        anomalyPanel = AtlasAnomalyPanel.open(panelRoot, PANEL_MARGIN + CHROME_PAD, sidePanelTop(), sidePanelHeight(), map.state(), this::focusedAnomalies, () -> {
            anomalyPanel = null;
            if (!chartMode) {
                leaveFocus();
            }
        });
    }

    private void addChooseButton(AtlasCell cell) {
        PositionAPI position = panelRoot.getPosition();
        TooltipMakerAPI element = panelRoot.createUIElement(CHOOSE_WIDTH, CHOOSE_HEIGHT, false);
        element.setButtonFontOrbitron20Bold();
        chooseButton = element.addButton(Translation.text("chart.atlas.focus.chooseChart"), Control.FOCUS_SWITCH, CHOOSE_WIDTH, CHOOSE_HEIGHT, 0f);
        element.setButtonFontDefault();
        chooseButton.setEnabled(map.isReachable(cell));
        panelRoot.addUIElement(element).inTL((position.getWidth() - CHOOSE_WIDTH) / 2f, chooseTop());
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
        if (chartMode) {
            closeChartParts();
            chartMode = false;
        }
        if (chooseButton != null) {
            chooseButton.setEnabled(false);
        }
        if (anomalyPanel != null) {
            anomalyPanel.close();
        }
    }

    private void enterChartMode() {
        if (!focusing || chartMode || focusedCell == null) {
            return;
        }
        if (!map.isReachable(focusedCell)) {
            SkillTreeSounds.refused();
            return;
        }
        chartMode = true;
        map.pulse(focusedCell);
        SkillTreeSounds.panelOpened();
        if (anomalyPanel != null) {
            anomalyPanel.close();
        }
        I18n.forGameText(() -> {
            openChartParts(focusedCell);
            chooseButton.setText(Translation.text("chart.atlas.focus.viewAnomalies"));
            chooseButton.setEnabled(true);
        });
    }

    private void leaveChartMode() {
        if (!chartMode) {
            return;
        }
        closeChartParts();
        chartMode = false;
        SkillTreeSounds.panelOpened();
        I18n.forGameText(() -> {
            openAnomalyPanel();
            if (chooseButton != null) {
                chooseButton.setText(Translation.text("chart.atlas.focus.chooseChart"));
                chooseButton.setEnabled(map.isReachable(focusedCell));
            }
        });
    }

    private void openChartParts(AtlasCell cell) {
        BenchLayout layout = chartLayout();
        BenchModeSupport.OpenTarget target = new BenchModeSupport.OpenTarget(cell,
                () -> AtlasNodeText.confirmLines(map.generator(), map.state(), cell, CraftingMaterials.forPlayer(PlayerFleet.cargoOrNull())::count), () -> AtlasNodeText.entryChoices(map.generator(), map.state(), cell));
        chartBench = CraftingBenchPanel.openEmbedded(panelRoot, BenchMode.STAR_CHARTS, layout, new CraftingBenchPanel.Listener() {
            @Override
            public void changed(Item loaded) {
                if (chartPicker != null) {
                    chartPicker.changed(loaded);
                }
            }

            @Override
            public void closed() {
                chartBench = null;
            }

            @Override
            public void pressedInside() {
            }

            @Override
            public BenchModeSupport.OpenTarget openTarget() {
                return target;
            }

            @Override
            public void wormholeOpened(String message) {
                wormholeMessage = message;
                close();
            }

            @Override
            public void debugActionApplied() {
                debugCompleted(cell);
            }
        });
        chartPicker = StarChartCraftingBench.INSTANCE.pickerSession();
        chartPicker.open(panelRoot, new SocketPanelPlacement(PANEL_MARGIN + CHROME_PAD, sidePanelTop(), SIDE_PANEL_WIDTH, sidePanelHeight(), false),
                null, new BenchModeSupport.PickerListener() {
                    @Override
                    public boolean accepting() {
                        return chartBench != null;
                    }

                    @Override
                    public void picked(Item item) {
                        chartBench.load(item);
                        chartPicker.refresh();
                    }

                    @Override
                    public void closed() {
                        if (!closingChartParts) {
                            chartPicker = null;
                            leaveChartMode();
                        }
                    }

                    @Override
                    public void pressedInside() {
                    }

                    @Override
                    public void storeChanged() {
                        if (chartBench != null) {
                            chartBench.refresh();
                        }
                    }
                });
        addDebugButton();
    }

    private void closeChartParts() {
        closingChartParts = true;
        if (chartPicker != null) {
            chartPicker.close();
            chartPicker = null;
        }
        if (chartBench != null) {
            chartBench.close();
            chartBench = null;
        }
        if (debugElement != null) {
            panelRoot.removeComponent(debugElement);
            debugElement = null;
        }
        closingChartParts = false;
    }

    private void addDebugButton() {
        BenchModeSupport.DebugAction debugAction = StarChartCraftingBench.INSTANCE.debugAction();
        if (debugAction == null) {
            return;
        }
        PositionAPI position = panelRoot.getPosition();
        TooltipMakerAPI element = panelRoot.createUIElement(DEBUG_WIDTH, DEBUG_HEIGHT, false);
        element.addButton(Translation.text(debugAction.buttonKey()), Control.DEBUG_COMPLETE, Misc.getNegativeHighlightColor(), Misc.getDarkPlayerColor(),
                Alignment.MID, CutStyle.ALL, DEBUG_WIDTH, DEBUG_HEIGHT, 0f);
        String debugTooltipText = Translation.text(debugAction.tooltipKey());
        element.addTooltipToPrevious(new GameTextTooltip(DEBUG_TOOLTIP_WIDTH, tooltip -> VanillaText.plainPara(tooltip, debugTooltipText, 0f,
                Misc.getTextColor())), TooltipMakerAPI.TooltipLocation.ABOVE);
        panelRoot.addUIElement(element).inTL(position.getWidth() - PANEL_MARGIN - CHROME_PAD - DEBUG_WIDTH,
                position.getHeight() - PANEL_MARGIN - CHROME_PAD - DEBUG_HEIGHT);
        debugElement = element;
    }

    private void debugCompleted(AtlasCell cell) {
        map = map.rebuilt();
        indexSearch();
        tooltipSubject = null;
        map.pulse(cell);
        leaveFocus();
        map.collapse(cell);
    }

    private BenchLayout chartLayout() {
        if (chartLayout != null) {
            return chartLayout;
        }
        PositionAPI position = panelRoot.getPosition();
        float top = sidePanelTop();
        float sideLeft = PANEL_MARGIN + CHROME_PAD;
        float rightLeft = position.getWidth() - PANEL_MARGIN - CHROME_PAD - SIDE_PANEL_WIDTH;
        float columnLeft = sideLeft + SIDE_PANEL_WIDTH + CHART_COLUMN_GAP;
        float columnWidth = rightLeft - CHART_COLUMN_GAP - columnLeft;
        float columnHeight = chooseTop() - BUTTON_GAP - top;
        float socketRadius = Math.max(CHART_MIN_SOCKET_RADIUS, Math.min(CHART_MAX_SOCKET_RADIUS, columnHeight * CHART_SOCKET_SHARE));
        float starPixels = socketRadius * CHART_STAR_SHARE;
        float socketY = starPixels * CHART_STAR_HEADROOM;
        float rowWidth = Math.min(CHART_DETAILS_MAX_WIDTH, columnWidth - CHART_FX_ROOM * 2f);
        float rowLeft = (columnWidth - rowWidth) / 2f;
        float openWidth = Math.min(CHOOSE_WIDTH, rowWidth);
        float openTop = columnHeight - CHART_FX_ROOM - CHART_OPEN_HEIGHT;
        float noticeTop = openTop - CHART_FX_ROOM - CHART_NOTICE_HEIGHT;
        float detailsTop = socketY + starPixels + CHART_DETAILS_GAP;
        float detailsHeight = Math.max(0f, noticeTop - CHART_ROW_GAP - detailsTop);
        float materialsHeight = position.getHeight() - PANEL_MARGIN - CHROME_PAD - DEBUG_HEIGHT - BUTTON_GAP - top;
        chartLayout = new BenchLayout(new BenchLayout.Rect(columnLeft, top, columnWidth, columnHeight), columnWidth / 2f, socketY, socketRadius,
                new BenchLayout.Rect(rowLeft, detailsTop, rowWidth, detailsHeight), new BenchLayout.Rect(rowLeft, noticeTop, rowWidth, CHART_NOTICE_HEIGHT),
                new BenchLayout.Rect((columnWidth - openWidth) / 2f, openTop, openWidth, CHART_OPEN_HEIGHT),
                new BenchLayout.Rect(rightLeft, top, SIDE_PANEL_WIDTH, materialsHeight));
        return chartLayout;
    }

    private AtlasAnomalyPanel.Listing focusedAnomalies() {
        if (focusedCell == null) {
            return new AtlasAnomalyPanel.Listing(List.of(), List.of());
        }
        List<AtlasAnomalyPanel.Entry> system = new ArrayList<>();
        List<String> present = AtlasAnomalies.effective(map.generator(), map.state(), focusedCell);
        for (String anomalyId : present) {
            system.add(new AtlasAnomalyPanel.Entry(AtlasNames.anomaly(anomalyId), anomalyIcon(anomalyId),
                    AtlasNodeText.anomalyBody(map.generator(), map.state(), focusedCell, anomalyId)));
        }
        AtlasOfficer officer = map.shownOfficerAt(focusedCell);
        if (officer != null) {
            String portraitPath = OfficerText.portraitPath(officer);
            system.add(new AtlasAnomalyPanel.Entry(OfficerText.title(officer), portraitPath == null ? "" : portraitPath, AtlasNodeText.officerBody(officer)));
        }
        List<AtlasAnomalyPanel.Entry> catalysts = new ArrayList<>();
        CraftingMaterials materials = CraftingMaterials.forPlayer(PlayerFleet.cargoOrNull());
        for (ActiveCatalysts.Planned planned : ActiveCatalysts.plan(map.generator(), map.state(), focusedCell, materials::count)) {
            String anomalyId = planned.catalyst().anomalyId();
            if (present.contains(anomalyId)) {
                continue;
            }
            String refusal = planned.verdict().text(MaterialItems.displayName(planned.catalyst().itemId()), systemName(focusedCell));
            catalysts.add(new AtlasAnomalyPanel.Entry(AtlasNames.anomaly(anomalyId), planned.catalyst().iconPath(),
                    CatalystCargo.look(planned.catalyst().family()), AtlasNodeText.anomalyBody(anomalyId), refusal));
        }
        return new AtlasAnomalyPanel.Listing(system, catalysts);
    }

    private static String anomalyIcon(String anomalyId) {
        ChartAnomalies.Definition definition = ChartAnomalies.definition(anomalyId);
        return definition == null ? "" : definition.icon();
    }

    private float focusCentreX() {
        return frame.innerLeft() + frame.innerWidth() / 2f;
    }

    private float focusCentreY() {
        return frame.innerBottom() + frame.innerHeight() / 2f;
    }

    private float focusReach() {
        return Math.min(frame.innerWidth() / 2f - BORDER_INSET, frame.innerHeight() / 2f);
    }

    private AtlasMapCanvas.Focus focus() {
        float level = Fade.smooth(focusLevel);
        float chart = Fade.smooth(chartLevel);
        if (chart <= 0f) {
            return new AtlasMapCanvas.Focus(focusCentreX(), focusCentreY(), focusReach(), level, AtlasSystemPreview.Spotlight.NONE);
        }
        BenchLayout layout = chartLayout();
        float chartX = panelPosition.getX() + layout.panel().left() + layout.socketX();
        float chartY = panelPosition.getY() + panelPosition.getHeight() - layout.panel().top() - layout.socketY();
        return new AtlasMapCanvas.Focus(AtlasMapCanvas.lerp(focusCentreX(), chartX, chart), AtlasMapCanvas.lerp(focusCentreY(), chartY, chart),
                focusReach(), level, new AtlasSystemPreview.Spotlight(chart, layout.socketRadius() * CHART_STAR_SHARE));
    }

    private String systemName(AtlasCell cell) {
        return AtlasNodeText.title(map.generator(), cell);
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

    @Override
    public void positionChanged(PositionAPI position) {
        panelPosition = position;
        frame.place(position);
    }

    private TreeViewport viewport() {
        return camera.viewport(panelPosition);
    }

    @Override
    public void buttonPressed(Object buttonId) {
        if (fade.handedOver()) {
            return;
        }
        if (buttonId == Control.OFFICERS) {
            toggleOfficers();
            return;
        }
        if (buttonId == Control.OFFICERS_REPLAY) {
            if (officerBoard.isOpen()) {
                officerBoard.replay();
            }
            return;
        }
        if (officerBoard.isOpen()) {
            officerBoard.close();
        }
        if (buttonId == Control.CLOSE) {
            closeByPlayer();
        } else if (buttonId == Control.BORDERS && bordersButton != null) {
            toggleOverlay(Overlay.FACTIONS, bordersButton, biomesButton);
        } else if (buttonId == Control.BIOMES && biomesButton != null) {
            toggleOverlay(Overlay.BIOMES, biomesButton, bordersButton);
        } else if (buttonId == Control.TREE && !focusing && !fade.closing() && !host.replacing()) {
            openAtlasTree();
        } else if (buttonId == Control.FOCUS_SWITCH && focusing) {
            if (chartMode) {
                leaveChartMode();
            } else {
                enterChartMode();
            }
        } else if (buttonId == Control.DEBUG_COMPLETE && chartBench != null) {
            chartBench.runDebugAction();
        }
    }

    private void toggleOfficers() {
        if (officerBoard.isOpen()) {
            officerBoard.close();
            return;
        }
        if (focusing || fade.closing()) {
            return;
        }
        drag.release();
        hoveredCell = null;
        hoveredAnomaly = null;
        searchBar.unfocus();
        officerBoard.open(map.generator(), map.state());
        placeOfficerBoard();
        SkillTreeSounds.panelOpened();
    }

    private void placeOfficerBoard() {
        float boardBottom = panelPosition.getY() + PANEL_MARGIN + CHROME_PAD + ChromeMetrics.CONTROL_HEIGHT + BUTTON_GAP;
        float boardTop = panelPosition.getY() + panelPosition.getHeight() - sidePanelTop();
        officerBoard.place(frame.innerLeft() + CHROME_PAD, boardBottom, frame.innerWidth() - CHROME_PAD * 2f, boardTop - boardBottom);
    }

    private void officerBoardInput(InputEventAPI event) {
        if (event.isMouseMoveEvent()) {
            mouseX = event.getX();
            mouseY = event.getY();
            dragMapUnderBoard(event);
        } else if (event.isKeyboardEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
            if (event.isKeyDownEvent()) {
                officerBoard.close();
            }
        } else if (event.isKeyboardEvent() && event.getEventValue() == Keyboard.KEY_F2) {
            if (event.isKeyDownEvent()) {
                officerBoard.openHoveredCodex();
            }
        } else if (event.isMouseDownEvent() && event.isLMBEvent()) {
            if (!officerBoard.contains(event.getX(), event.getY())) {
                gesture.pressTarget(null, officerBoard, event.getX(), event.getY(), false, false, true);
            }
        } else if (event.isMouseUpEvent() && event.isLMBUpEvent()) {
            officerBoardReleased(event.getX(), event.getY());
        } else if (event.isMouseScrollEvent() && !officerBoard.contains(event.getX(), event.getY())) {
            camera.scrollAt(event.getEventValue() > 0, event.getX(), event.getY(), panelPosition);
        }
        event.consume();
    }

    private void dragMapUnderBoard(InputEventAPI event) {
        drag.pointerMoved(event.getX(), event.getY(), event.getDX(), event.getDY());
    }

    private void officerBoardReleased(float x, float y) {
        boolean pressedOutside = gesture.isActive();
        boolean clickedOutside = gesture.releasedOn(officerBoard) && !officerBoard.contains(x, y);
        drag.release();
        if (clickedOutside) {
            officerBoard.close();
        } else if (!pressedOutside && officerBoard.contains(x, y)) {
            officerBoardClicked(x, y);
        }
    }

    private void officerBoardClicked(float x, float y) {
        AtlasCell battleCell = officerBoard.clickedBattleCell(x, y);
        AtlasCell commandCentre = officerBoard.clickedCommandCentre(x, y);
        if (battleCell != null) {
            spotlightFromBoard(battleCell);
        } else if (commandCentre != null) {
            spotlightFromBoard(commandCentre);
        } else if (officerBoard.clickedOfficer(x, y)) {
            SkillTreeSounds.refused();
        }
    }

    private void spotlightFromBoard(AtlasCell cell) {
        float visibleCentreX = (frame.innerLeft() + officerBoard.left()) / 2f;
        float panelCentreX = panelPosition.getX() + panelPosition.getWidth() / 2f;
        camera.panTo(map.displayX(cell) + (panelCentreX - visibleCentreX) / camera.zoom(), map.displayY(cell));
        spotlight.start(cell);
        SkillTreeSounds.panelOpened();
    }

    private void openAtlasTree() {
        AtlasCameraMemory.remember(Global.getSector(), camera.centreTreeX(), camera.centreTreeY());
        officerBoard.close();
        officerTooltip.cleanup();
        fade.handOver();
        host.switchTo(AtlasTreeScreen::new);
    }

    private void closeByPlayer() {
        if (!host.replacing()) {
            close();
        }
    }

    private void close() {
        fade.close();
        officerBoard.close();
        officerTooltip.cleanup();
        leaveFocus();
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (panelPosition == null || map == null || fade.handedOver()) {
            return;
        }
        for (InputEventAPI event : events) {
            if (event.isConsumed()) {
                continue;
            }
            if (fade.closing()) {
                event.consume();
            } else if (officerBoard.isOpen()) {
                officerBoardInput(event);
            } else if (focusedCell != null) {
                focusInput(event);
            } else if (event.isKeyboardEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
                if (event.isKeyDownEvent() && !searchBar.escape()) {
                    closeByPlayer();
                }
                event.consume();
            } else if (event.isKeyboardEvent() && searchBar.handleKey(event)) {
                event.consume();
            } else if (event.isKeyboardEvent() && event.getEventValue() == Keyboard.KEY_F2) {
                if (event.isKeyDownEvent() && hoveredAnomaly != null && hoveredAnomaly.isOfficer()) {
                    officerTooltip.openCodex(hoveredAnomaly.officer().recordId());
                }
                event.consume();
            } else if (event.isMouseEvent()) {
                mapInput(event);
            } else {
                event.consume();
            }
        }
    }

    private void focusInput(InputEventAPI event) {
        if (event.isMouseMoveEvent()) {
            mouseX = event.getX();
            mouseY = event.getY();
        }
        if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE && focusing) {
            focusEscape();
        }
        event.consume();
    }

    private void focusEscape() {
        if (chartMode) {
            boolean handled = chartBench != null && chartBench.escape() || chartPicker != null && chartPicker.escape();
            if (!handled) {
                leaveChartMode();
            }
        } else {
            leaveFocus();
        }
    }

    private void mapInput(InputEventAPI event) {
        float x = event.getX();
        float y = event.getY();
        if (event.isMouseMoveEvent()) {
            mouseX = x;
            mouseY = y;
            drag.pointerMoved(x, y, event.getDX(), event.getDY());
        } else if (event.isMouseDownEvent() && event.isLMBEvent()) {
            if (!searchBar.handleClick(searchBarLeft(), searchBarBottom(), x, y)) {
                pressMap(event, x, y);
            }
        } else if (event.isMouseUpEvent() && event.isLMBUpEvent()) {
            AtlasCell released = map.cellAt(viewport(), x, y);
            if (released != null && gesture.releasedOn(released)) {
                clicked(released);
            }
            drag.release();
        } else if (event.isMouseScrollEvent()) {
            camera.scrollAt(event.getEventValue() > 0, x, y, panelPosition);
        }
        event.consume();
    }

    private void pressMap(InputEventAPI event, float x, float y) {
        AtlasCell target = map.cellAt(viewport(), x, y);
        if (target != null) {
            gesture.pressTarget(null, target, x, y, event.isCtrlDown(), event.isShiftDown(), true);
        } else {
            drag.pressPan(null, x, y);
        }
    }

    private void clicked(AtlasCell cell) {
        if (map.canFocus(cell)) {
            enterFocus(cell);
        } else {
            SkillTreeSounds.refused();
        }
    }

    @Override
    public void advance(float amount) {
        if (panelRoot == null || map == null) {
            return;
        }
        boolean finishedClosing = fade.advance(amount);
        if (fade.fullyOpen()) {
            host.shown();
        }
        focusLevel = Fade.step(focusLevel, focusing, amount / FOCUS_SECONDS);
        chartLevel = Fade.step(chartLevel, chartMode, amount / FOCUS_SECONDS);
        if (!focusing && focusLevel <= 0f) {
            focusedCell = null;
            removeChooseButton();
        }
        if (chooseElement != null) {
            chooseElement.setOpacity(Fade.smooth(focusLevel));
        }
        if (finishedClosing) {
            AtlasCameraMemory.remember(Global.getSector(), camera.centreTreeX(), camera.centreTreeY());
            if (wormholeMessage != null) {
                host.leave(wormholeMessage);
            } else {
                host.close();
            }
            return;
        }
        starfield.advance(amount);
        searchBar.advance(amount);
        map.advance(amount);
        camera.advanceZoom(amount);
        camera.advancePan(amount);
        advanceSpotlight(amount);
        advanceOfficerBoard(amount);
        if (panelPosition != null) {
            boolean hovering = focusedCell == null && !officerBoard.isShowing() && !camera.isDragging() && !overSearchBar(mouseX, mouseY);
            hoveredAnomaly = hovering ? map.anomalyAt(viewport(), mouseX, mouseY) : null;
            if (hoveredAnomaly == null || !hoveredAnomaly.isOfficer()) {
                officerTooltip.cleanup();
            }
            AtlasCell hovered = hovering && hoveredAnomaly == null ? map.cellAt(viewport(), mouseX, mouseY) : null;
            if (hovered != null && !hovered.equals(hoveredCell)) {
                map.pulse(hovered);
            }
            hoveredCell = hovered;
        }
    }

    private void advanceSpotlight(float amount) {
        spotlight.advance(amount, camera.isPanning());
    }

    private void renderSpotlight(float alpha) {
        AtlasCell cell = spotlight.cell();
        if (cell != null) {
            TreeViewport view = viewport();
            spotlight.render(map.screenX(view, cell), map.screenY(view, cell), AtlasMapCanvas.footprint(view), panelStyle.getAccentColor(), alpha);
        }
    }

    private void renderChartCount(float alpha) {
        float countRight = panelPosition.getX() + officersLeft(panelPosition) - BUTTON_GAP * 2f - REPLAY_WIDTH;
        float rowCentreY = panelPosition.getY() + PANEL_MARGIN + CHROME_PAD + ChromeMetrics.CONTROL_HEIGHT / 2f;
        officerBoard.renderChartCount(countRight, rowCentreY, alpha);
    }

    private void advanceOfficerBoard(float amount) {
        if (panelPosition != null && officerBoard.isShowing()) {
            placeOfficerBoard();
        }
        officerBoard.advance(amount, mouseX, mouseY);
        float officersOpacity = 1f - Fade.smooth(focusLevel);
        if (officersElement != null) {
            officersElement.setOpacity(officersOpacity);
            officersButton.setEnabled(!focusing);
        }
        if (replayElement != null) {
            boolean replayShown = officerBoard.isOpen() && officerBoard.canReplay();
            replayElement.setOpacity(replayShown ? officersOpacity : 0f);
            replayButton.setEnabled(replayShown);
        }
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (panelPosition == null || map == null) {
            return;
        }
        float alpha = fade.smoothLevel() * alphaMult;
        if (alpha <= 0f) {
            return;
        }
        frame.drawBackdrop(alpha, !host.replacing());
        try (GlScope clip = frame.clipContent()) {
            starfield.render(panelPosition, camera.panX(), camera.panY(), search.backgroundAlpha() * alpha);
            map.render(viewport(), panelStyle.getAccentColor(), alpha, focusedCell);
            renderSpotlight(alpha);
            if (focusedCell != null) {
                float level = Fade.smooth(focusLevel);
                frame.fillInner(Color.BLACK, FOCUS_DIM_ALPHA * level * alpha);
                map.renderFocus(viewport(), focusedCell, focus(), alpha);
            }
        }
        drawChrome(alpha);
        renderChartCount(alpha);
        officerBoard.render(alpha, mouseX, mouseY, panelStyle.getAccentColor());
        if (hoveredAnomaly != null && hoveredAnomaly.isOfficer() && !gesture.isPanning()) {
            officerTooltip.show(hoveredAnomaly.officer());
            officerTooltip.draw(panelStyle.getAccentColor(), mouseX, mouseY, alpha);
        } else if (hoveredAnomaly != null && !gesture.isPanning()) {
            drawTooltip(hoveredAnomaly, this::writeAnomalyTooltip, alpha);
        } else if (hoveredCell != null && !gesture.isPanning()) {
            if (!hoveredCell.equals(tooltipSubject)) {
                tooltipSubject = hoveredCell;
                I18n.forGameText(this::writeSystemCard);
            }
            systemCard.draw(mouseX, mouseY, alpha);
        }
    }

    private void drawChrome(float alpha) {
        titleLabel.setAlpha(alpha).draw(frame.chromeLeft(), frame.chromeTop());
        float searchOpacity = searchBarOpacity();
        if (searchOpacity > 0f) {
            searchBar.render(searchBarLeft(), searchBarBottom(), searchOpacity * alpha);
        }
    }

    private void drawTooltip(Object subject, Runnable writer, float alpha) {
        if (!subject.equals(tooltipSubject)) {
            tooltipSubject = subject;
            I18n.forGameText(writer);
        }
        panelStyle.drawTitleBodyTooltip(tooltipTitle, tooltipBody, mouseX, mouseY, alpha);
    }

    private void writeAnomalyTooltip() {
        writeTooltipText(AtlasNames.anomaly(hoveredAnomaly.anomalyId()),
                AtlasNodeText.anomalyBody(map.generator(), map.state(), hoveredAnomaly.cell(), hoveredAnomaly.anomalyId()));
    }

    private void writeTooltipText(String title, StyledText body) {
        tooltipTitle.setWrapped(title, tooltipText.maxTextWidth(), TOOLTIP_MAX_TEXT_HEIGHT);
        tooltipBody.setParagraphs(List.of(body), tooltipText.maxTextWidth(), TOOLTIP_MAX_TEXT_HEIGHT,
                SkillTreePanelStyle::standardHighlightColor);
    }

    private void writeSystemCard() {
        systemCard.write(AtlasNodeText.card(map.generator(), map.state(), hoveredCell, map.isReachable(hoveredCell)));
    }
}
