package exiledsector.ui.hyperspace;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.skills.decoration.Decorations;
import exiledsector.skills.decoration.Star;
import exiledsector.skills.layout.TreeTopology;
import exiledsector.ui.CanvasCamera;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.SmoothZoom;
import exiledsector.ui.TreeViewport;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class HyperspaceMode {

    public record MapContent(Decorations decorations, Set<String> lockedVolumes, List<? extends TreeTopology.WormholePair<?>> wormholePairs) {
    }

    public record Framing(String centreVolume, Set<String> offScreenVolumes) {

        public static final Framing ALL_VOLUMES = new Framing(null, Set.of());
    }

    private static final float MARGIN = 60f;
    private static final float RETURN_ZOOM = 0.35f;
    private static final float STAR_SCALE = 1.5f;

    private final CanvasCamera camera;
    private final Supplier<MapContent> mapContent;
    private final Framing framing;
    private final Runnable onEnter;
    private final HyperspaceTransition hyperspaceTransition = new HyperspaceTransition();
    private final HyperspaceLabels anchorLabels;
    private final HyperspaceGhostFlights ghostFlights = new HyperspaceGhostFlights(new Random());
    private List<HyperspaceAnchor> anchors = List.of();
    private Set<String> anchorIds = Set.of();
    private Set<String> hiddenStarIds = Set.of();
    private float mapStarRadius;

    public HyperspaceMode(CanvasCamera camera, Supplier<MapContent> mapContent, Framing framing, Function<String, String> volumeLabel,
                          Runnable onEnter) {
        this.camera = camera;
        this.mapContent = mapContent;
        this.framing = framing;
        this.anchorLabels = new HyperspaceLabels(volumeLabel);
        this.onEnter = onEnter;
    }

    public Set<String> hiddenStarIds() {
        return hiddenStarIds;
    }

    public boolean isActive() {
        return hyperspaceTransition.isActive();
    }

    public boolean isOnMap() {
        return hyperspaceTransition.isOnMap();
    }

    public float treeAlpha() {
        return hyperspaceTransition.isActive() ? hyperspaceTransition.treeAlpha() : 1f;
    }

    public float chromeAlpha() {
        return hyperspaceTransition.isActive() ? hyperspaceTransition.chromeAlpha() : 1f;
    }

    public float mapAmount() {
        return hyperspaceTransition.isActive() ? hyperspaceTransition.mapAmount() : 0f;
    }

    public float starRadius() {
        return mapStarRadius;
    }

    public Set<String> anchorIds() {
        return anchorIds;
    }

    public void advance(float amount) {
        if (hyperspaceTransition.isActive() && !hyperspaceTransition.isOnMap()) {
            hyperspaceTransition.advance(amount);
            camera.jumpTo(hyperspaceTransition.camera());
        }
        if (hyperspaceTransition.isActive()) {
            ghostFlights.advance(amount);
        }
    }

    public boolean enter(PositionAPI canvasPosition) {
        MapContent content = mapContent.get();
        Set<String> lockedVolumes = content.lockedVolumes();
        Decorations decorations = content.decorations();
        hiddenStarIds = decorations.stars().stream().filter(star -> lockedVolumes.contains(star.getVolume())).map(Star::getId)
                .collect(Collectors.toUnmodifiableSet());
        anchors = HyperspaceAnchor.collect(decorations).stream().filter(anchor -> !lockedVolumes.contains(anchor.volume())).toList();
        if (anchors.isEmpty()) {
            return false;
        }
        Set<String> ids = new HashSet<>();
        for (HyperspaceAnchor anchor : anchors) {
            ids.add(anchor.id());
        }
        anchorIds = ids;
        ghostFlights.setRoutes(HyperspaceRoute.between(anchors, content.wormholePairs()));
        camera.stopMoving();
        mapStarRadius = HyperspaceAnchor.mapStarRadius(anchors, STAR_SCALE);
        HyperspaceCamera fittedCamera = framing(canvasPosition);
        HyperspaceCamera mapCamera = new HyperspaceCamera(fittedCamera.x(), fittedCamera.y(), Math.min(fittedCamera.zoom(), camera.zoom()));
        hyperspaceTransition.enter(camera.current(), mapCamera);
        SkillTreeSounds.hyperspaceOut();
        onEnter.run();
        return true;
    }

    private HyperspaceCamera framing(PositionAPI canvasPosition) {
        List<HyperspaceAnchor> framed = anchors.stream().filter(anchor -> !framing.offScreenVolumes().contains(anchor.volume())).toList();
        HyperspaceAnchor centre = framing.centreVolume() == null ? null
                : framed.stream().filter(anchor -> framing.centreVolume().equals(anchor.volume())).findFirst().orElse(null);
        if (centre == null) {
            return HyperspaceCamera.fit(framed.isEmpty() ? anchors : framed, mapStarRadius, canvasPosition.getWidth(), canvasPosition.getHeight(),
                    MARGIN, HyperspaceLabels.LABEL_SPACE);
        }
        return HyperspaceCamera.fitAround(centre.x(), centre.y(), framed, mapStarRadius, canvasPosition.getWidth(), canvasPosition.getHeight(), MARGIN,
                HyperspaceLabels.LABEL_SPACE);
    }

    public void handleEvent(InputEventAPI event, PositionAPI canvasPosition, boolean overOverlay) {
        boolean inside = canvasPosition.containsEvent(event);
        if (event.isLMBDownEvent() && inside && overOverlay) {
            event.consume();
        } else if (event.isLMBDownEvent() && inside) {
            if (hyperspaceTransition.isOnMap()) {
                HyperspaceAnchor clicked = anchorLabels.anchorAt(camera.viewport(canvasPosition), anchors, mapStarRadius, 1f, event.getX(), event.getY());
                if (clicked != null) {
                    hyperspaceTransition.leaveTo(camera.current(), new HyperspaceCamera(clicked.x(), clicked.y(), RETURN_ZOOM));
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
            if (hyperspaceTransition.isOnMap() && event.getEventValue() > 0) {
                camera.stopDrag();
                TreeViewport viewport = camera.viewport(canvasPosition);
                float worldX = (event.getX() - viewport.centerX()) / camera.zoom();
                float worldY = (viewport.centerY() - event.getY()) / camera.zoom();
                hyperspaceTransition.zoomInAbout(camera.current(), worldX, worldY, SmoothZoom.MIN_ZOOM);
                SkillTreeSounds.hyperspaceIn();
            }
            event.consume();
        }
    }

    public void render(TreeViewport viewport, float alphaMult, boolean hoverable, float mouseX, float mouseY) {
        HyperspaceAnchor hovered = hyperspaceTransition.isOnMap() && hoverable
                ? anchorLabels.anchorAt(viewport, anchors, mapStarRadius, hyperspaceTransition.mapAmount(), mouseX, mouseY) : null;
        ghostFlights.draw(viewport, alphaMult * hyperspaceTransition.labelAlpha());
        anchorLabels.render(viewport, anchors, mapStarRadius, hyperspaceTransition.mapAmount(), hovered, alphaMult * hyperspaceTransition.labelAlpha());
    }
}
