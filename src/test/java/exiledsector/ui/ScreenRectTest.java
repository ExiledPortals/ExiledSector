package exiledsector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenRectTest {

    @Test
    void screenRectsIncludeTheirEdgesAndAnEmptyRectContainsNothing() {
        ScreenRect rect = new ScreenRect(10f, 20f, 100f, 50f);

        assertTrue(rect.contains(10f, 20f));
        assertTrue(rect.contains(110f, 70f));
        assertFalse(rect.contains(111f, 70f));
        assertFalse(rect.contains(50f, 19f));
        assertFalse(ScreenRect.NONE.contains(0f, 0f));
    }

    @Test
    void aCentredBoxSitsInTheMiddleOfItsHost() {
        ScreenRect box = ScreenRect.centeredIn(100f, 50f, 1000f, 600f, 400f, 200f);

        assertEquals(new ScreenRect(400f, 250f, 400f, 200f), box);
    }
}
