package exiledsector.ui.atlas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerBoardGeometryTest {

    @Test
    void portraitsNeverGrowPastTheirLargestSize() {
        OfficerBoardGeometry geometry = OfficerBoardGeometry.fit(0f, 0f, 2000f, 2000f, 5, 2);

        assertEquals(OfficerBoardGeometry.MAX_PORTRAIT_SIZE, geometry.portraitSize(), 1e-4f);
    }

    @Test
    void theWidestRowFitsTheWidth() {
        OfficerBoardGeometry geometry = OfficerBoardGeometry.fit(100f, 0f, 1000f, 600f, 5, 20);

        assertEquals(50f, geometry.slotWidth(), 1e-4f);
        assertEquals(125f, geometry.centreX(new OfficerBoardLayout.Seat(0, 0, 20)), 1e-4f);
        assertEquals(1075f, geometry.centreX(new OfficerBoardLayout.Seat(0, 19, 20)), 1e-4f);
    }

    @Test
    void aRowIsCentredAndRowsRunTopDownInsideTheBlock() {
        OfficerBoardGeometry geometry = OfficerBoardGeometry.fit(0f, 0f, 1000f, 500f, 5, 4);

        assertEquals(500f, geometry.centreX(new OfficerBoardLayout.Seat(1, 0, 1)), 1e-4f);
        assertTrue(geometry.rowCentreY(0) > geometry.rowCentreY(4));
        assertTrue(geometry.rowCentreY(0) + geometry.portraitSize() / 2f <= 500f);
        assertTrue(geometry.rowCentreY(4) - geometry.portraitSize() / 2f >= 0f);
    }

    @Test
    void theBoardFitsTheSmallestScreen() {
        OfficerBoardGeometry geometry = OfficerBoardGeometry.fit(0f, 0f, 1040f, 504f, 5, 16);

        assertTrue(geometry.portraitSize() * 16 <= 1040f);
        assertTrue(geometry.rowPitch() * 5 <= 504f);
    }
}
