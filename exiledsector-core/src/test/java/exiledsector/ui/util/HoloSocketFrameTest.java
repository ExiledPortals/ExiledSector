package exiledsector.ui.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HoloSocketFrameTest {

    @Test
    void tickLengthsAreFixedOrAShareOfTheRadius() {
        HoloSocketFrame.Ticks fixed = new HoloSocketFrame.Ticks(0.8f, 90, 9, 9f, 4f, false, -6f);
        HoloSocketFrame.Ticks scaled = new HoloSocketFrame.Ticks(0.84f, 60, 5, 0.12f, 0.05f, true, -8f);

        assertEquals(9f, fixed.majorLengthAt(100f), 1e-6f);
        assertEquals(4f, fixed.minorLengthAt(100f), 1e-6f);
        assertEquals(12f, scaled.majorLengthAt(100f), 1e-4f);
        assertEquals(5f, scaled.minorLengthAt(100f), 1e-4f);
    }

    @Test
    void theSocketSquareTakesItsShareOfTheRadius() {
        assertEquals(42f, HoloSocketFrame.socketHalf(100f), 1e-4f);
    }
}
