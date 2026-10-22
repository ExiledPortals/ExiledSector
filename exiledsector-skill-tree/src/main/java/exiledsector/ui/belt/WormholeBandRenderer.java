package exiledsector.ui.belt;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.ui.util.UnitCircle;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class WormholeBandRenderer {
    private static final float BAND_WIDTH_IN_TEXTURE = 64f;
    private static final float TILE_DENSITY = 1f;
    private static final float PHASE_DEG_PER_SEC = 30f;
    private static final int WOBBLE_FREQUENCY = 6;
    private static final float MIN_SEGMENTS_FOR_WOBBLE = RadialBandMaths.minSegmentsForWobble(WOBBLE_FREQUENCY);

    private WormholeBandRenderer() {
    }

    public static void render(SpriteAPI texture, RadialBand band, int bandSlot, float rotationDeg,
                               Color color, float alphaMult, float elapsedSeconds) {
        float circumference = RadialBandMaths.circumference(band);
        int segments = RadialBandGL.computeSegments(circumference, RadialBandMaths.PIXELS_PER_SEGMENT, MIN_SEGMENTS_FOR_WOBBLE);

        float aspectRatio = texture.getHeight() / BAND_WIDTH_IN_TEXTURE;
        float tileCount = RadialBandMaths.wholeTileCount(circumference, RadialBandMaths.thickness(band), aspectRatio, TILE_DENSITY,
                RadialBandMaths.MAX_TILE_COUNT);
        float texPerSegment = tileCount / segments;
        float wobble = RadialBandMaths.wobbleAmplitude(band, WOBBLE_FREQUENCY);

        float innerTexX = RadialBandMaths.slotInnerTexX(texture, bandSlot, BAND_WIDTH_IN_TEXTURE);
        float outerTexX = RadialBandMaths.slotOuterTexX(texture, bandSlot, BAND_WIDTH_IN_TEXTURE);
        float phaseRad = RadialBandMaths.phaseRad(elapsedSeconds, PHASE_DEG_PER_SEC);

        RadialBandGL.begin(texture, band.center().x, band.center().y, GL11.GL_SRC_ALPHA, GL11.GL_ONE, color, alphaMult);
        GL11.glRotatef(rotationDeg, 0f, 0f, 1f);
        RadialBandGL.strip(UnitCircle.of(segments), 0, segments, new RadialBandGL.StripSpec(band.innerRadius(), band.outerRadius(),
                innerTexX, outerTexX, texPerSegment, new RingWave(WOBBLE_FREQUENCY, phaseRad, wobble), RingWave.NONE));
        RadialBandGL.end();
    }
}
