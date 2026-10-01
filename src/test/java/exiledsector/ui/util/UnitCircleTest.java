package exiledsector.ui.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class UnitCircleTest {

    @Test
    void eachPointIsAnEvenStepRoundTheCircle() {
        UnitCircle circle = UnitCircle.of(8);

        assertEquals(8, circle.segments());
        for (int i = 0; i < 8; i++) {
            double angle = 2 * Math.PI * i / 8;
            assertEquals(Math.cos(angle), circle.cos(i), 1e-6);
            assertEquals(Math.sin(angle), circle.sin(i), 1e-6);
        }
    }

    @Test
    void theSameSegmentCountSharesOneTable() {
        assertSame(UnitCircle.of(160), UnitCircle.of(160));
    }

    @Test
    void aSegmentCountBelowOneStillGivesACircle() {
        assertEquals(1, UnitCircle.of(0).segments());
    }
}
