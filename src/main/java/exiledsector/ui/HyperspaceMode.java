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

    private final TreeCamera camera;
    private final SkillTreeStatPanel statPanel;
    private final HyperspaceTransition transition = new HyperspaceTransition();
    private final HyperspaceLabels labels = new HyperspaceLabels();
    private final HyperspaceGhostFlights ghosts = new HyperspaceGhostFlights(new Random());
    private List<HyperspaceAnchor> anchors = List.of();
    private Set<String> anchorIds = Set.of();
    private float starRadius;
    private boolean reopenStats;

    HyperspaceMode(TreeCamera camera, SkillTreeStatPanel statPanel) {
        this.camera = camera;
        this.statPanel = statPanel;
    }

    boolean isActive() {
        return transition.isActive();
    }

    boolean isOnMap() {
        return transition.isOnMap();
    }

    float treeAlpha() {
        return transition.isActive() ? transition.treeAlpha() : 1f;
    }

    float chromeAlpha() {
        return transition.isActive() ? transition.chromeAlpha() : 1f;
    }

    float mapAmount() {
        return transition.isActive() ? transition.mapAmount() : 0f;
    }

    float starRadius() {
        return starRadius;
    }

    Set<String> anchorIds() {
        return anchorIds;
    }

    void advance(float amount) {
        if (transition.isActive() && !transition.isOnMap()) {
            transition.advance(amount);
            camera.jumpTo(transition.camera());
        }
        if (transition.isActive()) {
            ghosts.advance(amount);
        }
    }

    void reopenStatsWhenBack() {
        if (reopenStats && (!transition.isActive() || (transition.isLeaving() && transition.chromeAlpha() >= 1f))) {
            reopenStats = false;
            statPanel.open(false);
        }
    }

    boolean enter(PositionAPI position) {
        anchors = HyperspaceAnchor.collect(SkillTree.getStars(), SkillTree.getStaticImages());
        if (anchors.isEmpty()) {
            return false;
        }
        Set<String> ids = new HashSet<>();
        for (HyperspaceAnchor anchor : anchors) {
            ids.add(anchor.id());
        }
        anchorIds = ids;
        ghosts.setRoutes(HyperspaceRoute.between(anchors, SkillTree.topology().wormholePairs()));
        camera.stopMoving();
        starRadius = HyperspaceAnchor.mapStarRadius(anchors, STAR_SCALE);
        HyperspaceCamera fit = HyperspaceCamera.fit(anchors, starRadius, position.getWidth(), position.getHeight(), MARGIN,
                HyperspaceLabels.LABEL_SPACE);
        HyperspaceCamera map = new HyperspaceCamera(fit.x(), fit.y(), Math.min(fit.zoom(), camera.zoom()));
        transition.enter(camera.current(), map);
        SkillTreeSounds.hyperspaceOut();
        reopenStats = reopenStats || statPanel.isOpen();
        statPanel.close();
        return true;
    }

    void handleEvent(InputEventAPI event, PositionAPI position, boolean overOverlay) {
        boolean inside = position.containsEvent(event);
        if (event.isLMBDownEvent() && inside && overOverlay) {
            event.consume();
        } else if (event.isLMBDownEvent() && inside) {
            if (transition.isOnMap()) {
                HyperspaceAnchor clicked = labels.anchorAt(camera.viewport(position), anchors, starRadius, 1f, event.getX(), event.getY());
                if (clicked != null) {
                    transition.leaveTo(camera.current(), new HyperspaceCamera(clicked.x(), clicked.y(), RETURN_ZOOM));
                    SkillTreeSounds.hyperspaceIn();
                } else {
                    camera.startDrag();
                }
            }
            event.consume();
        } else if (event.isLMBUpEvent() && camera.isDragging()) {
            camera.stopDrag();
            event.consume();
        } else if (event.isMouseScrollEvent() && inside) {
            if (transition.isOnMap() && event.getEventValue() > 0) {
                camera.stopDrag();
                TreeViewport viewport = camera.viewport(position);
                float worldX = (event.getX() - viewport.centerX()) / camera.zoom();
                float worldY = (viewport.centerY() - event.getY()) / camera.zoom();
                transition.zoomInAbout(camera.current(), worldX, worldY, SmoothZoom.MIN_ZOOM);
                SkillTreeSounds.hyperspaceIn();
            }
            event.consume();
        }
    }

    void render(TreeViewport viewport, float alphaMult, boolean hoverable, float mouseX, float mouseY) {
        HyperspaceAnchor hovered = transition.isOnMap() && hoverable
                ? labels.anchorAt(viewport, anchors, starRadius, transition.mapAmount(), mouseX, mouseY) : null;
        ghosts.draw(viewport, alphaMult * transition.labelAlpha());
        labels.render(viewport, anchors, starRadius, transition.mapAmount(), hovered, alphaMult * transition.labelAlpha());
    }
}
