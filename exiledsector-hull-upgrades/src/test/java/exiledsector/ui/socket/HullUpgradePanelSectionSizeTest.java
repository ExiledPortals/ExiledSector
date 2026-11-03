package exiledsector.ui.socket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HullUpgradePanelSectionSizeTest {

    private static float rowsHeight(float tileSize, int rows) {
        return rows * (HullUpgradePanel.SECTION_ROW_GAP + tileSize + HullUpgradePanel.SECTION_NAME_HEIGHT + HullUpgradePanel.STATE_HEIGHT);
    }

    @Test
    void plentyOfRoomKeepsTilesAsWideAsTheirColumn() {
        assertEquals(120f, HullUpgradePanel.sectionTileSize(120f, 2000f, 3), 0.001f);
    }

    @Test
    void shortPanelsShrinkTilesSoEveryRowFitsWithoutScrolling() {
        float room = 450f;
        float tileSize = HullUpgradePanel.sectionTileSize(120f, room, 3);

        assertTrue(tileSize < 120f, "tile " + tileSize);
        assertTrue(rowsHeight(tileSize, 3) <= room, "rows " + rowsHeight(tileSize, 3) + " in " + room);
    }

    @Test
    void tilesNeverShrinkBelowTheirFloor() {
        assertEquals(HullUpgradePanel.MIN_SECTION_TILE_SIZE, HullUpgradePanel.sectionTileSize(120f, 100f, 3), 0.001f);
    }
}
