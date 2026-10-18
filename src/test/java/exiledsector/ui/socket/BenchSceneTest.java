package exiledsector.ui.socket;

import exiledsector.items.CraftingCurrency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchSceneTest {

    @Test
    void theArtScalesByWholePixelsAndFallsBackBelowDoubleSize() {
        assertEquals(4f, BenchScene.scaleFor(1300f, 740f));
        assertEquals(3f, BenchScene.scaleFor(1279f, 900f), "width limits to x3");
        assertEquals(3f, BenchScene.scaleFor(2000f, 719f), "height limits to x3");
        assertEquals(2f, BenchScene.scaleFor(640f, 360f));
        assertEquals(1.5f, BenchScene.scaleFor(480f, 300f), 1e-6f, "tiny areas fit exactly instead of snapping to x1");
    }

    @Test
    void thePanelWrapsTheArtInItsFrame() {
        float scale = SocketWorkbenchPanel.scaleFor(1300f + SocketWorkbenchPanel.FRAME_INSET * 2f, 740f + SocketWorkbenchPanel.FRAME_INSET * 2f);
        assertEquals(4f, scale);
        assertEquals(1280f + SocketWorkbenchPanel.FRAME_INSET * 2f, SocketWorkbenchPanel.sizeFor(scale, BenchScene.ART_WIDTH));
        assertEquals(720f + SocketWorkbenchPanel.FRAME_INSET * 2f, SocketWorkbenchPanel.sizeFor(scale, BenchScene.ART_HEIGHT));
    }

    @Test
    void storageSitsBesideTheBenchOnlyWhenTheBenchCanStillDrawAtTripleSize() {
        assertTrue(ModificationBay.fitsSideBySide(1840f, 1000f), "1920x1080 with margins");
        assertFalse(ModificationBay.fitsSideBySide(1286f, 688f), "1366x768 with margins uses the drawer");
    }

    @Test
    void tilesStayClearOfTheConsolesTheNoticeAndTheDetailsPlate() {
        float halfTile = BenchScene.TILE_SIZE / 2f;
        int tileCount = CraftingCurrency.values().length;
        float[][] centres = new float[tileCount][];
        for (int index = 0; index < tileCount; index++) {
            float angle = SocketWorkbenchPanel.tileAngle(index, tileCount);
            float centreX = BenchScene.SOCKET_X + BenchScene.ORBIT_RADIUS_X * (float) Math.cos(angle);
            float centreY = BenchScene.SOCKET_Y - BenchScene.ORBIT_RADIUS_Y * (float) Math.sin(angle);
            centres[index] = new float[]{centreX, centreY};
            assertTrue(centreY + halfTile < BenchScene.DETAILS_TOP, "tile " + index + " overlaps the details plate");
            assertTrue(centreY - halfTile > BenchScene.NOTICE_TOP + BenchScene.NOTICE_HEIGHT, "tile " + index + " overlaps the notice");
            assertTrue(centreX - halfTile > BenchScene.LEFT_CONSOLE_X + halfTile, "tile " + index + " overlaps the left console");
            assertTrue(centreX + halfTile < BenchScene.RIGHT_CONSOLE_X - halfTile, "tile " + index + " overlaps the right console");
        }
        for (int index = 1; index < tileCount; index++) {
            float gapX = Math.abs(centres[index][0] - centres[index - 1][0]);
            float gapY = Math.abs(centres[index][1] - centres[index - 1][1]);
            assertTrue(Math.max(gapX, gapY) >= BenchScene.TILE_SIZE, "tiles " + (index - 1) + " and " + index + " overlap");
        }
    }

    @Test
    void theDetailsPlateStartsJustUnderTheSocketGraphicAndFitsTheArt() {
        float socketBottom = BenchScene.SOCKET_Y + BenchScene.SLOT_RADIUS * 1.05f;
        assertTrue(BenchScene.DETAILS_TOP > socketBottom);
        assertTrue(BenchScene.DETAILS_TOP - socketBottom < 5f);
        assertTrue(BenchScene.DETAILS_TOP + BenchScene.DETAILS_HEIGHT <= BenchScene.ART_HEIGHT);
        assertTrue(BenchScene.DETAILS_LEFT - BenchScene.TILE_SIZE / 2f > BenchScene.LEFT_CONSOLE_X);
        assertTrue(BenchScene.DETAILS_LEFT + BenchScene.DETAILS_WIDTH + BenchScene.TILE_SIZE / 2f < BenchScene.RIGHT_CONSOLE_X);
    }
}
