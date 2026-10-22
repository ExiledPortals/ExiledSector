package exiledsector.ui.util;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorUtilTest {

    private static final Color FALLBACK = new Color(1, 2, 3);
    private static final Logger LOGGER = Logger.getLogger(ColorUtilTest.class);

    @Test
    void parsesA6DigitHexAsOpaque() {
        Color color = ColorUtil.parseHexColor("FF00AA", FALLBACK);

        assertEquals(new Color(0xFF, 0x00, 0xAA), color);
        assertEquals(255, color.getAlpha());
    }

    @Test
    void parsesAnExplicit8DigitArgbHex() {
        Color color = ColorUtil.parseHexColor("80FF00AA", FALLBACK);

        assertEquals(new Color(0xFF, 0x00, 0xAA, 0x80), color);
    }

    @Test
    void stripsLeadingHash() {
        Color color = ColorUtil.parseHexColor("#FF00AA", FALLBACK);

        assertEquals(new Color(0xFF, 0x00, 0xAA), color);
    }

    @Test
    void returnsFallbackForNullOrEmptyHex() {
        assertEquals(FALLBACK, ColorUtil.parseHexColor(null, FALLBACK));
        assertEquals(FALLBACK, ColorUtil.parseHexColor("", FALLBACK));
    }

    @Test
    void returnsFallbackOnInvalidHex() {
        assertEquals(FALLBACK, ColorUtil.parseHexColor("not-a-color", FALLBACK));
    }

    @Test
    void loggingOverloadReturnsFallbackAndDoesNotThrowOnInvalidHex() {
        Color color = ColorUtil.parseHexColor("not-a-color", FALLBACK, LOGGER, "testField");

        assertEquals(FALLBACK, color);
    }
}
