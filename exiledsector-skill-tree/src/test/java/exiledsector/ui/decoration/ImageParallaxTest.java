package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageParallaxTest {

    @Test
    void anImageOnTheTreePlaneNeverDrifts() {
        assertEquals(0f, ImageParallax.driftFraction(500f, 1f, 400f));
    }

    @Test
    void anImageAtTheViewCentreHasNothingToDriftToward() {
        assertEquals(0f, ImageParallax.driftFraction(0f, 0.8f, 400f));
    }

    @Test
    void closeToTheViewCentreADeepImageLagsByItsParallax() {
        assertEquals(0.2f, ImageParallax.driftFraction(10f, 0.8f, 400f), 1e-3f);
    }

    @Test
    void farFromTheViewCentreTheDriftLevelsOffAtTheReach() {
        float distance = 100_000f;

        assertEquals(400f, ImageParallax.driftFraction(distance, 0.8f, 400f) * distance, 1f);
    }
}
