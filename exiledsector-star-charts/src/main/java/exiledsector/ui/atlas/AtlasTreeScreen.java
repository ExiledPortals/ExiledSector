package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.tree.AtlasTree;
import exiledsector.charts.tree.AtlasTreeAllocation;
import exiledsector.charts.tree.AtlasTreeData;
import exiledsector.charts.tree.AtlasTreeNode;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillTier;
import exiledsector.skills.layout.TreeTopology;
import exiledsector.ui.CampaignOverlay;
import exiledsector.ui.CanvasCamera;
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
import exiledsector.ui.node.TreeNodeLayer;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.TextLabel;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.List;
import java.util.Set;

public final class AtlasTreeScreen extends BaseCustomUIPanelPlugin implements CampaignOverlay.Screen {

    private static final float PANEL_MARGIN = AtlasScreen.PANEL_MARGIN;
    private static final float BORDER_INSET = AtlasScreen.BORDER_INSET;
    private static final float CHROME_PAD = AtlasScreen.CHROME_PAD;
    private static final float BACK_WIDTH = 160f;
    private static final float OPEN_SECONDS = AtlasScreen.OPEN_SECONDS;
    private static final float BACKDROP_ALPHA = AtlasScreen.BACKDROP_ALPHA;
    private static final float START_ZOOM = 0.6f;
    private static final float TITLE_GAP = 8f;
    private static final float TOOLTIP_MAX_TEXT_HEIGHT = AtlasScreen.TOOLTIP_MAX_TEXT_HEIGHT;
    private static final String VOLUME_KEY_PREFIX = "atlasTree.volume.";

    private enum Control { BACK }

    private final CampaignOverlay.Host host;
    private final AtlasState state;
    private final AtlasTreeData data = AtlasTreeData.get();
    private final SkillTreePanelStyle panelStyle = new SkillTreePanelStyle();
    private final StarfieldRenderer starfield = new StarfieldRenderer(panelStyle);
    private final CanvasCamera camera = new CanvasCamera();
    private final PointerGesture<Void> gesture = new PointerGesture<>();
    private final TreeDecorations decorations;
    private final TreeNodeLayer<AtlasTreeNode> nodeLayer = new TreeNodeLayer<>(panelStyle);
    private final HyperspaceMode hyperspace;
    private final TextLabel titleLabel = new TextLabel(UiFonts.TITLE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel pointsLabel = new TextLabel(UiFonts.HINT, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final TextLabel hintLabel = new TextLabel(UiFonts.HINT, AtlasScreen.HINT_COLOUR);
    private final TooltipText tooltipText = SkillTreePanelStyle.tooltipText();
    private final TextLabel tooltipTitle = new TextLabel(tooltipText.title(), SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel tooltipBody = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final OverlayFade fade = new OverlayFade(OPEN_SECONDS);
    private final TreeNodeLayer.NodeStates<AtlasTreeNode> nodeStates = new TreeNodeLayer.NodeStates<>() {
        @Override
        public boolean allocated(AtlasTreeNode node) {
            return allocation.isAllocated(node.getId());
        }

        @Override
        public boolean allocatable(AtlasTreeNode node) {
            return unspentPoints > 0 && allocation.canAllocate(node);
        }
    };
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
            camera.centreOn(roots.get(0).getX(), roots.get(0).getY());
        }
        I18n.forGameText(() -> buildChrome(panel));
    }

    private void buildChrome(CustomPanelAPI panel) {
        titleLabel.set(Translation.text("atlasTree.title"));
        hintLabel.set(Translation.text("atlasTree.hint"));
        refreshPointsLabel();
        PositionAPI position = panel.getPosition();
        TooltipMakerAPI element = panel.createUIElement(BACK_WIDTH, ChromeMetrics.CONTROL_HEIGHT, false);
        element.addButton(Translation.text("atlasTree.back"), Control.BACK, BACK_WIDTH, ChromeMetrics.CONTROL_HEIGHT, 0f);
        panel.addUIElement(element).inTL(position.getWidth() - PANEL_MARGIN - CHROME_PAD - BACK_WIDTH, PANEL_MARGIN + CHROME_PAD);
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
        if (buttonId == Control.BACK) {
            fade.close();
        }
    }

    @Override
    public void positionChanged(PositionAPI position) {
        panelPosition = position;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (panelPosition == null) {
            return;
        }
        for (InputEventAPI event : events) {
            if (event.isConsumed()) {
                continue;
            }
            if (fade.closing()) {
                event.consume();
            } else if (event.isKeyboardEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
                if (event.isKeyUpEvent()) {
                    fade.close();
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
        if (gesture.moveTo(mouseX, mouseY)) {
            camera.startDrag();
            camera.dragBy(mouseX - gesture.pressX(), mouseY - gesture.pressY());
        } else {
            camera.dragBy(event.getDX(), event.getDY());
        }
        event.consume();
    }

    private void treeInput(InputEventAPI event) {
        float x = event.getX();
        float y = event.getY();
        if (event.isLMBDownEvent()) {
            AtlasTreeNode target = nodeLayer.nodeAt(viewport(), data.topology(), x, y);
            camera.stopDrag();
            if (target != null) {
                gesture.pressTarget(null, target, x, y, event.isCtrlDown(), event.isShiftDown(), true);
            } else {
                gesture.pressPan(null, x, y);
                camera.startDrag();
            }
        } else if (event.isLMBUpEvent()) {
            AtlasTreeNode released = nodeLayer.nodeAt(viewport(), data.topology(), x, y);
            if (!gesture.isPanning() && released != null && gesture.releasedOn(released)) {
                clicked(released, gesture.ctrlDown() && gesture.shiftDown());
            }
            gesture.clear();
            camera.stopDrag();
        } else if (event.isRMBDownEvent()) {
            travelThroughWormhole(nodeLayer.nodeAt(viewport(), data.topology(), x, y));
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
        nodeLayer.pulse(node.getId());
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
        camera.panTo(partner.getX(), partner.getY());
        SkillTreeSounds.wormholeJumped();
    }

    @Override
    public void advance(float amount) {
        if (panelPosition == null) {
            return;
        }
        if (fade.advance(amount)) {
            host.switchTo(AtlasScreen::create);
            return;
        }
        starfield.advance(amount);
        decorations.advance(amount);
        hyperspace.advance(amount);
        camera.advanceZoom(amount);
        camera.advancePan(amount);
        TreeTopology<AtlasTreeNode> topology = data.topology();
        nodeLayer.advance(amount, topology, nodeStates);
        boolean hovering = mouseKnown && !hyperspace.isActive() && !camera.isDragging() && !gesture.isPanning();
        hoveredNode = hovering ? nodeLayer.nodeAt(viewport(), topology, mouseX, mouseY) : null;
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
        GLDraw.fillQuad(panelPosition.getX(), panelPosition.getY(), panelPosition.getWidth(), panelPosition.getHeight(), Color.BLACK,
                BACKDROP_ALPHA * alpha);
        GLDraw.fillQuad(innerLeft(), innerBottom(), innerWidth(), innerHeight(), Color.BLACK, alpha);
        BorderedPanel.draw(innerLeft(), innerBottom(), innerWidth(), innerHeight(), alpha);
        float screenScale = Global.getSettings().getScreenScaleMult();
        try (GlScope clip = GlScope.clip((int) ((innerLeft() + BORDER_INSET) * screenScale), (int) (innerBottom() * screenScale),
                (int) ((innerWidth() - BORDER_INSET * 2f) * screenScale), (int) (innerHeight() * screenScale))) {
            renderCanvas(alpha);
        }
        drawChrome(alpha);
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
        nodeLayer.render(viewport, data.topology(), nodeStates, alpha * treeAlpha);
        decorations.renderOverTree(scene);
        if (hyperspace.isActive()) {
            hyperspace.render(viewport, alpha, mouseKnown, mouseX, mouseY);
        }
    }

    private void drawChrome(float alpha) {
        float left = innerLeft() + CHROME_PAD;
        float top = innerBottom() + innerHeight() - CHROME_PAD;
        titleLabel.setAlpha(alpha).draw(left, top);
        pointsLabel.setAlpha(alpha).draw(left, top - titleLabel.height() - TITLE_GAP);
        hintLabel.setAlpha(alpha).draw(left, innerBottom() + CHROME_PAD + hintLabel.height());
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
        List<StyledText> body = AtlasTreeNodeText.body(node.type(), node.getTier() == SkillTier.ROOT
                ? Translation.msg("atlasTree.pointsSource").arg("max", AtlasState.MAX_ATLAS_SKILL_POINTS).styled() : null);
        tooltipBody.setParagraphs(body, tooltipText.maxTextWidth(), TOOLTIP_MAX_TEXT_HEIGHT, SkillTreePanelStyle::standardHighlightColor);
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
}
