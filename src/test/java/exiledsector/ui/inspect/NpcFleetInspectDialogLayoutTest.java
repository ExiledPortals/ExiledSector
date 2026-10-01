package exiledsector.ui.inspect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcFleetInspectDialogLayoutTest {

    private static final float EPSILON = 0.001f;

    @Test
    void oneColumnKeepsTheOriginalDialogWidth() {
        assertEquals(NpcFleetInspectDialog.COLUMN_WIDTH, NpcFleetInspectDialog.widthFor(1), EPSILON);
    }

    @Test
    void twoColumnsAreSeparatedByOneGap() {
        assertEquals(NpcFleetInspectDialog.COLUMN_WIDTH * 2f + NpcFleetInspectDialog.COLUMN_GAP,
                NpcFleetInspectDialog.widthFor(2), EPSILON);
    }

    @Test
    void twoColumnsOnlyWhenTheScreenFitsThemWithAMargin() {
        float needed = NpcFleetInspectDialog.widthFor(2) + NpcFleetInspectDialog.SCREEN_MARGIN;

        assertEquals(2, NpcFleetInspectDialog.columnsFor(1920f));
        assertEquals(2, NpcFleetInspectDialog.columnsFor(needed));
        assertEquals(1, NpcFleetInspectDialog.columnsFor(needed - 1f));
        assertEquals(1, NpcFleetInspectDialog.columnsFor(1366f));
    }
}
