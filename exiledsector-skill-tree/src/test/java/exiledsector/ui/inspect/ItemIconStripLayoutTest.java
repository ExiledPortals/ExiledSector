package exiledsector.ui.inspect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemIconStripLayoutTest {

    @Test
    void aOneColumnDialogFitsNineIconsPerRow() {
        assertEquals(9, ItemIconStrip.cellsPerRow(700f));
        assertEquals(1, ItemIconStrip.cellsPerRow(10f));
    }

    @Test
    void moreIconsThanFitWrapOntoExtraRowsInsteadOfRunningPastTheDialog() {
        assertEquals(ItemIconStrip.CELL_SIZE, ItemIconStrip.height(9, 9));
        assertEquals(ItemIconStrip.CELL_SIZE * 2f + 8f, ItemIconStrip.height(10, 9));
        assertEquals(ItemIconStrip.CELL_SIZE * 3f + 16f, ItemIconStrip.height(19, 9));
        assertEquals(ItemIconStrip.CELL_SIZE, ItemIconStrip.height(0, 9));
    }
}
