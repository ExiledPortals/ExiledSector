package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.anomaly.FlareLuck;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.charts.atlas.AtlasNames;
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
import exiledsector.ui.CanvasSearchBar;
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
import exiledsector.ui.socket.GameTextTooltip;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.Rects;
import exiledsector.ui.util.TextLabel;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
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
    private static final float LOADOUT_ROW_GAP = 8f;
    private static final float LOADOUT_TOOLTIP_WIDTH = 360f;

    private enum Control { BACK, EFFECTS }

    private record LoadoutButton(int loadout) {
    }

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
    private final TextLabel titleLabel = new TextLabel(UiFonts.TITLE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel pointsLabel = new TextLabel(UiFonts.HINT, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final TooltipText tooltipText = SkillTreePanelStyle.tooltipText();
    private final TextLabel tooltipTitle = new TextLabel(tooltipText.title(), SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel tooltipBody = new TextLabel(tooltipText.body(), SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final OverlayFade fade = new OverlayFade(OPEN_SECONDS);
    private final AtlasTreeEffectsPanel effectsPanel;
    private final AtlasTreeSearch search = new AtlasTreeSearch();
    private final CanvasSearchBar searchBar;
    private final AtlasTreeCatalystColumn catalystColumn = new AtlasTreeCatalystColumn(AtlasTreeCatalystColumn.shown());
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

        @Override
        public float emphasis(String nodeId) {
            return search.nodeAlpha(nodeId);
        }

        @Override
        public float backgroundEmphasis() {
            return search.backgroundAlpha();
        }
    };
    private ButtonAPI effectsButton;
    private final List<ButtonAPI> loadoutButtons = new ArrayList<>();
    private AtlasTreeAllocation allocation;
    private int unspentPoints;
    private PositionAPI panelPosition;
    private AtlasTreeNode hoveredNode;
    private Object tooltipSubject;
    private AtlasCatalysts.Catalyst hoveredCatalyst;
    private float mouseX;
    private float mouseY;
    private boolean mouseKnown;

    AtlasTreeScreen(CampaignOverlay.Host host) {
        this.host = host;
        this.state = AtlasState.get(Global.getSector());
        this.decorations = TreeDecorations.of(data.decorations());
        this.effectsPanel = new AtlasTreeEffectsPanel(() -> AtlasTree.effectGroups(state));
        this.searchBar = new CanvasSearchBar(search, I18n.forGameText(() -> Translation.text("atlasTree.search.placeholder")), this::jumpToNextSearchMatch);
        refreshAllocation();
    }

    @Override
    public void attach(CustomPanelAPI panel) {
        camera.jumpZoomTo(START_ZOOM);
        List<AtlasTreeNode> roots = data.topology().roots();
        if (!roots.isEmpty()) {
            camera.centreOn(treeView.x(roots.get(0).getId()), treeView.y(roots.get(0).getId()));
        }
        I18n.forGameText(() -> buildChrome(panel));
        indexSearch();
    }

    private void indexSearch() {
        search.index(I18n.forGameText(() -> AtlasTreeSearchTerms.of(data.topology().nodes(), FlareLuck.treeRolls(AtlasTree.chartLines(state)))));
    }

    private void jumpToNextSearchMatch() {
        String match = search.nextMatch();
        if (match == null) {
            SkillTreeSounds.refused();
            return;
        }
        camera.panTo(treeView.x(match), treeView.y(match));
    }

    private float searchBarLeft() {
        return frame.innerLeft() + (frame.innerWidth() - searchBar.barWidth()) / 2f;
    }

    private float searchBarBottom() {
        return frame.chromeTop() - ChromeMetrics.CONTROL_HEIGHT;
    }

    private void placeCatalystColumn() {
        float columnTop = frame.chromeTop() - titleLabel.height() - TITLE_GAP - pointsLabel.height() - PANEL_GAP;
        catalystColumn.place(frame.chromeLeft(), columnTop, frame.chromeBottom() + PANEL_GAP);
    }

    private float loadoutRowBottom() {
        return searchBarBottom() - LOADOUT_ROW_GAP - ChromeMetrics.CONTROL_HEIGHT;
    }

    private boolean overChrome(float x, float y) {
        return searchBar.contains(searchBarLeft(), searchBarBottom(), x, y) || catalystColumn.contains(x, y)
                || Rects.contains(searchBarLeft(), loadoutRowBottom(), searchBar.barWidth(), ChromeMetrics.CONTROL_HEIGHT, x, y);
    }

    private void buildChrome(CustomPanelAPI panel) {
        titleLabel.set(Translation.text("atlasTree.title"));
        refreshPointsLabel();
        AtlasOverlayFrame.addTopRightButton(panel, Translation.text("atlasTree.back"), Control.BACK, BACK_WIDTH);
        effectsButton = AtlasOverlayFrame.addTopRightButton(panel, effectsButtonText(), Control.EFFECTS, EFFECTS_WIDTH, BACK_WIDTH + BUTTON_GAP);
        addLoadoutButtons(panel);
    }

    private void addLoadoutButtons(CustomPanelAPI panel) {
        float rowWidth = searchBar.barWidth();
        float buttonWidth = (rowWidth - BUTTON_GAP * (AtlasState.ATLAS_LOADOUTS - 1)) / AtlasState.ATLAS_LOADOUTS;
        TooltipMakerAPI element = panel.createUIElement(rowWidth, ChromeMetrics.CONTROL_HEIGHT, false);
        for (int loadout = 0; loadout < AtlasState.ATLAS_LOADOUTS; loadout++) {
            ButtonAPI button = element.addAreaCheckbox(Translation.text("atlasTree.loadout." + (loadout + 1)), new LoadoutButton(loadout),
                    Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), buttonWidth, ChromeMetrics.CONTROL_HEIGHT, 0f);
            button.getPosition().inTL(loadout * (buttonWidth + BUTTON_GAP), 0f);
            int tooltipLoadout = loadout;
            element.addTooltipToPrevious(new GameTextTooltip(LOADOUT_TOOLTIP_WIDTH, tooltip -> writeLoadoutTooltip(tooltip, tooltipLoadout)),
                    TooltipMakerAPI.TooltipLocation.BELOW);
            loadoutButtons.add(button);
        }
        float rowTop = AtlasScreen.PANEL_MARGIN + AtlasScreen.CHROME_PAD + ChromeMetrics.CONTROL_HEIGHT + LOADOUT_ROW_GAP;
        panel.addUIElement(element).inTL((panel.getPosition().getWidth() - rowWidth) / 2f, rowTop);
        checkActiveLoadout();
    }

    private void writeLoadoutTooltip(TooltipMakerAPI tooltip, int loadout) {
        tooltip.addPara(Translation.msg("atlasTree.loadout.spent").arg("spent", AtlasTree.spentPoints(state, loadout))
                .arg("earned", state.atlasPoints()).text(), 0f);
        tooltip.addPara(Translation.text("atlasTree.loadout.hint"), Misc.getGrayColor(), ChromeMetrics.CONTROL_HEIGHT / 4f);
    }

    private void checkActiveLoadout() {
        for (int loadout = 0; loadout < loadoutButtons.size(); loadout++) {
            loadoutButtons.get(loadout).setChecked(loadout == state.activeAtlasLoadout());
        }
    }

    private void selectLoadout(int loadout) {
        if (state.selectAtlasLoadout(loadout)) {
            refreshAllocation();
            refreshPointsLabel();
            indexSearch();
            effectsPanel.invalidate();
            tooltipSubject = null;
        }
        checkActiveLoadout();
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
        } else if (buttonId == Control.EFFECTS) {
            toggleEffects();
        } else if (buttonId instanceof LoadoutButton loadoutButton) {
            selectLoadout(loadoutButton.loadout());
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
                if (event.isKeyDownEvent() && !searchBar.escape()) {
                    if (effectsPanel.isOpen()) {
                        toggleEffects();
                    } else {
                        back();
                    }
                }
                event.consume();
            } else if (event.isKeyboardEvent() && searchBar.handleKey(event)) {
                event.consume();
            } else if (event.isMouseMoveEvent()) {
                mouseMoved(event);
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
        if (event.isLMBDownEvent() && searchBar.handleClick(searchBarLeft(), searchBarBottom(), x, y)) {
            event.consume();
            return;
        }
        if (event.isLMBDownEvent() && catalystColumn.catalystAt(x, y) != null) {
            catalystColumn.toggle(catalystColumn.catalystAt(x, y));
            event.consume();
            return;
        }
        if (overChrome(x, y) && (event.isMouseDownEvent() || event.isMouseScrollEvent())) {
            event.consume();
            return;
        }
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
            camera.scrollAt(event.getEventValue() > 0, x, y, panelPosition);
        }
        event.consume();
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
        indexSearch();
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
        searchBar.advance(amount);
        placeCatalystColumn();
        hoveredCatalyst = mouseKnown ? catalystColumn.catalystAt(mouseX, mouseY) : null;
        search.focusAnomalies(catalystColumn.focusedAnomalies(hoveredCatalyst));
        camera.advanceZoom(amount);
        camera.advancePan(amount);
        treeView.advance(amount);
        effectsPanel.advance(amount);
        boolean hovering = mouseKnown && !camera.isDragging() && !gesture.isPanning() && !effectsPanel.contains(mouseX, mouseY) && !overChrome(mouseX, mouseY);
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
                frame.chromeBottom() + PANEL_GAP, alpha);
        if (hoveredNode != null) {
            drawTooltip(hoveredNode, alpha);
        } else if (hoveredCatalyst != null) {
            drawCatalystTooltip(hoveredCatalyst, alpha);
        }
    }

    private void renderCanvas(float alpha) {
        TreeViewport viewport = viewport();
        float backgroundAlpha = alpha * search.backgroundAlpha();
        starfield.render(panelPosition, camera.panX(), camera.panY(), backgroundAlpha);
        DecorationScene scene = new DecorationScene(viewport, backgroundAlpha, 1f, Set.of(), DecorationScene.StarMap.NONE);
        decorations.renderBehindTree(scene);
        treeView.render(viewport, nodeStates, alpha);
        decorations.renderOverTree(scene);
    }

    private void drawChrome(float alpha) {
        float left = frame.chromeLeft();
        float top = frame.chromeTop();
        titleLabel.setAlpha(alpha).draw(left, top);
        pointsLabel.setAlpha(alpha).draw(left, top - titleLabel.height() - TITLE_GAP);
        searchBar.render(searchBarLeft(), searchBarBottom(), alpha);
        catalystColumn.render(hoveredCatalyst, alpha);
    }

    private void drawTooltip(AtlasTreeNode node, float alpha) {
        if (node != tooltipSubject) {
            tooltipSubject = node;
            I18n.forGameText(() -> writeTooltip(node));
        }
        panelStyle.drawTitleBodyTooltip(tooltipTitle, tooltipBody, mouseX, mouseY, alpha);
    }

    private void drawCatalystTooltip(AtlasCatalysts.Catalyst catalyst, float alpha) {
        if (catalyst != tooltipSubject) {
            tooltipSubject = catalyst;
            I18n.forGameText(() -> writeCatalystTooltip(catalyst.anomalyId()));
        }
        panelStyle.drawTitleBodyTooltip(tooltipTitle, tooltipBody, mouseX, mouseY, alpha);
    }

    private void writeCatalystTooltip(String anomalyId) {
        tooltipTitle.setWrapped(AtlasNames.anomaly(anomalyId), tooltipText.maxTextWidth(), TOOLTIP_MAX_TEXT_HEIGHT);
        tooltipBody.setParagraphs(List.of(AtlasNodeText.anomalyBody(anomalyId)), tooltipText.maxTextWidth(), TOOLTIP_MAX_TEXT_HEIGHT,
                SkillTreePanelStyle::standardHighlightColor);
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
