package exiledsector.ui.belt;

import com.fs.starfarer.api.graphics.SpriteAPI;

final class RadialBandMaths {
    static final float PIXELS_PER_SEGMENT = 5f;
    static final float MAX_TILE_COUNT = 60f;
    static final float UNCAPPED_TILE_COUNT = Float.MAX_VALUE;
    private static final float MIN_SAMPLES_PER_WOBBLE_CYCLE = 16f;
    private static final float WOBBLE_RATIO = 0.06f;
    private static final float MAX_SAFE_WOBBLE_FRACTION = 0.3f;
    private static final float SLOT_EDGE_INSET = 0.001f;

    private RadialBandMaths() {
    }

    static float circumference(RadialBand band) {
        return (float) (2 * Math.PI * (band.innerRadius() + band.outerRadius()) / 2f);
    }

    static float thickness(RadialBand band) {
        return band.outerRadius() - band.innerRadius();
    }

    static float minSegmentsForWobble(float maxWobbleCycles) {
        return maxWobbleCycles * MIN_SAMPLES_PER_WOBBLE_CYCLE;
    }

    static float wholeTileCount(float circumference, float thickness, float aspectRatio, float tileDensity, float maxTileCount) {
        return Math.min(maxTileCount, Math.max(1f, Math.round(tileDensity * circumference / (thickness * aspectRatio))));
    }

    static float wobbleAmplitude(RadialBand band, float maxWobbleCycles) {
        return Math.min(thickness(band) * WOBBLE_RATIO, band.outerRadius() * MAX_SAFE_WOBBLE_FRACTION / maxWobbleCycles);
    }

    static float phaseRad(float elapsedSeconds, float degreesPerSecond) {
        return (float) Math.toRadians((elapsedSeconds * degreesPerSecond) % 360f);
    }

    static float slotInnerTexX(SpriteAPI texture, float slot, float slotWidthInImage) {
        return slot * texture.getTextureWidth() * slotWidthInImage / texture.getWidth();
    }

    static float slotOuterTexX(SpriteAPI texture, float slot, float slotWidthInImage) {
        return (slot + 1f) * texture.getTextureWidth() * slotWidthInImage / texture.getWidth() - SLOT_EDGE_INSET;
    }
}
