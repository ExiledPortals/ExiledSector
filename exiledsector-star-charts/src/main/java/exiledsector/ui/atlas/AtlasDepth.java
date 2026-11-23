package exiledsector.ui.atlas;

import exiledsector.Seeds;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.decoration.DepthLens;

final class AtlasDepth implements DepthLens.Field {

    static final float WAVELENGTH_CELLS = 4f;
    static final float DETAIL_FREQUENCY = 2.3f;
    static final float DETAIL_SHARE = 0.35f;
    static final float SPREAD = 1.6f;
    static final float STRENGTH_ZOOMED_OUT = 0.2f;
    static final float STRENGTH_ZOOMED_IN = 0.4f;
    static final float PARALLAX_REACH_CELLS = 0.3f;
    static final float FAR_FADE = 0.3f;
    static final float NEAREST_SYSTEM_SIZE = 2f;
    private static final long BROAD_SALT = 0x4445505448L;
    private static final long DETAIL_SALT = 0x44455441494cL;
    private static final float DETAIL_SHIFT = 17.3f;

    private final long seed;

    AtlasDepth(long seed) {
        this.seed = seed;
    }

    @Override
    public float depth(float worldX, float worldY) {
        float u = worldX / (WAVELENGTH_CELLS * AtlasMapCanvas.CELL_PIXELS);
        float v = worldY / (WAVELENGTH_CELLS * AtlasMapCanvas.CELL_PIXELS);
        float broad = valueNoise(seed ^ BROAD_SALT, u, v);
        float detail = valueNoise(seed ^ DETAIL_SALT, u * DETAIL_FREQUENCY + DETAIL_SHIFT, v * DETAIL_FREQUENCY - DETAIL_SHIFT);
        float blended = (broad + detail * DETAIL_SHARE) / (1f + DETAIL_SHARE) * SPREAD;
        return Math.max(-1f, Math.min(1f, blended));
    }

    DepthLens lens(TreeViewport view) {
        return new DepthLens(view, this, strength(view.zoom()), parallaxReach());
    }

    static float parallaxReach() {
        return PARALLAX_REACH_CELLS * AtlasMapCanvas.CELL_PIXELS;
    }

    static float strength(float zoom) {
        double span = Math.log(AtlasMapCanvas.MAX_ZOOM / AtlasMapCanvas.MIN_ZOOM);
        float level = (float) (Math.log(Math.max(zoom, 1e-6f) / AtlasMapCanvas.MIN_ZOOM) / span);
        level = Math.max(0f, Math.min(1f, level));
        return STRENGTH_ZOOMED_OUT + (STRENGTH_ZOOMED_IN - STRENGTH_ZOOMED_OUT) * level;
    }

    static float systemSize(float depth) {
        return (float) Math.pow(NEAREST_SYSTEM_SIZE, Math.max(-1f, Math.min(1f, depth)));
    }

    static float fade(float depth) {
        return 1f - FAR_FADE * Math.max(0f, Math.min(1f, -depth));
    }

    static float valueNoise(long salt, float u, float v) {
        int cellU = (int) Math.floor(u);
        int cellV = (int) Math.floor(v);
        float fracU = smooth(u - cellU);
        float fracV = smooth(v - cellV);
        float bottom = lerp(lattice(salt, cellU, cellV), lattice(salt, cellU + 1, cellV), fracU);
        float top = lerp(lattice(salt, cellU, cellV + 1), lattice(salt, cellU + 1, cellV + 1), fracU);
        return lerp(bottom, top, fracV);
    }

    private static float lattice(long salt, int cellU, int cellV) {
        long mixed = Seeds.mix(salt + cellU * 0xc2b2ae3d27d4eb4fL);
        mixed = Seeds.mix(mixed + cellV * 0x165667b19e3779f9L);
        return (float) ((mixed >>> 11) * 0x1.0p-53) * 2f - 1f;
    }

    private static float smooth(float fraction) {
        return fraction * fraction * (3f - 2f * fraction);
    }

    private static float lerp(float from, float to, float share) {
        return from + (to - from) * share;
    }
}
