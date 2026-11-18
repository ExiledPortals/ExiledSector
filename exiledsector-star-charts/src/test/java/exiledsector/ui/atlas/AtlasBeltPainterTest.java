package exiledsector.ui.atlas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtlasBeltPainterTest {

    @Test
    void anAsteroidUsesVanillasSpriteForItsSize() {
        assertEquals(0, AtlasBeltPainter.spriteFor(3f));
        assertEquals(1, AtlasBeltPainter.spriteFor(5f));
        assertEquals(2, AtlasBeltPainter.spriteFor(12f));
        assertEquals(3, AtlasBeltPainter.spriteFor(15f));
    }
}
