package exiledsector.ui.belt;

import com.fs.starfarer.api.graphics.SpriteAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RadialBandMathsTest {

    @Test
    void tileCountsAreWholeNumbersSoTheTextureMeetsItselfSeamlessly() {
        for (float circumference = 100f; circumference < 5000f; circumference += 37.3f) {
            float tileCount = RadialBandMaths.wholeTileCount(circumference, 40f, 0.7f, 1f, RadialBandMaths.MAX_TILE_COUNT);
            assertEquals(Math.round(tileCount), tileCount, 0f, "circumference " + circumference);
        }
    }

    @Test
    void tileCountsStayBetweenOneAndTheCap() {
        assertEquals(1f, RadialBandMaths.wholeTileCount(1f, 100f, 1f, 1f, RadialBandMaths.MAX_TILE_COUNT));
        assertEquals(RadialBandMaths.MAX_TILE_COUNT, RadialBandMaths.wholeTileCount(1e6f, 1f, 1f, 1f, RadialBandMaths.MAX_TILE_COUNT));
        assertEquals(3000f, RadialBandMaths.wholeTileCount(1000f, 1f, 1f, 3f, RadialBandMaths.UNCAPPED_TILE_COUNT));
    }

    @Test
    void circumferenceUsesTheMiddleRadius() {
        RadialBand band = new RadialBand(new Vector2f(), 90f, 110f);
        assertEquals((float) (2 * Math.PI * 100f), RadialBandMaths.circumference(band), 1e-3f);
        assertEquals(20f, RadialBandMaths.thickness(band));
    }

    @Test
    void wobbleIsClampedSoTheRingNeverFoldsOverItself() {
        RadialBand thinBand = new RadialBand(new Vector2f(), 100f, 110f);
        assertEquals(10f * 0.06f, RadialBandMaths.wobbleAmplitude(thinBand, 6f), 1e-5f);
        RadialBand thickBand = new RadialBand(new Vector2f(), 0f, 100f);
        assertEquals(100f * 0.3f / 10f, RadialBandMaths.wobbleAmplitude(thickBand, 10f), 1e-5f);
    }

    @Test
    void bandSlotsPickTheirColumnOfTheTextureWithoutBleedingIntoTheNext() {
        SpriteAPI texture = mock(SpriteAPI.class);
        when(texture.getTextureWidth()).thenReturn(1f);
        when(texture.getWidth()).thenReturn(256f);
        assertEquals(0.25f, RadialBandMaths.slotInnerTexX(texture, 1f, 64f));
        assertEquals(0.5f - 0.001f, RadialBandMaths.slotOuterTexX(texture, 1f, 64f));
    }
}
