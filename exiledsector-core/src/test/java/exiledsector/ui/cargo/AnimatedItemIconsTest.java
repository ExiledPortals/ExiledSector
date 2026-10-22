package exiledsector.ui.cargo;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimatedItemIconsTest {

    @Test
    void onlyTheBeaconSpriteIsAnimated() {
        assertTrue(AnimatedItemIcons.isAnimated(AnimatedItemIcons.BEACON_PATH));
        assertFalse(AnimatedItemIcons.isAnimated("graphics/icons/rat_exogrid.png"));
        assertFalse(AnimatedItemIcons.isAnimated(null));
    }

    @Test
    void theGlowRisesThenFallsOverOneCycle() {
        assertEquals(0f, AnimatedItemIcons.pulse(0f), 1e-6f);
        assertEquals(1f, AnimatedItemIcons.pulse(0.5f), 1e-6f);
        assertEquals(0.5f, AnimatedItemIcons.pulse(0.25f), 1e-6f);
        assertEquals(0.5f, AnimatedItemIcons.pulse(0.75f), 1e-6f);
    }

    @Test
    void aDimmedTintDimsTheGlow() {
        assertEquals(1f, AnimatedItemIcons.brightness(null), 1e-6f);
        assertEquals(1f, AnimatedItemIcons.brightness(Color.WHITE), 1e-6f);
        assertEquals(90f / 255f, AnimatedItemIcons.brightness(new Color(90, 90, 90)), 1e-6f);
    }
}
