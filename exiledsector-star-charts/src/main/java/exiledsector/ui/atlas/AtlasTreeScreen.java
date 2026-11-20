package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.charts.anomaly.FlareLuck;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.tree.AtlasTree;
import exiledsector.charts.tree.AtlasTreeAllocation;
import exiledsector.charts.tree.AtlasTreeData;
import exiledsector.charts.tree.AtlasTreeNode;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillTier;
import exiledsector.ui.CampaignOverlay;
import exiledsector.ui.CanvasCamera;
import exiledsector.ui.CanvasDrag;
import exiledsector.ui.ChromeMetrics;
import exiledsector.ui.OverlayFade;
import exiledsector.ui.PointerGesture;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.TooltipText;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.UiFonts;
import exiledsector.ui.decoration.DecorationScene;
import exiledsector.ui.decoration.StarfieldRenderer;
import exiledsector.ui.decoration.TreeDecorations;
import exiledsector.ui.hyperspace.HyperspaceMode;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.TextLabel;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.Set;

public final class AtlasTreeScreen extends BaseCustomUIPanelPlugin implements CampaignOverlay.Screen {

    private static final float BACK_WIDTH = 160f;
    private static final float EFFECTS_WIDTH = 160f;
    private static final float BUTTON_GAP = 8f;
    private static final float PANEL_GAP = 8f;
    private static final float OPEN_SECONDS = AtlasScreen.OPEN_SECONDS;
    private static final float START_ZOOM = 0.3f;
    private static final float TITLE_GAP = 8f;
    private static final float TOOLTIP_MAX_TEXT_HEIGHT = AtlasScreen.TOOLTIP_MAX_TEXT_HEIGHT;
    private static final String VOLUME_KEY_PREFIX = "atlasTree.volume.";

    private enum Control { BACK, EFFECTS }

    private final CampaignOverlay.Host host;
    private final AtlasState state;
    private final AtlasTreeData data = AtlasTreeData.get();
    private final SkillTreePanelStyle panelStyle = new SkillTreePanelStyle();
    private final StarfieldRenderer starfield = new StarfieldRenderer(panelStyle);
    private final CanvasCamera camera = new CanvasCamera();
    private final CanvasDrag<Void> drag = new CanvasDrag<>(camera);
    private final PointerGesture<Void> gesture = drag.gesture();
    private final AtlasOverlayFrame frame = new AtlasOverlayFrame();
    private final TreeDecorations decorations;
    private final AtlasTreeView treeView = new AtlasTreeView(data.bodies(), panelStyle);
    private final HyperspaceMode hyperspace;
    private final TextLabel titleLabel = new TextLabel(UiFonts.TITLE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel pointsLabel = new TextLabel(UiFonts.HINT, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final TooltipText tooltipText = SkillTreePanelStyle.tooltipText();
    private final TextLabel tooltipTitle = new TextLabel(tooltipText.title(), SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel tooltipBody = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final OverlayFade fade = new OverlayFade(OPEN_SECONDS);
    private final AtlasTreeEffectsPanel effectsPanel;
    private final AtlasTreeView.States nodeStates = new AtlasTreeView.States() {
        @Override
        public boolean allocated(String nodeId) {
            return allocation.isAllocated(nodeId);
        }

        @Override
        public boolean allocatable(String nodeId) {
            AtlasTreeNode node = data.node(nodeId);
            return unspentPoints > 0 && node != null && allocation.canAllocate(node);
        }
    };
    private ButtonAPI effectsButton;
    private AtlasTreeAllocation allocation;
    private int unspentPoints;
    private PositionAPI panelPosition;
    private AtlasTreeNode hoveredNode;
    private AtlasTreeNode tooltipSubject;
    private float mouseX;
    private float mouseY;
    private boolean mouseKnown;

    AtlasTreeScreen(CampaignOverlay.Host host) {
        this.host = host;
        this.state = AtlasState.get(Global.getSector());
        this.decorations = TreeDecorations.of(data.decorations());
        this.effectsPanel = new AtlasTreeEffectsPanel(() -> AtlasTree.effectGroups(state));
        this.hyperspace = new HyperspaceMode(camera, this::hyperspaceContent, HyperspaceMode.Framing.ALL_VOLUMES, AtlasTreeScreen::volumeLabel, () -> { });
        refreshAllocation();
    }

    private HyperspaceMode.MapContent hyperspaceContent() {
        return new HyperspaceMode.MapContent(data.decorations(), Set.of(), data.topology().wormholePairs());
    }

    private static String volumeLabel(String volumeId) {
        String key = VOLUME_KEY_PREFIX + volumeId;
        return Translation.has(key) ? Translation.text(key) : volumeId;
    }

    @Override
    public void attach(CustomPanelAPI panel) {
        camera.jumpZoomTo(START_ZOOM);
        List<AtlasTreeNode> roots = data.topology().roots();
        if (!roots.isEmpty()) {
            camera.centreOn(treeView.x(roots.get(0).getId()), treeView.y(roots.get(0).getId()));
        }
        I18n.forGameText(() -> buildChrome(panel));
    }

    private void buildChrome(CustomPanelAPI panel) {
        titleLabel.set(Translation.text("atlasTree.title"));
        refreshPointsLabel();
        AtlasOverlayFrame.addTopRightButton(panel, Translation.text("atlasTree.back"), Control.BACK, BACK_WIDTH);
        effectsButton = AtlasOverlayFrame.addTopRightButton(panel, effectsButtonText(), Control.EFFECTS, EFFECTS_WIDTH, BACK_WIDTH + BUTTON_GAP);
    }

    private String effectsButtonText() {
        return Translation.text(effectsPanel.isOpen() ? "atlasTree.effects.hide" : "atlasTree.effects.show");
    }

    private void toggleEffects() {
        effectsPanel.toggle();
        if (effectsButton != null) {
            I18n.forGameText(() -> effectsButton.setText(effectsButtonText()));
        }
    }

    private void refreshAllocation() {
        allocation = AtlasTree.allocation(state);
        unspentPoints = AtlasTree.unspentPoints(state, allocation);
    }

    private void refreshPointsLabel() {
        I18n.forGameText(() -> pointsLabel.set(Translation.msg("atlasTree.points").arg("unspent", unspentPoints).arg("earned", state.atlasPoints()).text()));
    }

    @Override
    public void buttonPressed(Object buttonId) {
        if (fade.handedOver()) {
            return;
        }
        if (buttonId == Control.BACK) {
            back();
        } else if (buttonId == Control.EFFECTS && !hyperspace.isActive()) {
            toggleEffects();
        }
    }

    private void back() {
        if (!fade.closing() && !host.replacing()) {
            fade.handOver();
            host.switchTo(AtlasScreen::create);
        }
    }

    @Override
    public void positionChanged(PositionAPI position) {
        panelPosition = position;
        frame.place(position);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (panelPosition == null || fade.handedOver()) {
            return;
        }
        for (InputEventAPI event : events) {
            if (event.isConsumed()) {
                continue;
            }
            if (fade.closing()) {
                event.consume();
            } else if (event.isKeyboardEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
                if (event.isKeyDownEvent() && effectsPanel.isOpen() && !hyperspace.isActive()) {
                    toggleEffects();
                } else if (event.isKeyDownEvent()) {
                    back();
                }
                event.consume();
            } else if (event.isMouseMoveEvent()) {
                mouseMoved(event);
            } else if (hyperspace.isActive()) {
                hyperspace.handleEvent(event, panelPosition, false);
                event.consume();
            } else if (event.isMouseEvent()) {
                treeInput(event);
            } else {
                event.consume();
            }
        }
    }

    private void mouseMoved(InputEventAPI event) {
        mouseX = event.getX();
        mouseY = event.getY();
        mouseKnown = true;
        drag.pointerMoved(mouseX, mouseY, event.getDX(), event.getDY());
        event.consume();
    }

    private void treeInput(InputEventAPI event) {
        float x = event.getX();
        float y = event.getY();
        if (effectsPanel.contains(x, y) && (event.isMouseDownEvent() || event.isMouseScrollEvent())) {
            if (event.isMouseScrollEvent()) {
                effectsPanel.scroll(x, y, event.getEventValue());
            }
            event.consume();
            return;
        }
        if (event.isLMBDownEvent()) {
            AtlasTreeNode target = nodeAt(x, y);
            camera.stopDrag();
            if (target != null) {
                gesture.pressTarget(null, target, x, y, event.isCtrlDown(), event.isShiftDown(), true);
            } else {
                drag.pressPan(null, x, y);
            }
        } else if (event.isLMBUpEvent()) {
            AtlasTreeNode released = nodeAt(x, y);
            if (!gesture.isPanning() && released != null && gesture.releasedOn(released)) {
                clicked(released, gesture.ctrlDown() && gesture.shiftDown());
            }
            drag.release();
        } else if (event.isRMBDownEvent()) {
            travelThroughWormhole(nodeAt(x, y));
        } else if (event.isMouseScrollEvent()) {
            scrolled(event);
        }
        event.consume();
    }

    private void scrolled(InputEventAPI event) {
        boolean scrollingIn = event.getEventValue() > 0;
        if (!scrollingIn && camera.isSettledAtMinimum() && !camera.isPanning()) {
            if (hyperspace.enter(panelPosition)) {
                gesture.clear();
            }
        } else {
            camera.scrollAt(scrollingIn, event.getX(), event.getY(), panelPosition);
        }
    }

    private void clicked(AtlasTreeNode node, boolean wholePath) {
        boolean changed;
        boolean allocating = !allocation.isAllocated(node.getId());
        if (!allocating) {
            changed = AtlasTree.refund(state, node);
        } else if (wholePath) {
            changed = AtlasTree.allocatePath(state, node);
        } else {
            changed = AtlasTree.allocate(state, node);
        }
        if (!changed) {
            SkillTreeSounds.refused();
            return;
        }
        refreshAllocation();
        refreshPointsLabel();
        effectsPanel.invalidate();
        tooltipSubject = null;
        if (allocating) {
            SkillTreeSounds.allocated(soundSize(node.getTier()));
        } else {
            SkillTreeSounds.deallocated();
        }
    }

    private static SkillTreeSounds.NodeSize soundSize(SkillTier tier) {
        return switch (tier) {
            case KEYSTONE -> SkillTreeSounds.NodeSize.LARGE;
            case NOTABLE, WORMHOLE, WORMHOLE_EXIT -> SkillTreeSounds.NodeSize.MEDIUM;
            default -> SkillTreeSounds.NodeSize.SMALL;
        };
    }

    private void travelThroughWormhole(AtlasTreeNode node) {
        if (node == null || node.getTier() != SkillTier.WORMHOLE || !allocation.isAllocated(node.getId())) {
            return;
        }
        AtlasTreeNode partner = node.getPairedNodeId() == null ? null : data.node(node.getPairedNodeId());
        if (partner == null) {
            return;
        }
        camera.panTo(treeView.x(partner.getId()), treeView.y(partner.getId()));
        SkillTreeSounds.wormholeJumped();
    }

    @Override
    public void advance(float amount) {
        if (panelPosition == null) {
            return;
        }
        fade.advance(amount);
        if (fade.fullyOpen()) {
            host.shown();
        }
        starfield.advance(amount);
        decorations.advance(amount);
        hyperspace.advance(amount);
        camera.advanceZoom(amount);
        camera.advancePan(amount);
        treeView.advance(amount);
        effectsPanel.advance(amount);
        boolean hovering = mouseKnown && !hyperspace.isActive() && !camera.isDragging() && !gesture.isPanning() && !effectsPanel.contains(mouseX, mouseY);
        hoveredNode = hovering ? nodeAt(mouseX, mouseY) : null;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (panelPosition == null) {
            return;
        }
        float alpha = fade.smoothLevel() * alphaMult;
        if (alpha <= 0f) {
            return;
        }
        frame.drawBackdrop(alpha, !host.replacing());
        try (GlScope clip = frame.clipContent()) {
            renderCanvas(alpha);
        }
        drawChrome(alpha);
        effectsPanel.render(frame.chromeRight(), frame.chromeTop() - ChromeMetrics.CONTROL_HEIGHT - PANEL_GAP,
                frame.chromeBottom() + PANEL_GAP, alpha * hyperspace.treeAlpha());
        if (hoveredNode != null) {
            drawTooltip(hoveredNode, alpha);
        }
    }

    private void renderCanvas(float alpha) {
        TreeViewport viewport = viewport();
        float treeAlpha = hyperspace.treeAlpha();
        starfield.render(panelPosition, camera.panX(), camera.panY(), alpha);
        DecorationScene scene = new DecorationScene(viewport, alpha, treeAlpha, hyperspace.anchorIds(),
                new DecorationScene.StarMap(hyperspace.starRadius(), hyperspace.mapAmount(), hyperspace.hiddenStarIds()));
        decorations.renderBehindTree(scene);
        treeView.render(viewport, nodeStates, alpha * treeAlpha);
        decorations.renderOverTree(scene);
        if (hyperspace.isActive()) {
            hyperspace.render(viewport, alpha, mouseKnown, mouseX, mouseY);
        }
    }

    private void drawChrome(float alpha) {
        float left = frame.chromeLeft();
        float top = frame.chromeTop();
        titleLabel.setAlpha(alpha).draw(left, top);
        pointsLabel.setAlpha(alpha).draw(left, top - titleLabel.height() - TITLE_GAP);
    }

    private void drawTooltip(AtlasTreeNode node, float alpha) {
        if (node != tooltipSubject) {
            tooltipSubject = node;
            I18n.forGameText(() -> writeTooltip(node));
        }
        panelStyle.drawTitleBodyTooltip(tooltipTitle, tooltipBody, mouseX, mouseY, alpha);
    }

    private void writeTooltip(AtlasTreeNode node) {
        tooltipTitle.setWrapped(node.getDisplayName(), tooltipText.maxTextWidth(), TOOLTIP_MAX_TEXT_HEIGHT);
        List<StyledText> body = AtlasTreeNodeText.tooltipBody(node.type(), FlareLuck.treeRolls(AtlasTree.chartLines(state)));
        tooltipBody.setParagraphs(body, tooltipText.maxTextWidth(), TOOLTIP_MAX_TEXT_HEIGHT, SkillTreePanelStyle::standardHighlightColor);
    }

    private AtlasTreeNode nodeAt(float screenX, float screenY) {
        String nodeId = treeView.nodeAt(viewport(), screenX, screenY);
        return nodeId == null ? null : data.node(nodeId);
    }

    private TreeViewport viewport() {
        return camera.viewport(panelPosition);
    }
}
