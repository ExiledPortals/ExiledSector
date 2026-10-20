package exiledsector.ui;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.layout.Star;
import exiledsector.skills.tags.SkillTags;
import exiledsector.ui.hyperspace.HyperspaceAnchor;
import exiledsector.ui.hyperspace.HyperspaceCamera;
import exiledsector.ui.hyperspace.HyperspaceRoute;
import exiledsector.ui.hyperspace.HyperspaceTransition;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

final class HyperspaceMode {

    private static final float MARGIN = 60f;
    private static final float RETURN_ZOOM = 0.35f;
    private static final float STAR_SCALE = 1.5f;
    private static final Set<String> OFF_SCREEN_REGIONS = SkillTags.ABYSSAL_REGIONS;

    private final TreeCamera treeCamera;
    private final SkillTreeStatPanel statPanel;
    private final HyperspaceTransition hyperspaceTransition = new HyperspaceTransition();
    private final HyperspaceLabels anchorLabels = new HyperspaceLabels();
    private final HyperspaceGhostFlights ghostFlights = new HyperspaceGhostFlights(new Random());
    private List<HyperspaceAnchor> anchors = List.of();
    private Set<String> anchorIds = Set.of();
    private Set<String> hiddenStarIds = Set.of();
    private final Predicate<SkillNode> nodeHidden;
    private final Supplier<List<SkillTreeTopology.WormholePair>> shipWormholePairs;
    private float mapStarRadius;
    private boolean reopenStats;

    HyperspaceMode(TreeCamera treeCamera, SkillTreeStatPanel statPanel, Predicate<SkillNode> nodeHidden,
                   Supplier<List<SkillTreeTopology.WormholePair>> shipWormholePairs) {
        this.treeCamera = treeCamera;
        this.statPanel = statPanel;
        this.nodeHidden = nodeHidden;
        this.shipWormholePairs = shipWormholePairs;
    }

    Set<String> hiddenStarIds() {
        return hiddenStarIds;
    }

    boolean isActive() {
        return hyperspaceTransition.isActive();
    }

    boolean isOnMap() {
        return hyperspaceTransition.isOnMap();
    }

    float treeAlpha() {
        return hyperspaceTransition.isActive() ? hyperspaceTransition.treeAlpha() : 1f;
    }

    float chromeAlpha() {
        return hyperspaceTransition.isActive() ? hyperspaceTransition.chromeAlpha() : 1f;
    }

    float mapAmount() {
        return hyperspaceTransition.isActive() ? hyperspaceTransition.mapAmount() : 0f;
    }

    float starRadius() {
        return mapStarRadius;
    }

    Set<String> anchorIds() {
        return anchorIds;
    }

    void advance(float amount) {
        if (hyperspaceTransition.isActive() && !hyperspaceTransition.isOnMap()) {
            hyperspaceTransition.advance(amount);
            treeCamera.jumpTo(hyperspaceTransition.camera());
        }
        if (hyperspaceTransition.isActive()) {
            ghostFlights.advance(amount);
        }
    }

    void reopenStatsWhenBack() {
        if (reopenStats && (!hyperspaceTransition.isActive() || (hyperspaceTransition.isLeaving() && hyperspaceTransition.chromeAlpha() >= 1f))) {
            reopenStats = false;
            statPanel.open(false);
        }
    }

    boolean enter(PositionAPI canvasPosition) {
        Set<String> lockedRegions = HyperspaceAnchor.lockedRegions(SkillTree.getAllNodes().values(), SkillNode::getRegion, nodeHidden);
        hiddenStarIds = SkillTree.getStars().stream().filter(star -> lockedRegions.contains(star.getRegion())).map(Star::getId)
                .collect(Collectors.toUnmodifiableSet());
        anchors = HyperspaceAnchor.collect(SkillTree.getStars(), SkillTree.getThreatUnits(), SkillTree.getStaticImages()).stream()
                .filter(anchor -> !lockedRegions.contains(anchor.region())).toList();
        if (anchors.isEmpty()) {
            return false;
        }
        Set<String> ids = new HashSet<>();
        for (HyperspaceAnchor anchor : anchors) {
            ids.add(anchor.id());
        }
        anchorIds = ids;
        ghostFlights.setRoutes(HyperspaceRoute.between(anchors, shipWormholePairs.get()));
        treeCamera.stopMoving();
        mapStarRadius = HyperspaceAnchor.mapStarRadius(anchors, STAR_SCALE);
        HyperspaceCamera fittedCamera = framing(canvasPosition);
        HyperspaceCamera mapCamera = new HyperspaceCamera(fittedCamera.x(), fittedCamera.y(), Math.min(fittedCamera.zoom(), treeCamera.zoom()));
        hyperspaceTransition.enter(treeCamera.current(), mapCamera);
        SkillTreeSounds.hyperspaceOut();
        reopenStats = reopenStats || statPanel.isOpen();
        statPanel.close();
        return true;
    }

    private HyperspaceCamera framing(PositionAPI canvasPosition) {
        List<HyperspaceAnchor> framed = anchors.stream().filter(anchor -> !OFF_SCREEN_REGIONS.contains(anchor.region())).toList();
        HyperspaceAnchor core = framed.stream().filter(anchor -> SkillTags.CORE_REGION.equals(anchor.region())).findFirst().orElse(null);
        if (core == null) {
            return HyperspaceCamera.fit(framed.isEmpty() ? anchors : framed, mapStarRadius, canvasPosition.getWidth(), canvasPosition.getHeight(),
                    MARGIN, HyperspaceLabels.LABEL_SPACE);
        }
        return HyperspaceCamera.fitAround(core.x(), core.y(), framed, mapStarRadius, canvasPosition.getWidth(), canvasPosition.getHeight(), MARGIN,
                HyperspaceLabels.LABEL_SPACE);
    }

    void handleEvent(InputEventAPI event, PositionAPI canvasPosition, boolean overOverlay) {
        boolean inside = canvasPosition.containsEvent(event);
        if (event.isLMBDownEvent() && inside && overOverlay) {
            event.consume();
        } else if (event.isLMBDownEvent() && inside) {
            if (hyperspaceTransition.isOnMap()) {
                HyperspaceAnchor clicked = anchorLabels.anchorAt(treeCamera.viewport(canvasPosition), anchors, mapStarRadius, 1f, event.getX(), event.getY());
                if (clicked != null) {
                    hyperspaceTransition.leaveTo(treeCamera.current(), new HyperspaceCamera(clicked.x(), clicked.y(), RETURN_ZOOM));
                    SkillTreeSounds.hyperspaceIn();
                } else {
                    treeCamera.startDrag();
                }
            }
            event.consume();
        } else if (event.isLMBUpEvent() && treeCamera.isDragging()) {
            treeCamera.stopDrag();
            event.consume();
        } else if (event.isMouseScrollEvent() && inside) {
            if (hyperspaceTransition.isOnMap() && event.getEventValue() > 0) {
                treeCamera.stopDrag();
                TreeViewport viewport = treeCamera.viewport(canvasPosition);
                float worldX = (event.getX() - viewport.centerX()) / treeCamera.zoom();
                float worldY = (viewport.centerY() - event.getY()) / treeCamera.zoom();
                hyperspaceTransition.zoomInAbout(treeCamera.current(), worldX, worldY, SmoothZoom.MIN_ZOOM);
                SkillTreeSounds.hyperspaceIn();
            }
            event.consume();
        }
    }

    void render(TreeViewport viewport, float alphaMult, boolean hoverable, float mouseX, float mouseY) {
        HyperspaceAnchor hovered = hyperspaceTransition.isOnMap() && hoverable
                ? anchorLabels.anchorAt(viewport, anchors, mapStarRadius, hyperspaceTransition.mapAmount(), mouseX, mouseY) : null;
        ghostFlights.draw(viewport, alphaMult * hyperspaceTransition.labelAlpha());
        anchorLabels.render(viewport, anchors, mapStarRadius, hyperspaceTransition.mapAmount(), hovered, alphaMult * hyperspaceTransition.labelAlpha());
    }
}
