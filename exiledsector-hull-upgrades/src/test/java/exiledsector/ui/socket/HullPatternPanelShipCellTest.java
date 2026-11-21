package exiledsector.ui.socket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HullPatternPanelShipCellTest {

    @Test
    void fewShipsGrowTheirCellsUpToTheTallestShape() {
        assertEquals(250f, HullPatternPanel.shipCellHeight(200f, 2000f, 1), 0.001f);
    }

    @Test
    void someShipsShrinkTheirCellsSoEveryRowFitsTheGrid() {
        float gridHeight = 600f;
        float cellHeight = HullPatternPanel.shipCellHeight(200f, gridHeight, 4);

        assertTrue(cellHeight < 250f, "cell " + cellHeight);
        assertTrue(HullPatternPanel.rowsHeight(cellHeight, 4) <= gridHeight, "rows " + HullPatternPanel.rowsHeight(cellHeight, 4));
    }

    @Test
    void manyShipsKeepTheirFloorHeightAndScrollTheGrid() {
        float gridHeight = 600f;
        float cellHeight = HullPatternPanel.shipCellHeight(200f, gridHeight, 10);

        assertEquals(110f, cellHeight, 0.001f);
        assertTrue(HullPatternPanel.rowsHeight(cellHeight, 10) > gridHeight);
    }

    @Test
    void theGridAlwaysShowsAtLeastTwoRowsOfShips() {
        assertEquals(HullPatternPanel.rowsHeight(110f, 2), HullPatternPanel.shipGridHeight(50f), 0.001f);
        assertEquals(400f, HullPatternPanel.shipGridHeight(400f), 0.001f);
    }
}
