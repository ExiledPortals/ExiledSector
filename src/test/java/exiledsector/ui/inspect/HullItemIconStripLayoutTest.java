package exiledsector.ui.inspect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HullItemIconStripLayoutTest {

    @Test
    void aOneColumnDialogFitsNineIconsPerRow() {
        assertEquals(9, HullItemIconStrip.cellsPerRow(700f));
        assertEquals(1, HullItemIconStrip.cellsPerRow(10f));
    }

    @Test
    void moreIconsThanFitWrapOntoExtraRowsInsteadOfRunningPastTheDialog() {
        assertEquals(HullItemIconStrip.CELL_SIZE, HullItemIconStrip.height(9, 9));
        assertEquals(HullItemIconStrip.CELL_SIZE * 2f + 8f, HullItemIconStrip.height(10, 9));
        assertEquals(HullItemIconStrip.CELL_SIZE * 3f + 16f, HullItemIconStrip.height(19, 9));
        assertEquals(HullItemIconStrip.CELL_SIZE, HullItemIconStrip.height(0, 9));
    }
}
