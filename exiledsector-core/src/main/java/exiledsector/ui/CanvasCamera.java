package exiledsector.ui;

import com.fs.starfarer.api.ui.PositionAPI;
import exiledlib.skills.skilleffect.render.Fade;

public class CanvasCamera {

    public static final float FOLLOW_SETTLE_SECONDS = 1.2f;

    private final SmoothZoom smoothZoom = new SmoothZoom(1f);
    private float panX;
    private float panY;
    private float currentZoom = 1f;
    private float zoomPivotX;
    private float zoomPivotY;
    private boolean dragging;
    private CameraPanAnimation panAnimation;
    private boolean followingTarget;
    private float followStartX;
    private float followStartY;
    private float followSeconds;
    private float followNudgeX;
    private float followNudgeY;

    public void startFollowing(float zoomTarget) {
        followingTarget = true;
        followStartX = -panX / currentZoom;
        followStartY = panY / currentZoom;
        followSeconds = 0f;
        dragging = false;
        panAnimation = null;
        zoomPivotX = 0f;
        zoomPivotY = 0f;
        followNudgeX = 0f;
        followNudgeY = 0f;
        smoothZoom.aimAt(zoomTarget);
    }

    public void stopFollowing() {
        followingTarget = false;
        followNudgeX = 0f;
        followNudgeY = 0f;
    }

    public boolean isFollowing() {
        return followingTarget;
    }

    public void follow(float treeX, float treeY, float amount) {
        if (!followingTarget) {
            return;
        }
        followSeconds += amount;
        float progress = Math.min(1f, followSeconds / FOLLOW_SETTLE_SECONDS);
        float eased = Fade.smooth(progress);
        centreOn(followStartX + (treeX - followStartX) * eased + followNudgeX, followStartY + (treeY - followStartY) * eased + followNudgeY);
    }

    public void advanceZoom(float amount) {
        smoothZoom.advance(amount);
        float zoomRatio = smoothZoom.current() / currentZoom;
        currentZoom = smoothZoom.current();
        panX = SmoothZoom.panAbout(panX, zoomPivotX, zoomRatio);
        panY = SmoothZoom.panAbout(panY, zoomPivotY, zoomRatio);
    }

    public void advancePan(float amount) {
        if (panAnimation == null) {
            return;
        }
        panAnimation.advance(amount);
        centreOn(panAnimation.x(), panAnimation.y());
        if (panAnimation.isFinished()) {
            panAnimation = null;
        }
    }

    public void centreOn(float treeX, float treeY) {
        panX = -treeX * currentZoom;
        panY = treeY * currentZoom;
    }

    public void jumpZoomTo(float zoom) {
        currentZoom = zoom;
        smoothZoom.jumpTo(zoom);
    }

    public void panTo(float treeX, float treeY) {
        panAnimation = new CameraPanAnimation(-panX / currentZoom, panY / currentZoom, treeX, treeY);
    }

    public boolean isPanning() {
        return panAnimation != null;
    }

    public void startDrag() {
        dragging = true;
        panAnimation = null;
    }

    public void stopDrag() {
        dragging = false;
    }

    public void stopMoving() {
        dragging = false;
        panAnimation = null;
    }

    public boolean isDragging() {
        return dragging;
    }

    public boolean dragBy(float dx, float dy) {
        if (!dragging) {
            return false;
        }
        panX += dx;
        panY += dy;
        if (followingTarget && currentZoom > 0f) {
            followNudgeX -= dx / currentZoom;
            followNudgeY += dy / currentZoom;
        }
        return true;
    }

    public void scrollAt(boolean in, float screenX, float screenY, PositionAPI canvasPosition) {
        scroll(in, screenX - (canvasPosition.getX() + canvasPosition.getWidth() / 2f), screenY - (canvasPosition.getY() + canvasPosition.getHeight() / 2f));
    }

    public void scroll(boolean in, float pivotX, float pivotY) {
        zoomPivotX = pivotX;
        zoomPivotY = pivotY;
        smoothZoom.scroll(in);
    }

    public boolean isSettledAtMinimum() {
        return smoothZoom.isSettledAtMinimum();
    }

    public float zoom() {
        return currentZoom;
    }

    public float centreTreeX() {
        return -panX / currentZoom;
    }

    public float centreTreeY() {
        return panY / currentZoom;
    }

    public float panX() {
        return panX;
    }

    public float panY() {
        return panY;
    }

    public TreeViewport viewport(PositionAPI canvasPosition) {
        return new TreeViewport(canvasPosition.getX() + canvasPosition.getWidth() / 2f + panX,
                canvasPosition.getY() + canvasPosition.getHeight() / 2f + panY, currentZoom,
                canvasPosition.getX(), canvasPosition.getY(), canvasPosition.getX() + canvasPosition.getWidth(), canvasPosition.getY() + canvasPosition.getHeight());
    }
}
