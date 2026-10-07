package exiledsector.ui;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import exiledsector.effects.OpReserveParity;
import exiledsector.effects.ShipTreeSync;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillType;
import exiledsector.socketables.SocketCustody;
import exiledsector.ui.decoration.SkillTreeRingBeltRenderer;
import exiledsector.ui.decoration.SkillTreeStarRenderer;
import exiledsector.ui.decoration.SkillTreeStarfieldRenderer;
import exiledsector.ui.decoration.SkillTreeStaticImageRenderer;
import exiledsector.ui.node.NodeSearch;
import exiledsector.ui.node.SkillTreeNodeDrawer;
import exiledsector.ui.node.TreeAllocationSession;
import exiledsector.ui.util.GLDraw;
import lunalib.lunaRefit.BaseRefitButton;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.List;

public class SkillTreeCanvasPlugin extends BaseCustomUIPanelPlugin {

    private static final float WORKBENCH_DIM_SECONDS = 0.25f;
    private static final float WORKBENCH_DIM_ALPHA = 0.45f;

    private final SkillTreeStarfieldRenderer starfieldRenderer;
    private final SkillTreeStaticImageRenderer staticImageRenderer = new SkillTreeStaticImageRenderer();
    private final SkillTreeRingBeltRenderer ringBeltRenderer = new SkillTreeRingBeltRenderer();
    private final SkillTreeStarRenderer starRenderer = new SkillTreeStarRenderer();
    private final TreeAllocationSession treeSession;
    private final SkillTreeNodeDrawer nodeDrawer;
    private final SkillTreeStatPanel statPanel;
    private final NodeSearch nodeSearch = new NodeSearch();
    private final SkillTreeSearchBar searchBar = new SkillTreeSearchBar(nodeSearch);
    private final SkillTreeTemplateController templateUi;
    private final TreeCamera camera = new TreeCamera();
    private final HyperspaceMode hyperspaceMode;
    private final SocketPlacement socketPlacement;
    private final SkillTreeChrome chrome;
    private final PointerGesture gesture = new PointerGesture();

    private PositionAPI canvasPosition;
    private float mouseX = 0f;
    private float mouseY = 0f;
    private boolean mouseKnown = false;
    private boolean swallowEscapeUp;
    private float workbenchDimProgress;
    private boolean reopenStatsAfterWorkbench;

    public SkillTreeCanvasPlugin(FleetMemberAPI member, ShipVariantAPI variant, float shipCardHeight, BaseRefitButton refitButton,
                                 CustomPanelAPI hostPanel) {
        OpReserveParity.warnIfOutOfSync(member, variant, "before the skill tree re-synced it");
        if (ShipTreeSync.memberChanged(member, variant) && refitButton != null) {
            refitButton.refreshVariant();
        }
        SocketCustody.reconcile();
        SkillTreePanelStyle panelStyle = new SkillTreePanelStyle();
        this.starfieldRenderer = new SkillTreeStarfieldRenderer(panelStyle);
        this.treeSession = new TreeAllocationSession(member, variant, refitButton, nodeSearch);
        this.nodeDrawer = SkillTreeNodeDrawer.attachedTo(treeSession, member, panelStyle, nodeSearch);
        this.templateUi = new SkillTreeTemplateController(member, treeSession, panelStyle);
        this.statPanel = new SkillTreeStatPanel(member);
        this.hyperspaceMode = new HyperspaceMode(camera, statPanel);
        this.socketPlacement = new SocketPlacement(hostPanel, treeSession, nodeSearch, searchBar);
        this.chrome = new SkillTreeChrome(member, panelStyle, statPanel, searchBar, templateUi, socketPlacement, shipCardHeight);

        SkillNode startingRoot = treeSession.getStartingRoot();
        if (startingRoot != null) {
            camera.centreOn(startingRoot.getOffsetX(), startingRoot.getOffsetY());
        }
    }

    @Override
    public void positionChanged(PositionAPI canvasPosition) {
        this.canvasPosition = canvasPosition;
    }

    void setShipCard(UIComponentAPI component) {
        chrome.setShipCard(component);
    }

    private CanvasMode mode() {
        return CanvasMode.resolve(treeSession.isStartingRootInputLocked(), templateUi.isModalOpen(), socketPlacement.isWorkbenchOpen(),
                hyperspaceMode.isActive(), treeSession.isAutoAllocating() || treeSession.isRespeccing());
    }

    @Override
    public void advance(float amount) {
        if (canvasPosition == null) return;

        camera.advanceZoom(amount);
        hyperspaceMode.advance(amount);
        camera.advancePan(amount);
        searchBar.advance(amount);
        starfieldRenderer.advance(amount);
        staticImageRenderer.advance(amount);
        ringBeltRenderer.advance(amount);
        starRenderer.advance(amount);
        boolean followingStartingRoot = treeSession.isStartingRootMoving();
        camera.beginStartingRootFollow(treeSession);
        treeSession.advance(amount);
        nodeDrawer.advance(amount);
        camera.applyStartingRootFollow(treeSession, followingStartingRoot);
        CanvasMode mode = mode();
        dropStaleGesture(mode);
        templateUi.advance(amount, canvasPosition, mode.enables(CanvasMode.Chrome.TEMPLATE_BAR));
        socketPlacement.advance(mode.closesStorage());
        statPanel.refresh(treeSession.budget(), treeSession.statsRevision());
        hyperspaceMode.reopenStatsWhenBack();
        hideStatsForWorkbench();
        statPanel.advance(amount);
        chrome.advance(amount, canvasPosition, mode, hyperspaceMode.chromeAlpha(), treeSession.budget(), mouseX, mouseY, mouseKnown);
        float dimStep = amount / WORKBENCH_DIM_SECONDS;
        workbenchDimProgress = Math.max(0f, Math.min(1f, workbenchDimProgress + (socketPlacement.isWorkbenchOpen() ? dimStep : -dimStep)));
    }

    private void hideStatsForWorkbench() {
        if (socketPlacement.isWorkbenchOpen()) {
            if (statPanel.isOpen()) {
                reopenStatsAfterWorkbench = true;
                statPanel.close();
            }
        } else if (reopenStatsAfterWorkbench) {
            reopenStatsAfterWorkbench = false;
            if (!hyperspaceMode.isActive()) {
                statPanel.open(false);
            }
        }
    }

    private void dropStaleGesture(CanvasMode mode) {
        if (gesture.isStaleIn(mode)) {
            if (gesture.isPanning()) {
                camera.stopDrag();
            }
            gesture.clear();
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (canvasPosition == null) return;

        for (InputEventAPI event : events) {
            if (!event.isConsumed()) {
                handleEvent(event);
            }
        }
    }

    private void handleEvent(InputEventAPI event) {
        CanvasMode mode = mode();
        dropStaleGesture(mode);
        boolean escapeUp = event.isKeyUpEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE;
        if (event.isMouseMoveEvent()) {
            handleMouseMove(event);
        } else if (escapeUp && (swallowEscapeUp || socketPlacement.isEngaged())) {
            swallowEscapeUp = false;
            event.consume();
        } else if (mode != CanvasMode.MODAL && TooltipExpansion.isToggle(event)) {
            TooltipExpansion.toggle();
            event.consume();
        } else {
            switch (mode) {
                case ROOT_CHOICE -> handleRootChoiceEvent(event, mode);
                case MODAL -> handleModalEvent(event);
                case WORKBENCH -> handleWorkbenchEvent(event, mode);
                case HYPERSPACE -> hyperspaceMode.handleEvent(event, canvasPosition,
                        event.isLMBDownEvent() && chrome.contains(canvasPosition, mode, event.getX(), event.getY()));
                case ALLOCATION_RUN -> handleAllocationRunEvent(event, mode);
                case TREE -> handleTreeEvent(event, mode);
            }
        }
    }

    private void handleMouseMove(InputEventAPI event) {
        mouseX = event.getX();
        mouseY = event.getY();
        mouseKnown = true;
        if (gesture.moveTo(mouseX, mouseY)) {
            camera.startDrag();
            camera.dragBy(mouseX - gesture.pressX(), mouseY - gesture.pressY());
            event.consume();
        } else if (camera.dragBy(event.getDX(), event.getDY())) {
            event.consume();
        }
    }

    private void handleRootChoiceEvent(InputEventAPI event, CanvasMode mode) {
        if (event.isLMBDownEvent() && canvasPosition.containsEvent(event)) {
            if (!chrome.press(canvasPosition, mode, event.getX(), event.getY())) {
                pressTarget(mode, nodeDrawer.findNodeAt(viewport(), event.getX(), event.getY()), event, false);
            }
            event.consume();
        } else if (event.isLMBUpEvent() && gesture.isActive()) {
            SkillNode released = nodeDrawer.findNodeAt(viewport(), event.getX(), event.getY());
            if (gesture.releasedOn(released)) {
                treeSession.chooseStartingRoot(released);
            }
            gesture.clear();
            event.consume();
        } else if (event.isMouseScrollEvent() && canvasPosition.containsEvent(event)) {
            event.consume();
        }
    }

    private void handleModalEvent(InputEventAPI event) {
        if (event.isLMBDownEvent()) {
            templateUi.handleLmbDown(event.getX(), event.getY());
        } else if (event.isLMBUpEvent()) {
            templateUi.handleLmbUp(event.getX(), event.getY());
        } else if (event.isMouseScrollEvent()) {
            templateUi.handleScroll(event);
        } else if (event.isKeyboardEvent()) {
            templateUi.handleKey(event);
        }
        consume(event);
    }

    private void handleWorkbenchEvent(InputEventAPI event, CanvasMode mode) {
        if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
            socketPlacement.escape();
            consume(event);
        } else if ((event.isMouseDownEvent() || event.isMouseScrollEvent()) && canvasPosition.containsEvent(event)) {
            if (event.isLMBDownEvent()) {
                chrome.press(canvasPosition, mode, event.getX(), event.getY());
            }
            consume(event);
        }
    }

    private void handleAllocationRunEvent(InputEventAPI event, CanvasMode mode) {
        boolean interrupts = (event.isLMBDownEvent() && canvasPosition.containsEvent(event))
                || (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE);
        if (interrupts) {
            treeSession.cancelAutoAllocate();
            treeSession.cancelRespec();
            consume(event);
        } else {
            handleTreeEvent(event, mode);
        }
    }

    private void handleTreeEvent(InputEventAPI event, CanvasMode mode) {
        if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE && socketPlacement.isEngaged()) {
            socketPlacement.escape();
            consume(event);
        } else if (event.isRMBDownEvent() && canvasPosition.containsEvent(event) && emptySocketAt(mode, event.getX(), event.getY())) {
            event.consume();
        } else if (event.isLMBDownEvent() && canvasPosition.containsEvent(event)) {
            handleTreePress(event, mode);
        } else if (event.isLMBUpEvent()) {
            handleTreeRelease(event);
        } else if (event.isMouseScrollEvent() && canvasPosition.containsEvent(event)) {
            handleMouseScroll(event, mode);
        } else if (event.isKeyboardEvent() && searchBar.handleKey(event)) {
            consume(event);
        }
    }

    private void consume(InputEventAPI event) {
        if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
            swallowEscapeUp = true;
        }
        event.consume();
    }

    private boolean emptySocketAt(CanvasMode mode, float x, float y) {
        return !chrome.contains(canvasPosition, mode, x, y) && socketPlacement.emptySocket(nodeDrawer.findNodeAt(viewport(), x, y));
    }

    private void handleTreePress(InputEventAPI event, CanvasMode mode) {
        float x = event.getX();
        float y = event.getY();
        if (chrome.press(canvasPosition, mode, x, y)) {
            event.consume();
            return;
        }
        camera.stopDrag();
        if (treeSession.isDropdownOpen()) {
            SkillType option = nodeDrawer.findDropdownOptionAt(viewport(), x, y);
            if (option != null) {
                gesture.pressTarget(mode, option, x, y, false, false, true);
            } else {
                treeSession.closeDropdown();
            }
        } else if (!pressTarget(mode, nodeDrawer.findNodeAt(viewport(), x, y), event, true)) {
            gesture.pressPan(mode, x, y);
            camera.startDrag();
        }
        event.consume();
    }

    private boolean pressTarget(CanvasMode mode, Object target, InputEventAPI event, boolean mayPan) {
        if (target == null) {
            return false;
        }
        gesture.pressTarget(mode, target, event.getX(), event.getY(), event.isCtrlDown(), event.isShiftDown(), mayPan);
        return true;
    }

    private void handleTreeRelease(InputEventAPI event) {
        boolean panning = gesture.isPanning() || camera.isDragging();
        if (!gesture.isActive() && !panning) {
            if (chrome.release(event.getX(), event.getY())) {
                event.consume();
            }
            return;
        }
        if (panning) {
            camera.stopDrag();
        } else if (treeSession.isDropdownOpen()) {
            SkillType releasedOption = nodeDrawer.findDropdownOptionAt(viewport(), event.getX(), event.getY());
            if (gesture.releasedOn(releasedOption)) {
                treeSession.commitDropdownSelection(releasedOption);
            }
        } else {
            SkillNode releasedNode = nodeDrawer.findNodeAt(viewport(), event.getX(), event.getY());
            if (gesture.releasedOn(releasedNode)) {
                commitNodeClick(releasedNode, gesture.ctrlDown(), gesture.shiftDown());
            }
        }
        gesture.clear();
        event.consume();
    }

    private void commitNodeClick(SkillNode node, boolean ctrlDown, boolean shiftDown) {
        SkillNode jumpTarget = treeSession.wormholeJumpTarget(node, ctrlDown);
        if (socketPlacement.placeInto(node)) {
            return;
        }
        if (ctrlDown && shiftDown && treeSession.isAllocated(node)) {
            treeSession.startRespec(node);
            return;
        }
        if (socketPlacement.handleSocketClick(node, ctrlDown, canvasPosition, chrome.shipCardFrame(canvasPosition))) {
            return;
        }
        if (jumpTarget != null) {
            camera.panTo(jumpTarget.getOffsetX(), jumpTarget.getOffsetY());
            nodeDrawer.launchWormholeGhosts(node, jumpTarget);
            SkillTreeSounds.wormholeJumped();
        } else {
            treeSession.clickNode(node, ctrlDown);
        }
    }

    private void handleMouseScroll(InputEventAPI event, CanvasMode mode) {
        boolean scrollingIn = event.getEventValue() > 0;
        if (!scrollingIn && mode.entersHyperspaceOnScrollOut() && camera.isSettledAtMinimum() && !camera.isPanning()) {
            enterHyperspace();
        } else {
            camera.scroll(scrollingIn, scrollingIn ? event.getX() - (canvasPosition.getX() + canvasPosition.getWidth() / 2f) : 0f,
                    scrollingIn ? event.getY() - (canvasPosition.getY() + canvasPosition.getHeight() / 2f) : 0f);
        }
        event.consume();
    }

    private void enterHyperspace() {
        if (!hyperspaceMode.enter(canvasPosition)) {
            return;
        }
        gesture.clear();
        treeSession.closeDropdown();
        searchBar.unfocus();
    }

    @Override
    public void render(float alphaMult) {
        if (canvasPosition == null) return;

        TreeViewport viewport = viewport();
        CanvasMode mode = mode();
        float treeAlpha = hyperspaceMode.treeAlpha();
        float searchDim = nodeSearch.backgroundAlpha() + (1f - nodeSearch.backgroundAlpha()) * (1f - treeAlpha);
        float backgroundAlpha = alphaMult * searchDim * treeSession.treeAlpha();
        starRenderer.setMapRadius(hyperspaceMode.starRadius(), hyperspaceMode.mapAmount());
        starfieldRenderer.render(canvasPosition, camera.panX(), camera.panY(), backgroundAlpha);
        starRenderer.renderDisc(viewport, backgroundAlpha);
        starRenderer.renderAtmosphere(viewport, backgroundAlpha);
        starRenderer.renderAurora(viewport, backgroundAlpha);
        ringBeltRenderer.render(viewport, backgroundAlpha * treeAlpha);
        staticImageRenderer.render(viewport, backgroundAlpha, hyperspaceMode.anchorIds(), treeAlpha);
        boolean pointerOverTree = mouseKnown && !chrome.contains(canvasPosition, mode, mouseX, mouseY);
        boolean treeHovered = pointerOverTree && mode.hoversTree();
        if (treeAlpha > 0f) {
            nodeDrawer.render(viewport, alphaMult * treeAlpha, mouseX, mouseY, treeHovered);
        }
        starRenderer.renderGlow(viewport, backgroundAlpha);
        if (workbenchDimProgress > 0f) {
            GLDraw.fillQuad(canvasPosition.getX(), canvasPosition.getY(), canvasPosition.getWidth(), canvasPosition.getHeight(), Color.BLACK,
                    WORKBENCH_DIM_ALPHA * workbenchDimProgress * alphaMult);
        }
        if (hyperspaceMode.isActive()) {
            hyperspaceMode.render(viewport, alphaMult, pointerOverTree, mouseX, mouseY);
        }
        chrome.render(canvasPosition, mode, mouseX, mouseY, alphaMult, hyperspaceMode.chromeAlpha(), treeAlpha, workbenchDimProgress);
        if (!camera.isDragging() && mouseKnown) {
            if (treeHovered) {
                nodeDrawer.renderHoverTooltip(viewport, mouseX, mouseY, alphaMult);
            }
            chrome.renderTooltips(canvasPosition, mode, mouseX, mouseY, alphaMult);
        }
        templateUi.renderModals(canvasPosition, mouseX, mouseY, alphaMult);
    }

    private TreeViewport viewport() {
        return camera.viewport(canvasPosition);
    }
}
