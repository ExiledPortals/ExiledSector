package exiledsector.ui;

import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.ui.hyperspace.HyperspaceCamera;
import exiledsector.ui.node.TreeAllocationSession;

final class TreeCamera {

    static final float FOLLOW_SETTLE_SECONDS = 1.2f;
    static final float FOLLOW_OFFSET_RESPONSE_PER_SECOND = 6f;

    private final SmoothZoom smoothZoom = new SmoothZoom(1f);
    private float panX;
    private float panY;
    private float currentZoom = 1f;
    private float zoomPivotX;
    private float zoomPivotY;
    private boolean dragging;
    private CameraPanAnimation panAnimation;
    private StartingRootCameraFollow startingRootFollow;
    private boolean followingTarget;
    private float followStartX;
    private float followStartY;
    private float followSeconds;
    private float followOffsetX;
    private float followOffsetTargetX;

    void startFollowing() {
        followingTarget = true;
        followStartX = -panX / currentZoom;
        followStartY = panY / currentZoom;
        followSeconds = 0f;
        dragging = false;
        panAnimation = null;
        zoomPivotX = 0f;
        zoomPivotY = 0f;
        smoothZoom.aimAt(SmoothZoom.MAX_ZOOM);
    }

    void stopFollowing() {
        followingTarget = false;
        followOffsetX = 0f;
        followOffsetTargetX = 0f;
    }

    void setFollowOffset(float screenOffsetX) {
        followOffsetTargetX = screenOffsetX;
    }

    void inspect(float zoom) {
        smoothZoom.aimBeyondMax(zoom);
    }

    void stopInspecting() {
        if (smoothZoom.target() > SmoothZoom.MAX_ZOOM) {
            smoothZoom.aimAt(SmoothZoom.MAX_ZOOM);
        }
    }

    boolean isFollowSettled() {
        return followingTarget && followSeconds >= FOLLOW_SETTLE_SECONDS;
    }

    boolean isFollowing() {
        return followingTarget;
    }

    void follow(float treeX, float treeY, float amount) {
        if (!followingTarget) {
            return;
        }
        followSeconds += amount;
        float progress = Math.min(1f, followSeconds / FOLLOW_SETTLE_SECONDS);
        float eased = progress * progress * (3f - 2f * progress);
        centreOn(followStartX + (treeX - followStartX) * eased, followStartY + (treeY - followStartY) * eased);
        followOffsetX += (followOffsetTargetX - followOffsetX) * Math.min(1f, amount * FOLLOW_OFFSET_RESPONSE_PER_SECOND);
        panX += followOffsetX;
    }

    void advanceZoom(float amount) {
        smoothZoom.advance(amount);
        float zoomRatio = smoothZoom.current() / currentZoom;
        currentZoom = smoothZoom.current();
        panX = SmoothZoom.panAbout(panX, zoomPivotX, zoomRatio);
        panY = SmoothZoom.panAbout(panY, zoomPivotY, zoomRatio);
    }

    void advancePan(float amount) {
        if (panAnimation == null) {
            return;
        }
        panAnimation.advance(amount);
        centreOn(panAnimation.x(), panAnimation.y());
        if (panAnimation.isFinished()) {
            panAnimation = null;
        }
    }

    void beginStartingRootFollow(TreeAllocationSession treeSession) {
        if (treeSession.isStartingRootMoving() && startingRootFollow == null) {
            startingRootFollow = new StartingRootCameraFollow(-panX / currentZoom, panY / currentZoom,
                    treeSession.startingRootCameraTargetX(), treeSession.startingRootCameraTargetY(), currentZoom,
                    treeSession.isStartingRootFlyingOut() ? SmoothZoom.MIN_ZOOM : StartingRootCameraFollow.CHOOSING_ZOOM);
        }
    }

    void applyStartingRootFollow(TreeAllocationSession treeSession, boolean wasMoving) {
        if (!wasMoving) {
            return;
        }
        float followProgress = treeSession.startingRootCameraProgress();
        currentZoom = startingRootFollow.zoom(followProgress);
        smoothZoom.jumpTo(currentZoom);
        centreOn(startingRootFollow.x(treeSession.startingRootCameraTargetX(), followProgress),
                startingRootFollow.y(treeSession.startingRootCameraTargetY(), followProgress));
        if (!treeSession.isStartingRootMoving()) {
            startingRootFollow = null;
        }
    }

    void centreOn(float treeX, float treeY) {
        panX = -treeX * currentZoom;
        panY = treeY * currentZoom;
    }

    void jumpTo(HyperspaceCamera hyperspaceCamera) {
        currentZoom = hyperspaceCamera.zoom();
        smoothZoom.jumpTo(currentZoom);
        centreOn(hyperspaceCamera.x(), hyperspaceCamera.y());
    }

    HyperspaceCamera current() {
        return new HyperspaceCamera(-panX / currentZoom, panY / currentZoom, currentZoom);
    }

    void panTo(float treeX, float treeY) {
        panAnimation = new CameraPanAnimation(-panX / currentZoom, panY / currentZoom, treeX, treeY);
    }

    boolean isPanning() {
        return panAnimation != null;
    }

    void startDrag() {
        dragging = true;
        panAnimation = null;
    }

    void stopDrag() {
        dragging = false;
    }

    void stopMoving() {
        dragging = false;
        panAnimation = null;
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
        return currentZoom;
    }

    float panX() {
        return panX;
    }

    float panY() {
        return panY;
    }

    TreeViewport viewport(PositionAPI canvasPosition) {
        return new TreeViewport(canvasPosition.getX() + canvasPosition.getWidth() / 2f + panX,
                canvasPosition.getY() + canvasPosition.getHeight() / 2f + panY, currentZoom,
                canvasPosition.getX(), canvasPosition.getY(), canvasPosition.getX() + canvasPosition.getWidth(), canvasPosition.getY() + canvasPosition.getHeight());
    }
}
