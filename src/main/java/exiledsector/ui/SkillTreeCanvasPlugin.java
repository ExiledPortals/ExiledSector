package exiledsector.ui;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.effects.OpReserveParity;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.effects.SkillTreeInstaller;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillType;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipLevelSystem;
import exiledsector.skills.progression.ShipOpBudget;
import exiledsector.skills.progression.SkillNodeOpCost;
import exiledsector.ui.decoration.SkillTreeRingBeltRenderer;
import exiledsector.ui.decoration.SkillTreeStarRenderer;
import exiledsector.ui.decoration.SkillTreeStarfieldRenderer;
import exiledsector.ui.decoration.SkillTreeStaticImageRenderer;
import exiledsector.ui.node.NodeSearch;
import exiledsector.ui.node.SkillTreeNodeRenderer;
import exiledsector.ui.util.BorderedPanel;
import lunalib.lunaRefit.BaseRefitButton;
import org.lwjgl.input.Keyboard;

import java.util.List;

public class SkillTreeCanvasPlugin extends BaseCustomUIPanelPlugin {

    private static final float MIN_ZOOM = 0.2f;
    private static final float MAX_ZOOM = 2.5f;
    private static final float ZOOM_STEP = 1.1f;
    private static final float SHIP_CARD_FRAME_OUTSET = 8f;


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
    private final NodeSearch search = new NodeSearch();
    private final SkillTreeSearchBar searchBar;
    private final SkillTreeTemplateController templateUi;
    private final BorderedPanel shipCardPanel = new BorderedPanel(SkillTreeCanvasPlugin.class);
    private final float shipCardHeight;

    private PositionAPI position;
    private boolean dragging = false;
    private float panX = 0f;
    private float panY = 0f;
    private float zoom = 1f;
    private float mouseX = 0f;
    private float mouseY = 0f;
    private boolean mouseKnown = false;
    private SkillNode pendingClickNode;
    private boolean pendingClickCtrlDown;
    private boolean pendingClickShiftDown;
    private SkillType pendingDropdownOption;
    private CameraPanAnimation cameraPan;
    private StartingRootCameraFollow startingRootFollow;
    private boolean swallowEscapeUp;

    public SkillTreeCanvasPlugin(FleetMemberAPI member, ShipVariantAPI variant, float shipCardHeight, BaseRefitButton refitButton) {
        if (SkillTreeInstaller.ensureInstalled(member, variant) && refitButton != null) {
            refitButton.refreshVariant();
        }
        OpReserveParity.warnIfOutOfSync(member, variant, "before the skill tree re-synced it");
        SkillTreeHullMod.syncOpSpentHullMod(member, variant);
        SkillTreePanelStyle style = new SkillTreePanelStyle();
        this.starfieldRenderer = new SkillTreeStarfieldRenderer(style);
        this.staticImageRenderer = new SkillTreeStaticImageRenderer();
        this.ringBeltRenderer = new SkillTreeRingBeltRenderer();
        this.starRenderer = new SkillTreeStarRenderer();
        this.nodeRenderer = new SkillTreeNodeRenderer(member, variant, style, refitButton, search);
        this.searchBar = new SkillTreeSearchBar(search);
        this.templateUi = new SkillTreeTemplateController(member, nodeRenderer, style);
        this.statPanel = new SkillTreeStatPanel(member);
        this.levelBar = new SkillTreeLevelBar(member, 1);
        this.readoutTooltipRenderer = new SkillTreeInfoTooltipRenderer(style);
        this.readoutTooltipBody = buildReadoutTooltipBody(member);
        this.shipCardHeight = shipCardHeight;

        SkillNode startingRoot = nodeRenderer.getStartingRoot();
        if (startingRoot != null) {
            centreOn(startingRoot.getOffsetX(), startingRoot.getOffsetY());
        }
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void advance(float amount) {
        advanceCameraPan(amount);
        searchBar.advance(amount);
        starfieldRenderer.advance(amount);
        staticImageRenderer.advance(amount);
        ringBeltRenderer.advance(amount);
        starRenderer.advance(amount);
        boolean followingStartingRoot = nodeRenderer.isStartingRootMoving();
        if (followingStartingRoot && startingRootFollow == null) {
            startingRootFollow = new StartingRootCameraFollow(-panX / zoom, panY / zoom,
                    nodeRenderer.startingRootCameraTargetX(), nodeRenderer.startingRootCameraTargetY(), zoom);
        }
        nodeRenderer.advance(amount);
        if (followingStartingRoot) {
            float progress = nodeRenderer.startingRootCameraProgress();
            zoom = startingRootFollow.zoom(progress);
            centreOn(startingRootFollow.x(nodeRenderer.startingRootCameraTargetX(), progress),
                    startingRootFollow.y(nodeRenderer.startingRootCameraTargetY(), progress));
            if (!nodeRenderer.isStartingRootMoving()) {
                startingRootFollow = null;
            }
        }
        templateUi.advance(amount, position);
        ShipOpBudget budget = nodeRenderer.budget();
        boolean pointerLive = mouseKnown && !templateUi.isModalOpen();
        statPanel.refresh(budget, nodeRenderer.statsRevision());
        ordnancePointsBar.advance(amount, position, budget.used, budget.total, mouseX, mouseY, pointerLive);
        levelBar.advance(amount, position, mouseX, mouseY, pointerLive);
    }

    private void advanceCameraPan(float amount) {
        if (cameraPan == null) {
            return;
        }
        cameraPan.advance(amount);
        centreOn(cameraPan.x(), cameraPan.y());
        if (cameraPan.isFinished()) {
            cameraPan = null;
        }
    }

    private void centreOn(float treeX, float treeY) {
        panX = -treeX * zoom;
        panY = treeY * zoom;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (position == null) return;

        for (InputEventAPI event : events) {
            if (!event.isConsumed()) {
                handleEvent(event);
            }
        }
    }

    private void handleEvent(InputEventAPI event) {
        if (swallowEscapeUp && event.isKeyUpEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
            swallowEscapeUp = false;
            event.consume();
        } else if (nodeRenderer.isStartingRootInputLocked()) {
            handleStartingRootEvent(event);
        } else if (templateUi.isModalOpen()) {
            handleModalEvent(event);
        } else if (nodeRenderer.isAutoAllocating() && interruptsAutoAllocate(event)) {
            nodeRenderer.cancelAutoAllocate();
            consume(event);
        } else if (nodeRenderer.isRespeccing() && interruptsAutoAllocate(event)) {
            nodeRenderer.cancelRespec();
            consume(event);
        } else if (event.isLMBDownEvent() && position.containsEvent(event)) {
            handleLmbDown(event);
        } else if (event.isLMBUpEvent()) {
            handleLmbUp(event);
        } else if (event.isMouseMoveEvent()) {
            handleMouseMove(event);
        } else if (event.isMouseScrollEvent() && position.containsEvent(event)) {
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
        return (event.isLMBDownEvent() && position.containsEvent(event))
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

    private void handleStartingRootEvent(InputEventAPI event) {
        if (event.isMouseMoveEvent()) {
            handleMouseMove(event);
        } else if (event.isLMBDownEvent() && position.containsEvent(event)) {
            pendingClickNode = isOverOverlay(event.getX(), event.getY())
                    ? null : nodeRenderer.findNodeAt(viewport(), event.getX(), event.getY());
            pendingClickCtrlDown = false;
            pendingClickShiftDown = false;
            event.consume();
        } else if (event.isLMBUpEvent() && pendingClickNode != null) {
            nodeRenderer.chooseStartingRoot(pendingClickNode);
            pendingClickNode = null;
            event.consume();
        } else if (event.isMouseScrollEvent() && position.containsEvent(event)) {
            event.consume();
        }
    }

    private void handleLmbDown(InputEventAPI event) {
        if (searchBar.handleClick(position, event.getX(), event.getY())) {
            event.consume();
            return;
        }
        if (templateUi.handleLmbDown(event.getX(), event.getY())) {
            event.consume();
            return;
        }
        if (statPanel.isCollapseButtonHit(position, event.getX(), event.getY())) {
            statPanel.toggleCollapsed();
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
                dragging = true;
                cameraPan = null;
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
        boolean wasOurGesture = dragging || pendingDropdownOption != null || pendingClickNode != null;
        dragging = false;
        if (pendingDropdownOption != null) {
            nodeRenderer.commitDropdownSelection(pendingDropdownOption);
            pendingDropdownOption = null;
        } else if (pendingClickNode != null) {
            SkillNode jumpTarget = nodeRenderer.wormholeJumpTarget(pendingClickNode, pendingClickCtrlDown);
            if (pendingClickCtrlDown && pendingClickShiftDown && nodeRenderer.isAllocated(pendingClickNode)) {
                nodeRenderer.startRespec(pendingClickNode);
            } else if (jumpTarget != null) {
                cameraPan = new CameraPanAnimation(-panX / zoom, panY / zoom, jumpTarget.getOffsetX(), jumpTarget.getOffsetY());
                nodeRenderer.launchWormholeGhosts(pendingClickNode, jumpTarget);
            } else {
                nodeRenderer.toggleAllocation(pendingClickNode, pendingClickCtrlDown);
            }
            pendingClickNode = null;
        }
        if (wasOurGesture) {
            event.consume();
        }
    }

    private void handleMouseMove(InputEventAPI event) {
        mouseX = event.getX();
        mouseY = event.getY();
        mouseKnown = true;
        if (dragging) {
            panX += event.getDX();
            panY += event.getDY();
            event.consume();
        }
    }

    private void handleMouseScroll(InputEventAPI event) {
        float oldZoom = zoom;
        if (event.getEventValue() > 0) {
            zoom = Math.min(MAX_ZOOM, zoom * ZOOM_STEP);
        } else {
            zoom = Math.max(MIN_ZOOM, zoom / ZOOM_STEP);
        }
        float zoomRatio = zoom / oldZoom;
        panX *= zoomRatio;
        panY *= zoomRatio;
        event.consume();
    }

    @Override
    public void render(float alphaMult) {
        if (position == null) return;

        TreeViewport viewport = viewport();

        float backgroundAlpha = alphaMult * search.backgroundAlpha() * nodeRenderer.treeAlpha();
        starfieldRenderer.render(position, panX, panY, backgroundAlpha);
        starRenderer.renderDisc(viewport, backgroundAlpha);
        starRenderer.renderAtmosphere(viewport, backgroundAlpha);
        starRenderer.renderAurora(viewport, backgroundAlpha);
        ringBeltRenderer.render(viewport, backgroundAlpha);
        staticImageRenderer.render(viewport, backgroundAlpha);
        boolean treeHovered = mouseKnown && !isOverOverlay(mouseX, mouseY) && !templateUi.isModalOpen();
        nodeRenderer.render(viewport, alphaMult, mouseX, mouseY, treeHovered);
        starRenderer.renderGlow(viewport, backgroundAlpha);
        statPanel.render(position, alphaMult);
        ordnancePointsBar.render(position, alphaMult, null);
        levelBar.render(position, alphaMult);
        if (!nodeRenderer.isStartingRootInputLocked()) {
            searchBar.render(position, alphaMult);
        }
        templateUi.renderBar(position, mouseX, mouseY, alphaMult);
        drawShipCardFrame(alphaMult);

        if (!dragging && mouseKnown && !templateUi.isModalOpen()) {
            if (treeHovered) {
                nodeRenderer.renderHoverTooltip(viewport, mouseX, mouseY, alphaMult);
            }
            if (ordnancePointsBar.isHovered(position, mouseX, mouseY) || levelBar.isHovered(position, mouseX, mouseY)) {
                readoutTooltipRenderer.render(readoutTooltipTitle, readoutTooltipBody, mouseX, mouseY, alphaMult);
            }
            templateUi.renderBarTooltip(mouseX, mouseY, alphaMult);
        }
        templateUi.renderModals(position, mouseX, mouseY, alphaMult);
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
        return new ScreenRect(position.getX() + SkillTreeRefitButton.SHIP_CARD_MARGIN - SHIP_CARD_FRAME_OUTSET,
                position.getY() + SkillTreeRefitButton.SHIP_CARD_MARGIN - SHIP_CARD_FRAME_OUTSET,
                SkillTreeRefitButton.SHIP_CARD_ICON_SIZE + SHIP_CARD_FRAME_OUTSET * 2f,
                shipCardHeight + SHIP_CARD_FRAME_OUTSET * 2f);
    }

    private boolean isOverOverlay(float x, float y) {
        return statPanel.contains(x, y)
                || ordnancePointsBar.isHovered(position, x, y)
                || levelBar.isHovered(position, x, y)
                || (!nodeRenderer.isStartingRootInputLocked() && SkillTreeSearchBar.contains(position, x, y))
                || templateUi.barContains(x, y)
                || shipCardFrame().contains(x, y);
    }

    private TreeViewport viewport() {
        return new TreeViewport(position.getX() + position.getWidth() / 2f + panX,
                position.getY() + position.getHeight() / 2f + panY, zoom,
                position.getX(), position.getY(), position.getX() + position.getWidth(), position.getY() + position.getHeight());
    }
}
