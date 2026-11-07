package exiledsector.ui.util;

import com.fs.starfarer.api.graphics.SpriteAPI;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class HoloBeamsTest {

    @Test
    void ringArtIsHiddenWhileItsLineWouldBeSubPixel() {
        assertEquals(0f, HoloBeams.ringArtShare(HoloBeams.RING_LINE_FADE_FROM), 1e-6f);
        assertEquals(0f, HoloBeams.ringArtShare(0.2f), 1e-6f);
    }

    @Test
    void ringArtFullyReplacesTheBeamCircleOnceItsLineIsVisible() {
        assertEquals(1f, HoloBeams.ringArtShare(HoloBeams.RING_LINE_FADE_TO), 1e-6f);
        assertEquals(1f, HoloBeams.ringArtShare(5f), 1e-6f);
    }

    @Test
    void ringArtFadesInBetween() {
        float midway = (HoloBeams.RING_LINE_FADE_FROM + HoloBeams.RING_LINE_FADE_TO) / 2f;

        assertEquals(0.5f, HoloBeams.ringArtShare(midway), 1e-6f);
    }

    @Test
    void theCorePenWhitensTheColourWhileTheHaloKeepsIt() {
        SpriteAPI texture = mock(SpriteAPI.class);
        HoloBeams.Pen core = new HoloBeams.Pen(false);
        HoloBeams.Pen halo = new HoloBeams.Pen(true);
        Color orange = new Color(255, 120, 30, 200);

        core.load(orange, 2f, 0.7f, texture);
        halo.load(orange, 2f, 0.7f, texture);

        assertEquals(255, core.red());
        assertEquals(181, core.green());
        assertEquals(131, core.blue());
        assertEquals((int) (225 * 0.7f), core.glAlpha());
        assertEquals(120, halo.green());
        assertEquals((int) (200 * (0.7f * 1f * HoloBeams.HALO_ALPHA)), halo.glAlpha());
    }

    @Test
    void thinStrokesKeepTheMinimumBandAndFadeInstead() {
        SpriteAPI texture = mock(SpriteAPI.class);
        HoloBeams.Pen core = new HoloBeams.Pen(false);
        HoloBeams.Pen halo = new HoloBeams.Pen(true);

        core.load(Color.WHITE, 0.75f, 1f, texture);
        halo.load(Color.WHITE, 2f, 1f, texture);

        assertEquals(HoloBeams.MIN_CORE_WIDTH / HoloBeams.BEAM_BAND_SHARE / 2f, core.halfWidth(), 1e-6f);
        assertEquals(127, core.glAlpha());
        assertEquals(2f * HoloBeams.HALO_WIDTH_SCALE / HoloBeams.BEAM_BAND_SHARE / 2f, halo.halfWidth(), 1e-6f);
    }
}
