package exiledsector.ui.socket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedSearchLayoutTest {

    private static AdvancedSearchLayout paneIn(float areaWidth) {
        return AdvancedSearchLayout.in(new FreeArea(10f, 10f, areaWidth, 800f));
    }

    private static void assertRowFits(AdvancedSearchLayout layout) {
        AdvancedSearchLayout.RowColumns columns = layout.columns();
        assertTrue(columns.removeLeft() + AdvancedSearchLayout.REMOVE_BUTTON_WIDTH <= layout.rowWidth() + 0.001f,
                "row " + (columns.removeLeft() + AdvancedSearchLayout.REMOVE_BUTTON_WIDTH) + " of " + layout.rowWidth());
        assertTrue(columns.pickerWidth() > 0f && columns.boundWidth() > 0f);
        assertEquals(columns.pickerWidth() + HoloPanel.GAP, columns.clearLeft(), 0.001f, "the clear button sits right of the stat field");
        assertTrue(columns.clearLeft() + AdvancedSearchLayout.CLEAR_BUTTON_WIDTH <= layout.rowWidth() + 0.001f);
    }

    @Test
    void aOneLineRowPutsTheControlsRightOfThePicker() {
        AdvancedSearchLayout layout = paneIn(535f);
        AdvancedSearchLayout.RowColumns columns = layout.columns();

        assertFalse(layout.twoLineRows());
        assertRowFits(layout);
        assertEquals(columns.clearLeft() + AdvancedSearchLayout.CLEAR_BUTTON_WIDTH + HoloPanel.GAP, columns.minLeft(), 0.001f);
        assertEquals(0f, columns.controlsTop(), 0.001f);
        assertTrue(columns.pickerWidth() >= AdvancedSearchLayout.MIN_PICKER_WIDTH);
    }

    @Test
    void aTwoLineRowPutsThePickerOnTheFirstLineAndTheControlsUnderIt() {
        AdvancedSearchLayout layout = paneIn(215f);
        AdvancedSearchLayout.RowColumns columns = layout.columns();

        assertTrue(layout.twoLineRows());
        assertRowFits(layout);
        assertEquals(layout.rowWidth(), columns.clearLeft() + AdvancedSearchLayout.CLEAR_BUTTON_WIDTH, 0.001f);
        assertEquals(0f, columns.minLeft(), 0.001f);
        assertTrue(columns.controlsTop() >= AdvancedSearchLayout.CONTROL_HEIGHT);
        assertEquals(columns.controlsTop() + AdvancedSearchLayout.CONTROL_HEIGHT, columns.rowHeight(), 0.001f);
    }

    @Test
    void theOneLineWidthIsTheNarrowestPaneThatKeepsRowsOnOneLine() {
        assertFalse(paneIn(AdvancedSearchLayout.MIN_ONE_LINE_PANE_WIDTH).twoLineRows());
        assertTrue(paneIn(AdvancedSearchLayout.MIN_ONE_LINE_PANE_WIDTH - 1f).twoLineRows());
    }

    @Test
    void thePaneFillsTheWholeFreeArea() {
        AdvancedSearchLayout layout = paneIn(2000f);

        assertEquals(2000f, layout.paneWidth(), 0.001f);
        assertEquals(10f, layout.paneLeft(), 0.001f);
        assertEquals(800f, layout.paneHeight(), 0.001f);
        assertEquals(10f, layout.paneTop(), 0.001f);
    }
}
