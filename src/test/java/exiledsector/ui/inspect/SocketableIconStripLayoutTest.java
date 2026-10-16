package exiledsector.ui.inspect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SocketableIconStripLayoutTest {

    @Test
    void aOneColumnDialogFitsNineIconsPerRow() {
        assertEquals(9, SocketableIconStrip.cellsPerRow(700f));
        assertEquals(1, SocketableIconStrip.cellsPerRow(10f));
    }

    @Test
    void moreIconsThanFitWrapOntoExtraRowsInsteadOfRunningPastTheDialog() {
        assertEquals(SocketableIconStrip.CELL_SIZE, SocketableIconStrip.height(9, 9));
        assertEquals(SocketableIconStrip.CELL_SIZE * 2f + 8f, SocketableIconStrip.height(10, 9));
        assertEquals(SocketableIconStrip.CELL_SIZE * 3f + 16f, SocketableIconStrip.height(19, 9));
        assertEquals(SocketableIconStrip.CELL_SIZE, SocketableIconStrip.height(0, 9));
    }
}
