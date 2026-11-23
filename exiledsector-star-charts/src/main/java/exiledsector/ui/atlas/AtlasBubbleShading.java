package exiledsector.ui.atlas;

final class AtlasBubbleShading {

    static final float BUBBLE_ALPHA = 0.4f;
    static final float FILM_SHARE = 0.1f;
    static final float RIM_POWER = 2.5f;
    static final float RIM_WHITENESS = 0.3f;
    static final float AMBIENT_SHARE = 0.45f;
    static final float SHEEN_ALPHA = 0.12f;
    static final float SHEEN_POWER = 24f;
    static final float SPOT_ALPHA = 0.5f;
    static final float GLOSS_WHITENESS = 0.85f;
    static final float[] LIGHT = unit(-0.5f, 0.6f, 0.62f);
    static final float[] GLOSS_DIRECTION = unit(LIGHT[0], LIGHT[1], LIGHT[2] + 1f);
    static final int ALPHA = 0;
    static final int WHITENESS = 1;

    private AtlasBubbleShading() {
    }

    static void shade(float x, float y, float edge, float spot, float[] out) {
        float facing = (float) Math.sqrt(Math.max(0f, 1f - x * x - y * y));
        float rim = Math.max(edge, (float) Math.pow(1f - facing, RIM_POWER));
        float lambert = Math.max(0f, x * LIGHT[0] + y * LIGHT[1] + facing * LIGHT[2]);
        float sheen = (float) Math.pow(Math.max(0f, x * GLOSS_DIRECTION[0] + y * GLOSS_DIRECTION[1] + facing * GLOSS_DIRECTION[2]), SHEEN_POWER);
        float body = BUBBLE_ALPHA * (FILM_SHARE + (1f - FILM_SHARE) * rim) * (AMBIENT_SHARE + (1f - AMBIENT_SHARE) * lambert);
        float shine = SHEEN_ALPHA * sheen + SPOT_ALPHA * spot;
        out[ALPHA] = Math.min(1f, body + shine);
        out[WHITENESS] = out[ALPHA] > 0f ? Math.min(1f, (GLOSS_WHITENESS * shine + RIM_WHITENESS * rim * body) / (body + shine)) : 0f;
    }

    private static float[] unit(float x, float y, float z) {
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        return new float[] {x / length, y / length, z / length};
    }
}
