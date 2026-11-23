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
    private final float sampleShare;
    private final float cameraX;
    private final float cameraY;

    public DepthLens(TreeViewport view, Field field, float strength) {
        this(view, field, strength, 1f);
    }

    private DepthLens(TreeViewport view, Field field, float strength, float sampleShare) {
        this.view = view;
        this.field = field;
        this.strength = strength;
        this.sampleShare = sampleShare;
        float eyeX = (view.left() + view.right()) / 2f;
        float eyeY = (view.bottom() + view.top()) / 2f;
        boolean bounded = Float.isFinite(eyeX) && Float.isFinite(eyeY) && view.zoom() > 0f;
        cameraX = bounded ? (eyeX - view.centerX()) / view.zoom() : 0f;
        cameraY = bounded ? (view.centerY() - eyeY) / view.zoom() : 0f;
    }

    public static DepthLens flat(TreeViewport view) {
        return new DepthLens(view, LEVEL, 0f);
    }

    public TreeViewport view() {
        return view;
    }

    public float zoom() {
        return view.zoom();
    }

    public float depthAt(float worldX, float worldY) {
        return field.depth(worldX * sampleShare, worldY * sampleShare);
    }

    public float scale(float depth) {
        float lift = Math.max(-MOST_SINK, Math.min(MOST_LIFT, depth * strength));
        return 1f / (1f - lift);
    }

    public void place(float worldX, float worldY, float[] out) {
        place(worldX, worldY, depthAt(worldX, worldY), out);
    }

    public void place(float worldX, float worldY, float depth, float[] out) {
        out[X] = view.screenX(worldX);
        out[Y] = view.screenY(worldY);
        out[SCALE] = scale(depth);
    }

    public float cameraX() {
        return cameraX;
    }

    public float cameraY() {
        return cameraY;
    }

    public DepthLens rescaled(float worldShare) {
        TreeViewport scaled = new TreeViewport(view.centerX(), view.centerY(), view.zoom() * worldShare, view.left(), view.bottom(), view.right(),
                view.top());
        return new DepthLens(scaled, field, strength, sampleShare * worldShare);
    }
}
