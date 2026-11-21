package exiledsector.ui.socket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasAnomalyPanelTest {

    @Test
    void catalystsFillAtMostThreeRows() {
        for (int count = 1; count <= 40; count++) {
            int columns = AtlasAnomalyPanel.catalystColumns(count);
            int rows = (count + columns - 1) / columns;
            assertTrue(rows <= AtlasAnomalyPanel.CATALYST_ROWS, count + " catalysts need " + rows + " rows");
        }
    }

    @Test
    void thirteenCatalystsUseFiveColumns() {
        assertEquals(5, AtlasAnomalyPanel.catalystColumns(13));
        assertEquals(1, AtlasAnomalyPanel.catalystColumns(0));
    }

    @Test
    void fiveColumnsStillLeaveRoomForIcons() {
        float cellSize = SectionedTileGrid.cellSizeFor(AtlasAnomalyPanel.PANEL_WIDTH - HoloPanel.PAD * 2f, AtlasAnomalyPanel.catalystColumns(13));
        assertTrue(cellSize > 60f && cellSize < 72f, "cell size " + cellSize);
    }
}
