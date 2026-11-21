package exiledsector.ui.atlas;

import exiledsector.ui.belt.PlaneProjection;

final class OrbitalPlaneCamera implements PlaneProjection {

    static final float MAX_TILT_DEGREES = 60f;
    static final float CAMERA_DISTANCE_REACHES = 3f;
    private static final float MIN_PERSPECTIVE_DIVISOR = 0.1f;
    private static final float HEADING_STEP = 1f;

    private float centreX;
    private float centreY;
    private float liftY;
    private float cosTilt = 1f;
    private float fitScale = 1f;
    private float depthPerPlaneUnit;

    void aim(float centreX, float centreY, float reach, float tiltLevel) {
        double tilt = Math.toRadians(MAX_TILT_DEGREES * Math.max(0f, Math.min(1f, tiltLevel)));
        float sinTilt = (float) Math.sin(tilt);
        float cameraDistance = CAMERA_DISTANCE_REACHES;
        float foreshortenedDistance = cameraDistance * cameraDistance - sinTilt * sinTilt;
        this.centreX = centreX;
        this.centreY = centreY;
        cosTilt = (float) Math.cos(tilt);
        fitScale = (float) Math.sqrt(foreshortenedDistance) / cameraDistance;
        depthPerPlaneUnit = reach > 0f ? sinTilt / (cameraDistance * reach) : 0f;
        liftY = Math.max(0f, reach) * fitScale * cosTilt * cameraDistance * sinTilt / foreshortenedDistance;
    }

    float flattening() {
        return cosTilt;
    }

    boolean tilted() {
        return depthPerPlaneUnit > 0f;
    }

    float perspective(float planeY) {
        return 1f / Math.max(MIN_PERSPECTIVE_DIVISOR, 1f + planeY * depthPerPlaneUnit);
    }

    float sizeAt(float planeY) {
        return fitScale * perspective(planeY);
    }

    @Override
    public float screenX(float planeX, float planeY) {
        return centreX + planeX * sizeAt(planeY);
    }

    @Override
    public float screenY(float planeX, float planeY) {
        return screenY(planeY);
    }

    float screenY(float planeY) {
        return centreY + liftY + planeY * cosTilt * sizeAt(planeY);
    }

    float screenHeading(float planeX, float planeY, float planeHeadingDegrees) {
        if (!tilted()) {
            return planeHeadingDegrees;
        }
        double heading = Math.toRadians(planeHeadingDegrees);
        float stepX = (float) Math.cos(heading) * HEADING_STEP;
        float stepY = (float) Math.sin(heading) * HEADING_STEP;
        float acrossX = screenX(planeX + stepX, planeY + stepY) - screenX(planeX - stepX, planeY - stepY);
        float acrossY = screenY(planeY + stepY) - screenY(planeY - stepY);
        return (float) Math.toDegrees(Math.atan2(acrossY, acrossX));
    }
}
