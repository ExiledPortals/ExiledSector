package exiledsector.ui.socket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WormholeButtonFxTest {

    @Test
    void theGlowOutlineFollowsTheButtonsCutCorners() {
        float[] outline = WormholeButtonFx.chamferedOutline(0f, 0f, 100f, 40f, 7f);

        assertArrayEquals(new float[]{7f, 0f, 93f, 0f, 100f, 7f, 100f, 33f, 93f, 40f, 7f, 40f, 0f, 33f, 0f, 7f}, outline);
    }

    @Test
    void theOpenWormholeButtonKeepsItsGentlePulse() {
        WormholeButtonFx.Intensity ready = WormholeButtonFx.Intensity.READY;

        assertEquals(0.75f, WormholeButtonFx.glowScale(ready, 0f), 1e-6f);
        assertEquals(1f, WormholeButtonFx.glowScale(ready, 1f), 1e-6f);
        assertEquals(0.92f, WormholeButtonFx.mouthScale(ready, 0f), 1e-6f);
        assertEquals(1f, WormholeButtonFx.mouthScale(ready, 1f), 1e-6f);
    }

    @Test
    void aGlowWithoutPulseDepthHoldsSteady() {
        WormholeButtonFx.Intensity steady = new WormholeButtonFx.Intensity(0.3f, 0f);

        assertEquals(1f, WormholeButtonFx.glowScale(steady, 0f), 1e-6f);
        assertEquals(1f, WormholeButtonFx.glowScale(steady, 1f), 1e-6f);
        assertEquals(1f, WormholeButtonFx.mouthScale(steady, 0f), 1e-6f);
    }

    @Test
    void aDeeperPulseSwingsTheGlowFurther() {
        WormholeButtonFx.Intensity deep = new WormholeButtonFx.Intensity(1f, 0.7f);

        assertEquals(0.3f, WormholeButtonFx.glowScale(deep, 0f), 1e-6f);
        assertEquals(1f, WormholeButtonFx.glowScale(deep, 1f), 1e-6f);
    }
}
