package exiledsector.charts.atlas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasCellTest {

    @Test
    void packingRoundTripsNegativeCoordinates() {
        for (AtlasCell cell : AtlasCell.ORIGIN.around(3)) {
            AtlasCell shifted = new AtlasCell(cell.gridX() * 100_000, cell.gridY() * -70_000);
            assertEquals(shifted, AtlasCell.unpack(shifted.pack()));
        }
    }

    @Test
    void aWindowHoldsItsSquare() {
        for (int radius = 0; radius <= 5; radius++) {
            assertEquals((2 * radius + 1) * (2 * radius + 1), new AtlasCell(-4, 9).around(radius).size());
        }
    }

    @Test
    void everyTwoByTwoBlockHasTwoCellsTheEmptySpaceRollSkips() {
        for (AtlasCell corner : AtlasCell.ORIGIN.around(6)) {
            int open = 0;
            for (int dx = 0; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    open += new AtlasCell(corner.gridX() + dx, corner.gridY() + dy).mayBeVoid() ? 0 : 1;
                }
            }
            assertEquals(2, open);
        }
        assertFalse(AtlasCell.ORIGIN.mayBeVoid());
        assertTrue(new AtlasCell(1, 0).mayBeVoid());
    }

    @Test
    void chunksFloorNegativeCoordinates() {
        assertEquals(-1, new AtlasCell(-1, 0).chunkX());
        assertEquals(0, new AtlasCell(AtlasCell.CHUNK_SIZE - 1, 0).chunkX());
        assertEquals(-1, new AtlasCell(0, -AtlasCell.CHUNK_SIZE).chunkY());
    }

    @Test
    void theNearestCellRoundsAMapPosition() {
        assertEquals(new AtlasCell(3, -2), AtlasCell.nearest(2.6f, -2.4f));
    }
}
