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
    private float progress;
    private int direction;

    public boolean isActive() {
        return progress > 0f || direction != 0;
    }

    public boolean isOnMap() {
        return progress >= 1f && direction == 0;
    }

    public void enter(HyperspaceCamera from, HyperspaceCamera map) {
        treeCamera = from;
        mapCamera = map;
        pivoted = false;
        direction = 1;
    }

    public void leaveTo(HyperspaceCamera current, HyperspaceCamera destination) {
        mapCamera = current;
        treeCamera = destination;
        pivoted = false;
        direction = -1;
    }

    public void zoomInAbout(HyperspaceCamera current, float worldX, float worldY, float treeZoom) {
        mapCamera = current;
        treeCamera = current.zoomedAbout(worldX, worldY, treeZoom);
        pivoted = true;
        pivotX = worldX;
        pivotY = worldY;
        direction = -1;
    }

    public void advance(float amount) {
        if (direction == 0) {
            return;
        }
        progress = Math.max(0f, Math.min(1f, progress + direction * amount / DURATION_SECONDS));
        if (progress == 0f || progress == 1f) {
            direction = 0;
        }
    }

    public HyperspaceCamera camera() {
        HyperspaceCamera blended = HyperspaceCamera.between(treeCamera, mapCamera, eased(progress));
        return pivoted ? mapCamera.zoomedAbout(pivotX, pivotY, blended.zoom()) : blended;
    }

    public float mapAmount() {
        return eased(progress);
    }

    public float treeAlpha() {
        return 1f - smoothstep(0f, TREE_FADE_END, progress);
    }

    public boolean isLeaving() {
        return direction < 0;
    }

    public float chromeAlpha() {
        return 1f - smoothstep(CHROME_FADE_START, CHROME_FADE_END, progress);
    }

    public float labelAlpha() {
        return smoothstep(LABEL_FADE_START, 1f, progress);
    }

    private static float eased(float t) {
        return t * t * (3f - 2f * t);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        return eased(Math.max(0f, Math.min(1f, (value - edge0) / (edge1 - edge0))));
    }
}
