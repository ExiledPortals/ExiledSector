package exiledsector.ui;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.skills.SkillTree;
import exiledsector.ui.hyperspace.HyperspaceAnchor;
import exiledsector.ui.hyperspace.HyperspaceCamera;
import exiledsector.ui.hyperspace.HyperspaceRoute;
import exiledsector.ui.hyperspace.HyperspaceTransition;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

final class HyperspaceMode {

    private static final float MARGIN = 60f;
    private static final float RETURN_ZOOM = 0.35f;
    private static final float STAR_SCALE = 1.5f;

    private final TreeCamera treeCamera;
    private final SkillTreeStatPanel statPanel;
    private final HyperspaceTransition hyperspaceTransition = new HyperspaceTransition();
    private final HyperspaceLabels anchorLabels = new HyperspaceLabels();
    private final HyperspaceGhostFlights ghostFlights = new HyperspaceGhostFlights(new Random());
    private List<HyperspaceAnchor> anchors = List.of();
    private Set<String> anchorIds = Set.of();
    private float mapStarRadius;
    private boolean reopenStats;

    HyperspaceMode(TreeCamera treeCamera, SkillTreeStatPanel statPanel) {
        this.treeCamera = treeCamera;
        this.statPanel = statPanel;
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
        anchors = HyperspaceAnchor.collect(SkillTree.getStars(), SkillTree.getStaticImages());
        if (anchors.isEmpty()) {
            return false;
        }
        Set<String> ids = new HashSet<>();
        for (HyperspaceAnchor anchor : anchors) {
            ids.add(anchor.id());
        }
        anchorIds = ids;
        ghostFlights.setRoutes(HyperspaceRoute.between(anchors, SkillTree.topology().wormholePairs()));
        treeCamera.stopMoving();
        mapStarRadius = HyperspaceAnchor.mapStarRadius(anchors, STAR_SCALE);
        HyperspaceCamera fittedCamera = HyperspaceCamera.fit(anchors, mapStarRadius, canvasPosition.getWidth(), canvasPosition.getHeight(), MARGIN,
                HyperspaceLabels.LABEL_SPACE);
        HyperspaceCamera mapCamera = new HyperspaceCamera(fittedCamera.x(), fittedCamera.y(), Math.min(fittedCamera.zoom(), treeCamera.zoom()));
        hyperspaceTransition.enter(treeCamera.current(), mapCamera);
        SkillTreeSounds.hyperspaceOut();
        reopenStats = reopenStats || statPanel.isOpen();
        statPanel.close();
        return true;
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
