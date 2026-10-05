package exiledsector.ui;

final class StartingRootCameraFollow {

    static final float CHOOSING_ZOOM = 1f;

    private final float gapX;
    private final float gapY;
    private final float startZoom;
    private final float endZoom;

    StartingRootCameraFollow(float cameraX, float cameraY, float targetX, float targetY, float startZoom, float endZoom) {
        this.gapX = cameraX - targetX;
        this.gapY = cameraY - targetY;
        this.startZoom = startZoom;
        this.endZoom = endZoom;
    }

    float x(float targetX, float progress) {
        return targetX + gapX * (1f - progress);
    }

    float y(float targetY, float progress) {
        return targetY + gapY * (1f - progress);
    }

    float zoom(float progress) {
        return (float) (startZoom * Math.pow(endZoom / startZoom, progress));
    }
}
