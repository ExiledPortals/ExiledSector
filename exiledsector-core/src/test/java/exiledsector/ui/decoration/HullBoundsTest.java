package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HullBoundsTest {

    private static final float TOLERANCE = 0.001f;

    @Test
    void aSpriteWithoutACentreSpreadsEvenlyAroundItsMiddle() {
        HullBounds bounds = HullBounds.EMPTY.withSprite(100f, 200f, Float.NaN, Float.NaN, 0f, 0f, 0f);

        assertEquals(100f, bounds.halfLengthAround(0f), TOLERANCE);
        assertEquals(50f, bounds.halfWidthAround(0f), TOLERANCE);
    }

    @Test
    void anOffCentrePivotStillMeasuresTheSpriteFromItsImageCentre() {
        HullBounds bounds = HullBounds.EMPTY.withSprite(100f, 200f, 30f, 60f, 0f, 0f, 0f);

        assertEquals(140f, bounds.maxForward(), TOLERANCE);
        assertEquals(-60f, bounds.minForward(), TOLERANCE);
        assertEquals(30f, bounds.maxLeft(), TOLERANCE);
        assertEquals(-70f, bounds.minLeft(), TOLERANCE);
        assertEquals(100f, bounds.halfLengthAround(200f / 2f - 60f), TOLERANCE);
        assertEquals(50f, bounds.halfWidthAround(30f - 100f / 2f), TOLERANCE);
    }

    @Test
    void aTurnedModuleOnTheSideWidensTheHull() {
        HullModule sidePod = HullModule.attached("graphics/ships/pod.png", 40f, 120f, Float.NaN, Float.NaN, 0f, 80f, 90f, 0f, 0f,
                2f, false, HullEngines.NONE);
        HullBounds core = HullBounds.EMPTY.withSprite(100f, 200f, Float.NaN, Float.NaN, 0f, 0f, 0f);

        HullBounds withPod = HullModules.of(List.of(sidePod)).withModules(core);

        assertEquals(140f, withPod.maxLeft(), TOLERANCE);
        assertEquals(-50f, withPod.minLeft(), TOLERANCE);
        assertEquals(100f, withPod.maxForward(), TOLERANCE);
    }
}
