package exiledsector.ui;

import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.ui.hyperspace.HyperspaceCamera;
import exiledsector.ui.node.SkillTreeNodeRenderer;

final class TreeCamera {

    private final SmoothZoom smoothZoom = new SmoothZoom(1f);
    private float panX;
    private float panY;
    private float zoom = 1f;
    private float zoomPivotX;
    private float zoomPivotY;
    private boolean dragging;
    private CameraPanAnimation pan;
    private StartingRootCameraFollow startingRootFollow;

    void advanceZoom(float amount) {
        smoothZoom.advance(amount);
        float zoomRatio = smoothZoom.current() / zoom;
        zoom = smoothZoom.current();
        panX = SmoothZoom.panAbout(panX, zoomPivotX, zoomRatio);
        panY = SmoothZoom.panAbout(panY, zoomPivotY, zoomRatio);
    }

    void advancePan(float amount) {
        if (pan == null) {
            return;
        }
        pan.advance(amount);
        centreOn(pan.x(), pan.y());
        if (pan.isFinished()) {
            pan = null;
        }
    }

    void beginStartingRootFollow(SkillTreeNodeRenderer nodeRenderer) {
        if (nodeRenderer.isStartingRootMoving() && startingRootFollow == null) {
            startingRootFollow = new StartingRootCameraFollow(-panX / zoom, panY / zoom,
                    nodeRenderer.startingRootCameraTargetX(), nodeRenderer.startingRootCameraTargetY(), zoom,
                    nodeRenderer.isStartingRootFlyingOut() ? SmoothZoom.MIN_ZOOM : StartingRootCameraFollow.CHOOSING_ZOOM);
        }
    }

    void applyStartingRootFollow(SkillTreeNodeRenderer nodeRenderer, boolean wasMoving) {
        if (!wasMoving) {
            return;
        }
        float progress = nodeRenderer.startingRootCameraProgress();
        zoom = startingRootFollow.zoom(progress);
        smoothZoom.jumpTo(zoom);
        centreOn(startingRootFollow.x(nodeRenderer.startingRootCameraTargetX(), progress),
                startingRootFollow.y(nodeRenderer.startingRootCameraTargetY(), progress));
        if (!nodeRenderer.isStartingRootMoving()) {
            startingRootFollow = null;
        }
    }

    void centreOn(float treeX, float treeY) {
        panX = -treeX * zoom;
        panY = treeY * zoom;
    }

    void jumpTo(HyperspaceCamera camera) {
        zoom = camera.zoom();
        smoothZoom.jumpTo(zoom);
        centreOn(camera.x(), camera.y());
    }

    HyperspaceCamera current() {
        return new HyperspaceCamera(-panX / zoom, panY / zoom, zoom);
    }

    void panTo(float treeX, float treeY) {
        pan = new CameraPanAnimation(-panX / zoom, panY / zoom, treeX, treeY);
    }

    boolean isPanning() {
        return pan != null;
    }

    void startDrag() {
        dragging = true;
        pan = null;
    }

    void stopDrag() {
        dragging = false;
    }

    void stopMoving() {
        dragging = false;
        pan = null;
    }

    boolean isDragging() {
        return dragging;
    }

    boolean dragBy(float dx, float dy) {
        if (!dragging) {
            return false;
        }
        panX += dx;
        panY += dy;
        return true;
    }

    void scroll(boolean in, float pivotX, float pivotY) {
        zoomPivotX = pivotX;
        zoomPivotY = pivotY;
        smoothZoom.scroll(in);
    }

    boolean isSettledAtMinimum() {
        return smoothZoom.isSettledAtMinimum();
    }

    float zoom() {
        return zoom;
    }

    float panX() {
        return panX;
    }

    float panY() {
        return panY;
    }

    TreeViewport viewport(PositionAPI position) {
        return new TreeViewport(position.getX() + position.getWidth() / 2f + panX,
                position.getY() + position.getHeight() / 2f + panY, zoom,
                position.getX(), position.getY(), position.getX() + position.getWidth(), position.getY() + position.getHeight());
    }
}
