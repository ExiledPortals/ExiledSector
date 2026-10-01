package exiledsector.ui;

final class StartingRootCameraFollow {

    static final float CHOOSING_ZOOM = 1f;

    private final float gapX;
    private final float gapY;
    private final float startZoom;

    StartingRootCameraFollow(float cameraX, float cameraY, float targetX, float targetY, float startZoom) {
        this.gapX = cameraX - targetX;
        this.gapY = cameraY - targetY;
        this.startZoom = startZoom;
    }

    float x(float targetX, float progress) {
        return targetX + gapX * (1f - progress);
    }

    float y(float targetY, float progress) {
        return targetY + gapY * (1f - progress);
    }

    float zoom(float progress) {
        return startZoom + (CHOOSING_ZOOM - startZoom) * progress;
    }
}
