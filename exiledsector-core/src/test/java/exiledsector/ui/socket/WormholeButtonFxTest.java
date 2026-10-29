package exiledsector.ui.socket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class WormholeButtonFxTest {

    @Test
    void theGlowOutlineFollowsTheButtonsCutCorners() {
        float[] outline = WormholeButtonFx.chamferedOutline(0f, 0f, 100f, 40f, 7f);

        assertArrayEquals(new float[]{7f, 0f, 93f, 0f, 100f, 7f, 100f, 33f, 93f, 40f, 7f, 40f, 0f, 33f, 0f, 7f}, outline);
    }
}
