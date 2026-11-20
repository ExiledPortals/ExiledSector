package exiledsector.ui.atlas;

import exiledsector.ui.atlas.AtlasBodyPainter.Lighting;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarShadeTest {

    private static final Color YELLOW = new Color(255, 230, 120);

    @Test
    void anUnallocatedStarDimsInItsOwnColourInsteadOfTurningGrey() {
        StarShade shade = new StarShade();
        Color available = shade.of(YELLOW, Lighting.AVAILABLE);
        Color dull = shade.of(YELLOW, Lighting.DULL);

        assertSame(YELLOW, shade.of(YELLOW, Lighting.LIT));
        assertTrue(dull.getRed() < available.getRed() && available.getRed() < YELLOW.getRed());
        assertEquals(YELLOW.getRed() / (float) YELLOW.getBlue(), dull.getRed() / (float) dull.getBlue(), 0.05f);
        assertSame(dull, shade.of(YELLOW, Lighting.DULL));
    }
}
