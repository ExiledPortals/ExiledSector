package exiledsector.ui.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
