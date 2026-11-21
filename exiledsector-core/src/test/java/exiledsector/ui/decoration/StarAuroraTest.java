package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StarAuroraTest {

    @Test
    void aWholeAuroraShowsEveryAngle() {
        assertEquals(1f, StarAurora.sideAlpha(StarAurora.Side.WHOLE, 90f), 1e-4f);
        assertEquals(1f, StarAurora.sideAlpha(StarAurora.Side.WHOLE, 270f), 1e-4f);
    }

    @Test
    void theBackHalfShowsOnlyTheAnglesAboveTheStar() {
        assertEquals(1f, StarAurora.sideAlpha(StarAurora.Side.BACK, 90f), 1e-4f);
        assertEquals(0f, StarAurora.sideAlpha(StarAurora.Side.BACK, 270f), 1e-4f);
        assertEquals(0.5f, StarAurora.sideAlpha(StarAurora.Side.BACK, 0f), 1e-4f);
    }

    @Test
    void theTwoHalvesAddUpToTheWholeAurora() {
        for (float angle = 0f; angle < 360f; angle += 15f) {
            assertEquals(1f, StarAurora.sideAlpha(StarAurora.Side.BACK, angle) + StarAurora.sideAlpha(StarAurora.Side.FRONT, angle), 1e-4f);
        }
    }
}
