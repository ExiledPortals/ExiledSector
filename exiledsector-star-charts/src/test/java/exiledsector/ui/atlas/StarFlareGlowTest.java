package exiledsector.ui.atlas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarFlareGlowTest {

    @Test
    void aPerfectArchFadesInAndOutAndIsBrightestHalfwayThrough() {
        assertEquals(0f, StarFlareGlow.archAlpha(0f));
        assertEquals(0f, StarFlareGlow.archAlpha(1f));
        assertEquals(1f, StarFlareGlow.archAlpha(0.5f), 0.0001f);
        assertEquals(StarFlareGlow.archAlpha(0.2f), StarFlareGlow.archAlpha(0.8f), 0.0001f);
        assertTrue(StarFlareGlow.archAlpha(0.3f) < StarFlareGlow.archAlpha(0.5f));
    }

    @Test
    void aPerfectArchRisesSteadilyToItsFullHeight() {
        float previous = StarFlareGlow.archHeight(0f);
        assertEquals(0f, previous);
        for (float progress = 0.05f; progress <= 1f; progress += 0.05f) {
            float height = StarFlareGlow.archHeight(progress);
            assertTrue(height > previous, "at " + progress);
            previous = height;
        }

        assertEquals(StarFlareGlow.ARCH_HEIGHT, StarFlareGlow.archHeight(1f), 0.0001f);
    }

    @Test
    void aPerfectArchIsTheSameOnBothSidesAndStandsOnTheSurfaceAtItsFeet() {
        assertEquals(0f, StarFlareGlow.archProfile(0f), 0.0001f);
        assertEquals(0f, StarFlareGlow.archProfile(1f), 0.0001f);
        assertEquals(1f, StarFlareGlow.archProfile(0.5f), 0.0001f);
        for (int glow = 0; glow < StarFlareGlow.ARCH_GLOWS; glow++) {
            float along = glow / (float) (StarFlareGlow.ARCH_GLOWS - 1);
            assertEquals(StarFlareGlow.archProfile(along), StarFlareGlow.archProfile(1f - along), 0.0001f);
        }
    }
}
