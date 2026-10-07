package exiledsector.ui;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import exiledsector.effects.OpReserveHullMods;
import exiledsector.effects.OpReserveParity;
import exiledsector.effects.SkillTreeInstaller;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillType;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipLevelSystem;
import exiledsector.skills.progression.ShipOpBudget;
import exiledsector.skills.progression.SkillNodeOpCost;
import exiledsector.socketables.SocketCustody;
import exiledsector.ui.decoration.SkillTreeRingBeltRenderer;
import exiledsector.ui.decoration.SkillTreeStarRenderer;
import exiledsector.ui.decoration.SkillTreeStarfieldRenderer;
import exiledsector.ui.decoration.SkillTreeStaticImageRenderer;
import exiledsector.ui.node.NodeSearch;
import exiledsector.ui.node.SkillTreeNodeRenderer;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.GLDraw;
import lunalib.lunaRefit.BaseRefitButton;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.List;

public class SkillTreeCanvasPlugin extends BaseCustomUIPanelPlugin {

    private static final float SHIP_CARD_FRAME_OUTSET = 8f;
    private static final float WORKBENCH_DIM_SECONDS = 0.25f;
    private static final float WORKBENCH_DIM_ALPHA = 0.45f;

    private final String readoutTooltipTitle = Translation.text("ui.readout.title");
    private final String readoutTooltipBody;

    private final SkillTreeStarfieldRenderer starfieldRenderer;
    private final SkillTreeStaticImageRenderer staticImageRenderer;
    private final SkillTreeRingBeltRenderer ringBeltRenderer;
    private final SkillTreeStarRenderer starRenderer;
    private final SkillTreeNodeRenderer nodeRenderer;
    private final SkillTreeStatPanel statPanel;
    private final SkillTreeReadoutBar ordnancePointsBar = new SkillTreeReadoutBar(SkillTreeCanvasPlugin.class, 0);
    private final SkillTreeLevelBar levelBar;
    private final SkillTreeInfoTooltipRenderer readoutTooltipRenderer;
    private final NodeSearch nodeSearch = new NodeSearch();
    private final SkillTreeSearchBar searchBar;
    private final SkillTreeTemplateController templateUi;
    private final BorderedPanel shipCardPanel = new BorderedPanel(SkillTreeCanvasPlugin.class);
    private final TreeCamera camera = new TreeCamera();
    private final HyperspaceMode hyperspaceMode;
    private final SocketPlacement socketPlacement;
    private final OverlayHitTest overlayHitTest;
    private final float shipCardHeight;
    private UIComponentAPI shipCard;
    private boolean shipCardHidden;

    private PositionAPI canvasPosition;
    private float mouseX = 0f;
    private float mouseY = 0f;
    private boolean mouseKnown = false;
    private SkillNode pendingClickNode;
    private boolean pendingClickCtrlDown;
    private boolean pendingClickShiftDown;
    private SkillType pendingDropdownOption;
    private boolean swallowEscapeUp;
    private float workbenchDimProgress;
    private boolean reopenStatsAfterWorkbench;

    public SkillTreeCanvasPlugin(FleetMemberAPI member, ShipVariantAPI variant, float shipCardHeight, BaseRefitButton refitButton,
                                 CustomPanelAPI hostPanel) {
        if (SkillTreeInstaller.ensureInstalled(member, variant) && refitButton != null) {
            refitButton.refreshVariant();
        }
        OpReserveParity.warnIfOutOfSync(member, variant, "before the skill tree re-synced it");
        OpReserveHullMods.sync(member, variant);
        SocketCustody.reconcile();
        SkillTreePanelStyle panelStyle = new SkillTreePanelStyle();
        this.starfieldRenderer = new SkillTreeStarfieldRenderer(panelStyle);
        this.staticImageRenderer = new SkillTreeStaticImageRenderer();
        this.ringBeltRenderer = new SkillTreeRingBeltRenderer();
        this.starRenderer = new SkillTreeStarRenderer();
        this.nodeRenderer = new SkillTreeNodeRenderer(member, variant, panelStyle, refitButton, nodeSearch);
        this.searchBar = new SkillTreeSearchBar(nodeSearch);
        this.templateUi = new SkillTreeTemplateController(member, nodeRenderer, panelStyle);
        this.statPanel = new SkillTreeStatPanel(member);
        this.levelBar = new SkillTreeLevelBar(member, 1);
        this.readoutTooltipRenderer = new SkillTreeInfoTooltipRenderer(panelStyle);
        this.readoutTooltipBody = buildReadoutTooltipBody(member);
        this.shipCardHeight = shipCardHeight;
        this.hyperspaceMode = new HyperspaceMode(camera, statPanel);
        this.socketPlacement = new SocketPlacement(hostPanel, nodeRenderer, nodeSearch, searchBar);
        this.overlayHitTest = new OverlayHitTest(statPanel, ordnancePointsBar, levelBar, templateUi, socketPlacement, nodeRenderer, hyperspaceMode);

        SkillNode startingRoot = nodeRenderer.getStartingRoot();
        if (startingRoot != null) {
            camera.centreOn(startingRoot.getOffsetX(), startingRoot.getOffsetY());
        }
    }

    @Override
    public void positionChanged(PositionAPI canvasPosition) {
        this.canvasPosition = canvasPosition;
    }

    @Override
    public void advance(float amount) {
        camera.advanceZoom(amount);
        hyperspaceMode.advance(amount);
        camera.advancePan(amount);
        searchBar.advance(amount);
        starfieldRenderer.advance(amount);
        staticImageRenderer.advance(amount);
        ringBeltRenderer.advance(amount);
        starRenderer.advance(amount);
        boolean followingStartingRoot = nodeRenderer.isStartingRootMoving();
        camera.beginStartingRootFollow(nodeRenderer);
        nodeRenderer.advance(amount);
        camera.applyStartingRootFollow(nodeRenderer, followingStartingRoot);
        templateUi.advance(amount, canvasPosition, hyperspaceMode.isActive());
        socketPlacement.layoutButton(canvasPosition, shipCardFrame(), isStorageButtonShown());
        socketPlacement.advance(templateUi.isModalOpen() || hyperspaceMode.isActive() || nodeRenderer.isStartingRootInputLocked());
        ShipOpBudget budget = nodeRenderer.budget();
        boolean pointerLive = mouseKnown && !isModalOpen() && !socketPlacement.isWorkbenchOpen();
        hideShipCardBehindModals();
        statPanel.refresh(budget, nodeRenderer.statsRevision());
        hyperspaceMode.reopenStatsWhenBack();
        hideStatsForWorkbench();
        statPanel.advance(amount);
        ordnancePointsBar.advance(amount, canvasPosition, budget.used, budget.total, mouseX, mouseY, pointerLive);
        levelBar.advance(amount, canvasPosition, mouseX, mouseY, pointerLive);
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

    private void enterHyperspace() {
        if (!hyperspaceMode.enter(canvasPosition)) {
            return;
        }
        pendingClickNode = null;
        pendingDropdownOption = null;
        nodeRenderer.closeDropdown();
        searchBar.unfocus();
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
        boolean escapeUp = event.isKeyUpEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE;
        if (escapeUp && (swallowEscapeUp || socketPlacement.isEngaged())) {
            swallowEscapeUp = false;
            event.consume();
        } else if (!templateUi.isModalOpen() && TooltipExpansion.isToggle(event)) {
            TooltipExpansion.toggle();
            event.consume();
        } else if (nodeRenderer.isStartingRootInputLocked()) {
            handleStartingRootEvent(event);
        } else if (templateUi.isModalOpen()) {
            handleModalEvent(event);
        } else if (socketPlacement.isWorkbenchOpen()) {
            handleWorkbenchEvent(event);
        } else if (hyperspaceMode.isActive()) {
            handleHyperspaceEvent(event);
        } else {
            handleTreeEvent(event);
        }
    }

    private void handleWorkbenchEvent(InputEventAPI event) {
        if (event.isMouseMoveEvent()) {
            mouseX = event.getX();
            mouseY = event.getY();
            mouseKnown = true;
        } else if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
            socketPlacement.escape();
            consume(event);
        } else if (event.isLMBDownEvent() && socketPlacement.isButtonClickable(event.getX(), event.getY())) {
            socketPlacement.toggle(canvasPosition, shipCardFrame());
            consume(event);
        } else if ((event.isMouseDownEvent() || event.isMouseScrollEvent()) && canvasPosition.containsEvent(event)) {
            consume(event);
        }
    }

    private void handleHyperspaceEvent(InputEventAPI event) {
        if (event.isMouseMoveEvent()) {
            handleMouseMove(event);
        } else {
            hyperspaceMode.handleEvent(event, canvasPosition, event.isLMBDownEvent() && isOverOverlay(event.getX(), event.getY()));
        }
    }

    private void handleTreeEvent(InputEventAPI event) {
        if (!handleInterruption(event)) {
            handlePointerOrKey(event);
        }
    }

    private boolean handleInterruption(InputEventAPI event) {
        if (nodeRenderer.isAutoAllocating() && interruptsAutoAllocate(event)) {
            nodeRenderer.cancelAutoAllocate();
        } else if (nodeRenderer.isRespeccing() && interruptsAutoAllocate(event)) {
            nodeRenderer.cancelRespec();
        } else if (socketPlacement.isEngaged() && event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
            socketPlacement.escape();
        } else {
            return false;
        }
        consume(event);
        return true;
    }

    private void handlePointerOrKey(InputEventAPI event) {
        if (event.isRMBDownEvent() && canvasPosition.containsEvent(event) && emptySocketAt(event.getX(), event.getY())) {
            event.consume();
        } else if (event.isLMBDownEvent() && canvasPosition.containsEvent(event)) {
            handleLmbDown(event);
        } else if (event.isLMBUpEvent()) {
            handleLmbUp(event);
        } else if (event.isMouseMoveEvent()) {
            handleMouseMove(event);
        } else if (event.isMouseScrollEvent() && canvasPosition.containsEvent(event)) {
            handleMouseScroll(event);
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

    private boolean interruptsAutoAllocate(InputEventAPI event) {
        return (event.isLMBDownEvent() && canvasPosition.containsEvent(event))
                || (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE);
    }

    private void handleModalEvent(InputEventAPI event) {
        if (event.isMouseMoveEvent()) {
            handleMouseMove(event);
            return;
        }
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

    private boolean emptySocketAt(float x, float y) {
        return !isOverOverlay(x, y) && socketPlacement.emptySocket(nodeRenderer.findNodeAt(viewport(), x, y));
    }

    void setShipCard(UIComponentAPI component) {
        shipCard = component;
        shipCardHidden = false;
    }

    private void hideShipCardBehindModals() {
        boolean hide = isModalOpen();
        if (shipCard != null && hide != shipCardHidden) {
            shipCardHidden = hide;
            shipCard.setOpacity(hide ? 0f : 1f);
        }
    }

    private boolean isModalOpen() {
        return templateUi.isModalOpen();
    }

    private boolean isStorageButtonShown() {
        return !nodeRenderer.isStartingRootInputLocked() && (!hyperspaceMode.isActive() || hyperspaceMode.chromeAlpha() > 0f);
    }

    private void handleStartingRootEvent(InputEventAPI event) {
        if (event.isMouseMoveEvent()) {
            handleMouseMove(event);
        } else if (event.isLMBDownEvent() && canvasPosition.containsEvent(event)) {
            pendingClickNode = isOverOverlay(event.getX(), event.getY())
                    ? null : nodeRenderer.findNodeAt(viewport(), event.getX(), event.getY());
            pendingClickCtrlDown = false;
            pendingClickShiftDown = false;
            event.consume();
        } else if (event.isLMBUpEvent() && pendingClickNode != null) {
            nodeRenderer.chooseStartingRoot(pendingClickNode);
            pendingClickNode = null;
            event.consume();
        } else if (event.isMouseScrollEvent() && canvasPosition.containsEvent(event)) {
            event.consume();
        }
    }

    private void handleLmbDown(InputEventAPI event) {
        if (searchBar.handleClick(canvasPosition, event.getX(), event.getY())) {
            event.consume();
            return;
        }
        if (socketPlacement.isButtonClickable(event.getX(), event.getY())) {
            socketPlacement.toggle(canvasPosition, shipCardFrame());
            event.consume();
            return;
        }
        if (templateUi.handleLmbDown(event.getX(), event.getY())) {
            event.consume();
            return;
        }
        if (statPanel.isToggleHit(event.getX(), event.getY())) {
            statPanel.toggle();
            event.consume();
            return;
        }
        if (isOverOverlay(event.getX(), event.getY())) {
            event.consume();
            return;
        }
        if (nodeRenderer.isDropdownOpen()) {
            SkillType option = nodeRenderer.findDropdownOptionAt(viewport(), event.getX(), event.getY());
            if (option != null) {
                pendingDropdownOption = option;
            } else {
                nodeRenderer.closeDropdown();
            }
        } else {
            SkillNode clicked = nodeRenderer.findNodeAt(viewport(), event.getX(), event.getY());
            if (clicked != null) {
                pendingClickNode = clicked;
                pendingClickCtrlDown = event.isCtrlDown();
                pendingClickShiftDown = event.isShiftDown();
            } else {
                camera.startDrag();
            }
        }
        event.consume();
    }

    private void handleLmbUp(InputEventAPI event) {
        if (templateUi.handleLmbUp(event.getX(), event.getY())) {
            if (templateUi.isModalOpen()) {
                searchBar.unfocus();
            }
            event.consume();
            return;
        }
        boolean wasOurGesture = camera.isDragging() || pendingDropdownOption != null || pendingClickNode != null;
        camera.stopDrag();
        if (pendingDropdownOption != null) {
            nodeRenderer.commitDropdownSelection(pendingDropdownOption);
            pendingDropdownOption = null;
        } else if (pendingClickNode != null) {
            clickNode(pendingClickNode, pendingClickCtrlDown, pendingClickShiftDown);
            pendingClickNode = null;
        }
        if (wasOurGesture) {
            event.consume();
        }
    }

    private void clickNode(SkillNode node, boolean ctrlDown, boolean shiftDown) {
        SkillNode jumpTarget = nodeRenderer.wormholeJumpTarget(node, ctrlDown);
        if (socketPlacement.placeInto(node)) {
            return;
        }
        if (ctrlDown && shiftDown && nodeRenderer.isAllocated(node)) {
            nodeRenderer.startRespec(node);
            return;
        }
        if (socketPlacement.handleSocketClick(node, ctrlDown, canvasPosition, shipCardFrame())) {
            return;
        }
        if (jumpTarget != null) {
            camera.panTo(jumpTarget.getOffsetX(), jumpTarget.getOffsetY());
            nodeRenderer.launchWormholeGhosts(node, jumpTarget);
            SkillTreeSounds.wormholeJumped();
        } else {
            nodeRenderer.toggleAllocation(node, ctrlDown);
        }
    }

    private void handleMouseMove(InputEventAPI event) {
        mouseX = event.getX();
        mouseY = event.getY();
        mouseKnown = true;
        if (camera.dragBy(event.getDX(), event.getDY())) {
            event.consume();
        }
    }

    private void handleMouseScroll(InputEventAPI event) {
        boolean scrollingIn = event.getEventValue() > 0;
        if (!scrollingIn && camera.isSettledAtMinimum() && !camera.isPanning()
                && !nodeRenderer.isAutoAllocating() && !nodeRenderer.isRespeccing()) {
            enterHyperspace();
        } else {
            camera.scroll(scrollingIn, scrollingIn ? event.getX() - (canvasPosition.getX() + canvasPosition.getWidth() / 2f) : 0f,
                    scrollingIn ? event.getY() - (canvasPosition.getY() + canvasPosition.getHeight() / 2f) : 0f);
        }
        event.consume();
    }

    @Override
    public void render(float alphaMult) {
        if (canvasPosition == null) return;

        TreeViewport viewport = viewport();

        boolean inHyperspace = hyperspaceMode.isActive();
        float treeAlpha = hyperspaceMode.treeAlpha();
        float searchDim = nodeSearch.backgroundAlpha() + (1f - nodeSearch.backgroundAlpha()) * (1f - treeAlpha);
        float backgroundAlpha = alphaMult * searchDim * nodeRenderer.treeAlpha();
        starRenderer.setMapRadius(hyperspaceMode.starRadius(), hyperspaceMode.mapAmount());
        starfieldRenderer.render(canvasPosition, camera.panX(), camera.panY(), backgroundAlpha);
        starRenderer.renderDisc(viewport, backgroundAlpha);
        starRenderer.renderAtmosphere(viewport, backgroundAlpha);
        starRenderer.renderAurora(viewport, backgroundAlpha);
        ringBeltRenderer.render(viewport, backgroundAlpha * treeAlpha);
        staticImageRenderer.render(viewport, backgroundAlpha, hyperspaceMode.anchorIds(), treeAlpha);
        boolean pointerOverTree = mouseKnown && !isOverOverlay(mouseX, mouseY);
        boolean treeHovered = !inHyperspace && pointerOverTree && !isModalOpen() && !socketPlacement.isWorkbenchOpen();
        if (treeAlpha > 0f) {
            nodeRenderer.render(viewport, alphaMult * treeAlpha, mouseX, mouseY, treeHovered);
        }
        starRenderer.renderGlow(viewport, backgroundAlpha);
        if (workbenchDimProgress > 0f) {
            GLDraw.fillQuad(canvasPosition.getX(), canvasPosition.getY(), canvasPosition.getWidth(), canvasPosition.getHeight(), Color.BLACK,
                    WORKBENCH_DIM_ALPHA * workbenchDimProgress * alphaMult);
        }
        if (inHyperspace) {
            hyperspaceMode.render(viewport, alphaMult, pointerOverTree, mouseX, mouseY);
        }
        renderChrome(alphaMult, treeAlpha);
        if (!camera.isDragging() && mouseKnown && !isModalOpen()) {
            if (treeHovered) {
                nodeRenderer.renderHoverTooltip(viewport, mouseX, mouseY, alphaMult);
            }
            renderChromeTooltips(alphaMult, inHyperspace);
        }
        templateUi.renderModals(canvasPosition, mouseX, mouseY, alphaMult);
    }

    private void renderChrome(float alphaMult, float treeAlpha) {
        float chromeAlpha = hyperspaceMode.chromeAlpha();
        statPanel.render(canvasPosition, mouseX, mouseY, alphaMult, chromeAlpha * (1f - workbenchDimProgress));
        ordnancePointsBar.render(canvasPosition, alphaMult, null);
        levelBar.render(canvasPosition, alphaMult);
        if (!nodeRenderer.isStartingRootInputLocked() && treeAlpha > 0f) {
            searchBar.render(canvasPosition, alphaMult * treeAlpha);
        }
        if (chromeAlpha > 0f) {
            templateUi.renderBar(canvasPosition, mouseX, mouseY, alphaMult * chromeAlpha);
        }
        socketPlacement.renderButton(mouseX, mouseY, alphaMult * chromeAlpha);
        drawShipCardFrame(alphaMult);
    }

    private void renderChromeTooltips(float alphaMult, boolean inHyperspace) {
        if (socketPlacement.isWorkbenchOpen()) {
            return;
        }
        if (ordnancePointsBar.isHovered(canvasPosition, mouseX, mouseY) || levelBar.isHovered(canvasPosition, mouseX, mouseY)) {
            readoutTooltipRenderer.render(readoutTooltipTitle, readoutTooltipBody, mouseX, mouseY, alphaMult);
        }
        if (!inHyperspace && socketPlacement.buttonContains(mouseX, mouseY)) {
            readoutTooltipRenderer.render(Translation.text("ui.socketStorage.title"), Translation.text("ui.socketStorage.hint"),
                    mouseX, mouseY, alphaMult);
        }
        if (!inHyperspace) {
            templateUi.renderBarTooltip(mouseX, mouseY, alphaMult);
        }
    }

    private static String buildReadoutTooltipBody(FleetMemberAPI member) {
        int opCost = SkillNodeOpCost.perNode(member.getHullSpec());
        int maxNodes = ShipLevelConfig.maxAllocatedNodes();
        return Translation.msg("ui.readout.body").arg("opCost", opCost).arg("maxNodes", maxNodes)
                .arg("xpRules", xpRulesText()).text();
    }

    static String xpRulesText() {
        int lossPercent = Math.round(ShipLevelConfig.xpLossMultiplier() * 100f);
        float maxMultiplier = ShipLevelSystem.difficultyMultiplier(FleetEncounterContext.MAX_XP_MULT,
                ShipLevelConfig.xpDifficultyStrength(), ShipLevelConfig.xpDifficultyMaxMultiplier());
        int maxBonus = Math.round((maxMultiplier - 1f) * 100f);
        if (maxBonus < 1) {
            return Translation.msg("ui.readout.xp").arg("lossPercent", lossPercent).text();
        }
        return Translation.msg("ui.readout.xpWithDifficulty").arg("lossPercent", lossPercent).arg("maxBonus", maxBonus).text();
    }

    private void drawShipCardFrame(float alphaMult) {
        ScreenRect frame = shipCardFrame();
        shipCardPanel.draw(frame.left(), frame.bottom(), frame.width(), frame.height(), alphaMult);
    }

    private ScreenRect shipCardFrame() {
        return new ScreenRect(canvasPosition.getX() + SkillTreeRefitButton.SHIP_CARD_MARGIN - SHIP_CARD_FRAME_OUTSET,
                canvasPosition.getY() + SkillTreeRefitButton.SHIP_CARD_MARGIN - SHIP_CARD_FRAME_OUTSET,
                SkillTreeRefitButton.SHIP_CARD_ICON_SIZE + SHIP_CARD_FRAME_OUTSET * 2f,
                shipCardHeight + SHIP_CARD_FRAME_OUTSET * 2f);
    }

    private boolean isOverOverlay(float x, float y) {
        return overlayHitTest.contains(canvasPosition, shipCardFrame(), x, y);
    }

    private TreeViewport viewport() {
        return camera.viewport(canvasPosition);
    }
}
