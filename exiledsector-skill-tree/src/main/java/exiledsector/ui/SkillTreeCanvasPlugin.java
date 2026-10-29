package exiledsector.ui;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import exiledlib.skills.skilleffect.render.Fade;
import exiledsector.effects.OpReserveParity;
import exiledsector.effects.PhantomConflictWatch;
import exiledsector.effects.ShipTreeSync;
import exiledsector.items.Item;
import exiledsector.items.SocketCustody;
import exiledsector.items.SubroutineRadius;
import exiledsector.skills.ExoshipNodes;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.radius.RadiusSize;
import exiledsector.ui.decoration.AbyssDarknessRenderer;
import exiledsector.ui.decoration.DecorationScene;
import exiledsector.ui.decoration.FactionFleetRenderer;
import exiledsector.ui.decoration.SkillTreeFleetRenderer;
import exiledsector.ui.decoration.StarfieldRenderer;
import exiledsector.ui.decoration.TreeDecorations;
import exiledsector.ui.hyperspace.HyperspaceCamera;
import exiledsector.ui.inspect.ItemCodex;
import exiledsector.ui.node.DoctrineColors;
import exiledsector.ui.node.NodeRadiusRingRenderer;
import exiledsector.ui.node.NodeSearch;
import exiledsector.ui.node.SkillTreeNodeDrawer;
import exiledsector.ui.node.TreeAllocationSession;
import exiledsector.ui.util.GLDraw;
import lunalib.lunaRefit.BaseRefitButton;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;

public class SkillTreeCanvasPlugin extends BaseCustomUIPanelPlugin {

    private static final float WORKBENCH_DIM_SECONDS = 0.25f;
    private static final float WORKBENCH_DIM_ALPHA = 0.45f;

    private final StarfieldRenderer starfieldRenderer;
    private final AbyssDarknessRenderer abyssDarkness = AbyssDarknessRenderer.forActiveTree();
    private final TreeDecorations decorations = TreeDecorations.forActiveTree();
    private final ExoshipView exoshipView = new ExoshipView();
    private final SkillTreeFleetRenderer fleetRenderer;
    private final FactionFleetRenderer factionFleets;
    private final NodeRadiusRingRenderer radiusRingRenderer = new NodeRadiusRingRenderer();
    private final TreeAllocationSession treeSession;
    private final SkillTreeNodeDrawer nodeDrawer;
    private final SkillTreeStatPanel statPanel;
    private final NodeSearch nodeSearch;
    private final SkillTreeSearchBar searchBar;
    private final SkillTreeTemplateController templateUi;
    private final TreeCamera camera = new TreeCamera();
    private final HyperspaceMode hyperspaceMode;
    private final SocketPlacement socketPlacement;
    private final SkillTreeChrome chrome;
    private final PointerGesture<CanvasMode> gesture = new PointerGesture<>();
    private final TreeCameraMemory.View cameraMemory;
    private final FleetShipList shipList;

    private PositionAPI canvasPosition;
    private float mouseX = 0f;
    private float mouseY = 0f;
    private boolean mouseKnown = false;
    private boolean swallowEscapeUp;
    private float workbenchDimProgress;
    private boolean hoverRingShown;

    public SkillTreeCanvasPlugin(FleetMemberAPI member, ShipVariantAPI variant, float shipCardHeight, BaseRefitButton refitButton,
                                 CustomPanelAPI hostPanel, Consumer<FleetMemberAPI> shipSwitcher) {
        OpReserveParity.warnIfOutOfSync(member, variant, "before the skill tree re-synced it");
        if (member != null) {
            PhantomConflictWatch.applyPendingReverts(member.getId());
        }
        if (ShipTreeSync.memberChanged(member, variant) && refitButton != null) {
            refitButton.refreshVariant();
        }
        SocketCustody.reconcile();
        SkillTreePanelStyle panelStyle = new SkillTreePanelStyle();
        this.starfieldRenderer = new StarfieldRenderer(panelStyle);
        this.fleetRenderer = SkillTreeFleetRenderer.forPlayerFleet(member == null ? null : member.getId());
        exoshipView.setPlayerShip(member);
        this.nodeSearch = new NodeSearch(member == null ? null : member.getHullSpec().getHullSize());
        this.searchBar = new SkillTreeSearchBar(nodeSearch, this::jumpToNextSearchMatch);
        this.treeSession = new TreeAllocationSession(member, variant, refitButton, nodeSearch);
        this.factionFleets = FactionFleetRenderer.forActiveTree(treeSession::isLocked);
        this.nodeDrawer = SkillTreeNodeDrawer.attachedTo(treeSession, member, panelStyle, nodeSearch);
        this.templateUi = new SkillTreeTemplateController(member, treeSession, panelStyle);
        this.statPanel = new SkillTreeStatPanel(member, variant, panelStyle);
        this.hyperspaceMode = new HyperspaceMode(camera, statPanel, treeSession::isHidden, treeSession::wormholePairs);
        this.socketPlacement = new SocketPlacement(hostPanel, treeSession, nodeSearch, searchBar);
        this.chrome = new SkillTreeChrome(member, panelStyle, statPanel, searchBar, templateUi, socketPlacement, shipCardHeight);
        this.shipList = new FleetShipList(hostPanel, shipCardHeight, shipSwitcher);

        this.cameraMemory = TreeCameraMemory.forShip(member == null ? null : member.getId());
        SkillNode startingRoot = treeSession.getStartingRoot();
        if (startingRoot != null && cameraMemory.isSaved()) {
            camera.jumpTo(cameraMemory.camera());
        } else if (startingRoot != null) {
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

    HyperspaceCamera rememberedCamera() {
        return cameraMemory.isSaved() ? cameraMemory.camera() : null;
    }

    void carryCamera(HyperspaceCamera carried) {
        if (carried != null && treeSession.getStartingRoot() != null) {
            camera.jumpTo(carried);
        }
    }

    boolean shipListAvailable() {
        return canvasPosition != null && mode().switchesShips();
    }

    void toggleShipList() {
        if (shipList.isOpen()) {
            shipList.close();
            return;
        }
        if (!shipListAvailable()) {
            return;
        }
        socketPlacement.dismiss();
        treeSession.closeDropdown();
        searchBar.unfocus();
        ScreenRect cardFrame = chrome.shipCardFrame(canvasPosition);
        float listLeft = cardFrame.left() - canvasPosition.getX();
        float listBottom = cardFrame.bottom() + cardFrame.height() - canvasPosition.getY() + FleetShipList.CARD_GAP;
        float listTop = canvasPosition.getHeight() - SkillTreeChrome.sidePanelTop();
        shipList.open(listLeft, listBottom, listTop - listBottom);
    }

    public void setHighlightedRadius(SkillNode centre, RadiusSize size) {
        radiusRingRenderer.setHighlightedRadius(centre, size);
    }

    public void clearHighlightedRadius() {
        radiusRingRenderer.clearHighlightedRadius();
    }

    private CanvasMode mode() {
        return CanvasMode.resolve(treeSession.isStartingRootInputLocked(), templateUi.isModalOpen(), socketPlacement.isWorkbenchOpen(),
                hyperspaceMode.isActive(), exoshipView.isActive());
    }

    @Override
    public void advance(float amount) {
        if (canvasPosition == null) return;

        camera.advanceZoom(amount);
        hyperspaceMode.advance(amount);
        camera.advancePan(amount);
        searchBar.advance(amount);
        starfieldRenderer.advance(amount);
        decorations.advance(amount);
        factionFleets.advance(amount);
        if (camera.isFollowing() && hyperspaceMode.isActive()) {
            stopFollowing();
        }
        fleetRenderer.advance(amount);
        exoshipView.setBoardable(!treeSession.isHidden(exoshipView.exoshipNode()));
        exoshipView.advance(amount, exoshipView.isAvailable() && treeSession.isAllocated(exoshipView.exoshipNode()));
        if (exoshipView.isActive()) {
            camera.follow(exoshipView.focusX(), exoshipView.focusY(), amount);
        }
        boolean followingStartingRoot = treeSession.isStartingRootMoving();
        camera.beginStartingRootFollow(treeSession);
        treeSession.advance(amount);
        nodeDrawer.advance(amount);
        refreshSocketedRadiusRing();
        radiusRingRenderer.advance(amount);
        camera.applyStartingRootFollow(treeSession, followingStartingRoot);
        CanvasMode mode = mode();
        dropStaleGesture(mode);
        if (!mode.switchesShips()) {
            shipList.close();
        }
        templateUi.advance(amount, canvasPosition, mode.enables(CanvasMode.Chrome.TEMPLATE_BAR));
        socketPlacement.advance(mode.closesStorage());
        statPanel.refresh(treeSession.budget(), treeSession.statsRevision());
        statPanel.advance(amount);
        nodeSearch.setStatFocus(statPanel.hoveredNodeIds());
        chrome.advanceVisibility(amount, mode, hyperspaceMode.chromeAlpha(), hyperspaceMode.treeAlpha());
        chrome.advance(amount, canvasPosition, mode, treeSession.budget(), mouseX, mouseY, mouseKnown);
        float dimStep = amount / WORKBENCH_DIM_SECONDS;
        workbenchDimProgress = Fade.step(workbenchDimProgress, socketPlacement.isWorkbenchOpen(), dimStep);
        rememberCamera(mode);
    }

    private void rememberCamera(CanvasMode mode) {
        if (mode == CanvasMode.TREE && treeSession.getStartingRoot() != null) {
            cameraMemory.save(camera.centreTreeX(), camera.centreTreeY(), camera.zoom());
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
        } else if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE && shipList.isOpen()) {
            shipList.close();
            consume(event);
        } else if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE && escape(mode)) {
            consume(event);
        } else if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE && mode != CanvasMode.MODAL && statPanel.collapse()) {
            consume(event);
        } else if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_F2 && openHoveredItemCodex(mode)) {
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
                case EXOSHIP_VIEW, TREE -> handleTreeEvent(event, mode);
            }
        }
    }

    private boolean openHoveredItemCodex(CanvasMode mode) {
        if (!mouseKnown || !mode.hoversTree() || treeSession.isDropdownOpen() || socketPlacement.isEngaged()
                || chrome.contains(canvasPosition, mode, mouseX, mouseY)) {
            return false;
        }
        Item socketed = treeSession.socketedItem(nodeDrawer.findNodeAt(layers(), mouseX, mouseY));
        return socketed != null && ItemCodex.open(socketed.definitionId());
    }

    private boolean escape(CanvasMode mode) {
        EscapeAction action = EscapeAction.resolve(mode, socketPlacement.isEngaged(), treeSession.isDropdownOpen(),
                templateUi.isPlanning() && !searchBar.isFocused());
        switch (action) {
            case NONE -> {
                return false;
            }
            case SOCKET_PLACEMENT -> socketPlacement.escape();
            case CLOSE_DROPDOWN -> treeSession.closeDropdown();
            case STOP_FOLLOWING -> stopFollowing();
            case EXIT_TEMPLATE_MODE -> templateUi.exitTemplateMode();
        }
        return true;
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
                pressTarget(mode, nodeDrawer.findNodeAt(layers(), event.getX(), event.getY()), event, false);
            }
            event.consume();
        } else if (event.isLMBUpEvent() && gesture.isActive()) {
            SkillNode released = nodeDrawer.findNodeAt(layers(), event.getX(), event.getY());
            if (gesture.releasedOn(released)) {
                treeSession.chooseStartingRoot(released);
            }
            gesture.clear();
            event.consume();
        } else if (event.isMouseScrollEvent() && canvasPosition.containsEvent(event)) {
            statPanel.handleScroll(event.getX(), event.getY(), event.getEventValue());
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
        if ((event.isMouseDownEvent() || event.isMouseScrollEvent()) && canvasPosition.containsEvent(event)) {
            if (event.isLMBDownEvent()) {
                chrome.press(canvasPosition, mode, event.getX(), event.getY());
            }
            consume(event);
        }
    }

    private void boardExoship() {
        if (!exoshipView.isBoardable()) {
            return;
        }
        stopFollowing();
        gesture.clear();
        camera.stopDrag();
        treeSession.closeDropdown();
        exoshipView.enter();
        camera.startFollowing(exoshipView.fitZoom(canvasPosition));
        searchBar.unfocus();
    }

    private void stopFollowing() {
        if (exoshipView.isActive()) {
            treeSession.closeDropdown();
        }
        camera.stopFollowing();
        exoshipView.leave();
    }

    private void handleTreeEvent(InputEventAPI event, CanvasMode mode) {
        if (event.isRMBDownEvent() && canvasPosition.containsEvent(event) && emptySocketAt(mode, event.getX(), event.getY())) {
            event.consume();
        } else if (event.isLMBDownEvent() && canvasPosition.containsEvent(event)) {
            handleTreePress(event, mode);
        } else if (event.isLMBUpEvent()) {
            handleTreeRelease(event);
        } else if (event.isMouseScrollEvent() && statPanel.handleScroll(event.getX(), event.getY(), event.getEventValue())) {
            event.consume();
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
        return !chrome.contains(canvasPosition, mode, x, y) && socketPlacement.emptySocket(nodeDrawer.findNodeAt(layers(), x, y));
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
            SkillType option = nodeDrawer.findDropdownOptionAt(layers(), x, y);
            if (option != null) {
                gesture.pressTarget(mode, option, x, y, false, false, true);
            } else {
                treeSession.closeDropdown();
            }
        } else if (!pressTarget(mode, treeTargetAt(x, y), event, true)) {
            gesture.pressPan(mode, x, y);
            camera.startDrag();
        }
        event.consume();
    }

    private Object treeTargetAt(float x, float y) {
        if (exoshipView.shipContains(viewport(), x, y)) {
            return exoshipView;
        }
        return nodeDrawer.findNodeAt(layers(), x, y);
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
            SkillType releasedOption = nodeDrawer.findDropdownOptionAt(layers(), event.getX(), event.getY());
            if (gesture.releasedOn(releasedOption)) {
                treeSession.commitDropdownSelection(releasedOption);
            }
        } else {
            Object released = treeTargetAt(event.getX(), event.getY());
            if (gesture.releasedOn(released) && released == exoshipView) {
                boardExoship();
                SkillTreeSounds.wormholeJumped();
            } else if (gesture.releasedOn(released) && released instanceof SkillNode releasedNode) {
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
            treeSession.respec(node);
            return;
        }
        if (jumpTarget == null && socketPlacement.handleSocketClick(node, ctrlDown, canvasPosition, chrome.shipCardFrame(canvasPosition))) {
            return;
        }
        if (jumpTarget != null && ExoshipNodes.isExoship(jumpTarget)) {
            boardExoship();
            SkillTreeSounds.wormholeJumped();
        } else if (jumpTarget != null) {
            stopFollowing();
            camera.panTo(jumpTarget.getOffsetX(), jumpTarget.getOffsetY());
            if (!ExoshipNodes.isExoship(node)) {
                nodeDrawer.launchWormholeGhosts(node, jumpTarget);
            }
            SkillTreeSounds.wormholeJumped();
        } else {
            treeSession.clickNode(node, ctrlDown);
        }
    }

    private void jumpToNextSearchMatch() {
        SkillNode match = treeSession.nextSearchMatch();
        if (match != null) {
            stopFollowing();
            camera.panTo(match.getOffsetX(), match.getOffsetY());
        }
    }

    private void handleMouseScroll(InputEventAPI event, CanvasMode mode) {
        boolean scrollingIn = event.getEventValue() > 0;
        if (!scrollingIn && mode.entersHyperspaceOnScrollOut() && camera.isSettledAtMinimum() && !camera.isPanning()) {
            enterHyperspace();
        } else if (scrollingIn) {
            camera.scrollAt(true, event.getX(), event.getY(), canvasPosition);
        } else {
            camera.scroll(false, 0f, 0f);
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
        float exoshipFade = 1f - exoshipView.viewLevel();
        float sceneryAlpha = backgroundAlpha * exoshipFade;
        starfieldRenderer.render(canvasPosition, camera.panX(), camera.panY(), backgroundAlpha * exoshipView.starfieldAlpha());
        abyssDarkness.render(canvasPosition, viewport, alphaMult);
        DecorationScene decorationScene = new DecorationScene(viewport, sceneryAlpha, treeAlpha, hyperspaceMode.anchorIds(),
                new DecorationScene.StarMap(hyperspaceMode.starRadius(), hyperspaceMode.mapAmount(), hyperspaceMode.hiddenStarIds()));
        decorations.renderBehindTree(decorationScene);
        factionFleets.render(viewport, sceneryAlpha * treeAlpha);
        boolean pointerOverTree = mouseKnown && !chrome.contains(canvasPosition, mode, mouseX, mouseY) && !shipList.contains(mouseX, mouseY);
        boolean shipHovered = pointerOverTree && mode.hoversTree() && exoshipView.shipContains(viewport, mouseX, mouseY);
        boolean treeHovered = pointerOverTree && mode.hoversTree() && !shipHovered;
        exoshipView.setHovered(shipHovered);
        TreeLayers layers = exoshipView.layers(viewport, alphaMult * treeAlpha);
        if (treeAlpha > 0f) {
            refreshRadiusRing(layers, treeHovered);
            radiusRingRenderer.renderSocketed(layers);
            radiusRingRenderer.render(layers);
            nodeDrawer.render(layers, mouseX, mouseY, treeHovered);
        }
        fleetRenderer.render(viewport, backgroundAlpha * treeAlpha * exoshipFade);
        float shipAlpha = backgroundAlpha + (alphaMult * treeSession.treeAlpha() - backgroundAlpha) * exoshipView.viewLevel();
        exoshipView.render(viewport, shipAlpha * treeAlpha);
        decorations.renderOverTree(decorationScene);
        if (workbenchDimProgress > 0f) {
            GLDraw.fillQuad(canvasPosition.getX(), canvasPosition.getY(), canvasPosition.getWidth(), canvasPosition.getHeight(), Color.BLACK,
                    WORKBENCH_DIM_ALPHA * workbenchDimProgress * alphaMult);
        }
        if (hyperspaceMode.isActive()) {
            hyperspaceMode.render(viewport, alphaMult, pointerOverTree, mouseX, mouseY);
        }
        chrome.render(canvasPosition, mode, mouseX, mouseY, alphaMult, workbenchDimProgress);
        boolean nodeTooltipShown = !camera.isDragging() && mouseKnown && treeHovered;
        if (nodeTooltipShown) {
            nodeDrawer.renderHoverTooltip(layers, mouseX, mouseY, alphaMult);
        } else {
            nodeDrawer.endHover();
        }
        if (!camera.isDragging() && mouseKnown) {
            chrome.renderTooltips(canvasPosition, mode, mouseX, mouseY, alphaMult);
        }
        templateUi.renderModals(canvasPosition, mouseX, mouseY, alphaMult);
    }

    private void refreshRadiusRing(TreeLayers layers, boolean treeHovered) {
        SubroutineRadius.Footprint footprint = treeHovered ? socketFootprint(nodeDrawer.findSocketAt(layers, mouseX, mouseY)) : null;
        if (footprint == null) {
            footprint = socketPlacement.hoveredStorageFootprint();
        }
        SkillNode centre = footprint == null ? null : SkillTree.get(footprint.centreNodeId());
        if (centre != null) {
            radiusRingRenderer.setHighlightedRadius(centre, footprint.size());
            hoverRingShown = true;
        } else if (hoverRingShown) {
            radiusRingRenderer.clearHighlightedRadius();
            hoverRingShown = false;
        }
    }

    private void refreshSocketedRadiusRing() {
        SubroutineRadius.Footprint footprint = treeSession.socketedRadiusFootprint();
        SkillNode centre = footprint == null ? null : SkillTree.get(footprint.centreNodeId());
        Color factionColor = footprint == null ? null : DoctrineColors.forFaction(footprint.factionId());
        radiusRingRenderer.setSocketedRadius(centre, footprint == null ? null : footprint.size(), factionColor);
    }

    private SubroutineRadius.Footprint socketFootprint(SkillNode socket) {
        if (socket == null) {
            return null;
        }
        Item placing = socketPlacement.placingItem();
        Item item = placing != null && treeSession.isAllocatedSocket(socket) ? placing : treeSession.socketedItem(socket);
        return SubroutineRadius.footprint(item, socket.getId());
    }

    private TreeViewport viewport() {
        return camera.viewport(canvasPosition);
    }

    private TreeLayers layers() {
        return exoshipView.layers(viewport(), 1f);
    }
}
