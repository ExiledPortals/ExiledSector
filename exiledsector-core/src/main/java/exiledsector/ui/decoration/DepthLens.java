package exiledsector.ui.decoration;

import exiledsector.ui.TreeViewport;

public final class DepthLens {

    public interface Field {

        float depth(float worldX, float worldY);
    }

    public static final float MOST_LIFT = 0.6f;
    public static final float MOST_SINK = 0.9f;
    public static final int X = 0;
    public static final int Y = 1;
    public static final int SCALE = 2;
    private static final Field LEVEL = (worldX, worldY) -> 0f;

    private final TreeViewport view;
    private final Field field;
    private final float strength;
    private final float reachWorld;
    private final float sampleShare;
    private final float eyeX;
    private final float eyeY;
    private final boolean bounded;

    public DepthLens(TreeViewport view, Field field, float strength, float reachWorld) {
        this(view, field, strength, reachWorld, 1f);
    }

    private DepthLens(TreeViewport view, Field field, float strength, float reachWorld, float sampleShare) {
        this.view = view;
        this.field = field;
        this.strength = strength;
        this.reachWorld = reachWorld;
        this.sampleShare = sampleShare;
        this.eyeX = (view.left() + view.right()) / 2f;
        this.eyeY = (view.bottom() + view.top()) / 2f;
        this.bounded = Float.isFinite(eyeX) && Float.isFinite(eyeY);
    }

    public static DepthLens flat(TreeViewport view) {
        return new DepthLens(view, LEVEL, 0f, 0f);
    }

    public TreeViewport view() {
        return view;
    }

    public float zoom() {
        return view.zoom();
    }

    public float reachWorld() {
        return reachWorld;
    }

    public float depthAt(float worldX, float worldY) {
        return field.depth(worldX * sampleShare, worldY * sampleShare);
    }

    public float scale(float depth) {
        float lift = Math.max(-MOST_SINK, Math.min(MOST_LIFT, depth * strength));
        return 1f / (1f - lift);
    }

    public void place(float worldX, float worldY, float[] out) {
        place(worldX, worldY, depthAt(worldX, worldY), reachWorld, out);
    }

    public void place(float worldX, float worldY, float depth, float[] out) {
        place(worldX, worldY, depth, reachWorld, out);
    }

    public void place(float worldX, float worldY, float depth, float parallaxReachWorld, float[] out) {
        float flatX = view.screenX(worldX);
        float flatY = view.screenY(worldY);
        float scale = scale(depth);
        out[SCALE] = scale;
        out[X] = flatX;
        out[Y] = flatY;
        if (!bounded) {
            return;
        }
        float offsetX = flatX - eyeX;
        float offsetY = flatY - eyeY;
        float pull = ImageParallax.driftFraction((float) Math.hypot(offsetX, offsetY), scale, parallaxReachWorld * view.zoom());
        out[X] = flatX - offsetX * pull;
        out[Y] = flatY - offsetY * pull;
    }

    public DepthLens rescaled(float worldShare) {
        TreeViewport scaled = new TreeViewport(view.centerX(), view.centerY(), view.zoom() * worldShare, view.left(), view.bottom(), view.right(),
                view.top());
        return new DepthLens(scaled, field, strength, reachWorld / worldShare, sampleShare * worldShare);
    }
}
