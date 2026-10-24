package exiledsector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraPanAnimationTest {

    @Test
    void startsAtTheStartAndEndsExactlyOnTheTargetAfterOneSecond() {
        CameraPanAnimation pan = new CameraPanAnimation(0f, 100f, 200f, -100f);
        assertEquals(0f, pan.x(), 1e-4f);
        assertEquals(100f, pan.y(), 1e-4f);

        pan.advance(0.5f);
        assertFalse(pan.isFinished());
        assertEquals(100f, pan.x(), 1e-4f);
        assertEquals(0f, pan.y(), 1e-4f);

        pan.advance(0.6f);
        assertTrue(pan.isFinished());
        assertEquals(200f, pan.x(), 1e-4f);
        assertEquals(-100f, pan.y(), 1e-4f);
    }

    @Test
    void easesInSoTheFirstQuarterCoversLessThanAQuarterOfTheDistance() {
        CameraPanAnimation pan = new CameraPanAnimation(0f, 0f, 100f, 0f);

        pan.advance(0.25f);

        assertTrue(pan.x() < 25f);
    }
}
