package exiledsector.ui.hyperspace;

public final class HyperspaceTransition {

    static final float DURATION_SECONDS = 0.9f;
    static final float TREE_FADE_END = 0.55f;
    static final float LABEL_FADE_START = 0.7f;
    static final float CHROME_FADE_START = 0.25f;
    static final float CHROME_FADE_END = 0.6f;

    private HyperspaceCamera treeCamera;
    private HyperspaceCamera mapCamera;
    private boolean pivoted;
    private float pivotX;
    private float pivotY;
    private float transitionProgress;
    private int travelDirection;

    public boolean isActive() {
        return transitionProgress > 0f || travelDirection != 0;
    }

    public boolean isOnMap() {
        return transitionProgress >= 1f && travelDirection == 0;
    }

    public void enter(HyperspaceCamera fromTreeCamera, HyperspaceCamera targetMapCamera) {
        treeCamera = fromTreeCamera;
        mapCamera = targetMapCamera;
        pivoted = false;
        travelDirection = 1;
    }

    public void leaveTo(HyperspaceCamera currentMapCamera, HyperspaceCamera destinationTreeCamera) {
        mapCamera = currentMapCamera;
        treeCamera = destinationTreeCamera;
        pivoted = false;
        travelDirection = -1;
    }

    public void zoomInAbout(HyperspaceCamera currentMapCamera, float worldX, float worldY, float treeZoom) {
        mapCamera = currentMapCamera;
        treeCamera = currentMapCamera.zoomedAbout(worldX, worldY, treeZoom);
        pivoted = true;
        pivotX = worldX;
        pivotY = worldY;
        travelDirection = -1;
    }

    public void advance(float amount) {
        if (travelDirection == 0) {
            return;
        }
        transitionProgress = Math.max(0f, Math.min(1f, transitionProgress + travelDirection * amount / DURATION_SECONDS));
        if (transitionProgress == 0f || transitionProgress == 1f) {
            travelDirection = 0;
        }
    }

    public HyperspaceCamera camera() {
        HyperspaceCamera blended = HyperspaceCamera.between(treeCamera, mapCamera, eased(transitionProgress));
        return pivoted ? mapCamera.zoomedAbout(pivotX, pivotY, blended.zoom()) : blended;
    }

    public float mapAmount() {
        return eased(transitionProgress);
    }

    public float treeAlpha() {
        return 1f - smoothstep(0f, TREE_FADE_END, transitionProgress);
    }

    public boolean isLeaving() {
        return travelDirection < 0;
    }

    public float chromeAlpha() {
        return 1f - smoothstep(CHROME_FADE_START, CHROME_FADE_END, transitionProgress);
    }

    public float labelAlpha() {
        return smoothstep(LABEL_FADE_START, 1f, transitionProgress);
    }

    private static float eased(float t) {
        return t * t * (3f - 2f * t);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        return eased(Math.max(0f, Math.min(1f, (value - edge0) / (edge1 - edge0))));
    }
}
