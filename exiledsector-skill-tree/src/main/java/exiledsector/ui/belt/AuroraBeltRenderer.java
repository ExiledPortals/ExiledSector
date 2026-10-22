package exiledsector.ui.belt;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledlib.ui.util.UnitCircle;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class AuroraBeltRenderer {
    private static final float BAND_WIDTH_IN_TEXTURE = 256f;
    private static final float TILE_DENSITY = 3f;
    private static final float PHASE_DEG_PER_SEC = 12f;
    private static final int FIRST_PASS_WOBBLE_CYCLES = 10;
    private static final int SECOND_PASS_WOBBLE_CYCLES = 5;
    private static final float MAX_WOBBLE_FREQUENCY = FIRST_PASS_WOBBLE_CYCLES;
    private static final float MIN_SEGMENTS_FOR_WOBBLE = RadialBandMaths.minSegmentsForWobble(MAX_WOBBLE_FREQUENCY);

    private AuroraBeltRenderer() {
    }

    public static void render(SpriteAPI texture, RadialBand band, Color color, float alphaMult, float elapsedSeconds,
                              float detailScale) {
        float phaseRad = RadialBandMaths.phaseRad(elapsedSeconds, PHASE_DEG_PER_SEC);

        float circumference = RadialBandMaths.circumference(band);
        int segments = RadialBandGL.computeSegments(circumference * detailScale, RadialBandMaths.PIXELS_PER_SEGMENT,
                MIN_SEGMENTS_FOR_WOBBLE);
        UnitCircle circle = UnitCircle.of(segments);

        float aspectRatio = texture.getHeight() / BAND_WIDTH_IN_TEXTURE;
        float tileCount = RadialBandMaths.wholeTileCount(circumference, RadialBandMaths.thickness(band), aspectRatio, TILE_DENSITY,
                RadialBandMaths.UNCAPPED_TILE_COUNT);
        float texPerSegment = tileCount / segments;
        float wobble = RadialBandMaths.wobbleAmplitude(band, MAX_WOBBLE_FREQUENCY);

        RadialBandGL.begin(texture, band.center().x, band.center().y, GL11.GL_SRC_ALPHA, GL11.GL_ONE, color, alphaMult);

        for (int iter = 0; iter < 2; iter++) {
            float bandIndex = iter == 0 ? 1f : 0f;
            float innerTexX = RadialBandMaths.slotInnerTexX(texture, bandIndex, BAND_WIDTH_IN_TEXTURE);
            float outerTexX = RadialBandMaths.slotOuterTexX(texture, bandIndex, BAND_WIDTH_IN_TEXTURE);
            RingWave outerWave = iter == 0
                    ? new RingWave(FIRST_PASS_WOBBLE_CYCLES, phaseRad, wobble)
                    : new RingWave(SECOND_PASS_WOBBLE_CYCLES, -phaseRad, wobble);

            RadialBandGL.strip(circle, 0, segments, new RadialBandGL.StripSpec(band.innerRadius(), band.outerRadius(), innerTexX,
                    outerTexX, texPerSegment, RingWave.NONE, outerWave));
            GL11.glRotatef(180f, 0f, 0f, 1f);
        }

        RadialBandGL.end();
    }
}
