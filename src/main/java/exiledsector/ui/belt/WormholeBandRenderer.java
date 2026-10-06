package exiledsector.ui.belt;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.ui.util.UnitCircle;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class WormholeBandRenderer {
    private static final float PIXELS_PER_SEGMENT = 5f;
    private static final float BAND_WIDTH_IN_TEXTURE = 64f;
    private static final float MAX_TILE_COUNT = 60f;
    private static final float WOBBLE_RATIO = 0.06f;
    private static final float MAX_SAFE_WOBBLE_FRACTION = 0.3f;
    private static final float PHASE_DEG_PER_SEC = 30f;
    private static final int WOBBLE_FREQUENCY = 6;
    private static final float MIN_SAMPLES_PER_WOBBLE_CYCLE = 16f;
    private static final float MIN_SEGMENTS_FOR_WOBBLE = WOBBLE_FREQUENCY * MIN_SAMPLES_PER_WOBBLE_CYCLE;

    private WormholeBandRenderer() {
    }

    public static void render(SpriteAPI texture, RadialBand band, int bandSlot, float rotationDeg,
                               Color color, float alphaMult, float elapsedSeconds) {
        float innerRadius = band.innerRadius();
        float outerRadius = band.outerRadius();
        float circumference = (float) (2 * Math.PI * (innerRadius + outerRadius) / 2f);
        int segments = RadialBandGL.computeSegments(circumference, PIXELS_PER_SEGMENT, MIN_SEGMENTS_FOR_WOBBLE);
        float thickness = outerRadius - innerRadius;

        float texWidth = texture.getTextureWidth();
        float imageWidth = texture.getWidth();
        float imageHeight = texture.getHeight();
        float aspectRatio = imageHeight / BAND_WIDTH_IN_TEXTURE;
        float tileCount = Math.min(MAX_TILE_COUNT, Math.max(1f, circumference / (thickness * aspectRatio)));
        float texPerSegment = tileCount / segments;
        float wobble = Math.min(thickness * WOBBLE_RATIO, outerRadius * MAX_SAFE_WOBBLE_FRACTION / WOBBLE_FREQUENCY);

        float leftTX = bandSlot * texWidth * BAND_WIDTH_IN_TEXTURE / imageWidth;
        float rightTX = (bandSlot + 1f) * texWidth * BAND_WIDTH_IN_TEXTURE / imageWidth - 0.001f;
        float phaseRad = (float) Math.toRadians((elapsedSeconds * PHASE_DEG_PER_SEC) % 360f);

        RadialBandGL.begin(texture, band.center().x, band.center().y, GL11.GL_SRC_ALPHA, GL11.GL_ONE, color, alphaMult);
        GL11.glRotatef(rotationDeg, 0f, 0f, 1f);
        RadialBandGL.strip(UnitCircle.of(segments), 0, segments, new RadialBandGL.StripSpec(innerRadius, outerRadius, leftTX, rightTX,
                texPerSegment, new RingWave(WOBBLE_FREQUENCY, phaseRad, wobble), RingWave.NONE));
        RadialBandGL.end();
    }
}
