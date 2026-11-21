package exiledsector.ui.belt;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.ui.util.UnitCircle;
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
        render(texture, band, color, alphaMult, elapsedSeconds, detailScale, GL11.GL_ONE, BandArc.WHOLE, null);
    }

    public static void renderArcOnPlane(SpriteAPI texture, RadialBand planeBand, Color color, float alphaMult, float elapsedSeconds,
                                        float detailScale, BandArc arc, PlaneProjection projection) {
        render(texture, planeBand, color, alphaMult, elapsedSeconds, detailScale, GL11.GL_ONE, arc, projection);
    }

    public static void renderShaded(SpriteAPI texture, RadialBand band, Color color, float alphaMult, float elapsedSeconds,
                                    float detailScale) {
        render(texture, band, color, alphaMult, elapsedSeconds, detailScale, GL11.GL_ONE_MINUS_SRC_ALPHA, BandArc.WHOLE, null);
    }

    private static void render(SpriteAPI texture, RadialBand band, Color color, float alphaMult, float elapsedSeconds,
                               float detailScale, int blendDstFactor, BandArc arc, PlaneProjection projection) {
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

        BandPlacement placement = BandPlacement.of(projection, band.center().x, band.center().y);
        placement.begin(texture, GL11.GL_SRC_ALPHA, blendDstFactor, color, alphaMult);

        for (int iter = 0; iter < 2; iter++) {
            int[] vertexRange = arc.vertexRange(iter * 180f, segments);
            float bandIndex = iter == 0 ? 1f : 0f;
            float innerTexX = RadialBandMaths.slotInnerTexX(texture, bandIndex, BAND_WIDTH_IN_TEXTURE);
            float outerTexX = RadialBandMaths.slotOuterTexX(texture, bandIndex, BAND_WIDTH_IN_TEXTURE);
            RingWave outerWave = iter == 0
                    ? new RingWave(FIRST_PASS_WOBBLE_CYCLES, phaseRad, wobble)
                    : new RingWave(SECOND_PASS_WOBBLE_CYCLES, -phaseRad, wobble);

            RadialBandGL.strip(circle, vertexRange, new RadialBandGL.StripSpec(band.innerRadius(), band.outerRadius(), innerTexX,
                    outerTexX, texPerSegment, RingWave.NONE, outerWave), placement);
            placement.turnBy(180f);
        }

        placement.end();
    }
}
