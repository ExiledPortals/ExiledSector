package exiledsector.ui.belt;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.ui.util.UnitCircle;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class AuroraBeltRenderer {
    private static final float PIXELS_PER_SEGMENT = 5f;
    private static final float BAND_WIDTH_IN_TEXTURE = 256f;
    private static final float TILE_DENSITY = 3f;
    private static final float WOBBLE_RATIO = 0.06f;
    private static final float MAX_SAFE_WOBBLE_FRACTION = 0.3f;
    private static final float PHASE_DEG_PER_SEC = 12f;
    private static final int FIRST_PASS_WOBBLE_CYCLES = 10;
    private static final int SECOND_PASS_WOBBLE_CYCLES = 5;
    private static final float MAX_WOBBLE_FREQUENCY = FIRST_PASS_WOBBLE_CYCLES;
    private static final float MIN_SAMPLES_PER_WOBBLE_CYCLE = 16f;
    private static final float MIN_SEGMENTS_FOR_WOBBLE = MAX_WOBBLE_FREQUENCY * MIN_SAMPLES_PER_WOBBLE_CYCLE;

    private AuroraBeltRenderer() {
    }

    public static void render(SpriteAPI texture, RadialBand band, Color color, float alphaMult, float elapsedSeconds) {
        float innerRadius = band.innerRadius();
        float outerRadius = band.outerRadius();
        float phaseRad = (float) Math.toRadians((elapsedSeconds * PHASE_DEG_PER_SEC) % 360f);

        float circumference = (float) (2 * Math.PI * (innerRadius + outerRadius) / 2f);
        int segments = RadialBandGL.computeSegments(circumference, PIXELS_PER_SEGMENT, MIN_SEGMENTS_FOR_WOBBLE);
        UnitCircle circle = UnitCircle.of(segments);
        float thickness = outerRadius - innerRadius;

        float texWidth = texture.getTextureWidth();
        float imageWidth = texture.getWidth();
        float imageHeight = texture.getHeight();
        float aspectRatio = imageHeight / BAND_WIDTH_IN_TEXTURE;
        float tileCount = Math.max(1f, TILE_DENSITY * circumference / (thickness * aspectRatio));
        float texPerSegment = tileCount / segments;
        float wobble = Math.min(thickness * WOBBLE_RATIO, outerRadius * MAX_SAFE_WOBBLE_FRACTION / MAX_WOBBLE_FREQUENCY);

        RadialBandGL.begin(texture, band.center().x, band.center().y, GL11.GL_SRC_ALPHA, GL11.GL_ONE, color, alphaMult);

        for (int iter = 0; iter < 2; iter++) {
            float bandIndex = iter == 0 ? 1f : 0f;
            float leftTX = bandIndex * texWidth * BAND_WIDTH_IN_TEXTURE / imageWidth;
            float rightTX = (bandIndex + 1f) * texWidth * BAND_WIDTH_IN_TEXTURE / imageWidth - 0.001f;
            RingWave outerWave = iter == 0
                    ? new RingWave(FIRST_PASS_WOBBLE_CYCLES, phaseRad, wobble)
                    : new RingWave(SECOND_PASS_WOBBLE_CYCLES, -phaseRad, wobble);

            RadialBandGL.strip(circle, 0, segments, innerRadius, outerRadius, leftTX, rightTX, texPerSegment, RingWave.NONE, outerWave);
            GL11.glRotatef(180f, 0f, 0f, 1f);
        }

        RadialBandGL.end();
    }
}
