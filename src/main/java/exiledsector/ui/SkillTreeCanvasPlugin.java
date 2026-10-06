package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.effects.OpReserveParity;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.effects.SkillTreeInstaller;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipLevelSystem;
import exiledsector.skills.progression.ShipOpBudget;
import exiledsector.skills.progression.SkillNodeOpCost;
import exiledsector.socketables.SocketCustody;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableStore;
import exiledsector.ui.decoration.SkillTreeRingBeltRenderer;
import exiledsector.ui.decoration.SkillTreeStarRenderer;
import exiledsector.ui.decoration.SkillTreeStarfieldRenderer;
import exiledsector.ui.decoration.SkillTreeStaticImageRenderer;
import exiledsector.ui.hyperspace.HyperspaceAnchor;
import exiledsector.ui.hyperspace.HyperspaceCamera;
import exiledsector.ui.hyperspace.HyperspaceRoute;
import exiledsector.ui.hyperspace.HyperspaceTransition;
import exiledsector.ui.node.NodeSearch;
import exiledsector.ui.node.SkillTreeNodeRenderer;
import exiledsector.ui.socket.SocketStoragePanel;
import exiledsector.ui.util.BorderedPanel;
import lunalib.lunaRefit.BaseRefitButton;
import org.lwjgl.input.Keyboard;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class SkillTreeCanvasPlugin extends BaseCustomUIPanelPlugin {

    private static final float SHIP_CARD_FRAME_OUTSET = 8f;
    private static final float HYPERSPACE_MARGIN = 60f;
    private static final float HYPERSPACE_RETURN_ZOOM = 0.35f;
    private static final float HYPERSPACE_STAR_SCALE = 1.5f;
    private static final float STORAGE_BUTTON_MARGIN = 16f;
    private static final float STORAGE_BUTTON_HEIGHT = 40f;
    private static final float STORAGE_BUTTON_MIN_WIDTH = 150f;
    private static final float STORAGE_PANEL_MARGIN = 16f;
    private static final float STORAGE_PANEL_TOP = 100f;
    private static final float STORAGE_PANEL_GAP = 12f;
    private static final float STORAGE_PANEL_WIDTH_FRACTION = 0.375f;

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
    private final SkillTreeUiButton storageButton = new SkillTreeUiButton("");
    private final CustomPanelAPI host;
    private SocketStoragePanel storagePanel;
    private Socketable placing;
    private int storageRevision;
    private Map<String, FleetMemberAPI> ownedShips = Map.of();
    private final float shipCardHeight;
    private final SmoothZoom smoothZoom = new SmoothZoom(1f);
    private final HyperspaceTransition hyperspace = new HyperspaceTransition();
    private final HyperspaceLabels hyperspaceLabels = new HyperspaceLabels();
    private final HyperspaceGhostFlights hyperspaceGhosts = new HyperspaceGhostFlights(new Random());
    private List<HyperspaceAnchor> hyperspaceAnchors = List.of();
    private Set<String> hyperspaceAnchorIds = Set.of();
    private float hyperspaceStarRadius;

    private PositionAPI position;
    private boolean dragging = false;
    private float panX = 0f;
    private float panY = 0f;
    private float zoom = 1f;
    private float zoomPivotX = 0f;
    private float zoomPivotY = 0f;
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

    public SkillTreeCanvasPlugin(FleetMemberAPI member, ShipVariantAPI variant, float shipCardHeight, BaseRefitButton refitButton,
                                 CustomPanelAPI host) {
        if (SkillTreeInstaller.ensureInstalled(member, variant) && refitButton != null) {
            refitButton.refreshVariant();
        }
        OpReserveParity.warnIfOutOfSync(member, variant, "before the skill tree re-synced it");
        SkillTreeHullMod.syncOpSpentHullMod(member, variant);
        SocketCustody.reconcile();
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
        this.host = host;
        refreshStorageButtonLabel();

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
        advanceZoom(amount);
        advanceHyperspace(amount);
        if (hyperspace.isActive()) {
            hyperspaceGhosts.advance(amount);
        }
        advanceCameraPan(amount);
        searchBar.advance(amount);
        starfieldRenderer.advance(amount);
        staticImageRenderer.advance(amount);
        ringBeltRenderer.advance(amount);
        starRenderer.advance(amount);
        boolean followingStartingRoot = nodeRenderer.isStartingRootMoving();
        if (followingStartingRoot && startingRootFollow == null) {
            startingRootFollow = new StartingRootCameraFollow(-panX / zoom, panY / zoom,
                    nodeRenderer.startingRootCameraTargetX(), nodeRenderer.startingRootCameraTargetY(), zoom,
                    nodeRenderer.isStartingRootFlyingOut() ? SmoothZoom.MIN_ZOOM : StartingRootCameraFollow.CHOOSING_ZOOM);
        }
        nodeRenderer.advance(amount);
        if (followingStartingRoot) {
            float progress = nodeRenderer.startingRootCameraProgress();
            zoom = startingRootFollow.zoom(progress);
            smoothZoom.jumpTo(zoom);
            centreOn(startingRootFollow.x(nodeRenderer.startingRootCameraTargetX(), progress),
                    startingRootFollow.y(nodeRenderer.startingRootCameraTargetY(), progress));
            if (!nodeRenderer.isStartingRootMoving()) {
                startingRootFollow = null;
            }
        }
        templateUi.advance(amount, position);
        layoutStorageButton();
        advanceStorage();
        ShipOpBudget budget = nodeRenderer.budget();
        boolean pointerLive = mouseKnown && !isModalOpen();
        statPanel.refresh(budget, nodeRenderer.statsRevision());
        ordnancePointsBar.advance(amount, position, budget.used, budget.total, mouseX, mouseY, pointerLive);
        levelBar.advance(amount, position, mouseX, mouseY, pointerLive);
    }

    private void advanceZoom(float amount) {
        smoothZoom.advance(amount);
        float zoomRatio = smoothZoom.current() / zoom;
        zoom = smoothZoom.current();
        panX = SmoothZoom.panAbout(panX, zoomPivotX, zoomRatio);
        panY = SmoothZoom.panAbout(panY, zoomPivotY, zoomRatio);
    }

    private void advanceHyperspace(float amount) {
        if (!hyperspace.isActive() || hyperspace.isOnMap()) {
            return;
        }
        hyperspace.advance(amount);
        HyperspaceCamera camera = hyperspace.camera();
        zoom = camera.zoom();
        smoothZoom.jumpTo(zoom);
        centreOn(camera.x(), camera.y());
    }

    private void enterHyperspace() {
        hyperspaceAnchors = HyperspaceAnchor.collect(SkillTree.getStars(), SkillTree.getStaticImages());
        if (hyperspaceAnchors.isEmpty()) {
            return;
        }
        Set<String> anchorIds = new HashSet<>();
        for (HyperspaceAnchor anchor : hyperspaceAnchors) {
            anchorIds.add(anchor.id());
        }
        hyperspaceAnchorIds = anchorIds;
        hyperspaceGhosts.setRoutes(HyperspaceRoute.between(hyperspaceAnchors, SkillTree.topology().wormholePairs()));
        dragging = false;
        cameraPan = null;
        pendingClickNode = null;
        pendingDropdownOption = null;
        nodeRenderer.closeDropdown();
        searchBar.unfocus();
        hyperspaceStarRadius = HyperspaceAnchor.mapStarRadius(hyperspaceAnchors, HYPERSPACE_STAR_SCALE);
        HyperspaceCamera fit = HyperspaceCamera.fit(hyperspaceAnchors, hyperspaceStarRadius, position.getWidth(), position.getHeight(),
                HYPERSPACE_MARGIN, HyperspaceLabels.LABEL_SPACE);
        HyperspaceCamera map = new HyperspaceCamera(fit.x(), fit.y(), Math.min(fit.zoom(), zoom));
        hyperspace.enter(currentCamera(), map);
        SkillTreeSounds.hyperspaceOut();
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
        boolean escapeUp = event.isKeyUpEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE;
        if (escapeUp && (swallowEscapeUp || storagePanel != null || placing != null)) {
            swallowEscapeUp = false;
            event.consume();
        } else if (!templateUi.isModalOpen() && TooltipExpansion.isToggle(event)) {
            TooltipExpansion.toggle();
            event.consume();
        } else if (nodeRenderer.isStartingRootInputLocked()) {
            handleStartingRootEvent(event);
        } else if (templateUi.isModalOpen()) {
            handleModalEvent(event);
        } else if (hyperspace.isActive()) {
            handleHyperspaceEvent(event);
        } else if (nodeRenderer.isAutoAllocating() && interruptsAutoAllocate(event)) {
            nodeRenderer.cancelAutoAllocate();
            consume(event);
        } else if (nodeRenderer.isRespeccing() && interruptsAutoAllocate(event)) {
            nodeRenderer.cancelRespec();
            consume(event);
        } else if ((placing != null || storagePanel != null) && event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
            escapeStorage();
            consume(event);
        } else if (event.isRMBDownEvent() && position.containsEvent(event) && emptySocketAt(event.getX(), event.getY())) {
            event.consume();
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

    private void escapeStorage() {
        if (storagePanel != null && storagePanel.escape()) {
            return;
        }
        if (placing != null) {
            stopPlacing();
        } else if (storagePanel != null) {
            storagePanel.close();
        }
    }

    private boolean emptySocketAt(float x, float y) {
        if (isOverOverlay(x, y)) {
            return false;
        }
        SkillNode node = nodeRenderer.findNodeAt(viewport(), x, y);
        if (node == null || node.getType().getTier() != SkillTier.SOCKET) {
            return false;
        }
        if (nodeRenderer.emptySocket(node)) {
            refreshStorage();
        }
        return true;
    }

    private void startPlacing(Socketable socketable) {
        placing = socketable;
        search.setSocketFocus(true);
        if (storagePanel != null) {
            storagePanel.setSelected(socketable);
        }
    }

    private void stopPlacing() {
        placing = null;
        search.setSocketFocus(false);
        if (storagePanel != null) {
            storagePanel.setSelected(null);
        }
    }

    private void refreshStorage() {
        refreshStorageButtonLabel();
        if (storagePanel != null) {
            storageRevision = nodeRenderer.statsRevision();
            storagePanel.refresh(SocketCustody.shipNames(ownedShips));
        }
    }

    private void advanceStorage() {
        if (storagePanel == null) {
            return;
        }
        if (templateUi.isModalOpen() || hyperspace.isActive() || nodeRenderer.isStartingRootInputLocked()) {
            storagePanel.close();
        } else if (nodeRenderer.statsRevision() != storageRevision) {
            refreshStorage();
        }
    }

    private boolean isModalOpen() {
        return templateUi.isModalOpen();
    }

    private boolean isStorageButtonShown() {
        return !nodeRenderer.isStartingRootInputLocked() && !hyperspace.isActive();
    }

    private void layoutStorageButton() {
        if (!isStorageButtonShown()) {
            storageButton.hide();
            return;
        }
        ScreenRect shipCard = shipCardFrame();
        float width = Math.max(STORAGE_BUTTON_MIN_WIDTH, storageButton.preferredWidth());
        storageButton.place(shipCard.left() + shipCard.width() + STORAGE_BUTTON_MARGIN, position.getY() + STORAGE_BUTTON_MARGIN, width,
                STORAGE_BUTTON_HEIGHT);
    }

    private void refreshStorageButtonLabel() {
        int stored = Global.getSector() == null ? 0 : SocketableStore.get().owned().size();
        storageButton.setLabel(Translation.msg("ui.socketStorage.button").arg("count", stored).text());
    }

    private void toggleStorage() {
        if (storagePanel != null) {
            storagePanel.close();
        } else {
            openStorage();
        }
    }

    private void openStorage() {
        if (storagePanel != null) {
            return;
        }
        SkillTreeSounds.panelOpened();
        nodeRenderer.closeDropdown();
        searchBar.unfocus();
        ownedShips = SocketCustody.reconcile();
        storageRevision = nodeRenderer.statsRevision();
        ScreenRect shipCard = shipCardFrame();
        float top = STORAGE_PANEL_TOP;
        float bottom = shipCard.bottom() + shipCard.height() - position.getY() + STORAGE_PANEL_GAP;
        storagePanel = SocketStoragePanel.open(host, STORAGE_PANEL_MARGIN, top, position.getWidth() * STORAGE_PANEL_WIDTH_FRACTION,
                position.getHeight() - top - bottom, SocketCustody.shipNames(ownedShips), new SocketStoragePanel.Listener() {
                    @Override
                    public void selected(Socketable socketable) {
                        if (socketable == null) {
                            stopPlacing();
                        } else {
                            startPlacing(socketable);
                        }
                    }

                    @Override
                    public void closed() {
                        storagePanel = null;
                        stopPlacing();
                        refreshStorageButtonLabel();
                    }
                });
    }

    private void handleHyperspaceEvent(InputEventAPI event) {
        boolean inside = position.containsEvent(event);
        if (event.isMouseMoveEvent()) {
            handleMouseMove(event);
        } else if (event.isLMBDownEvent() && inside && isOverOverlay(event.getX(), event.getY())) {
            if (statPanel.isCollapseButtonHit(position, event.getX(), event.getY())) {
                statPanel.toggleCollapsed();
            }
            event.consume();
        } else if (event.isLMBDownEvent() && inside) {
            if (hyperspace.isOnMap()) {
                HyperspaceAnchor clicked = hyperspaceLabels.anchorAt(viewport(), hyperspaceAnchors, hyperspaceStarRadius, 1f,
                        event.getX(), event.getY());
                if (clicked != null) {
                    hyperspace.leaveTo(currentCamera(), new HyperspaceCamera(clicked.x(), clicked.y(), HYPERSPACE_RETURN_ZOOM));
                    SkillTreeSounds.hyperspaceIn();
                } else {
                    dragging = true;
                }
            }
            event.consume();
        } else if (event.isLMBUpEvent() && dragging) {
            dragging = false;
            event.consume();
        } else if (event.isMouseScrollEvent() && inside) {
            if (hyperspace.isOnMap() && event.getEventValue() > 0) {
                dragging = false;
                TreeViewport viewport = viewport();
                float worldX = (event.getX() - viewport.centerX()) / zoom;
                float worldY = (viewport.centerY() - event.getY()) / zoom;
                hyperspace.zoomInAbout(currentCamera(), worldX, worldY, SmoothZoom.MIN_ZOOM);
                SkillTreeSounds.hyperspaceIn();
            }
            event.consume();
        }
    }

    private HyperspaceCamera currentCamera() {
        return new HyperspaceCamera(-panX / zoom, panY / zoom, zoom);
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
        if (storageButton.isClickable(event.getX(), event.getY())) {
            toggleStorage();
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
            if (placing != null) {
                if (nodeRenderer.installInSocket(pendingClickNode, placing)) {
                    stopPlacing();
                    refreshStorage();
                }
            } else if (pendingClickCtrlDown && pendingClickShiftDown && nodeRenderer.isAllocated(pendingClickNode)) {
                nodeRenderer.startRespec(pendingClickNode);
            } else if (pendingClickCtrlDown && pendingClickNode.getType().getTier() == SkillTier.SOCKET) {
                if (!nodeRenderer.isAllocated(pendingClickNode)) {
                    nodeRenderer.toggleAllocation(pendingClickNode, false);
                }
                if (nodeRenderer.isAllocated(pendingClickNode)) {
                    openStorage();
                }
            } else if (jumpTarget != null) {
                cameraPan = new CameraPanAnimation(-panX / zoom, panY / zoom, jumpTarget.getOffsetX(), jumpTarget.getOffsetY());
                nodeRenderer.launchWormholeGhosts(pendingClickNode, jumpTarget);
                SkillTreeSounds.wormholeJumped();
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
        boolean in = event.getEventValue() > 0;
        if (!in && smoothZoom.isSettledAtMinimum() && cameraPan == null
                && !nodeRenderer.isAutoAllocating() && !nodeRenderer.isRespeccing()) {
            enterHyperspace();
        } else {
            zoomPivotX = in ? event.getX() - (position.getX() + position.getWidth() / 2f) : 0f;
            zoomPivotY = in ? event.getY() - (position.getY() + position.getHeight() / 2f) : 0f;
            smoothZoom.scroll(in);
        }
        event.consume();
    }

    @Override
    public void render(float alphaMult) {
        if (position == null) return;

        TreeViewport viewport = viewport();

        boolean inHyperspace = hyperspace.isActive();
        float treeAlpha = inHyperspace ? hyperspace.treeAlpha() : 1f;
        float searchDim = search.backgroundAlpha() + (1f - search.backgroundAlpha()) * (1f - treeAlpha);
        float backgroundAlpha = alphaMult * searchDim * nodeRenderer.treeAlpha();
        float mapAmount = inHyperspace ? hyperspace.mapAmount() : 0f;
        starRenderer.setMapRadius(hyperspaceStarRadius, mapAmount);
        starfieldRenderer.render(position, panX, panY, backgroundAlpha);
        starRenderer.renderDisc(viewport, backgroundAlpha);
        starRenderer.renderAtmosphere(viewport, backgroundAlpha);
        starRenderer.renderAurora(viewport, backgroundAlpha);
        ringBeltRenderer.render(viewport, backgroundAlpha * treeAlpha);
        staticImageRenderer.render(viewport, backgroundAlpha, hyperspaceAnchorIds, treeAlpha);
        boolean treeHovered = !inHyperspace && mouseKnown && !isOverOverlay(mouseX, mouseY) && !isModalOpen();
        if (treeAlpha > 0f) {
            nodeRenderer.render(viewport, alphaMult * treeAlpha, mouseX, mouseY, treeHovered);
        }
        starRenderer.renderGlow(viewport, backgroundAlpha);
        if (inHyperspace) {
            HyperspaceAnchor hovered = hyperspace.isOnMap() && mouseKnown && !isOverOverlay(mouseX, mouseY)
                    ? hyperspaceLabels.anchorAt(viewport, hyperspaceAnchors, hyperspaceStarRadius, mapAmount, mouseX, mouseY) : null;
            hyperspaceGhosts.draw(viewport, alphaMult * hyperspace.labelAlpha());
            hyperspaceLabels.render(viewport, hyperspaceAnchors, hyperspaceStarRadius, mapAmount, hovered, alphaMult * hyperspace.labelAlpha());
        }
        statPanel.render(position, alphaMult);
        ordnancePointsBar.render(position, alphaMult, null);
        levelBar.render(position, alphaMult);
        if (!nodeRenderer.isStartingRootInputLocked() && treeAlpha > 0f) {
            searchBar.render(position, alphaMult * treeAlpha);
        }
        templateUi.renderBar(position, mouseX, mouseY, alphaMult);
        storageButton.render(mouseX, mouseY, alphaMult);
        drawShipCardFrame(alphaMult);

        if (!dragging && mouseKnown && !isModalOpen()) {
            if (treeHovered) {
                nodeRenderer.renderHoverTooltip(viewport, mouseX, mouseY, alphaMult);
            }
            if (ordnancePointsBar.isHovered(position, mouseX, mouseY) || levelBar.isHovered(position, mouseX, mouseY)) {
                readoutTooltipRenderer.render(readoutTooltipTitle, readoutTooltipBody, mouseX, mouseY, alphaMult);
            }
            if (storageButton.contains(mouseX, mouseY)) {
                readoutTooltipRenderer.render(Translation.text("ui.socketStorage.title"), Translation.text("ui.socketStorage.hint"),
                        mouseX, mouseY, alphaMult);
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
                || (!nodeRenderer.isStartingRootInputLocked() && !hyperspace.isActive() && SkillTreeSearchBar.contains(position, x, y))
                || templateUi.barContains(x, y)
                || storageButton.contains(x, y)
                || (storagePanel != null && storagePanel.contains(x, y))
                || shipCardFrame().contains(x, y);
    }

    private TreeViewport viewport() {
        return new TreeViewport(position.getX() + position.getWidth() / 2f + panX,
                position.getY() + position.getHeight() / 2f + panY, zoom,
                position.getX(), position.getY(), position.getX() + position.getWidth(), position.getY() + position.getHeight());
    }
}
